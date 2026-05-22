package com.carvalhorr.daysInOffice.core.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.carvalhorr.daysInOffice.core.detection.Detector
import com.carvalhorr.daysInOffice.core.detection.detector.GeofenceDetector
import com.carvalhorr.daysInOffice.core.detection.detector.WifiConnectedDetector
import com.carvalhorr.daysInOffice.core.detection.detector.WifiScanDetector
import com.carvalhorr.daysInOffice.core.detection.worker.DayDetectionWorker
import com.carvalhorr.daysInOffice.notification.DailyCheckInNotificationWorker
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetectorModule {

    @Provides
    @Singleton
    fun provideGeofenceDetector(
        @ApplicationContext context: Context,
        preferencesDataSource: PreferencesDataSource
    ): GeofenceDetector = GeofenceDetector(context, preferencesDataSource)

    @Provides
    @Singleton
    fun provideDetectorMap(
        @ApplicationContext context: Context,
        geofenceDetector: GeofenceDetector
    ): Map<DetectionMethod, @JvmSuppressWildcards Detector> = mapOf(
        DetectionMethod.WIFI_CONNECTED to WifiConnectedDetector(context),
        DetectionMethod.WIFI_SCAN to WifiScanDetector(context),
        DetectionMethod.GEOFENCE to geofenceDetector
    )

    @Provides
    @Singleton
    fun provideWorkerFactory(
        dayDetectionWorkerFactory: DayDetectionWorker.Factory,
        dailyCheckInNotificationWorkerFactory: DailyCheckInNotificationWorker.Factory
    ): WorkerFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? = when (workerClassName) {
            DayDetectionWorker::class.java.name ->
                dayDetectionWorkerFactory.create(appContext, workerParameters)
            DailyCheckInNotificationWorker::class.java.name ->
                dailyCheckInNotificationWorkerFactory.create(appContext, workerParameters)
            else -> null
        }
    }
}
