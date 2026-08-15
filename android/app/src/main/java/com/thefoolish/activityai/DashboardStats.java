package com.thefoolish.activityai;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class DashboardStats {
    private static final long MAX_REASONABLE_SESSION_MS = 24L * 60L * 60L * 1000L;

    public static final class Snapshot {
        public final int sessions;
        public final int apps;
        public final long trackedMs;
        public final long averageSessionMs;
        public final boolean sane;
        Snapshot(int sessions, int apps, long trackedMs, long averageSessionMs, boolean sane) {
            this.sessions = sessions; this.apps = apps; this.trackedMs = trackedMs;
            this.averageSessionMs = averageSessionMs; this.sane = sane;
        }
    }

    private final EventDatabase db;
    public DashboardStats(Context context) { this.db = new EventDatabase(context.getApplicationContext()); }

    public Snapshot between(long startMs, long endMs) {
        SQLiteDatabase r = db.getReadableDatabase();
        String overlap = "MAX(0, MIN(ended_at_ms, ?) - MAX(started_at_ms, ?))";
        String validSession = "platform='android' AND source='usage_stats' AND event_type='session' " +
                "AND ended_at_ms IS NOT NULL AND duration_ms>0 AND duration_ms<=?";
        String sql = "SELECT COUNT(*), COUNT(DISTINCT app_id), COALESCE(SUM(" + overlap + "),0), " +
                "COALESCE(AVG(" + overlap + "),0) FROM events WHERE " + validSession + " " +
                "AND started_at_ms<? AND ended_at_ms>?";
        String[] args = new String[]{
                Long.toString(endMs), Long.toString(startMs),
                Long.toString(endMs), Long.toString(startMs),
                Long.toString(MAX_REASONABLE_SESSION_MS),
                Long.toString(endMs), Long.toString(startMs)
        };
        try (Cursor c = r.rawQuery(sql, args)) {
            if (c.moveToFirst()) {
                int sessions = c.getInt(0);
                int apps = c.getInt(1);
                long tracked = c.getLong(2);
                long average = c.getLong(3);
                long reportSpan = Math.max(0L, endMs - startMs);
                boolean sane = tracked <= reportSpan && average <= MAX_REASONABLE_SESSION_MS;
                return new Snapshot(sessions, apps, tracked, average, sane);
            }
        }
        return new Snapshot(0, 0, 0L, 0L, true);
    }
}
