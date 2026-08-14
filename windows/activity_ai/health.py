from __future__ import annotations
import sqlite3
from pathlib import Path
from dataclasses import dataclass

@dataclass(frozen=True)
class Health:
    database_state: str
    event_count: int
    last_event_ms: int

def inspect_database(path: str | Path) -> Health:
    path = Path(path)
    if not path.exists():
        return Health("missing", 0, 0)
    try:
        conn = sqlite3.connect(path)
        state = conn.execute("PRAGMA quick_check").fetchone()[0]
        count, last = conn.execute(
            "SELECT COUNT(*), COALESCE(MAX(COALESCE(ended_at_ms,started_at_ms)),0) FROM events"
        ).fetchone()
        conn.close()
        return Health(str(state), int(count), int(last))
    except Exception as exc:
        return Health(f"error:{type(exc).__name__}", 0, 0)
