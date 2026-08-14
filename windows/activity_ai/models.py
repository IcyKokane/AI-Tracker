from __future__ import annotations
from dataclasses import dataclass, asdict
from typing import Any
import json

@dataclass(slots=True)
class ActivityEvent:
    event_id: str
    device_id: str
    platform: str
    event_type: str
    started_at_ms: int
    source: str
    privacy_level: str = "telemetry"
    app_id: str | None = None
    app_name: str | None = None
    window_title: str | None = None
    activity_label: str | None = None
    project_label: str | None = None
    ended_at_ms: int | None = None
    duration_ms: int | None = None
    confidence: float = 1.0
    metadata: dict[str, Any] | None = None

    def to_json(self) -> str:
        payload = asdict(self)
        if payload["metadata"] is None:
            payload["metadata"] = {}
        return json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
