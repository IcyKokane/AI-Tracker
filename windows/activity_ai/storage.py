from __future__ import annotations
import sqlite3
from pathlib import Path
from .models import ActivityEvent

SCHEMA = """
CREATE TABLE IF NOT EXISTS events (
  event_id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  platform TEXT NOT NULL,
  event_type TEXT NOT NULL,
  app_id TEXT,
  app_name TEXT,
  window_title TEXT,
  activity_label TEXT,
  project_label TEXT,
  started_at_ms INTEGER NOT NULL,
  ended_at_ms INTEGER,
  duration_ms INTEGER,
  source TEXT NOT NULL,
  confidence REAL NOT NULL,
  privacy_level TEXT NOT NULL,
  metadata_json TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_events_started ON events(started_at_ms);
CREATE INDEX IF NOT EXISTS idx_events_app ON events(app_id, started_at_ms);
CREATE TABLE IF NOT EXISTS privacy_policy (
  app_id TEXT PRIMARY KEY, telemetry INTEGER NOT NULL DEFAULT 1, metadata INTEGER NOT NULL DEFAULT 0,
  visual INTEGER NOT NULL DEFAULT 0, content_storage INTEGER NOT NULL DEFAULT 0
);
"""

class EventStore:
    def __init__(self, path: str | Path):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.conn = sqlite3.connect(self.path)
        self.conn.executescript(SCHEMA)

    def insert(self, event: ActivityEvent) -> None:
        import json
        self.conn.execute(
            """INSERT OR REPLACE INTO events VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (
                event.event_id, event.device_id, event.platform, event.event_type,
                event.app_id, event.app_name, event.window_title, event.activity_label,
                event.project_label, event.started_at_ms, event.ended_at_ms,
                event.duration_ms, event.source, event.confidence, event.privacy_level,
                json.dumps(event.metadata or {}, ensure_ascii=False)
            )
        )
        self.conn.commit()

    def totals_by_app(self, start_ms: int, end_ms: int) -> list[tuple[str, int]]:
        """Return foreground totals clipped to the report interval, including crossing sessions."""
        return self.conn.execute(
            """SELECT COALESCE(app_name, app_id, 'Unknown'),
                      SUM(MAX(0, MIN(COALESCE(ended_at_ms, ?), ?) - MAX(started_at_ms, ?))) AS overlap_ms
               FROM events
               WHERE event_type='session' AND started_at_ms<? AND COALESCE(ended_at_ms, ?) > ?
               GROUP BY COALESCE(app_name, app_id, 'Unknown')
               ORDER BY overlap_ms DESC""",
            (end_ms, end_ms, start_ms, end_ms, end_ms, start_ms)
        ).fetchall()

    def session_count(self, start_ms: int, end_ms: int) -> int:
        return int(self.conn.execute(
            """SELECT COUNT(*) FROM events
               WHERE event_type='session' AND started_at_ms<? AND COALESCE(ended_at_ms, ?) > ?""",
            (end_ms, end_ms, start_ms)
        ).fetchone()[0])

    def average_session_ms(self, start_ms: int, end_ms: int) -> int:
        row = self.conn.execute(
            """SELECT COALESCE(AVG(MAX(0, MIN(COALESCE(ended_at_ms, ?), ?) - MAX(started_at_ms, ?))),0)
               FROM events WHERE event_type='session'
               AND started_at_ms<? AND COALESCE(ended_at_ms, ?) > ?""",
            (end_ms, end_ms, start_ms, end_ms, end_ms, start_ms)
        ).fetchone()
        return int(row[0] or 0)

    def idle_total_ms(self, start_ms: int, end_ms: int) -> int:
        row = self.conn.execute(
            """SELECT COALESCE(SUM(MAX(0, MIN(COALESCE(ended_at_ms, ?), ?) - MAX(started_at_ms, ?))),0)
               FROM events WHERE event_type='idle_end'
               AND started_at_ms<? AND COALESCE(ended_at_ms, ?) > ?""",
            (end_ms, end_ms, start_ms, end_ms, end_ms, start_ms)
        ).fetchone()
        return int(row[0] or 0)

    def get_policy(self, app_id: str) -> dict[str, bool]:
        row = self.conn.execute(
            "SELECT telemetry,metadata,visual,content_storage FROM privacy_policy WHERE app_id=?", (app_id,)
        ).fetchone()
        if row is None:
            return {"telemetry": True, "metadata": False, "visual": False, "content_storage": False}
        return dict(zip(("telemetry","metadata","visual","content_storage"), map(bool, row)))

    def set_policy(self, app_id: str, *, telemetry=True, metadata=False, visual=False, content_storage=False) -> None:
        self.conn.execute(
            "INSERT OR REPLACE INTO privacy_policy VALUES (?,?,?,?,?)",
            (app_id, int(telemetry), int(metadata), int(visual), int(content_storage))
        )
        self.conn.commit()
