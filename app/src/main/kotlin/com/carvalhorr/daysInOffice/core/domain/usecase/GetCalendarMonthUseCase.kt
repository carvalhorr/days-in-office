package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.YearMonth
import javax.inject.Inject

class GetCalendarMonthUseCase @Inject constructor(
    private val dayRecordRepository: DayRecordRepository,
    private val holidayRepository: HolidayRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<DayRecord>> {
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()

        return combine(
            dayRecordRepository.getDayRecords(start, end),
            holidayRepository.getHolidays(start, end)
        ) { records, holidays ->
            val recordMap = records.associateBy { it.date }
            val holidayMap = holidays.associateBy { it.date }

            generateSequence(start) { it.plusDays(1) }
                .takeWhile { !it.isAfter(end) }
                .map { date ->
                    recordMap[date] ?: run {
                        val holiday = holidayMap[date]
                        val status = when {
                            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY ->
                                DayStatus.WEEKEND
                            holiday != null ->
                                if (holiday.isPublicHoliday) DayStatus.HOLIDAY else DayStatus.PTO
                            else -> DayStatus.UNKNOWN
                        }
                        DayRecord(
                            date = date,
                            status = status,
                            detectionMethod = null,
                            confirmedByUser = false
                        )
                    }
                }
                .toList()
        }
    }
}
