package com.carvalhorr.daysInOffice.core.data.repository

import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MandateConfigRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : MandateConfigRepository {

    override fun getMandateConfig(): Flow<MandateConfig> = combine(
        preferencesDataSource.mandatePercentage,
        preferencesDataSource.mandatePeriod,
        preferencesDataSource.workingDays,
        preferencesDataSource.fiscalYearStartMonth
    ) { percentage, period, workingDays, fiscalYearStartMonth ->
        MandateConfig(
            targetPercentage = percentage,
            period = period,
            workingDays = workingDays,
            fiscalYearStartMonth = fiscalYearStartMonth
        )
    }

    override suspend fun saveMandateConfig(config: MandateConfig) {
        preferencesDataSource.saveMandatePercentage(config.targetPercentage)
        preferencesDataSource.saveMandatePeriod(config.period)
        preferencesDataSource.saveWorkingDays(config.workingDays)
        preferencesDataSource.saveFiscalYearStartMonth(config.fiscalYearStartMonth)
    }

    override fun getDetectionConfig(): Flow<DetectionConfig> = combine(
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
    }

    override suspend fun saveDetectionConfig(config: DetectionConfig) {
        preferencesDataSource.saveDetectionMethods(config.enabledMethods)
        preferencesDataSource.saveWifiSsid(config.wifiSsid)
        preferencesDataSource.saveGeofenceLat(config.geofenceLatitude)
        preferencesDataSource.saveGeofenceLng(config.geofenceLongitude)
        preferencesDataSource.saveGeofenceRadius(config.geofenceRadiusMeters)
    }

    override fun getOnboardingComplete(): Flow<Boolean> = preferencesDataSource.onboardingComplete

    override suspend fun saveOnboardingComplete(complete: Boolean) {
        preferencesDataSource.saveOnboardingComplete(complete)
    }

    override fun getCalendarSyncEnabled(): Flow<Boolean> = preferencesDataSource.calendarSyncEnabled

    override suspend fun saveCalendarSyncEnabled(enabled: Boolean) {
        preferencesDataSource.saveCalendarSyncEnabled(enabled)
    }

    override suspend fun markDetectorDismissed(method: DetectionMethod, date: LocalDate) {
        preferencesDataSource.markDetectorDismissed(method, date)
    }

    override suspend fun isDetectorDismissedToday(method: DetectionMethod, date: LocalDate): Boolean =
        preferencesDataSource.isDetectorDismissedToday(method, date)
}
