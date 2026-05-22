package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.data.datasource.CalendarDataSource
import com.carvalhorr.daysInOffice.core.domain.model.Holiday
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SyncCalendarUseCaseTest {

    private val mockCalendarDataSource = mockk<CalendarDataSource>()
    private val mockHolidayRepository = mockk<HolidayRepository>()
    private lateinit var useCase: SyncCalendarUseCase

    @BeforeEach
    fun setup() {
        useCase = SyncCalendarUseCase(mockCalendarDataSource, mockHolidayRepository)
    }

    @Test
    fun `given CalendarDataSource returns 5 holidays when invoke then repository clearAndReplace called with 5 holidays`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 12, 31)
        val holidays = List(5) { index ->
            Holiday(
                date = start.plusDays(index.toLong()),
                name = "Holiday $index",
                isPublicHoliday = index % 2 == 0,
                source = "CALENDAR"
            )
        }

        coEvery { mockCalendarDataSource.readHolidays(start, end) } returns holidays
        coEvery { mockHolidayRepository.clearAndReplace(holidays) } returns Unit

        val result = runBlocking { useCase(start, end) }

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
        coVerify(exactly = 1) { mockHolidayRepository.clearAndReplace(holidays) }
    }

    @Test
    fun `given CalendarDataSource throws exception when invoke then Result_failure returned`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 12, 31)
        val exception = RuntimeException("Calendar read failed")

        coEvery { mockCalendarDataSource.readHolidays(start, end) } throws exception

        val result = runBlocking { useCase(start, end) }

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 0) { mockHolidayRepository.clearAndReplace(any()) }
    }
}
