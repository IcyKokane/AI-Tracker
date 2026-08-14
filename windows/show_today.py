from pathlib import Path
from activity_ai.report import daily_report, weekly_report

DB = Path.home()/".activity-ai"/"activity.db"
print(daily_report(DB))
print("\n" + "="*64 + "\n")
print(weekly_report(DB))
