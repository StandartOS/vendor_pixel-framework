/*
 * Copyright (C) 2022 The PixelExperience Project
 *               2023 The RisingOS Android Project
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

package com.google.android.systemui.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.internal.logging.UiEventLogger
import com.android.systemui.animation.DialogLaunchAnimator
import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.broadcast.BroadcastSender
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.power.PowerNotificationWarnings
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.BatteryController
import dagger.Lazy

class PowerNotificationWarningsGoogleImpl(
    context: Context,
    activityStarter: ActivityStarter,
    broadcastSender: BroadcastSender,
    batteryControllerLazy: Lazy<BatteryController>,
    dialogLaunchAnimator: DialogLaunchAnimator,
    uiEventLogger: UiEventLogger,
    broadcastDispatcher: BroadcastDispatcher,
    userTracker: UserTracker
) : PowerNotificationWarnings(
    context,
    activityStarter,
    broadcastSender,
    batteryControllerLazy,
    dialogLaunchAnimator,
    uiEventLogger,
    userTracker
) {
    private val mHandler: Handler = Handler(Looper.getMainLooper())

    private val mAdaptiveChargingNotification = AdaptiveChargingNotification(context)
    private val mBatteryDefenderNotification = BatteryDefenderNotification(context, uiEventLogger)
    private val mBatteryInfoBroadcast = BatteryInfoBroadcast(context)

    init {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("PNW.defenderResumeCharging")
            addAction("PNW.defenderResumeCharging.settings")
            addAction("android.os.action.POWER_SAVE_MODE_CHANGED")
            addAction("com.google.android.systemui.adaptivecharging.ADAPTIVE_CHARGING_DEADLINE_SET")
            addAction("PNW.acChargeNormally")
            addAction("PNW.normalCharge")
            addAction("PNW.batteryStatusChanged")
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.device.action.ALIAS_CHANGED")
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
            addAction("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED")
        }

        broadcastDispatcher.registerReceiverWithHandler(
            createBroadcastReceiver(),
            intentFilter,
            mHandler
        )

        mHandler.post {
            val currentTimeMillis = System.currentTimeMillis()
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
                createBroadcastReceiver().onReceive(context, it)
            }
            Log.d(
                "PowerNotificationWarningsGoogleImpl",
                String.format("Finish initialize in %d/ms", System.currentTimeMillis() - currentTimeMillis)
            )
        }
    }

    private fun createBroadcastReceiver() = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == null) {
                return
            }
            Log.d("PowerNotificationWarningsGoogleImpl", "onReceive: ${intent.action}")
            mBatteryInfoBroadcast.notifyBatteryStatusChanged(intent)
            mBatteryDefenderNotification.dispatchIntent(intent)
            mAdaptiveChargingNotification.dispatchIntent(intent)
        }
    }
}
