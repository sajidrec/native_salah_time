package com.infinitybd71.salah_time.data.remote

import com.infinitybd71.salah_time.data.remote.model.PrayerResponseModel
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PrayerTimesApi {
    @GET("timings/{date}")
    suspend fun getPrayerTimes(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): PrayerResponseModel

}