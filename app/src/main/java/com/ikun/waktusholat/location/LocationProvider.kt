package com.ikun.waktusholat.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class LatLng(val latitude: Double, val longitude: Double, val altitude: Double)

/**
 * Pengganti LocationHelper.java lama yang pakai LocationManager mentah.
 * FusedLocationProviderClient jauh lebih hemat baterai & lebih cepat fix
 * lokasi (gabungan GPS + network + sensor).
 *
 * Pemanggil WAJIB sudah punya izin ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION
 * sebelum memanggil [getCurrentLocation] — cek lewat PermissionHelper di UI.
 */
class LocationProvider(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LatLng? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()
        val location = client.getCurrentLocation(request, null).await() ?: return null
        return LatLng(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
        )
    }
}
