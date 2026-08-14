package com.thefoolish.activityai;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class UsageCollectionJobService extends JobService {
    @Override public boolean onStartJob(JobParameters params) {
        Thread worker = new Thread(() -> {
            try {
                if (UsageAccess.granted(this)) {
                    long end = System.currentTimeMillis();
                    long start = end - (24L * 60L * 60L * 1000L);
                    new UsageCollector(this).collectRange(start, end);
                }
            } finally {
                jobFinished(params, false);
            }
        }, "activity-ai-usage-collector");
        worker.start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
