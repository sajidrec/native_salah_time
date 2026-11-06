package com.infinitybd71.salah_time.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Stop the alarm service
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        context.stopService(stopIntent)

        // Cancel the alarm
        val prayerName = intent.getStringExtra(AlarmReceiver.EXTRA_PRAYER_NAME) ?: ""
        if (prayerName.isNotEmpty()) {
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                AlarmReceiver.getRequestCode(prayerName),
                alarmIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        }

        // Clear SharedPreferences state so bell shows unset
        val alarmPrefs = AlarmPrefs(context)
        if (prayerName.isNotEmpty()) {
            alarmPrefs.set(prayerName, false)
            alarmPrefs.setTime(prayerName, null)
        }
    }
}