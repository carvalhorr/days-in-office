package com.carvalhorr.daysInOffice.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.carvalhorr.daysInOffice.core.data.db.entity.DayRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records WHERE date >= :start AND date <= :end ORDER BY date ASC")
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<DayRecordEntity>>

    @Query("SELECT * FROM day_records WHERE date = :date LIMIT 1")
    fun getByDate(date: LocalDate): Flow<DayRecordEntity?>

    @Upsert
    suspend fun upsert(record: DayRecordEntity)

    @Query("DELETE FROM day_records WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
