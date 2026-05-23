package com.carvalhorr.daysInOffice.core.domain.usecase

import app.cash.turbine.test
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GetComplianceUseCaseTest {

    private val mandateConfigRepository: MandateConfigRepository = mockk()
    private val dayRecordRepository: DayRecordRepository = mockk()
    private val getWorkingDaysUseCase: GetWorkingDaysUseCase = mockk()
    private lateinit var useCase: GetComplianceUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetComplianceUseCase(mandateConfigRepository, dayRecordRepository, getWorkingDaysUseCase)
    }

    private fun weekdays() = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    )

    @Test
    fun `given monthly period and 10 working days and 5 office records when invoked then currentPercentage=0_5 and isCompliant=true`() = runTest {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.MONTHLY,
            workingDays = weekdays()
        )
        val workingDays = (1..10).map { LocalDate.of(2026, 5, it) }
        val officeRecords = workingDays.take(5).map { date ->
            DayRecord(date = date, status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false)
        }
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(officeRecords)
        coEvery { getWorkingDaysUseCase(any(), any()) } returns workingDays

        useCase().test {
            val result = awaitItem()
            assertEquals(0.5f, result.currentPercentage, 0.001f)
            assertTrue(result.isCompliant)
            awaitComplete()
        }
    }

    @Test
    fun `given monthly period and 10 working days and 4 office records when invoked then daysNeededToComply=1`() = runTest {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.MONTHLY,
            workingDays = weekdays()
        )
        val workingDays = (1..10).map { LocalDate.of(2026, 5, it) }
        val officeRecords = workingDays.take(4).map { date ->
            DayRecord(date = date, status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false)
        }
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(officeRecords)
        coEvery { getWorkingDaysUseCase(any(), any()) } returns workingDays

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.daysNeededToComply)
            awaitComplete()
        }
    }

    @Test
    fun `quarterly bounds with default (January) fiscal year start match calendar quarters`() {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.QUARTERLY,
            workingDays = weekdays()
        )
        // Feb 15 2026 → Q1 2026 (Jan-Mar)
        val (q1Start, q1End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 2, 15))
        assertEquals(LocalDate.of(2026, 1, 1), q1Start)
        assertEquals(LocalDate.of(2026, 3, 31), q1End)
        // May 1 2026 → Q2 (Apr-Jun)
        val (q2Start, q2End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 5, 1))
        assertEquals(LocalDate.of(2026, 4, 1), q2Start)
        assertEquals(LocalDate.of(2026, 6, 30), q2End)
        // Dec 31 2026 → Q4 (Oct-Dec)
        val (q4Start, q4End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 12, 31))
        assertEquals(LocalDate.of(2026, 10, 1), q4Start)
        assertEquals(LocalDate.of(2026, 12, 31), q4End)
    }

    @Test
    fun `quarterly bounds with April fiscal year start give Apr-Jun, Jul-Sep, Oct-Dec, Jan-Mar quarters`() {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.QUARTERLY,
            workingDays = weekdays(),
            fiscalYearStartMonth = 4
        )
        // May 2026 → Q1 of FY (Apr 2026 - Jun 2026)
        val (q1Start, q1End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 5, 15))
        assertEquals(LocalDate.of(2026, 4, 1), q1Start)
        assertEquals(LocalDate.of(2026, 6, 30), q1End)
        // Aug 2026 → Q2 (Jul-Sep)
        val (q2Start, q2End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 8, 1))
        assertEquals(LocalDate.of(2026, 7, 1), q2Start)
        assertEquals(LocalDate.of(2026, 9, 30), q2End)
        // Feb 2026 → Q4 of previous FY (Jan-Mar 2026)
        val (q4Start, q4End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 2, 15))
        assertEquals(LocalDate.of(2026, 1, 1), q4Start)
        assertEquals(LocalDate.of(2026, 3, 31), q4End)
    }

    @Test
    fun `quarterly bounds with October fiscal year start give Oct-Dec, Jan-Mar, Apr-Jun, Jul-Sep quarters`() {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.QUARTERLY,
            workingDays = weekdays(),
            fiscalYearStartMonth = 10
        )
        // Nov 2025 → Q1 of FY26 (Oct-Dec 2025)
        val (q1Start, q1End) = useCase.getPeriodBounds(config, LocalDate.of(2025, 11, 15))
        assertEquals(LocalDate.of(2025, 10, 1), q1Start)
        assertEquals(LocalDate.of(2025, 12, 31), q1End)
        // Feb 2026 → Q2 (Jan-Mar 2026)
        val (q2Start, q2End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 2, 1))
        assertEquals(LocalDate.of(2026, 1, 1), q2Start)
        assertEquals(LocalDate.of(2026, 3, 31), q2End)
        // Aug 2026 → Q4 (Jul-Sep 2026)
        val (q4Start, q4End) = useCase.getPeriodBounds(config, LocalDate.of(2026, 8, 15))
        assertEquals(LocalDate.of(2026, 7, 1), q4Start)
        assertEquals(LocalDate.of(2026, 9, 30), q4End)
    }

    @Test
    fun `given quarterly period when invoked then period spans correct calendar quarter`() = runTest {
        val today = LocalDate.now()
        val quarter = (today.monthValue - 1) / 3
        val startMonth = quarter * 3 + 1
        val expectedStart = LocalDate.of(today.year, startMonth, 1)
        val expectedEnd = expectedStart.plusMonths(3).minusDays(1)

        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.QUARTERLY,
            workingDays = weekdays()
        )
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(emptyList())
        coEvery { getWorkingDaysUseCase(any(), any()) } returns emptyList()

        useCase().test {
            val result = awaitItem()
            assertEquals(expectedStart, result.periodStart)
            assertEquals(expectedEnd, result.periodEnd)
            awaitComplete()
        }
    }

    @Test
    fun `given rolling 4 weeks when invoked then period spans last 28 days from today`() = runTest {
        val today = LocalDate.now()
        val expectedStart = today.minusDays(27)
        val expectedEnd = today

        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.ROLLING_4_WEEKS,
            workingDays = weekdays()
        )
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(emptyList())
        coEvery { getWorkingDaysUseCase(any(), any()) } returns emptyList()

        useCase().test {
            val result = awaitItem()
            assertEquals(expectedStart, result.periodStart)
            assertEquals(expectedEnd, result.periodEnd)
            awaitComplete()
        }
    }

    @Test
    fun `given a week with a holiday when invoked then holiday excluded from totalWorkingDays`() = runTest {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.WEEKLY,
            workingDays = weekdays()
        )
        // 4 working days because Wednesday is a holiday
        val workingDays = listOf(
            LocalDate.of(2026, 5, 11),
            LocalDate.of(2026, 5, 12),
            LocalDate.of(2026, 5, 14),
            LocalDate.of(2026, 5, 15)
        )
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(emptyList())
        coEvery { getWorkingDaysUseCase(any(), any()) } returns workingDays

        useCase().test {
            val result = awaitItem()
            assertEquals(4, result.totalWorkingDays)
            awaitComplete()
        }
    }

    @Test
    fun `given 10 working days with 2 marked PTO when invoked then totalWorkingDays equals 8`() = runTest {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.MONTHLY,
            workingDays = weekdays()
        )
        val workingDays = (1..10).map { LocalDate.of(2026, 5, it) }
        val records = listOf(
            DayRecord(date = workingDays[0], status = DayStatus.PTO, detectionMethod = null, confirmedByUser = true),
            DayRecord(date = workingDays[1], status = DayStatus.PTO, detectionMethod = null, confirmedByUser = true)
        )
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(records)
        coEvery { getWorkingDaysUseCase(any(), any()) } returns workingDays

        useCase().test {
            val result = awaitItem()
            assertEquals(8, result.totalWorkingDays)
            awaitComplete()
        }
    }

    @Test
    fun `given 10 working days 5 OFFICE and 2 PTO when invoked then currentPercentage computes against denominator 8`() = runTest {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.MONTHLY,
            workingDays = weekdays()
        )
        val workingDays = (1..10).map { LocalDate.of(2026, 5, it) }
        val records = listOf(
            DayRecord(date = workingDays[0], status = DayStatus.PTO, detectionMethod = null, confirmedByUser = true),
            DayRecord(date = workingDays[1], status = DayStatus.PTO, detectionMethod = null, confirmedByUser = true),
            DayRecord(date = workingDays[2], status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false),
            DayRecord(date = workingDays[3], status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false),
            DayRecord(date = workingDays[4], status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false),
            DayRecord(date = workingDays[5], status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false),
            DayRecord(date = workingDays[6], status = DayStatus.OFFICE, detectionMethod = null, confirmedByUser = false)
        )
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(records)
        coEvery { getWorkingDaysUseCase(any(), any()) } returns workingDays

        useCase().test {
            val result = awaitItem()
            assertEquals(8, result.totalWorkingDays)
            assertEquals(5, result.officeDays)
            assertEquals(5f / 8f, result.currentPercentage, 0.001f)
            awaitComplete()
        }
    }

    @Test
    fun `given 10 working days with 2 marked HOLIDAY when invoked then totalWorkingDays equals 8`() = runTest {
        val config = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.MONTHLY,
            workingDays = weekdays()
        )
        val workingDays = (1..10).map { LocalDate.of(2026, 5, it) }
        val records = listOf(
            DayRecord(date = workingDays[0], status = DayStatus.HOLIDAY, detectionMethod = null, confirmedByUser = false),
            DayRecord(date = workingDays[1], status = DayStatus.HOLIDAY, detectionMethod = null, confirmedByUser = false)
        )
        every { mandateConfigRepository.getMandateConfig() } returns flowOf(config)
        every { dayRecordRepository.getDayRecords(any(), any()) } returns flowOf(records)
        coEvery { getWorkingDaysUseCase(any(), any()) } returns workingDays

        useCase().test {
            val result = awaitItem()
            assertEquals(8, result.totalWorkingDays)
            awaitComplete()
        }
    }

    @Test
    fun `given day with confirmedByUser=true when automated detection invoked then record unchanged`() = runTest {
        val repo: DayRecordRepository = mockk()
        val date = LocalDate.of(2026, 5, 15)
        val confirmedRecord = DayRecord(
            date = date,
            status = DayStatus.REMOTE,
            detectionMethod = null,
            confirmedByUser = true
        )
        every { repo.getDayRecord(date) } returns flowOf(confirmedRecord)

        val recordOfficeDayUseCase = RecordOfficeDayUseCase(repo)
        recordOfficeDayUseCase(date, DetectionMethod.WIFI_CONNECTED)

        coVerify(exactly = 0) { repo.upsertDayRecord(any()) }
    }
}
