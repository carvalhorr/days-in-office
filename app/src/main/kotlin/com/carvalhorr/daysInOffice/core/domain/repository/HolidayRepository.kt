package com.carvalhorr.daysInOffice.core.domain.repository

import com.carvalhorr.daysInOffice.core.domain.model.Holiday
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HolidayRepository {
    fun getHolidays(start: LocalDate, end: LocalDate): Flow<List<Holiday>>
    suspend fun upsertHoliday(holiday: Holiday)
    suspend fun syncFromCalendar(start: LocalDate, end: LocalDate)
    suspend fun clearAndReplace(holidays: List<Holiday>)
}
