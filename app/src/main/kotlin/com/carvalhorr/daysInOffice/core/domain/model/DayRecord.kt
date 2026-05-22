package com.carvalhorr.daysInOffice.core.domain.model

import java.time.LocalDate

data class DayRecord(
    val date: LocalDate,
    val status: DayStatus,
    val detectionMethod: DetectionMethod?,
    val confirmedByUser: Boolean
)
