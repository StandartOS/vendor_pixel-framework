/*
 * Copyright (C) 2022 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.power;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import androidx.core.app.NotificationCompat;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.util.NotificationChannels;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;
import com.google.android.systemui.power.AdaptiveChargingNotification;
import java.util.concurrent.TimeUnit;

class AdaptiveChargingNotification {
    private final AdaptiveChargingManager mAdaptiveChargingManager;
    @VisibleForTesting
    private boolean mAdaptiveChargingQueryInBackground = true;
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final NotificationManager mNotificationManager;
    @VisibleForTesting
    private boolean mWasActive = false;

    AdaptiveChargingNotification(Context context) {
        this(context, new AdaptiveChargingManager(context));
    }

    @VisibleForTesting
    AdaptiveChargingNotification(Context context, AdaptiveChargingManager adaptiveChargingManager) {
        mContext = context;
        mNotificationManager = context.getSystemService(NotificationManager.class);
        mAdaptiveChargingManager = adaptiveChargingManager;
    }

    void dispatchIntent(Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        switch (action) {
            case "android.intent.action.BATTERY_CHANGED":
            case "PNW.batteryStatusChanged":
                resolveBatteryChangedIntent(intent);
                break;
            case "PNW.acChargeNormally":
            case "PNW.normalCharge":
                mAdaptiveChargingManager.setAdaptiveChargingDeadline(-1);
                cancelNotification();
                break;
            case "com.google.android.systemui.adaptivecharging.ADAPTIVE_CHARGING_DEADLINE_SET":
                checkAdaptiveChargingStatus(true);
                break;
        }
    }

    @VisibleForTesting
    public void resolveBatteryChangedIntent(Intent intent) {
        boolean z;
        boolean z2;
        boolean z3 = true;
        if (intent.getIntExtra("plugged", 0) != 0) {
            z = true;
        } else {
            z = false;
        }
        if (intent.getIntExtra("status", 1) == 5) {
            z2 = true;
        } else {
            z2 = false;
        }
        int i = -1;
        int intExtra = intent.getIntExtra("level", -1);
        int intExtra2 = intent.getIntExtra("scale", 0);
        if (intExtra2 != 0) {
            i = Math.round((intExtra / intExtra2) * 100.0f);
        }
        if (!z2 && i < 100) {
            z3 = false;
        }
        if (z && !z3) {
            checkAdaptiveChargingStatus(false);
        } else {
            cancelNotification();
        }
    }

    private void handleOnReceiveStatus(String str, int i, boolean z) {
        if (AdaptiveChargingManager.isActive(str, i)) {
            sendNotification(z, i);
        } else {
            cancelNotification();
        }
    }

    private void checkAdaptiveChargingStatus(boolean forceUpdate) {
        if (!mAdaptiveChargingManager.isAvailable()) {
            return;
        }
        final AdaptiveChargingStatusReceiver adaptiveChargingStatusReceiver = new AdaptiveChargingStatusReceiver(forceUpdate);
        if (!mAdaptiveChargingQueryInBackground) {
            mAdaptiveChargingManager.queryStatus(adaptiveChargingStatusReceiver);
        } else {
            AsyncTask.execute(() -> mAdaptiveChargingManager.queryStatus(adaptiveChargingStatusReceiver));
        }
    }

    class AdaptiveChargingStatusReceiver implements AdaptiveChargingManager.AdaptiveChargingStatusReceiver {
        final boolean forceUpdate;

        @Override
        public void onDestroyInterface() {
        }

        AdaptiveChargingStatusReceiver(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
        }

        @Override
        public void onReceiveStatus(final int i, final String str) {
            mHandler.post(() -> handleOnReceiveStatus(str, i, forceUpdate));
        }
    }

    private void sendNotification(boolean z, int i) {
        if (!mWasActive || z) {
            NotificationCompat.Builder addAction = new NotificationCompat.Builder(mContext, NotificationChannels.BATTERY).setShowWhen(false).setSmallIcon(R.drawable.ic_battery_charging).setContentTitle(mContext.getString(R.string.adaptive_charging_notify_title)).setContentText(mContext.getString(R.string.adaptive_charging_notify_des, mAdaptiveChargingManager.formatTimeToFull(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(i + 29)))).addAction(0, mContext.getString(R.string.adaptive_charging_notify_turn_off_once), PowerUtils.createNormalChargingIntent(mContext, "PNW.acChargeNormally"));
            PowerUtils.overrideNotificationAppName(mContext, addAction, 17039652);
            mNotificationManager.notifyAsUser("adaptive_charging", PowerUtils.AC_NOTIFICATION_ID, addAction.build(), UserHandle.ALL);
            mWasActive = true;
        }
    }

    private void cancelNotification() {
        if (!mWasActive) {
            return;
        }
        mNotificationManager.cancelAsUser("adaptive_charging", PowerUtils.AC_NOTIFICATION_ID, UserHandle.ALL);
        mWasActive = false;
    }
}
