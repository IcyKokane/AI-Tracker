package com.thefoolish.activityai;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class EventDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "activity_ai.db";
    private static final int DB_VERSION = 4;

    public EventDatabase(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (" +
                "event_id TEXT PRIMARY KEY, device_id TEXT NOT NULL, platform TEXT NOT NULL, " +
                "event_type TEXT NOT NULL, app_id TEXT, app_name TEXT, window_title TEXT, " +
                "activity_label TEXT, project_label TEXT, started_at_ms INTEGER NOT NULL, " +
                "ended_at_ms INTEGER, duration_ms INTEGER, source TEXT NOT NULL, confidence REAL NOT NULL, " +
                "privacy_level TEXT NOT NULL, metadata_json TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_events_started ON events(started_at_ms)");
        db.execSQL("CREATE INDEX idx_events_app ON events(app_id, started_at_ms)");
        db.execSQL("CREATE INDEX idx_events_type_started ON events(event_type, started_at_ms)");
        db.execSQL("CREATE INDEX idx_events_source_started ON events(source, started_at_ms)");
        db.execSQL("CREATE TABLE privacy_policy (app_id TEXT PRIMARY KEY, telemetry INTEGER NOT NULL DEFAULT 1, metadata INTEGER NOT NULL DEFAULT 0, visual INTEGER NOT NULL DEFAULT 0, content_storage INTEGER NOT NULL DEFAULT 0)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS privacy_policy (app_id TEXT PRIMARY KEY, telemetry INTEGER NOT NULL DEFAULT 1, metadata INTEGER NOT NULL DEFAULT 0, visual INTEGER NOT NULL DEFAULT 0, content_storage INTEGER NOT NULL DEFAULT 0)");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_type_started ON events(event_type, started_at_ms)");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_source_started ON events(source, started_at_ms)");
            // Earlier Milestone-1 builds used more than one label for completed usage rows.
            // A completed usage row is defined structurally, then normalized to the canonical type.
            db.execSQL("UPDATE events SET event_type='session' " +
                    "WHERE platform='android' AND source='usage_stats' " +
                    "AND ended_at_ms IS NOT NULL AND duration_ms IS NOT NULL AND duration_ms>0");
        }
    }
}
