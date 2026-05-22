package com.carvalhorr.daysInOffice.feature.calendar

import app.cash.turbine.test
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.GetCalendarMonthUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getCalendarMonthUseCase: GetCalendarMonthUseCase = mockk()
    private val dayRecordRepository: DayRecordRepository = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupMocks(days: List<DayRecord> = emptyList()) {
        every { getCalendarMonthUseCase(any()) } returns flowOf(days)
    }

    private fun createViewModel() = CalendarViewModel(
        getCalendarMonthUseCase,
        dayRecordRepository
    )

    @Test
    fun `given initial state when created then state is Loading`() {
        setupMocks()
        val viewModel = createViewModel()
        assertInstanceOf(CalendarUiState.Loading::class.java, viewModel.state.value)
    }

    @Test
    fun `given month data when collected then state transitions to Success`() = runTest {
        setupMocks()
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem()
            assertInstanceOf(CalendarUiState.Success::class.java, success)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given Success state when collected then currentMonth is the current month`() = runTest {
        setupMocks()
        val viewModel = createViewModel()
        val expectedMonth = YearMonth.now()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as CalendarUiState.Success
            assertEquals(expectedMonth, success.currentMonth)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given Success state when goToPreviousMonth called then currentMonth decrements`() = runTest {
        setupMocks()
        val viewModel = createViewModel()
        val initialMonth = YearMonth.now()

        viewModel.state.test {
            awaitItem() // Loading
            val first = awaitItem() as CalendarUiState.Success
            assertEquals(initialMonth, first.currentMonth)

            viewModel.goToPreviousMonth()

            val second = awaitItem() as CalendarUiState.Success
            assertEquals(initialMonth.minusMonths(1), second.currentMonth)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given Success state when goToNextMonth called then currentMonth increments`() = runTest {
        setupMocks()
        val viewModel = createViewModel()
        val initialMonth = YearMonth.now()

        viewModel.state.test {
            awaitItem() // Loading
            val first = awaitItem() as CalendarUiState.Success
            assertEquals(initialMonth, first.currentMonth)

            viewModel.goToNextMonth()

            val second = awaitItem() as CalendarUiState.Success
            assertEquals(initialMonth.plusMonths(1), second.currentMonth)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given days loaded when selectDay called then selectedDay is set`() = runTest {
        val today = LocalDate.now()
        val record = DayRecord(today, DayStatus.OFFICE, DetectionMethod.MANUAL, false)
        setupMocks(listOf(record))
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            awaitItem() // Success with no selection

            viewModel.selectDay(today)

            val success = awaitItem() as CalendarUiState.Success
            assertEquals(record, success.selectedDay)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given no matching record when selectDay called then selectedDay remains null`() = runTest {
        setupMocks(emptyList())
        val viewModel = createViewModel()
        val today = LocalDate.now()

        viewModel.state.test {
            awaitItem() // Loading
            val success = awaitItem() as CalendarUiState.Success
            assertNull(success.selectedDay)

            viewModel.selectDay(today)
            testDispatcher.scheduler.advanceUntilIdle()

            // No new emission: state is equivalent (selectedDay still null), StateFlow deduplicates
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given selected day when dismissDay called then selectedDay is null`() = runTest {
        val today = LocalDate.now()
        val record = DayRecord(today, DayStatus.OFFICE, DetectionMethod.MANUAL, false)
        setupMocks(listOf(record))
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            awaitItem() // Success

            viewModel.selectDay(today)
            awaitItem() // Success with selection

            viewModel.dismissDay()

            val success = awaitItem() as CalendarUiState.Success
            assertNull(success.selectedDay)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given day when overrideStatus with OFFICE called then upsertDayRecord called with confirmedByUser true`() = runTest {
        val today = LocalDate.now()
        setupMocks()
        coJustRun { dayRecordRepository.upsertDayRecord(any()) }
        val viewModel = createViewModel()

        viewModel.overrideStatus(today, DayStatus.OFFICE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            dayRecordRepository.upsertDayRecord(
                match { it.date == today && it.status == DayStatus.OFFICE && it.confirmedByUser }
            )
        }
    }

    @Test
    fun `given day when overrideStatus with REMOTE called then record has MANUAL detectionMethod`() = runTest {
        val today = LocalDate.now()
        setupMocks()
        coJustRun { dayRecordRepository.upsertDayRecord(any()) }
        val viewModel = createViewModel()

        viewModel.overrideStatus(today, DayStatus.REMOTE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            dayRecordRepository.upsertDayRecord(
                match {
                    it.date == today && it.status == DayStatus.REMOTE &&
                        it.confirmedByUser && it.detectionMethod == DetectionMethod.MANUAL
                }
            )
        }
    }

    @Test
    fun `given day when overrideStatus with PTO called then record has null detectionMethod`() = runTest {
        val today = LocalDate.now()
        setupMocks()
        coJustRun { dayRecordRepository.upsertDayRecord(any()) }
        val viewModel = createViewModel()

        viewModel.overrideStatus(today, DayStatus.PTO)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            dayRecordRepository.upsertDayRecord(
                match {
                    it.date == today && it.status == DayStatus.PTO &&
                        it.confirmedByUser && it.detectionMethod == null
                }
            )
        }
    }

    @Test
    fun `given selected day when overrideStatus called then selectedDay is dismissed`() = runTest {
        val today = LocalDate.now()
        val record = DayRecord(today, DayStatus.UNKNOWN, null, false)
        setupMocks(listOf(record))
        coJustRun { dayRecordRepository.upsertDayRecord(any()) }
        val viewModel = createViewModel()

        viewModel.state.test {
            awaitItem() // Loading
            awaitItem() // Success

            viewModel.selectDay(today)
            awaitItem() // Success with selection

            viewModel.overrideStatus(today, DayStatus.OFFICE)
            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = awaitItem() as CalendarUiState.Success
            assertNull(finalState.selectedDay)
            cancelAndConsumeRemainingEvents()
        }
    }
}
