package com.thefoolish.activityai;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class PrivacyPolicyStore {
    public static final class Policy {
        public final boolean telemetry, metadata, visual, contentStorage;
        Policy(boolean telemetry, boolean metadata, boolean visual, boolean contentStorage) {
            this.telemetry = telemetry; this.metadata = metadata; this.visual = visual; this.contentStorage = contentStorage;
        }
    }
    private final EventDatabase db;
    public PrivacyPolicyStore(Context context) { db = new EventDatabase(context.getApplicationContext()); }

    public Policy get(String appId) {
        SQLiteDatabase r = db.getReadableDatabase();
        try (Cursor c = r.query("privacy_policy", new String[]{"telemetry","metadata","visual","content_storage"}, "app_id=?", new String[]{appId}, null,null,null)) {
            if (c.moveToFirst()) return new Policy(c.getInt(0)!=0, c.getInt(1)!=0, c.getInt(2)!=0, c.getInt(3)!=0);
        }
        return new Policy(true, false, false, false);
    }

    public void set(String appId, Policy p) {
        ContentValues v = new ContentValues();
        v.put("app_id", appId); v.put("telemetry", p.telemetry ? 1:0); v.put("metadata", p.metadata ? 1:0);
        v.put("visual", p.visual ? 1:0); v.put("content_storage", p.contentStorage ? 1:0);
        db.getWritableDatabase().insertWithOnConflict("privacy_policy", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
