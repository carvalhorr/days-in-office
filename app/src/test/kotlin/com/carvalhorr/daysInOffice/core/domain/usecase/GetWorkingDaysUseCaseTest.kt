package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.Holiday
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class GetWorkingDaysUseCaseTest {

    private val holidayRepository: HolidayRepository = mockk()
    private lateinit var useCase: GetWorkingDaysUseCase

    @BeforeEach
    fun setUp() {
        useCase = GetWorkingDaysUseCase(holidayRepository)
    }

    @Test
    fun `given a week with no holidays when invoked then 5 working days returned`() = runTest {
        val start = LocalDate.of(2026, 5, 11) // Monday
        val end = LocalDate.of(2026, 5, 15)   // Friday
        every { holidayRepository.getHolidays(start, end) } returns flowOf(emptyList())

        val result = useCase(start, end)

        assertEquals(5, result.size)
    }

    @Test
    fun `given a week with one public holiday on Wednesday when invoked then 4 working days returned`() = runTest {
        val start = LocalDate.of(2026, 5, 11)
        val end = LocalDate.of(2026, 5, 15)
        val wednesday = LocalDate.of(2026, 5, 13)
        every { holidayRepository.getHolidays(start, end) } returns flowOf(
            listOf(Holiday(date = wednesday, name = "Test Holiday", isPublicHoliday = true, source = "MANUAL"))
        )

        val result = useCase(start, end)

        assertEquals(4, result.size)
        assertFalse(result.contains(wednesday))
    }

    @Test
    fun `given a range spanning a weekend when invoked then Saturday and Sunday excluded`() = runTest {
        val start = LocalDate.of(2026, 5, 11) // Monday
        val end = LocalDate.of(2026, 5, 17)   // Sunday
        every { holidayRepository.getHolidays(start, end) } returns flowOf(emptyList())

        val result = useCase(start, end)

        assertEquals(5, result.size)
        assertFalse(result.any { it.dayOfWeek == DayOfWeek.SATURDAY })
        assertFalse(result.any { it.dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test
    fun `given a week with one PTO day when invoked then that day excluded`() = runTest {
        val start = LocalDate.of(2026, 5, 11)
        val end = LocalDate.of(2026, 5, 15)
        val ptoDay = LocalDate.of(2026, 5, 12) // Tuesday
        every { holidayRepository.getHolidays(start, end) } returns flowOf(
            listOf(Holiday(date = ptoDay, name = "PTO", isPublicHoliday = false, source = "CALENDAR"))
        )

        val result = useCase(start, end)

        assertEquals(4, result.size)
        assertFalse(result.contains(ptoDay))
    }
}
