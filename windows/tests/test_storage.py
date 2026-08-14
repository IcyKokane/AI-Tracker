import tempfile
import unittest
from datetime import datetime
from pathlib import Path
from activity_ai.models import ActivityEvent
from activity_ai.storage import EventStore
from activity_ai.report import daily_report, weekly_report

class StoreTests(unittest.TestCase):
    def test_totals_and_session_count(self):
        with tempfile.TemporaryDirectory() as td:
            s = EventStore(Path(td)/"a.db")
            s.insert(ActivityEvent("event-0001", "dev1", "windows", "session", 1000, "windows_foreground", app_name="Code", duration_ms=60000, ended_at_ms=61000))
            self.assertEqual(s.totals_by_app(0, 100000)[0], ("Code", 60000))
            self.assertEqual(s.session_count(0, 100000), 1)
            self.assertEqual(s.average_session_ms(0, 100000), 60000)

    def test_cross_boundary_session_is_clipped(self):
        with tempfile.TemporaryDirectory() as td:
            s = EventStore(Path(td)/"a.db")
            s.insert(ActivityEvent("cross", "dev1", "windows", "session", 500, "windows_foreground", app_name="Code", duration_ms=1500, ended_at_ms=2000))
            self.assertEqual(s.totals_by_app(1000, 1500)[0], ("Code", 500))
            self.assertEqual(s.average_session_ms(1000, 1500), 500)

    def test_privacy_default_and_override(self):
        with tempfile.TemporaryDirectory() as td:
            s = EventStore(Path(td)/"a.db")
            self.assertEqual(s.get_policy("snapchat.exe"), {"telemetry": True, "metadata": False, "visual": False, "content_storage": False})
            s.set_policy("secret.exe", telemetry=False)
            self.assertFalse(s.get_policy("secret.exe")["telemetry"])

    def test_idle_total(self):
        with tempfile.TemporaryDirectory() as td:
            s = EventStore(Path(td)/"a.db")
            s.insert(ActivityEvent("idle-1", "dev1", "windows", "idle_end", 1000, "windows_idle", ended_at_ms=61000, duration_ms=60000))
            self.assertEqual(s.idle_total_ms(0, 100000), 60000)
            self.assertEqual(s.idle_total_ms(31000, 41000), 10000)

    def test_reports_render(self):
        with tempfile.TemporaryDirectory() as td:
            db = Path(td)/"a.db"
            s = EventStore(db)
            ts = int(datetime(2026, 8, 14, 12, 0).timestamp()*1000)
            s.insert(ActivityEvent("r1", "dev1", "windows", "session", ts, "windows_foreground", app_name="Code", ended_at_ms=ts+60000, duration_ms=60000))
            self.assertIn("Average session", daily_report(db, datetime(2026,8,14,13,0)))
            self.assertIn("Weekly Report", weekly_report(db, datetime(2026,8,14,13,0)))

if __name__ == '__main__': unittest.main()
