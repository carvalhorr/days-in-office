package com.carvalhorr.daysInOffice.core.domain.repository

import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DayRecordRepository {
    fun getDayRecords(start: LocalDate, end: LocalDate): Flow<List<DayRecord>>
    fun getDayRecord(date: LocalDate): Flow<DayRecord?>
    suspend fun upsertDayRecord(record: DayRecord)
    suspend fun deleteDayRecord(date: LocalDate)
}
