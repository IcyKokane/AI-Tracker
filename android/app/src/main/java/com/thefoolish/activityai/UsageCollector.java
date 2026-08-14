package com.thefoolish.activityai;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UsageCollector {
    private static final long STATE_LOOKBACK_MS = 24L * 60L * 60L * 1000L;

    public static final class Total {
        public final String packageName;
        public final String appName;
        public final long durationMs;
        public Total(String packageName, String appName, long durationMs) {
            this.packageName = packageName; this.appName = appName; this.durationMs = durationMs;
        }
    }

    private final Context context;
    private final EventDatabase db;
    private final UsageStatsManager usage;
    private final PrivacyPolicyStore policies;
    private final String deviceId;

    public UsageCollector(Context context) {
        this.context = context.getApplicationContext();
        this.db = new EventDatabase(this.context);
        this.usage = (UsageStatsManager) this.context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.policies = new PrivacyPolicyStore(this.context);
        this.deviceId = buildDeviceId(this.context);
    }

    public List<Total> collectRange(long startMs, long endMs) {
        if (endMs <= startMs) return new ArrayList<>();
        long queryStart = Math.max(0L, startMs - STATE_LOOKBACK_MS);
        UsageEvents events = usage.queryEvents(queryStart, endMs);
        UsageEvents.Event e = new UsageEvents.Event();
        Map<String, Long> totals = new HashMap<>();
        SQLiteDatabase out = db.getWritableDatabase();

        String foregroundPkg = null;
        long foregroundStart = -1L;
        boolean interactive = true;
        boolean keyguardShown = false;

        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(e);
            long ts = e.getTimeStamp();
            int type = e.getEventType();

            if (type == UsageEvents.Event.SCREEN_NON_INTERACTIVE ||
                    type == UsageEvents.Event.KEYGUARD_SHOWN ||
                    type == UsageEvents.Event.DEVICE_SHUTDOWN) {
                if (foregroundPkg != null) {
                    close(foregroundPkg, foregroundStart, ts, startMs, endMs, totals, out);
                    foregroundPkg = null;
                    foregroundStart = -1L;
                }
                if (type == UsageEvents.Event.SCREEN_NON_INTERACTIVE) interactive = false;
                if (type == UsageEvents.Event.KEYGUARD_SHOWN) keyguardShown = true;
                continue;
            }
            if (type == UsageEvents.Event.SCREEN_INTERACTIVE) {
                interactive = true;
                continue;
            }
            if (type == UsageEvents.Event.KEYGUARD_HIDDEN) {
                keyguardShown = false;
                continue;
            }

            String pkg = e.getPackageName();
            if (pkg == null) continue;

            if (type == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (foregroundPkg != null && !foregroundPkg.equals(pkg)) {
                    close(foregroundPkg, foregroundStart, ts, startMs, endMs, totals, out);
                    foregroundPkg = null;
                    foregroundStart = -1L;
                }
                if (interactive && !keyguardShown && policies.get(pkg).telemetry) {
                    if (!pkg.equals(foregroundPkg)) {
                        foregroundPkg = pkg;
                        foregroundStart = ts;
                    }
                }
            } else if ((type == UsageEvents.Event.ACTIVITY_PAUSED || type == UsageEvents.Event.ACTIVITY_STOPPED)
                    && pkg.equals(foregroundPkg)) {
                close(foregroundPkg, foregroundStart, ts, startMs, endMs, totals, out);
                foregroundPkg = null;
                foregroundStart = -1L;
            }
        }

        long stop = Math.min(System.currentTimeMillis(), endMs);
        if (foregroundPkg != null && stop > foregroundStart) {
            long clippedStart = Math.max(startMs, foregroundStart);
            if (stop > clippedStart) {
                totals.put(foregroundPkg, totals.getOrDefault(foregroundPkg, 0L) + (stop - clippedStart));
            }
        }

        List<Total> result = new ArrayList<>();
        for (Map.Entry<String, Long> row : totals.entrySet()) {
            result.add(new Total(row.getKey(), appLabel(row.getKey()), row.getValue()));
        }
        result.sort((a,b) -> Long.compare(b.durationMs, a.durationMs));
        return result;
    }

    private void close(String pkg, long began, long ended, long rangeStart, long rangeEnd,
                       Map<String, Long> totals, SQLiteDatabase out) {
        if (pkg == null || began < 0 || ended <= began) return;
        long start = Math.max(began, rangeStart);
        long end = Math.min(ended, rangeEnd);
        if (end <= start) return;
        long duration = end - start;
        totals.put(pkg, totals.getOrDefault(pkg, 0L) + duration);
        insertSession(out, pkg, start, end, duration);
    }

    private void insertSession(SQLiteDatabase out, String pkg, long start, long end, long duration) {
        ContentValues v = new ContentValues();
        v.put("event_id", sessionId(pkg, start, end));
        v.put("device_id", deviceId);
        v.put("platform", "android");
        v.put("event_type", "session");
        v.put("app_id", pkg);
        v.put("app_name", appLabel(pkg));
        v.putNull("window_title"); v.putNull("activity_label"); v.putNull("project_label");
        v.put("started_at_ms", start); v.put("ended_at_ms", end); v.put("duration_ms", duration);
        v.put("source", "usage_stats"); v.put("confidence", 1.0); v.put("privacy_level", "telemetry");
        v.put("metadata_json", "{}");
        out.insertWithOnConflict("events", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private String sessionId(String pkg, long start, long end) {
        try {
            String raw = deviceId + "|" + pkg + "|" + start + "|" + end;
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder("evt-");
            for (int i=0; i<16; i++) b.append(String.format(Locale.US, "%02x", d[i]));
            return b.toString();
        } catch (Exception e) { return deviceId + "-" + start + "-" + end; }
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception ignored) { return pkg; }
    }

    private static String buildDeviceId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest((androidId == null ? "unknown" : androidId).getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder("and-");
            for (int i=0; i<8; i++) b.append(String.format(Locale.US, "%02x", d[i]));
            return b.toString();
        } catch (Exception e) { return "and-unknown"; }
    }
}
