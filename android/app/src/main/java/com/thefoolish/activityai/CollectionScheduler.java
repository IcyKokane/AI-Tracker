package com.thefoolish.activityai;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class CollectionScheduler {
    private static final int JOB_ID = 41001;
    private CollectionScheduler() { }

    public static void ensureScheduled(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        for (JobInfo job : scheduler.getAllPendingJobs()) if (job.getId() == JOB_ID) return;

        JobInfo info = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, UsageCollectionJobService.class))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build();
        scheduler.schedule(info);
    }
}
