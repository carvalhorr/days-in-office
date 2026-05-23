package com.carvalhorr.daysInOffice.feature.settings

import androidx.work.WorkManager
import androidx.work.WorkRequest
import app.cash.turbine.test
import com.carvalhorr.daysInOffice.core.detection.detector.GeofenceDetector
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mandateConfigRepository: MandateConfigRepository = mockk()
    private val geofenceDetector: GeofenceDetector = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)

    private val defaultMandateConfig = MandateConfig(
        targetPercentage = 0.5f,
        period = MandatePeriod.MONTHLY,
        workingDays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
    )

    private val defaultDetectionConfig = DetectionConfig(
        enabledMethods = emptySet(),
        wifiSsid = null,
        geofenceLatitude = null,
        geofenceLongitude = null,
        geofenceRadiusMeters = null
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setupMocks()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupMocks() {
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(defaultMandateConfig)
        every { mandateConfigRepository.getDetectionConfig() } returns flowOf(defaultDetectionConfig)
        coJustRun { mandateConfigRepository.saveMandateConfig(any()) }
        coJustRun { mandateConfigRepository.saveDetectionConfig(any()) }
        coJustRun { mandateConfigRepository.saveOnboardingComplete(any()) }
    }

    private fun createViewModel() = SettingsViewModel(mandateConfigRepository, geofenceDetector, workManager)

    @Test
    fun `given initial state when created then state is Loading`() {
        val viewModel = createViewModel()
        assertInstanceOf(SettingsUiState.Loading::class.java, viewModel.state.value)
    }

    @Test
    fun `given flows when collected then state transitions to Success`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem()
            assertInstanceOf(SettingsUiState.Success::class.java, success)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given mandate config when state is Success then mandateConfig matches`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as SettingsUiState.Success
            assertEquals(defaultMandateConfig, success.mandateConfig)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given detection config when state is Success then detectionConfig matches`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as SettingsUiState.Success
            assertEquals(defaultDetectionConfig, success.detectionConfig)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given updateTargetPercentage called then saveMandateConfig invoked with new percentage`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateTargetPercentage(0.75f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveMandateConfig(match { it.targetPercentage == 0.75f }) }
    }

    @Test
    fun `given updateTargetPercentage called then period and workingDays preserved`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateTargetPercentage(0.75f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveMandateConfig(
                match { it.period == MandatePeriod.MONTHLY && it.workingDays.size == 5 }
            )
        }
    }

    @Test
    fun `given updatePeriod called with WEEKLY then saveMandateConfig invoked with WEEKLY`() = runTest {
        val viewModel = createViewModel()

        viewModel.updatePeriod(MandatePeriod.WEEKLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveMandateConfig(match { it.period == MandatePeriod.WEEKLY }) }
    }

    @Test
    fun `given updatePeriod called with QUARTERLY then saveMandateConfig invoked with QUARTERLY`() = runTest {
        val viewModel = createViewModel()

        viewModel.updatePeriod(MandatePeriod.QUARTERLY)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveMandateConfig(match { it.period == MandatePeriod.QUARTERLY }) }
    }

    @Test
    fun `given updateWorkingDays called then saveMandateConfig invoked with new days`() = runTest {
        val newDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val viewModel = createViewModel()

        viewModel.updateWorkingDays(newDays)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveMandateConfig(match { it.workingDays == newDays }) }
    }

    @Test
    fun `given updateWifiConnected enabled then saveDetectionConfig with WIFI_CONNECTED enabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiConnected(true, "OfficeNet")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(
                match { it.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED) && it.wifiSsid == "OfficeNet" }
            )
        }
    }

    @Test
    fun `given updateWifiConnected disabled then saveDetectionConfig without WIFI_CONNECTED`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiConnected(false, null)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(
                match { !it.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED) }
            )
        }
    }

    @Test
    fun `given updateWifiScan enabled then saveDetectionConfig with WIFI_SCAN enabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiScan(true, "OfficeNet")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(
                match { it.enabledMethods.contains(DetectionMethod.WIFI_SCAN) && it.wifiSsid == "OfficeNet" }
            )
        }
    }

    @Test
    fun `given updateWifiScan disabled then saveDetectionConfig without WIFI_SCAN`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiScan(false, null)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(
                match { !it.enabledMethods.contains(DetectionMethod.WIFI_SCAN) }
            )
        }
    }

    @Test
    fun `given updateGeofence enabled then saveDetectionConfig with GEOFENCE and coordinates`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(true, 51.5, -0.12, 200f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(
                match {
                    it.enabledMethods.contains(DetectionMethod.GEOFENCE) &&
                        it.geofenceLatitude == 51.5 &&
                        it.geofenceLongitude == -0.12 &&
                        it.geofenceRadiusMeters == 200f
                }
            )
        }
    }

    @Test
    fun `given updateGeofence disabled then saveDetectionConfig without GEOFENCE`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(false, null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(
                match { !it.enabledMethods.contains(DetectionMethod.GEOFENCE) }
            )
        }
    }

    @Test
    fun `given resetOnboarding called then saveOnboardingComplete false is invoked`() = runTest {
        val viewModel = createViewModel()

        viewModel.resetOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveOnboardingComplete(false) }
    }

    @Test
    fun `given resetOnboarding called then navigation event is NavigateToOnboarding`() = runTest {
        val viewModel = createViewModel()

        viewModel.resetOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        assertInstanceOf(
            SettingsNavigationEvent.NavigateToOnboarding::class.java,
            viewModel.navigationEvent.value
        )
    }

    @Test
    fun `given onNavigationHandled called then navigation event is cleared`() = runTest {
        val viewModel = createViewModel()

        viewModel.resetOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onNavigationHandled()

        assertNull(viewModel.navigationEvent.value)
    }

    @Test
    fun `given initial state when created then navigationEvent is null`() {
        val viewModel = createViewModel()
        assertNull(viewModel.navigationEvent.value)
    }

    @Test
    fun `given updateWifiConnected with blank ssid then ssid saved as null`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiConnected(true, "  ")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(match { it.wifiSsid == null })
        }
    }

    @Test
    fun `given updateGeofence enabled with coords then setupGeofence is called`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(true, 51.5, -0.12, 200f)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { geofenceDetector.setupGeofence(51.5, -0.12, 200f) }
    }

    @Test
    fun `given updateGeofence disabled then removeGeofence is called`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(false, null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { geofenceDetector.removeGeofence() }
    }

    @Test
    fun `given updateGeofence enabled but coords null then removeGeofence is called`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(true, null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { geofenceDetector.removeGeofence() }
    }

    @Test
    fun `given updateWifiScan with blank ssid then ssid saved as null`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiScan(true, "")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mandateConfigRepository.saveDetectionConfig(match { it.wifiSsid == null })
        }
    }

    @Test
    fun `given updateWifiConnected enabled then one-shot worker enqueued`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiConnected(true, "OfficeNet")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { workManager.enqueue(any<WorkRequest>()) }
    }

    @Test
    fun `given updateWifiConnected disabled then no one-shot worker enqueued`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiConnected(false, null)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { workManager.enqueue(any<WorkRequest>()) }
    }

    @Test
    fun `given updateWifiScan enabled then one-shot worker enqueued`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiScan(true, "OfficeNet")
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { workManager.enqueue(any<WorkRequest>()) }
    }

    @Test
    fun `given updateWifiScan disabled then no one-shot worker enqueued`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateWifiScan(false, null)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { workManager.enqueue(any<WorkRequest>()) }
    }

    @Test
    fun `given updateGeofence enabled with coords then one-shot worker enqueued`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(true, 51.5, -0.12, 200f)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { workManager.enqueue(any<WorkRequest>()) }
    }

    @Test
    fun `given updateGeofence disabled then no one-shot worker enqueued`() = runTest {
        val viewModel = createViewModel()

        viewModel.updateGeofence(false, null, null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { workManager.enqueue(any<WorkRequest>()) }
    }
}
