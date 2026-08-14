package com.thefoolish.activityai;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class CollectorHealth {
    public static final class Snapshot {
        public final long eventCount, lastEventMs;
        public final String databaseState;
        Snapshot(long eventCount, long lastEventMs, String databaseState) {
            this.eventCount = eventCount; this.lastEventMs = lastEventMs; this.databaseState = databaseState;
        }
    }

    private final Context context;
    public CollectorHealth(Context context) { this.context = context.getApplicationContext(); }

    public Snapshot read() {
        try (EventDatabase helper = new EventDatabase(context)) {
            SQLiteDatabase db = helper.getReadableDatabase();
            long count = 0, last = 0;
            try (Cursor c = db.rawQuery("SELECT COUNT(*), COALESCE(MAX(COALESCE(ended_at_ms,started_at_ms)),0) FROM events", null)) {
                if (c.moveToFirst()) { count = c.getLong(0); last = c.getLong(1); }
            }
            String state = "ok";
            try (Cursor c = db.rawQuery("PRAGMA quick_check", null)) {
                if (c.moveToFirst()) state = c.getString(0);
            }
            return new Snapshot(count, last, state);
        } catch (Exception e) {
            return new Snapshot(0, 0, "error: " + e.getClass().getSimpleName());
        }
    }
}
