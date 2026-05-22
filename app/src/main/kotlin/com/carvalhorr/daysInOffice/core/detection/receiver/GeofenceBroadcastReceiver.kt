package com.carvalhorr.daysInOffice.core.detection.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "Detection"

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        val transition = geofencingEvent.geofenceTransition
        val inside = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }

        Log.i(TAG, "GeofenceBroadcastReceiver.onReceive: transition=$transition, writing geofenceInside=$inside")
        CoroutineScope(Dispatchers.IO).launch {
            preferencesDataSource.saveGeofenceInside(inside)
        }
    }
}
