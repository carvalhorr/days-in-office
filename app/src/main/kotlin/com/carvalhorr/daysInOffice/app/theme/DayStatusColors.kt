package com.carvalhorr.daysInOffice.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus

// "Foreground" colours — used for dots, dashboard ring, day-status legend chips.
val DayStatusOfficeColor = Color(0xFF2E7D32)
val DayStatusRemoteColor = Color(0xFF1565C0)
val DayStatusHolidayColor = Color(0xFF9E9E9E)
val DayStatusWeekendColor = Color(0xFFBDBDBD)
val DayStatusUnknownColor = Color(0xFFF57F17)

// "Background" colours — light tints used as the calendar cell fill.
// Picked from the Material 100/50 tonal range so the day number stays
// readable in onSurface text.
val DayStatusOfficeBg = Color(0xFFC8E6C9)   // Green 100
val DayStatusRemoteBg = Color(0xFFBBDEFB)   // Blue 100
val DayStatusPtoBg = Color(0xFFEEEEEE)      // Grey 200
val DayStatusHolidayBg = Color(0xFFFFE0B2)  // Orange 100 — distinct from PTO
val DayStatusUnknownBg = Color(0xFFFFF3E0)  // Orange 50 — faint amber
val DayStatusWeekendBg = Color.Transparent  // Weekends recede into the page

fun ColorScheme.dayStatusColor(status: DayStatus): Color = when (status) {
    DayStatus.OFFICE -> DayStatusOfficeColor
    DayStatus.REMOTE -> DayStatusRemoteColor
    DayStatus.HOLIDAY -> DayStatusHolidayColor
    DayStatus.PTO -> DayStatusHolidayColor
    DayStatus.WEEKEND -> DayStatusWeekendColor
    DayStatus.UNKNOWN -> DayStatusUnknownColor
}

fun dayStatusBackground(status: DayStatus): Color = when (status) {
    DayStatus.OFFICE -> DayStatusOfficeBg
    DayStatus.REMOTE -> DayStatusRemoteBg
    DayStatus.PTO -> DayStatusPtoBg
    DayStatus.HOLIDAY -> DayStatusHolidayBg
    DayStatus.UNKNOWN -> DayStatusUnknownBg
    DayStatus.WEEKEND -> DayStatusWeekendBg
}
