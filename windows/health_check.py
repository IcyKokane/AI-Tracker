from pathlib import Path
from activity_ai.health import inspect_database

db = Path.home() / ".activity_ai" / "activity_ai.db"
h = inspect_database(db)
print(f"Database: {h.database_state}")
print(f"Stored events: {h.event_count}")
print(f"Last event (ms): {h.last_event_ms}")
