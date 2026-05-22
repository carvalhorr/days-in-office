package com.carvalhorr.daysInOffice.core.data.repository

import com.carvalhorr.daysInOffice.core.data.datasource.PreferencesDataSource
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class MandateConfigRepositoryImplTest {

    private val preferencesDataSource: PreferencesDataSource = mockk()
    private lateinit var repository: MandateConfigRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = MandateConfigRepositoryImpl(preferencesDataSource)
    }

    @Test
    fun `given no saved config when getMandateConfig then returns default config`() = runTest {
        val defaultWorkingDays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        every { preferencesDataSource.mandatePercentage } returns flowOf(0.5f)
        every { preferencesDataSource.mandatePeriod } returns flowOf(MandatePeriod.MONTHLY)
        every { preferencesDataSource.workingDays } returns flowOf(defaultWorkingDays)

        val result = repository.getMandateConfig().first()

        assertEquals(0.5f, result.targetPercentage)
        assertEquals(MandatePeriod.MONTHLY, result.period)
        assertEquals(defaultWorkingDays, result.workingDays)
    }

    @Test
    fun `given saved config when getMandateConfig then returns saved config`() = runTest {
        val savedWorkingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        every { preferencesDataSource.mandatePercentage } returns flowOf(0.75f)
        every { preferencesDataSource.mandatePeriod } returns flowOf(MandatePeriod.QUARTERLY)
        every { preferencesDataSource.workingDays } returns flowOf(savedWorkingDays)

        val result = repository.getMandateConfig().first()

        assertEquals(0.75f, result.targetPercentage)
        assertEquals(MandatePeriod.QUARTERLY, result.period)
        assertEquals(savedWorkingDays, result.workingDays)
    }

    @Test
    fun `given config when saveMandateConfig then preferences updated`() = runTest {
        val workingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
        val config = MandateConfig(
            targetPercentage = 0.6f,
            period = MandatePeriod.WEEKLY,
            workingDays = workingDays
        )
        coJustRun { preferencesDataSource.saveMandatePercentage(any()) }
        coJustRun { preferencesDataSource.saveMandatePeriod(any()) }
        coJustRun { preferencesDataSource.saveWorkingDays(any()) }

        repository.saveMandateConfig(config)

        coVerify { preferencesDataSource.saveMandatePercentage(0.6f) }
        coVerify { preferencesDataSource.saveMandatePeriod(MandatePeriod.WEEKLY) }
        coVerify { preferencesDataSource.saveWorkingDays(workingDays) }
    }

    @Test
    fun `geofenceRoundTrip writes and reads back all three geofence fields`() = runTest {
        val lat = 37.7749
        val lng = -122.4194
        val radius = 200f
        val config = DetectionConfig(
            enabledMethods = setOf(DetectionMethod.GEOFENCE),
            wifiSsid = null,
            geofenceLatitude = lat,
            geofenceLongitude = lng,
            geofenceRadiusMeters = radius
        )

        coJustRun { preferencesDataSource.saveDetectionMethods(any()) }
        coJustRun { preferencesDataSource.saveWifiSsid(any()) }
        coJustRun { preferencesDataSource.saveGeofenceLat(any()) }
        coJustRun { preferencesDataSource.saveGeofenceLng(any()) }
        coJustRun { preferencesDataSource.saveGeofenceRadius(any()) }
        every { preferencesDataSource.detectionMethods } returns flowOf(setOf(DetectionMethod.GEOFENCE))
        every { preferencesDataSource.wifiSsid } returns flowOf(null)
        every { preferencesDataSource.geofenceLat } returns flowOf(lat)
        every { preferencesDataSource.geofenceLng } returns flowOf(lng)
        every { preferencesDataSource.geofenceRadius } returns flowOf(radius)

        repository.saveDetectionConfig(config)

        coVerify { preferencesDataSource.saveGeofenceLat(lat) }
        coVerify { preferencesDataSource.saveGeofenceLng(lng) }
        coVerify { preferencesDataSource.saveGeofenceRadius(radius) }

        val result = repository.getDetectionConfig().first()

        assertEquals(lat, result.geofenceLatitude)
        assertEquals(lng, result.geofenceLongitude)
        assertEquals(radius, result.geofenceRadiusMeters)
    }
}
