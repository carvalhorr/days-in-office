package com.carvalhorr.daysInOffice.core.domain.model

import java.time.LocalDate

data class Holiday(
    val date: LocalDate,
    val name: String,
    val isPublicHoliday: Boolean,
    val source: String
)
