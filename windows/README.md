# Activity AI Windows — Milestone 1 Pass 3

Run from the `windows` directory:

```powershell
python run_collector.py
```

Show the current daily and weekly reports:

```powershell
python show_today.py
```

The collector defaults to a 5-minute idle threshold and 2-second foreground polling. Window titles are Level-2 metadata: they are not stored under the default per-app policy.

Database default: `%USERPROFILE%\\.activity-ai\\activity.db`
