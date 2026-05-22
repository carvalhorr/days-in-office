package com.carvalhorr.daysInOffice.feature.onboarding

import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mandateConfigRepository: MandateConfigRepository = mockk()
    private lateinit var viewModel: OnboardingViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OnboardingViewModel(mandateConfigRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given initial state when created then step is 0`() {
        assertEquals(0, viewModel.state.value.currentStep)
    }

    @Test
    fun `given initial state when created then mandate config has 50 percent target`() {
        assertEquals(0.5f, viewModel.state.value.mandateConfig.targetPercentage)
    }

    @Test
    fun `given initial state when created then mandate config period is monthly`() {
        assertEquals(MandatePeriod.MONTHLY, viewModel.state.value.mandateConfig.period)
    }

    @Test
    fun `given initial state when created then working days are mon to fri`() {
        val workingDays = viewModel.state.value.mandateConfig.workingDays
        assertTrue(workingDays.containsAll(setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )))
        assertFalse(workingDays.contains(DayOfWeek.SATURDAY))
        assertFalse(workingDays.contains(DayOfWeek.SUNDAY))
    }

    @Test
    fun `given initial state when created then detection methods are empty`() {
        assertTrue(viewModel.state.value.detectionConfig.enabledMethods.isEmpty())
    }

    @Test
    fun `given initial state when created then calendar sync is disabled`() {
        assertFalse(viewModel.state.value.calendarSyncEnabled)
    }

    @Test
    fun `given initial state when created then no navigation event`() {
        assertNull(viewModel.state.value.navigationEvent)
    }

    @Test
    fun `given step 0 when next called then step advances to 1`() {
        viewModel.next()
        assertEquals(1, viewModel.state.value.currentStep)
    }

    @Test
    fun `given step 0 when back called then step stays at 0`() {
        viewModel.back()
        assertEquals(0, viewModel.state.value.currentStep)
    }

    @Test
    fun `given step 1 when back called then step retreats to 0`() {
        viewModel.next()
        viewModel.back()
        assertEquals(0, viewModel.state.value.currentStep)
    }

    @Test
    fun `given step 2 when back called then step retreats to 1`() {
        viewModel.next()
        viewModel.next()
        viewModel.back()
        assertEquals(1, viewModel.state.value.currentStep)
    }

    @Test
    fun `given last step when next called then step does not exceed max`() {
        repeat(20) { viewModel.next() }
        assertEquals(OnboardingViewModel.TOTAL_STEPS - 1, viewModel.state.value.currentStep)
    }

    @Test
    fun `given updateMandatePercentage called when 0_75 then state updates`() {
        viewModel.updateMandatePercentage(0.75f)
        assertEquals(0.75f, viewModel.state.value.mandateConfig.targetPercentage)
    }

    @Test
    fun `given updateWorkingDays called when new set provided then state updates`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        viewModel.updateWorkingDays(days)
        assertEquals(days, viewModel.state.value.mandateConfig.workingDays)
    }

    @Test
    fun `given updatePeriod called when WEEKLY then state updates`() {
        viewModel.updatePeriod(MandatePeriod.WEEKLY)
        assertEquals(MandatePeriod.WEEKLY, viewModel.state.value.mandateConfig.period)
    }

    @Test
    fun `given updatePeriod called when QUARTERLY then state updates`() {
        viewModel.updatePeriod(MandatePeriod.QUARTERLY)
        assertEquals(MandatePeriod.QUARTERLY, viewModel.state.value.mandateConfig.period)
    }

    @Test
    fun `given toggleDetectionMethod called when method not present then method added`() {
        viewModel.toggleDetectionMethod(DetectionMethod.WIFI_CONNECTED)
        assertTrue(viewModel.state.value.detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED))
    }

    @Test
    fun `given toggleDetectionMethod called when method already present then method removed`() {
        viewModel.toggleDetectionMethod(DetectionMethod.WIFI_CONNECTED)
        viewModel.toggleDetectionMethod(DetectionMethod.WIFI_CONNECTED)
        assertFalse(viewModel.state.value.detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED))
    }

    @Test
    fun `given toggleDetectionMethod called multiple times then only toggled method changes`() {
        viewModel.toggleDetectionMethod(DetectionMethod.WIFI_CONNECTED)
        viewModel.toggleDetectionMethod(DetectionMethod.GEOFENCE)
        val methods = viewModel.state.value.detectionConfig.enabledMethods
        assertTrue(methods.contains(DetectionMethod.WIFI_CONNECTED))
        assertTrue(methods.contains(DetectionMethod.GEOFENCE))
    }

    @Test
    fun `given updateWifiSsid called when ssid provided then state updates`() {
        viewModel.updateWifiSsid("OfficeNetwork")
        assertEquals("OfficeNetwork", viewModel.state.value.detectionConfig.wifiSsid)
    }

    @Test
    fun `given updateWifiSsid called when blank string then ssid set to null`() {
        viewModel.updateWifiSsid("OfficeNetwork")
        viewModel.updateWifiSsid("")
        assertNull(viewModel.state.value.detectionConfig.wifiSsid)
    }

    @Test
    fun `given updateGeofence called when values provided then state updates`() {
        viewModel.updateGeofence(51.5, -0.12, 200f)
        val config = viewModel.state.value.detectionConfig
        assertEquals(51.5, config.geofenceLatitude)
        assertEquals(-0.12, config.geofenceLongitude)
        assertEquals(200f, config.geofenceRadiusMeters)
    }

    @Test
    fun `given updateCalendarSync called when enabled then state is true`() {
        viewModel.updateCalendarSync(true)
        assertTrue(viewModel.state.value.calendarSyncEnabled)
    }

    @Test
    fun `given updateCalendarSync called when disabled then state is false`() {
        viewModel.updateCalendarSync(true)
        viewModel.updateCalendarSync(false)
        assertFalse(viewModel.state.value.calendarSyncEnabled)
    }

    @Test
    fun `given complete called then saveMandateConfig is invoked`() = runTest {
        coJustRun { mandateConfigRepository.saveMandateConfig(any()) }
        coJustRun { mandateConfigRepository.saveDetectionConfig(any()) }
        coJustRun { mandateConfigRepository.saveCalendarSyncEnabled(any()) }
        coJustRun { mandateConfigRepository.saveOnboardingComplete(any()) }

        viewModel.complete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveMandateConfig(any()) }
    }

    @Test
    fun `given complete called then saveDetectionConfig is invoked`() = runTest {
        coJustRun { mandateConfigRepository.saveMandateConfig(any()) }
        coJustRun { mandateConfigRepository.saveDetectionConfig(any()) }
        coJustRun { mandateConfigRepository.saveCalendarSyncEnabled(any()) }
        coJustRun { mandateConfigRepository.saveOnboardingComplete(any()) }

        viewModel.complete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveDetectionConfig(any()) }
    }

    @Test
    fun `given complete called then saveOnboardingComplete true is invoked`() = runTest {
        coJustRun { mandateConfigRepository.saveMandateConfig(any()) }
        coJustRun { mandateConfigRepository.saveDetectionConfig(any()) }
        coJustRun { mandateConfigRepository.saveCalendarSyncEnabled(any()) }
        coJustRun { mandateConfigRepository.saveOnboardingComplete(any()) }

        viewModel.complete()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mandateConfigRepository.saveOnboardingComplete(true) }
    }

    @Test
    fun `given complete called then navigation event emitted`() = runTest {
        coJustRun { mandateConfigRepository.saveMandateConfig(any()) }
        coJustRun { mandateConfigRepository.saveDetectionConfig(any()) }
        coJustRun { mandateConfigRepository.saveCalendarSyncEnabled(any()) }
        coJustRun { mandateConfigRepository.saveOnboardingComplete(any()) }

        viewModel.complete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertInstanceOf(
            OnboardingNavigationEvent.NavigateToDashboard::class.java,
            viewModel.state.value.navigationEvent
        )
    }

    @Test
    fun `given navigation event when onNavigationHandled called then event cleared`() = runTest {
        coJustRun { mandateConfigRepository.saveMandateConfig(any()) }
        coJustRun { mandateConfigRepository.saveDetectionConfig(any()) }
        coJustRun { mandateConfigRepository.saveCalendarSyncEnabled(any()) }
        coJustRun { mandateConfigRepository.saveOnboardingComplete(any()) }

        viewModel.complete()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onNavigationHandled()

        assertNull(viewModel.state.value.navigationEvent)
    }
}
