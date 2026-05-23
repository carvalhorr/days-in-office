package com.carvalhorr.daysInOffice.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus

// "Foreground" colours — used for dots, dashboard ring, day-status legend chips.
val DayStatusOfficeColor = Color(0xFF2E7D32)
val DayStatusRemoteColor = Color(0xFF1565C0)
val DayStatusHolidayColor = Color(0xFF9E9E9E)
val DayStatusWeekendColor = Color(0xFFBDBDBD)
val DayStatusUnknownColor = Color(0xFFF57F17)

// "Background" colours for calendar cell fills — light theme.
// Picked from the Material 100/50 tonal range so the day number stays
// readable in onSurface (near-black) text.
val DayStatusOfficeBg = Color(0xFFC8E6C9)   // Green 100
val DayStatusRemoteBg = Color(0xFFBBDEFB)   // Blue 100
val DayStatusPtoBg = Color(0xFFEEEEEE)      // Grey 200
val DayStatusHolidayBg = Color(0xFFFFE0B2)  // Orange 100 — distinct from PTO
val DayStatusUnknownBg = Color(0xFFFFF3E0)  // Orange 50 — faint amber

// Dark-theme cell fills. Hand-picked so they:
//   - sit slightly above the surface (#1C1B1F-ish) for visible separation
//   - retain hue identity (a green tint stays "green")
//   - keep enough contrast with onSurface (~#E6E1E5) for the day number.
val DayStatusOfficeBgDark = Color(0xFF1F3A22)   // dark moss
val DayStatusRemoteBgDark = Color(0xFF143257)   // dark navy
val DayStatusPtoBgDark = Color(0xFF3A3A3D)      // medium dark grey
val DayStatusHolidayBgDark = Color(0xFF4A2B0E)  // dark sienna
val DayStatusUnknownBgDark = Color(0xFF3F2D12)  // dark amber

fun ColorScheme.dayStatusColor(status: DayStatus): Color = when (status) {
    DayStatus.OFFICE -> DayStatusOfficeColor
    DayStatus.REMOTE -> DayStatusRemoteColor
    DayStatus.HOLIDAY -> DayStatusHolidayColor
    DayStatus.PTO -> DayStatusHolidayColor
    DayStatus.WEEKEND -> DayStatusWeekendColor
    DayStatus.UNKNOWN -> DayStatusUnknownColor
}

@Composable
@ReadOnlyComposable
fun dayStatusBackground(status: DayStatus): Color {
    val dark = isSystemInDarkTheme()
    return when (status) {
        DayStatus.OFFICE -> if (dark) DayStatusOfficeBgDark else DayStatusOfficeBg
        DayStatus.REMOTE -> if (dark) DayStatusRemoteBgDark else DayStatusRemoteBg
        DayStatus.PTO -> if (dark) DayStatusPtoBgDark else DayStatusPtoBg
        DayStatus.HOLIDAY -> if (dark) DayStatusHolidayBgDark else DayStatusHolidayBg
        DayStatus.UNKNOWN -> if (dark) DayStatusUnknownBgDark else DayStatusUnknownBg
        DayStatus.WEEKEND -> Color.Transparent
    }
}
