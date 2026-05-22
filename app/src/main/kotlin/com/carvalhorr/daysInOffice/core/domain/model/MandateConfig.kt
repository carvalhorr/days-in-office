package com.carvalhorr.daysInOffice.core.domain.model

import java.time.DayOfWeek

data class MandateConfig(
    val targetPercentage: Float,
    val period: MandatePeriod,
    val workingDays: Set<DayOfWeek>
)
