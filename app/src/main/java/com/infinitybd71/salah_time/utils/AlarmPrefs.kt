package com.infinitybd71.salah_time.utils

import android.content.Context
import android.content.SharedPreferences

class AlarmPrefs(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("salah_time_alarms", Context.MODE_PRIVATE)
    }

    fun isSet(prayerName: String): Boolean {
        return prefs.getBoolean("alarm_$prayerName", false)
    }

    fun set(prayerName: String, value: Boolean) {
        prefs.edit().putBoolean("alarm_$prayerName", value).apply()
    }

    fun getTime(prayerName: String): String? {
        return prefs.getString("alarm_time_$prayerName", null)
    }

    fun setTime(prayerName: String, time: String?) {
        prefs.edit().putString("alarm_time_$prayerName", time).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}