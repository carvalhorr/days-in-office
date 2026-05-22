package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.data.datasource.CalendarDataSource
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class SyncCalendarUseCase @Inject constructor(
    private val calendarDataSource: CalendarDataSource,
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(
        start: LocalDate = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()),
        end: LocalDate = LocalDate.now().with(TemporalAdjusters.lastDayOfYear())
    ): Result<Int> = runCatching {
        val holidays = calendarDataSource.readHolidays(start, end)
        holidayRepository.clearAndReplace(holidays)
        holidays.size
    }
}
