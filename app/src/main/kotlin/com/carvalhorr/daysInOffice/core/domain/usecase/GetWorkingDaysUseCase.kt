package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class GetWorkingDaysUseCase @Inject constructor(
    private val holidayRepository: HolidayRepository
) {
    suspend operator fun invoke(
        start: LocalDate,
        end: LocalDate,
        workingDays: Set<DayOfWeek>
    ): List<LocalDate> {
        val holidays = holidayRepository.getHolidays(start, end).first()
        val holidayDates = holidays.map { it.date }.toSet()
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .filter { it.dayOfWeek in workingDays }
            .filter { it !in holidayDates }
            .toList()
    }
}
