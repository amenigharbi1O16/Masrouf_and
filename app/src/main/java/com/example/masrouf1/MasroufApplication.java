package com.example.masrouf1;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;
import com.masrouf.controller.DevelopmentSeedManager;

public final class MasroufApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        DevelopmentSeedManager.seedIfDebug(this);
        BudgetNotificationManager.createChannel(this);
        PeriodicWorkRequest rollover = new PeriodicWorkRequest.Builder(BudgetRolloverWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("masrouf-budget-rollover", ExistingPeriodicWorkPolicy.KEEP, rollover);
    }
}
