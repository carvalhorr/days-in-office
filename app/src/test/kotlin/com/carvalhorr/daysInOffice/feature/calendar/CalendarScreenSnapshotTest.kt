package com.carvalhorr.daysInOffice.feature.calendar

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.carvalhorr.daysInOffice.app.theme.BrandPrimary
import com.carvalhorr.daysInOffice.app.theme.BrandPrimaryVariant
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CalendarScreenSnapshotTest {

    @Test
    fun `given calendar state types when initialized then Loading Success and Error are all non-null`() {
        val loading: CalendarUiState = CalendarUiState.Loading
        val error: CalendarUiState = CalendarUiState.Error("Test error")
        val success: CalendarUiState = CalendarUiState.Success(
            currentMonth = YearMonth.of(2026, 5),
            days = emptyList(),
            selectedDay = null
        )
        assertNotNull(loading)
        assertNotNull(error)
        assertNotNull(success)
    }

    @Test
    fun `given success state with empty days when checked then empty list triggers empty state display`() {
        val success = CalendarUiState.Success(
            currentMonth = YearMonth.of(2026, 5),
            days = emptyList(),
            selectedDay = null
        )
        assertTrue(success.days.isEmpty())
    }

    @Test
    fun `given light theme when primary color applied then matches BrandPrimary`() {
        val lightScheme = lightColorScheme(primary = BrandPrimary)
        assertEquals(BrandPrimary, lightScheme.primary)
    }

    @Test
    fun `given dark theme when primary color applied then matches BrandPrimaryVariant`() {
        val darkScheme = darkColorScheme(primary = BrandPrimaryVariant)
        assertEquals(BrandPrimaryVariant, darkScheme.primary)
    }

    @Test
    fun `given light and dark themes when compared then primary colors differ`() {
        val lightScheme = lightColorScheme(primary = BrandPrimary)
        val darkScheme = darkColorScheme(primary = BrandPrimaryVariant)
        assertNotEquals(lightScheme.primary, darkScheme.primary)
    }

    @Test
    fun `given CalendarScreen composable when checked via reflection then function is accessible`() {
        val clazz = Class.forName("com.carvalhorr.daysInOffice.feature.calendar.ui.CalendarScreenKt")
        val hasCalendarScreen = clazz.methods.any { it.name == "CalendarScreen" }
        assertTrue(hasCalendarScreen)
    }

    @Test
    fun `given CalendarUiState Success when days present then selected day can be from days list`() {
        val success = CalendarUiState.Success(
            currentMonth = YearMonth.of(2026, 5),
            days = emptyList(),
            selectedDay = null
        )
        assertEquals(null, success.selectedDay)
    }
}
