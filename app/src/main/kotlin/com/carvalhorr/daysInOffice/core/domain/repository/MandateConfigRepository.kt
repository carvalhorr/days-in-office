package com.carvalhorr.daysInOffice.core.domain.repository

import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MandateConfigRepository {
    fun getMandateConfig(): Flow<MandateConfig>
    suspend fun saveMandateConfig(config: MandateConfig)
    fun getDetectionConfig(): Flow<DetectionConfig>
    suspend fun saveDetectionConfig(config: DetectionConfig)
    fun getOnboardingComplete(): Flow<Boolean>
    suspend fun saveOnboardingComplete(complete: Boolean)
    fun getCalendarSyncEnabled(): Flow<Boolean>
    suspend fun saveCalendarSyncEnabled(enabled: Boolean)
    suspend fun markDetectorDismissed(method: DetectionMethod, date: LocalDate)
    suspend fun isDetectorDismissedToday(method: DetectionMethod, date: LocalDate): Boolean
}
