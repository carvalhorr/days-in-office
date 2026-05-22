package com.carvalhorr.daysInOffice.app.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DayStatusColorsTest {

    private val colorScheme = lightColorScheme()

    @Test
    fun `given OFFICE status when dayStatusColor then returns green`() {
        assertEquals(Color(0xFF2E7D32), colorScheme.dayStatusColor(DayStatus.OFFICE))
    }

    @Test
    fun `given REMOTE status when dayStatusColor then returns blue`() {
        assertEquals(Color(0xFF1565C0), colorScheme.dayStatusColor(DayStatus.REMOTE))
    }

    @Test
    fun `given HOLIDAY status when dayStatusColor then returns grey`() {
        assertEquals(Color(0xFF9E9E9E), colorScheme.dayStatusColor(DayStatus.HOLIDAY))
    }

    @Test
    fun `given PTO status when dayStatusColor then returns grey`() {
        assertEquals(Color(0xFF9E9E9E), colorScheme.dayStatusColor(DayStatus.PTO))
    }

    @Test
    fun `given WEEKEND status when dayStatusColor then returns light grey`() {
        assertEquals(Color(0xFFBDBDBD), colorScheme.dayStatusColor(DayStatus.WEEKEND))
    }

    @Test
    fun `given UNKNOWN status when dayStatusColor then returns amber`() {
        assertEquals(Color(0xFFF57F17), colorScheme.dayStatusColor(DayStatus.UNKNOWN))
    }
}
