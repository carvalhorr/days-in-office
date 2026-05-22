package com.carvalhorr.daysInOffice.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.carvalhorr.daysInOffice.core.data.db.entity.HolidayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays WHERE date >= :start AND date <= :end ORDER BY date ASC")
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<HolidayEntity>>

    @Upsert
    suspend fun upsert(holiday: HolidayEntity)

    @Query("DELETE FROM holidays WHERE date >= :start AND date <= :end")
    suspend fun deleteByDateRange(start: LocalDate, end: LocalDate)

    @Query("DELETE FROM holidays")
    suspend fun deleteAll()
}
