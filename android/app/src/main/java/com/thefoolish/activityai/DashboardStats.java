package com.thefoolish.activityai;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class DashboardStats {
    public static final class Snapshot {
        public final int sessions;
        public final int apps;
        public final long trackedMs;
        public final long averageSessionMs;
        Snapshot(int sessions, int apps, long trackedMs, long averageSessionMs) {
            this.sessions = sessions; this.apps = apps; this.trackedMs = trackedMs;
            this.averageSessionMs = averageSessionMs;
        }
    }

    private final EventDatabase db;
    public DashboardStats(Context context) { this.db = new EventDatabase(context.getApplicationContext()); }

    public Snapshot between(long startMs, long endMs) {
        SQLiteDatabase r = db.getReadableDatabase();
        String overlap = "MAX(0, MIN(COALESCE(ended_at_ms, ?), ?) - MAX(started_at_ms, ?))";
        String sql = "SELECT COUNT(*), COUNT(DISTINCT app_id), COALESCE(SUM(" + overlap + "),0), " +
                "COALESCE(AVG(" + overlap + "),0) FROM events WHERE event_type='session' " +
                "AND started_at_ms<? AND COALESCE(ended_at_ms, ?) > ?";
        String[] args = new String[]{
                Long.toString(endMs), Long.toString(endMs), Long.toString(startMs),
                Long.toString(endMs), Long.toString(endMs), Long.toString(startMs),
                Long.toString(endMs), Long.toString(endMs), Long.toString(startMs)
        };
        try (Cursor c = r.rawQuery(sql, args)) {
            if (c.moveToFirst()) return new Snapshot(c.getInt(0), c.getInt(1), c.getLong(2), c.getLong(3));
        }
        return new Snapshot(0, 0, 0L, 0L);
    }
}
