package com.carvalhorr.daysInOffice.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters as RoomTypeConverters
import com.carvalhorr.daysInOffice.core.data.db.converter.TypeConverters
import com.carvalhorr.daysInOffice.core.data.db.dao.DayRecordDao
import com.carvalhorr.daysInOffice.core.data.db.dao.HolidayDao
import com.carvalhorr.daysInOffice.core.data.db.entity.DayRecordEntity
import com.carvalhorr.daysInOffice.core.data.db.entity.HolidayEntity

@Database(
    entities = [DayRecordEntity::class, HolidayEntity::class],
    version = 1,
    exportSchema = false
)
@RoomTypeConverters(TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun holidayDao(): HolidayDao
}
