package com.carvalhorr.daysInOffice.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus

val DayStatusOfficeColor = Color(0xFF2E7D32)
val DayStatusRemoteColor = Color(0xFF1565C0)
val DayStatusHolidayColor = Color(0xFF9E9E9E)
val DayStatusWeekendColor = Color(0xFFBDBDBD)
val DayStatusUnknownColor = Color(0xFFF57F17)

fun ColorScheme.dayStatusColor(status: DayStatus): Color = when (status) {
    DayStatus.OFFICE -> DayStatusOfficeColor
    DayStatus.REMOTE -> DayStatusRemoteColor
    DayStatus.HOLIDAY -> DayStatusHolidayColor
    DayStatus.PTO -> DayStatusHolidayColor
    DayStatus.WEEKEND -> DayStatusWeekendColor
    DayStatus.UNKNOWN -> DayStatusUnknownColor
}
