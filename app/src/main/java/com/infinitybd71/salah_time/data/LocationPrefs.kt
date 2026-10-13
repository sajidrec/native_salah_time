package com.infinitybd71.salah_time.data

import android.content.Context
import com.infinitybd71.salah_time.utils.Constants
import com.infinitybd71.salah_time.utils.Constants.KEY_LABEL
import com.infinitybd71.salah_time.utils.Constants.KEY_LAT
import com.infinitybd71.salah_time.utils.Constants.KEY_LNG
import com.infinitybd71.salah_time.utils.Constants.KEY_SET
import com.infinitybd71.salah_time.utils.Constants.KEY_SOURCE

class LocationPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        Constants.LOCATION_PREFS,
        Context.MODE_PRIVATE
    )

    enum class Source { GPS, MANUAL }

    var lat: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(Constants.KEY_LAT, 0L))
        set(value) = prefs.edit()
            .putLong(Constants.KEY_LAT, java.lang.Double.doubleToRawLongBits(value)).apply()

    var lng: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LNG, 0L))
        set(value) = prefs.edit().putLong(KEY_LNG, java.lang.Double.doubleToRawLongBits(value))
            .apply()

    var label: String?
        get() = prefs.getString(KEY_LABEL, null)
        set(value) = prefs.edit().putString(KEY_LABEL, value).apply()

    var source: Source
        get() = Source.valueOf(prefs.getString(KEY_SOURCE, Source.GPS.name) ?: Source.GPS.name)
        set(value) = prefs.edit().putString(KEY_SOURCE, value.name).apply()

    val isSet: Boolean
        get() = prefs.getBoolean(KEY_SET, false)

    fun save(lat: Double, lng: Double, label: String?, source: Source) {
        prefs.edit()
            .putBoolean(KEY_SET, true)
            .putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(lat))
            .putLong(KEY_LNG, java.lang.Double.doubleToRawLongBits(lng))
            .putString(KEY_LABEL, label)
            .putString(KEY_SOURCE, source.name)
            .apply()
    }

}