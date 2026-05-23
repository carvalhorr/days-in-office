package com.carvalhorr.daysInOffice.feature.dashboard

import app.cash.turbine.test
import com.carvalhorr.daysInOffice.core.domain.model.ComplianceResult
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.GetComplianceUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordRemoteDayUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getComplianceUseCase: GetComplianceUseCase = mockk()
    private val recordOfficeDayUseCase: RecordOfficeDayUseCase = mockk()
    private val recordRemoteDayUseCase: RecordRemoteDayUseCase = mockk()
    private val dayRecordRepository: DayRecordRepository = mockk()
    private val mandateConfigRepository: MandateConfigRepository = mockk()

    private val defaultCompliance = ComplianceResult(
        periodStart = LocalDate.of(2026, 5, 1),
        periodEnd = LocalDate.of(2026, 5, 31),
        totalWorkingDays = 21,
        officeDays = 10,
        remoteDays = 5,
        unknownDays = 6,
        targetPercentage = 0.5f
    )

    private val defaultMandateConfig = MandateConfig(
        targetPercentage = 0.5f,
        period = MandatePeriod.MONTHLY,
        workingDays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        )
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupMocks(todayRecord: DayRecord? = null) {
        every { getComplianceUseCase() } returns flowOf(defaultCompliance)
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(defaultMandateConfig)
        every { dayRecordRepository.getDayRecord(any()) } returns flowOf(todayRecord)
    }

    private fun createViewModel() = DashboardViewModel(
        getComplianceUseCase,
        recordOfficeDayUseCase,
        recordRemoteDayUseCase,
        dayRecordRepository,
        mandateConfigRepository
    )

    @Test
    fun `given initial state when created then state is Loading`() {
        setupMocks()
        val viewModel = createViewModel()
        assertInstanceOf(DashboardUiState.Loading::class.java, viewModel.state.value)
    }

    @Test
    fun `given compliance flow when collected then state transitions to Success`() = runTest {
        setupMocks()
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem()
            assertInstanceOf(DashboardUiState.Success::class.java, success)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given compliance data when state is Success then complianceResult matches`() = runTest {
        setupMocks()
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as DashboardUiState.Success
            assertEquals(defaultCompliance, success.complianceResult)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given mandate config when state is Success then mandatePeriod is MONTHLY`() = runTest {
        setupMocks()
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as DashboardUiState.Success
            assertEquals(MandatePeriod.MONTHLY, success.mandatePeriod)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given no today record when state is Success then todayRecord is null`() = runTest {
        setupMocks(todayRecord = null)
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as DashboardUiState.Success
            assertNull(success.todayRecord)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given today record exists when state is Success then todayRecord is present`() = runTest {
        val record = DayRecord(
            date = LocalDate.now(),
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = false
        )
        setupMocks(todayRecord = record)
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as DashboardUiState.Success
            assertEquals(record, success.todayRecord)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given checkInAsOffice called when invoked then RecordOfficeDayUseCase called with MANUAL`() = runTest {
        setupMocks()
        coJustRun { recordOfficeDayUseCase(any(), any()) }
        val viewModel = createViewModel()

        viewModel.checkInAsOffice()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recordOfficeDayUseCase(any(), DetectionMethod.MANUAL) }
    }

    @Test
    fun `given checkInAsRemote called when invoked then RecordRemoteDayUseCase is called`() = runTest {
        setupMocks()
        coJustRun { recordRemoteDayUseCase(any()) }
        val viewModel = createViewModel()

        viewModel.checkInAsRemote()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recordRemoteDayUseCase(any()) }
    }

    @Test
    fun `given today record with confirmedByUser true when state collected then todayRecord shows confirmed`() = runTest {
        val confirmedRecord = DayRecord(
            date = LocalDate.now(),
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = true
        )
        setupMocks(todayRecord = confirmedRecord)
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as DashboardUiState.Success
            assertTrue(success.todayRecord?.confirmedByUser == true)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given today record with confirmedByUser true when state collected then confirmedByUser is exposed`() = runTest {
        val confirmedRecord = DayRecord(
            date = LocalDate.now(),
            status = DayStatus.REMOTE,
            detectionMethod = null,
            confirmedByUser = true
        )
        setupMocks(todayRecord = confirmedRecord)
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as DashboardUiState.Success
            assertEquals(true, success.todayRecord?.confirmedByUser)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given office check-in when switching to remote then recordRemoteDayUseCase is called`() = runTest {
        setupMocks()
        coJustRun { recordOfficeDayUseCase(any(), any()) }
        coJustRun { recordRemoteDayUseCase(any()) }
        val viewModel = createViewModel()

        viewModel.checkInAsOffice()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.checkInAsRemote()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recordOfficeDayUseCase(any(), DetectionMethod.MANUAL) }
        coVerify { recordRemoteDayUseCase(any()) }
    }

    @Test
    fun `given remote check-in when switching to office then recordOfficeDayUseCase is called`() = runTest {
        setupMocks()
        coJustRun { recordOfficeDayUseCase(any(), any()) }
        coJustRun { recordRemoteDayUseCase(any()) }
        val viewModel = createViewModel()

        viewModel.checkInAsRemote()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.checkInAsOffice()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recordRemoteDayUseCase(any()) }
        coVerify { recordOfficeDayUseCase(any(), DetectionMethod.MANUAL) }
    }

    @Test
    fun `given office check-in when switching remote then back to office then all three use-case calls are made`() = runTest {
        setupMocks()
        coJustRun { recordOfficeDayUseCase(any(), any()) }
        coJustRun { recordRemoteDayUseCase(any()) }
        val viewModel = createViewModel()

        viewModel.checkInAsOffice()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.checkInAsRemote()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.checkInAsOffice()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { recordOfficeDayUseCase(any(), DetectionMethod.MANUAL) }
        coVerify(exactly = 1) { recordRemoteDayUseCase(any()) }
    }

    @Test
    fun `given checkInAsOffice when invoked then state Flow emits new value with OFFICE status`() = runTest {
        val todayFlow = MutableStateFlow<DayRecord?>(null)
        val officeRecord = DayRecord(
            date = LocalDate.now(),
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = false
        )
        every { getComplianceUseCase() } returns flowOf(defaultCompliance)
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(defaultMandateConfig)
        every { dayRecordRepository.getDayRecord(any()) } returns todayFlow
        coEvery { recordOfficeDayUseCase(any(), any()) } coAnswers { todayFlow.value = officeRecord }
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val initial = awaitItem() as DashboardUiState.Success
            assertNull(initial.todayRecord)

            viewModel.checkInAsOffice()
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem() as DashboardUiState.Success
            assertEquals(DayStatus.OFFICE, updated.todayRecord?.status)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given checkInAsRemote when invoked then state Flow emits new value with REMOTE status`() = runTest {
        val todayFlow = MutableStateFlow<DayRecord?>(null)
        val remoteRecord = DayRecord(
            date = LocalDate.now(),
            status = DayStatus.REMOTE,
            detectionMethod = null,
            confirmedByUser = true
        )
        every { getComplianceUseCase() } returns flowOf(defaultCompliance)
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(defaultMandateConfig)
        every { dayRecordRepository.getDayRecord(any()) } returns todayFlow
        coEvery { recordRemoteDayUseCase(any()) } coAnswers { todayFlow.value = remoteRecord }
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val initial = awaitItem() as DashboardUiState.Success
            assertNull(initial.todayRecord)

            viewModel.checkInAsRemote()
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem() as DashboardUiState.Success
            assertEquals(DayStatus.REMOTE, updated.todayRecord?.status)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given use case throws when checkInAsOffice then snackbarMessage emits error`() = runTest {
        setupMocks()
        coEvery { recordOfficeDayUseCase(any(), any()) } throws RuntimeException("office check-in failed")
        val viewModel = createViewModel()

        viewModel.snackbarMessage.test {
            viewModel.checkInAsOffice()
            testDispatcher.scheduler.advanceUntilIdle()
            val message = awaitItem()
            assertTrue(message.isNotEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given use case throws when checkInAsRemote then snackbarMessage emits error`() = runTest {
        setupMocks()
        coEvery { recordRemoteDayUseCase(any()) } throws RuntimeException("remote check-in failed")
        val viewModel = createViewModel()

        viewModel.snackbarMessage.test {
            viewModel.checkInAsRemote()
            testDispatcher.scheduler.advanceUntilIdle()
            val message = awaitItem()
            assertTrue(message.isNotEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }
}
