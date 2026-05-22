package com.carvalhorr.daysInOffice.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val date: LocalDate,
    val name: String,
    val isPublicHoliday: Boolean,
    val source: String
)
