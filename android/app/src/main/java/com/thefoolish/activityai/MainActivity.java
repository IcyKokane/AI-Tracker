package com.thefoolish.activityai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public final class MainActivity extends Activity {
    private static final long MIN_TOP_APP_MS = 60_000L;

    private TextView status;
    private TextView summary;
    private TextView report;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        summary = findViewById(R.id.summary);
        report = findViewById(R.id.report);
        Button grant = findViewById(R.id.grantUsage);
        Button refresh = findViewById(R.id.refresh);
        grant.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        refresh.setOnClickListener(v -> refresh());
        CollectionScheduler.ensureScheduled(this);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        boolean allowed = UsageAccess.granted(this);
        status.setText(allowed ? "Usage access: ACTIVE • background collection scheduled" : "Usage access: REQUIRED");
        if (!allowed) {
            summary.setText("Milestone 1 telemetry is ready but cannot collect until Usage Access is granted.");
            report.setText("");
            return;
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now();
        long start = today.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = System.currentTimeMillis();
        long previousStart = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long previousEnd = start;
        long sevenDayStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli();

        UsageCollector collector = new UsageCollector(this);
        // Rebuild/maintain a deterministic recent history first. Because session IDs now use
        // raw Android event boundaries, this pass is safe to repeat and does not duplicate rows.
        collector.collectRange(sevenDayStart, end);
        List<UsageCollector.Total> totals = collector.collectRange(start, end);

        DashboardStats statsReader = new DashboardStats(this);
        DashboardStats.Snapshot stored = statsReader.between(start, end);
        DashboardStats.Snapshot previous = statsReader.between(previousStart, previousEnd);
        DashboardStats.Snapshot sevenDays = statsReader.between(sevenDayStart, end);
        CollectorHealth.Snapshot health = new CollectorHealth(this).read();

        long liveTotal = 0;
        int meaningfulApps = 0;
        for (UsageCollector.Total t : totals) {
            liveTotal += t.durationMs;
            if (t.durationMs >= MIN_TOP_APP_MS) meaningfulApps++;
        }

        String historicalLine;
        if (!stored.sane || !previous.sane || !sevenDays.sane) {
            historicalLine = "Historical stats: repairing invalid overlap data";
        } else {
            String delta = comparison(stored.trackedMs, previous.trackedMs);
            historicalLine = "Stored time vs yesterday: " + delta +
                    " • 7-day daily avg: " + format(sevenDays.trackedMs / 7L);
        }

        summary.setText("Today: " + format(liveTotal) + " foreground • " + meaningfulApps +
                " active apps\nCompleted sessions: " + (stored.sane ? stored.sessions : 0) +
                " • average " + (stored.sane ? format(stored.averageSessionMs) : "repairing") +
                "\n" + historicalLine +
                "\nCollector DB: " + health.databaseState + " • stored events: " + health.eventCount);

        StringBuilder b = new StringBuilder("Top apps today\n\n");
        int shown = 0;
        for (UsageCollector.Total t : totals) {
            if (t.durationMs < MIN_TOP_APP_MS) continue;
            if (shown++ >= 20) break;
            b.append(t.appName).append(" — ").append(format(t.durationMs)).append('\n');
        }
        if (shown == 0) b.append("No apps have reached one minute of foreground use yet.");
        report.setText(b.toString());
    }

    private static String comparison(long current, long previous) {
        if (previous <= 0) return current <= 0 ? "no change" : "new activity";
        long pct = Math.round(((current - previous) * 100.0) / previous);
        if (pct == 0) return "about the same";
        return (pct > 0 ? "+" : "") + pct + "%";
    }

    private static String format(long ms) {
        long min = Math.max(0L, ms) / 60000L;
        return (min / 60L) + "h " + (min % 60L) + "m";
    }
}
