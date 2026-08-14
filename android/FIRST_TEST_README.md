# Activity AI — Milestone 1 First Device Test

This build is intentionally Level-1 only.

## What it should do
- Request Android Usage Access.
- Schedule background usage collection.
- Persist normalized foreground sessions locally.
- Show today's foreground time and top apps.
- Show session count and average session length.
- Show previous-day comparison and seven-day average.
- Surface collector database health.

## What it intentionally does NOT do yet
- No location collection.
- No Accessibility Service.
- No screen capture.
- No Snapchat visual observation.
- No message/content collection.

## Build
On Windows with Android Studio/SDK installed:

```powershell
.\BUILD_FIRST_APK.ps1
```

Expected output:

`ActivityAI-M1-FirstTest.apk`

To build and install over USB:

```powershell
.\BUILD_FIRST_APK.ps1 -Install
```

## First phone test
1. Install the APK.
2. Open Activity AI.
3. Tap the Usage Access button and grant access.
4. Return to Activity AI.
5. Use several different apps normally for at least a few minutes.
6. Reopen Activity AI and verify the top-app totals update.
7. Lock/unlock the phone once, then verify tracking resumes.
8. Reboot once, then verify Activity AI still reports background collection scheduled.

Do not enable battery restrictions testing yet; battery-optimization handling belongs to the next hardening pass.
