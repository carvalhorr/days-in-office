package com.carvalhorr.daysInOffice.core.detection.detector

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.carvalhorr.daysInOffice.core.detection.Detector
import com.carvalhorr.daysInOffice.core.detection.receiver.GeofenceBroadcastReceiver
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

private const val TAG = "Detection"

class GeofenceDetector(
    private val context: Context,
    private val preferencesDataSource: PreferencesDataSource,
    geofencingClientOverride: GeofencingClient? = null
) : Detector {

    private val geofencingClient: GeofencingClient by lazy {
        geofencingClientOverride ?: LocationServices.getGeofencingClient(context)
    }

    override suspend fun isAtOffice(config: DetectionConfig): Boolean =
        preferencesDataSource.geofenceInside.first()

    override suspend fun isAvailable(context: Context, config: DetectionConfig): Boolean =
        config.geofenceLatitude != null &&
            config.geofenceLongitude != null &&
            config.geofenceRadiusMeters != null

    suspend fun shouldActivate(): Boolean {
        val config = currentConfig()
        return DetectionMethod.GEOFENCE in config.enabledMethods &&
            config.geofenceLatitude != null &&
            config.geofenceLongitude != null &&
            config.geofenceRadiusMeters != null
    }

    private suspend fun currentConfig(): DetectionConfig = combine(
        preferencesDataSource.detectionMethods,
        preferencesDataSource.wifiSsid,
        preferencesDataSource.geofenceLat,
        preferencesDataSource.geofenceLng,
        preferencesDataSource.geofenceRadius
    ) { methods, ssid, lat, lng, radius ->
        DetectionConfig(
            enabledMethods = methods,
            wifiSsid = ssid,
            geofenceLatitude = lat,
            geofenceLongitude = lng,
            geofenceRadiusMeters = radius
        )
    }.first()

    @SuppressLint("MissingPermission")
    fun setupGeofence(
        lat: Double,
        lng: Double,
        radius: Float
    ) {
        val geofence = Geofence.Builder()
            .setRequestId("OFFICE_GEOFENCE")
            .setCircularRegion(lat, lng, radius)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        geofencingClient.addGeofences(request, pendingIntent)
            .addOnSuccessListener {
                Log.i(TAG, "GeofenceDetector.setupGeofence: geofence registered at ($lat,$lng,$radius)")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "GeofenceDetector.setupGeofence: addGeofences failed", e)
            }
    }

    fun removeGeofence() {
        geofencingClient.removeGeofences(listOf("OFFICE_GEOFENCE"))
    }
}
