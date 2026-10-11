package com.infinitybd71.salah_time.domain

import com.infinitybd71.salah_time.data.model.Prayer

object PrayerResolver {
    fun nextPrayerIndex(prayers: List<Prayer>, nowMillis: Long): Int {
        return 1
    }
}
