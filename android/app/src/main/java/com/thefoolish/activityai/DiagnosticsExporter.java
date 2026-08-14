package com.thefoolish.activityai;

import android.content.Context;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class DiagnosticsExporter {
    private DiagnosticsExporter() {}

    public static File export(Context context) throws Exception {
        CollectorHealth.Snapshot h = new CollectorHealth(context).read();
        JSONObject root = new JSONObject();
        root.put("format", "activity-ai-diagnostics-v1");
        root.put("generated_at_ms", System.currentTimeMillis());
        root.put("usage_access", UsageAccess.granted(context));
        root.put("event_count", h.eventCount);
        root.put("last_event_ms", h.lastEventMs);
        root.put("database_state", h.databaseState);
        root.put("note", "No event rows, window contents, messages, or screen content are included.");
        File dir = new File(context.getFilesDir(), "diagnostics");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create diagnostics directory");
        File out = new File(dir, "activity-ai-diagnostics.json");
        try (FileOutputStream stream = new FileOutputStream(out, false)) {
            stream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return out;
    }
}
