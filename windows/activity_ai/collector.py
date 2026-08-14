from __future__ import annotations
import argparse
import hashlib
import os
import socket
import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from .models import ActivityEvent
from .storage import EventStore

if os.name == "nt":
    from .win32 import foreground_window, idle_seconds, app_name
else:
    foreground_window = idle_seconds = app_name = None

@dataclass(slots=True)
class CurrentSession:
    app_id: str | None
    app_name: str | None
    window_title: str | None
    started_at_ms: int


def device_id() -> str:
    raw = f"{socket.gethostname()}|{os.environ.get('USERNAME','')}".encode()
    return "win-" + hashlib.sha256(raw).hexdigest()[:16]


def now_ms() -> int:
    return int(time.time() * 1000)


def _close_session(store: EventStore, did: str, current: CurrentSession | None, ts: int) -> None:
    if current is None:
        return
    duration = max(0, ts - current.started_at_ms)
    if duration == 0:
        return
    store.insert(ActivityEvent(
        event_id=str(uuid.uuid4()), device_id=did, platform="windows",
        event_type="session", app_id=current.app_id, app_name=current.app_name,
        window_title=current.window_title, started_at_ms=current.started_at_ms,
        ended_at_ms=ts, duration_ms=duration, source="windows_foreground"
    ))


def run(db_path: Path, poll_seconds: float = 2.0, idle_threshold: int = 300) -> None:
    if os.name != "nt":
        raise SystemExit("Windows collector must run on Windows.")
    store = EventStore(db_path)
    did = device_id()
    current: CurrentSession | None = None
    was_idle = False
    idle_started_at: int | None = None
    print(f"Activity AI collector running. device={did} db={db_path}")
    try:
        while True:
            ts = now_ms()
            idle = idle_seconds() >= idle_threshold

            if idle and not was_idle:
                _close_session(store, did, current, ts)
                current = None
                idle_started_at = ts
                store.insert(ActivityEvent(
                    event_id=str(uuid.uuid4()), device_id=did, platform="windows",
                    event_type="idle_start", started_at_ms=ts, source="windows_idle"
                ))
            elif not idle and was_idle:
                store.insert(ActivityEvent(
                    event_id=str(uuid.uuid4()), device_id=did, platform="windows",
                    event_type="idle_end", started_at_ms=ts,
                    ended_at_ms=ts,
                    duration_ms=max(0, ts - idle_started_at) if idle_started_at else None,
                    source="windows_idle"
                ))
                idle_started_at = None

            was_idle = idle
            if idle:
                time.sleep(poll_seconds)
                continue

            fg = foreground_window()
            process_path = fg.process_path if fg else None
            aid = process_path.lower() if process_path else None
            aname = app_name(process_path) if fg else None
            title = fg.title if fg else None

            if aid is not None:
                policy = store.get_policy(aid)
                if not policy["telemetry"]:
                    aid = aname = title = None
                elif not policy["metadata"]:
                    title = None

            changed = current is None or current.app_id != aid or current.window_title != title
            if changed:
                _close_session(store, did, current, ts)
                current = CurrentSession(aid, aname, title, ts) if aid is not None else None
            time.sleep(poll_seconds)
    except KeyboardInterrupt:
        ts = now_ms()
        _close_session(store, did, current, ts)
        print("Collector stopped cleanly.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Activity AI Windows foreground-app collector")
    parser.add_argument("--db", type=Path, default=Path.home()/".activity-ai"/"activity.db")
    parser.add_argument("--poll", type=float, default=2.0)
    parser.add_argument("--idle", type=int, default=300, help="idle threshold in seconds")
    args = parser.parse_args()
    run(args.db, args.poll, args.idle)

if __name__ == "__main__":
    main()
