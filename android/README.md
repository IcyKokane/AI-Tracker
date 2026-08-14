# Activity AI Android — Milestone 1 Pass 3

## Current behavior

1. Launch the app and grant **Usage Access** from Android settings.
2. The dashboard reads foreground usage events and stores completed normalized sessions locally.
3. Background collection is scheduled through `JobScheduler` and restored after boot/app replacement.
4. Screen-off, keyguard, and shutdown usage events close active sessions so locked-device time is not counted as foreground use.
5. Per-app telemetry defaults ON; Level-2 metadata, Level-3 visual observation, and raw content storage default OFF.

The app currently requests only Usage Stats access plus boot-completed delivery. Location, Accessibility, and MediaProjection are intentionally not part of Milestone 1.
