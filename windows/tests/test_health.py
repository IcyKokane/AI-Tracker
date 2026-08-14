import sqlite3
from pathlib import Path
from activity_ai.health import inspect_database
from activity_ai.storage import EventStore
from activity_ai.models import ActivityEvent

def test_missing_database(tmp_path):
    h = inspect_database(tmp_path / "none.db")
    assert h.database_state == "missing"
    assert h.event_count == 0

def test_database_health(tmp_path):
    p = tmp_path / "db.sqlite"
    store = EventStore(p)
    e = ActivityEvent(
        event_id="h1", device_id="pc", platform="windows", event_type="session",
        app_id="x.exe", app_name="X", window_title=None, activity_label=None,
        project_label=None, started_at_ms=100, ended_at_ms=200, duration_ms=100,
        source="foreground_window", confidence=1.0, privacy_level="telemetry",
        metadata={}
    )
    store.insert(e)
    store.conn.close()
    h = inspect_database(p)
    assert h.database_state == "ok"
    assert h.event_count == 1
    assert h.last_event_ms == 200
