package com.infinitybd71.salah_time.data.repository

import android.content.Context
import org.json.JSONArray
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Parses assets/cities.json ONCE into memory and serves both:
 *  - binary search by city name (array is alphabetically sorted)
 *  - nearest-city lookup by lat/lng via a latitude-sorted index
 * No network, no repeated JSON parsing.
 */
object CitiesRepository {

    data class City(val name: String, val lat: Double, val lng: Double) {
        /** "dhaka" -> "Dhaka" */
        val label: String
            get() = name.trim().replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }

    @Volatile
    private var loaded = false

    private lateinit var names: Array<String>
    private lateinit var lats: DoubleArray
    private lateinit var lngs: DoubleArray
    private lateinit var latSortedIdx: IntArray

    /** Idempotent. Call from a background dispatcher before the query functions. */
    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return

        val json = context.applicationContext.assets.open("cities.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONArray(json)

        val count = arr.length()
        names = Array(count) { "" }
        lats = DoubleArray(count)
        lngs = DoubleArray(count)
        for (i in 0 until count) {
            val city = arr.getJSONArray(i)
            names[i] = city.getString(0)
            lats[i] = city.getDouble(1)
            lngs[i] = city.getDouble(2)
        }

        // Spatial index: indices sorted by latitude (~few ms, done once).
        latSortedIdx = IntArray(count) { it }.sortedBy { lats[it] }.toIntArray()

        loaded = true
    }

    /** Binary search — O(log n). cities.json is sorted alphabetically. */
    fun findCityByName(query: String): City? {
        check(loaded) { "Call CitiesRepository.ensureLoaded() first" }
        val q = query.trim().lowercase()

        var left = 0
        var right = names.size - 1
        while (left <= right) {
            val mid = left + (right - left) / 2
            when {
                names[mid] == q -> return City(names[mid], lats[mid], lngs[mid])
                names[mid] < q -> left = mid + 1
                else -> right = mid - 1
            }
        }
        return null
    }

    /**
     * Nearest city within [maxKm]. Only the latitude band (±maxKm/111°) is scanned
     * via the sorted index — ~950 candidates out of 63k — then haversine-filtered.
     * Sub-millisecond, zero I/O.
     */
    fun nearestCity(lat: Double, lng: Double, maxKm: Double = 150.0): City? {
        check(loaded) { "Call CitiesRepository.ensureLoaded() first" }

        // 1 degree of latitude ≈ 111 km → half-width of the search band.
        val latDelta = maxKm / 111.0

        // Binary search: first index in latSortedIdx whose latitude >= lat - latDelta.
        var lo = 0
        var hi = latSortedIdx.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (lats[latSortedIdx[mid]] < lat - latDelta) lo = mid + 1 else hi = mid
        }

        var bestIdx = -1
        var bestDist = maxKm
        var i = lo
        while (i < latSortedIdx.size && lats[latSortedIdx[i]] <= lat + latDelta) {
            val idx = latSortedIdx[i]
            val dist = haversineKm(lat, lng, lats[idx], lngs[idx])
            if (dist <= bestDist) {
                bestDist = dist
                bestIdx = idx
            }
            i++
        }

        return if (bestIdx >= 0) City(names[bestIdx], lats[bestIdx], lngs[bestIdx]) else null
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a)) // km
    }
}
