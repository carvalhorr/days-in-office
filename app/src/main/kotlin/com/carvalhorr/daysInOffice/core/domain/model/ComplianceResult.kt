package com.carvalhorr.daysInOffice.core.domain.model

import java.time.LocalDate
import kotlin.math.ceil

data class ComplianceResult(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalWorkingDays: Int,
    val officeDays: Int,
    val remoteDays: Int,
    val unknownDays: Int,
    val targetPercentage: Float
) {
    val currentPercentage: Float
        get() = officeDays.toFloat() / totalWorkingDays.coerceAtLeast(1)

    val daysNeededToComply: Int
        get() = maxOf(0, ceil(targetPercentage * totalWorkingDays).toInt() - officeDays)

    val isCompliant: Boolean
        get() = currentPercentage >= targetPercentage

    val projectedPercentage: Float
        get() = (officeDays + unknownDays).toFloat() / totalWorkingDays.coerceAtLeast(1)
}
