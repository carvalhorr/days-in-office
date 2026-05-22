package com.carvalhorr.daysInOffice.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "day_records")
data class DayRecordEntity(
    @PrimaryKey val date: LocalDate,
    val status: DayStatus,
    val detectionMethod: DetectionMethod?,
    val confirmedByUser: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
