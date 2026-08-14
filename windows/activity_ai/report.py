from __future__ import annotations
from datetime import datetime, timedelta
from pathlib import Path
from .storage import EventStore

def _fmt(ms: int) -> str:
    minutes = max(0, int(ms // 60_000))
    return f"{minutes//60}h {minutes%60}m"

def _pct(current: int, previous: int) -> str:
    if previous <= 0:
        return "new activity" if current > 0 else "no change"
    pct = round((current - previous) * 100 / previous)
    return f"{pct:+d}%" if pct else "about the same"

def _range_report(store: EventStore, start: datetime, end: datetime, title: str) -> str:
    start_ms, end_ms = int(start.timestamp()*1000), int(end.timestamp()*1000)
    rows = store.totals_by_app(start_ms, end_ms)
    total = sum(ms or 0 for _, ms in rows)
    sessions = store.session_count(start_ms, end_ms)
    average = store.average_session_ms(start_ms, end_ms)
    idle = store.idle_total_ms(start_ms, end_ms)
    lines = [title,f"Tracked foreground time: {_fmt(total)}",f"Foreground sessions: {sessions}",f"Average session: {_fmt(average)}",f"Detected idle time: {_fmt(idle)}",""]
    for name, ms in rows:
        lines.append(f"{name}: {_fmt(ms)}")
    return "\n".join(lines)

def daily_report(db: Path, when: datetime | None = None) -> str:
    when = when or datetime.now()
    start = when.replace(hour=0, minute=0, second=0, microsecond=0)
    end = start + timedelta(days=1)
    store = EventStore(db)
    current_ms = sum(ms for _, ms in store.totals_by_app(int(start.timestamp()*1000), int(end.timestamp()*1000)))
    prev_start, prev_end = start - timedelta(days=1), start
    previous_ms = sum(ms for _, ms in store.totals_by_app(int(prev_start.timestamp()*1000), int(prev_end.timestamp()*1000)))
    body = _range_report(store, start, end, f"Activity AI Daily Report — {start.date()}")
    return body + f"\n\nForeground time vs previous day: {_pct(current_ms, previous_ms)}"

def weekly_report(db: Path, when: datetime | None = None) -> str:
    when = when or datetime.now()
    day = when.replace(hour=0, minute=0, second=0, microsecond=0)
    start = day - timedelta(days=day.weekday())
    end = start + timedelta(days=7)
    return _range_report(EventStore(db), start, end, f"Activity AI Weekly Report — {start.date()} to {(end-timedelta(days=1)).date()}")
