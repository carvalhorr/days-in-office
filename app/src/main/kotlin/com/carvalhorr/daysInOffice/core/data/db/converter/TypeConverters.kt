package com.carvalhorr.daysInOffice.core.data.db.converter

import androidx.room.TypeConverter
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import java.time.Instant
import java.time.LocalDate

class TypeConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromDayStatus(value: DayStatus?): String? = value?.name

    @TypeConverter
    fun toDayStatus(value: String?): DayStatus? = value?.let { DayStatus.valueOf(it) }

    @TypeConverter
    fun fromDetectionMethod(value: DetectionMethod?): String? = value?.name

    @TypeConverter
    fun toDetectionMethod(value: String?): DetectionMethod? = value?.let { DetectionMethod.valueOf(it) }
}
