package com.carvalhorr.daysInOffice.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.carvalhorr.daysInOffice.core.detection.detector.GeofenceDetector
import com.carvalhorr.daysInOffice.core.detection.worker.DayDetectionWorker
import com.carvalhorr.daysInOffice.notification.NotificationChannelManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "Detection"

@HiltAndroidApp
class DaysInOfficeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: WorkerFactory
    @Inject lateinit var geofenceDetector: GeofenceDetector
    @Inject lateinit var notificationChannelManager: NotificationChannelManager
    @Inject lateinit var preferencesDataSource: PreferencesDataSource

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "DaysInOfficeApp.onCreate: scheduling worker")
        DayDetectionWorker.schedule(WorkManager.getInstance(this))
        notificationChannelManager.createChannels()
        MainScope().launch {
            val shouldActivate = geofenceDetector.shouldActivate()
            if (shouldActivate) {
                val lat = preferencesDataSource.geofenceLat.first()
                val lng = preferencesDataSource.geofenceLng.first()
                val radius = preferencesDataSource.geofenceRadius.first()
                if (lat != null && lng != null && radius != null) {
                    Log.i(TAG, "DaysInOfficeApp.onCreate: calling setupGeofence (lat=$lat,lng=$lng,radius=$radius)")
                    geofenceDetector.setupGeofence(lat, lng, radius)
                } else {
                    Log.i(TAG, "DaysInOfficeApp.onCreate: skipping setupGeofence (coords missing lat=$lat,lng=$lng,radius=$radius)")
                }
            } else {
                Log.i(TAG, "DaysInOfficeApp.onCreate: skipping setupGeofence (shouldActivate=false)")
            }
        }
    }
}
