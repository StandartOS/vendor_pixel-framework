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

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.UserHandle
import java.util.Calendar

class BatteryInfoBroadcast(private val mContext: Context) {
    private val mPowerManager: PowerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private fun createIntent(str: String): Intent {
        return Intent(str).setPackage("com.google.android.settings.intelligence")
    }

    fun notifyBatteryStatusChanged(intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BATTERY_CHANGED 
            || action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
            || action == Intent.ACTION_POWER_CONNECTED
            || action == Intent.ACTION_POWER_DISCONNECTED
            || action == Intent.ACTION_TIME_CHANGED
            || action == Intent.ACTION_TIMEZONE_CHANGED) {
            val newIntent = createIntent("PNW.batteryStatusChanged")
            if (Intent.ACTION_BATTERY_CHANGED == action) {
                newIntent.putExtra("battery_changed_intent", intent)
            }
            val isPowerSaveMode = mPowerManager.isPowerSaveMode
            newIntent.putExtra("battery_save", isPowerSaveMode)
            mContext.sendBroadcastAsUser(newIntent, UserHandle.ALL)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            if (currentHour < 6 || (currentHour == 6 && currentMinute == 0) || currentHour >= 21) {
                val normalChargeIntent = createIntent("PNW.normalCharge")
                mContext.sendBroadcastAsUser(normalChargeIntent, UserHandle.ALL)
            }
            println("BatteryInfoBroadcast onReceive: $action isPowerSaveMode: $isPowerSaveMode")
        } else if (action == "android.bluetooth.adapter.action.STATE_CHANGED" ||
            action == "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED" ||
            action == "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED" ||
            action == "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" ||
            action == "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED" ||
            action == "android.bluetooth.device.action.ALIAS_CHANGED" ||
            action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"
        ) {
            val newIntent = createIntent("PNW.bluetoothStatusChanged")
            newIntent.putExtra(action, intent)
            mContext.sendBroadcastAsUser(newIntent, UserHandle.ALL)
        }
    }
}
