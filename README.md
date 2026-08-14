# Activity AI — Milestone 1 Foundation (Pass 3)

Cross-device personal activity intelligence foundation.

## Implemented

### Shared
- Normalized Activity Event JSON schema shared by Android and Windows.
- Privacy level carried on every event.
- Local-first design; no cloud upload or synchronization yet.

### Android
- Level-1 `UsageStatsManager` collection.
- Single-foreground state normalization to avoid overlapping app sessions.
- Screen-off, keyguard-shown, and device-shutdown events terminate active foreground sessions.
- 24-hour lookback reconstructs state across requested report boundaries.
- Completed sessions are clipped to the requested collection interval and use deterministic IDs to suppress duplicate imports.
- SQLite event and per-app privacy-policy storage.
- Dashboard with current foreground totals, completed-session count, average session duration, previous-day comparison, and 7-day daily average.
- Persistent `JobScheduler` collection approximately every 15 minutes, subject to Android background scheduling.
- Collector rescheduling after boot and app replacement.
- Telemetry opt-out enforcement before event persistence.

### Windows
- Foreground process/window collection using Win32 APIs.
- SQLite event and privacy-policy storage.
- Idle detection closes foreground sessions so idle time is not misclassified as active-app time.
- Report calculations clip sessions crossing day/week boundaries instead of dropping or over-counting them.
- Daily and weekly reports include foreground time, session count, average session duration, idle time, and per-app totals.
- Window titles are treated as Level-2 metadata and are stripped by default unless metadata permission is explicitly enabled for that app.
- Per-app telemetry opt-out enforcement before persistence.

## Explicitly not in Milestone 1

- Location.
- Accessibility/UI semantic observation.
- Screen capture / visual observation.
- Snapchat-specific inspection.
- Cross-device network synchronization.
- Cloud upload.
- Keystroke logging or message-content capture.

## Validation performed in this environment

- 5 Windows storage/report/privacy/boundary tests pass.
- Windows Python modules compile successfully.
- Shared JSON schema parses successfully.
- Android state-normalization source checks pass.
- Android source/manifest/layout consistency checks pass.

The Android app has not yet been compiled into an APK in this environment because the Android SDK/Gradle toolchain is not installed here.


## Milestone 1 — Pass 4
- Android DB version 3 migration/index hardening
- Android collector health/SQLite quick-check surfaced on dashboard
- Privacy-safe Android diagnostics exporter (health only; no event/content export)
- Windows database health checker
- Windows resilient PowerShell launcher with bounded crash-loop protection
- Additional health regression tests
