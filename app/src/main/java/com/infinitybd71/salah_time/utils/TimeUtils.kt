package com.infinitybd71.salah_time.utils

import java.text.SimpleDateFormat
import java.util.Locale

fun formatPrayerTime(time: String): String {
    return runCatching {
        SimpleDateFormat("h:mm a", Locale.US)
            .format(
                SimpleDateFormat("HH:mm", Locale.US)
                    .parse(time.take(5))!!
            )
    }.getOrDefault("--")
}