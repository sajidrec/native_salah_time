package com.infinitybd71.salah_time.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

sealed class DeviceLocationResult {
    data class Success(val lat: Double, val lng: Double) : DeviceLocationResult()
    object PermissionDenied : DeviceLocationResult()
    object Unavailable : DeviceLocationResult()
}

object DeviceLocationProvider {
    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")  // caller guarantees hasPermission() == true
    suspend fun getCurrentLocation(context: Context): DeviceLocationResult {
        if (!hasPermission(context)) return DeviceLocationResult.PermissionDenied

        val fused = LocationServices.getFusedLocationProviderClient(context)

        // 1) Cached fix FIRST — instant, usually available.
        val last = try {
            suspendCancellableCoroutine<android.location.Location?> { cont ->
                fused.lastLocation.addOnCompleteListener { task ->
                    if (cont.isActive) cont.resume(runCatching { task.result }.getOrNull())
                }
            }
        } catch (e: Exception) {
            null
        }
        if (last != null) {
            return DeviceLocationResult.Success(last.latitude, last.longitude)
        }

        // 2) Cache empty → request a fresh fix, hard 5s limit.
        val fresh = withTimeoutOrNull(5_000L.milliseconds) {
            suspendCancellableCoroutine<android.location.Location?> { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                fused.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,  // good accuracy vs battery
                    cts.token
                ).addOnCompleteListener { task ->
                    if (cont.isActive) cont.resume(runCatching { task.result }.getOrNull())
                }
            }
        }

        return if (fresh != null) {
            DeviceLocationResult.Success(fresh.latitude, fresh.longitude)
        } else {
            DeviceLocationResult.Unavailable
        }
    }
}
