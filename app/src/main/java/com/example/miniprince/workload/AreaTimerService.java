package com.example.miniprince.workload;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Starts a service that can constantly monitor the user's location, update the time spent there,
 * and determine if the area has changed.
 */

public class AreaTimerService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CurrentArea passed = (CurrentArea) intent;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
