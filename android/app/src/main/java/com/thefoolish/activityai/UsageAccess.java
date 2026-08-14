package com.thefoolish.activityai;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;

public final class UsageAccess {
    private UsageAccess() { }
    public static boolean granted(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo info = context.getApplicationInfo();
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), info.packageName);
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
