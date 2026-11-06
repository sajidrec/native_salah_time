package com.infinitybd71.salah_time.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "salah_time_prayer_alarms"
        const val CHANNEL_NAME = "Salah Time Prayer Alarms"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_TIME = "prayer_time"

        fun getRequestCode(prayerName: String): Int = when (prayerName) {
            "Fajr" -> 1001
            "Dhuhr" -> 1002
            "Asr" -> 1003
            "Magrib" -> 1004
            "Isha" -> 1005
            else -> 1000
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        // Start the foreground service that plays looping alarm sound
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(AlarmReceiver.EXTRA_PRAYER_TIME, prayerTime)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
