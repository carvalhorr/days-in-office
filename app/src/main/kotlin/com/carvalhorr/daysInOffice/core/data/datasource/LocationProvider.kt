package com.carvalhorr.daysInOffice.core.data.datasource

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Location> = suspendCancellableCoroutine { cont ->
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) cont.resume(Result.success(loc))
                    else cont.resume(Result.failure(IllegalStateException("Location unavailable")))
                }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        } catch (e: SecurityException) {
            cont.resume(Result.failure(e))
        }
    }
}
