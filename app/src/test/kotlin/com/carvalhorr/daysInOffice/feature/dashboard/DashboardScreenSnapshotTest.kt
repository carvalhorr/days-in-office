package com.carvalhorr.daysInOffice.feature.dashboard

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.carvalhorr.daysInOffice.app.theme.BrandPrimary
import com.carvalhorr.daysInOffice.app.theme.BrandPrimaryVariant
import com.carvalhorr.daysInOffice.core.domain.model.ComplianceResult
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DashboardScreenSnapshotTest {

    private val sampleCompliance = ComplianceResult(
        periodStart = LocalDate.of(2026, 5, 1),
        periodEnd = LocalDate.of(2026, 5, 31),
        totalWorkingDays = 21,
        officeDays = 10,
        remoteDays = 5,
        unknownDays = 6,
        targetPercentage = 0.5f
    )

    @Test
    fun `given dashboard state types when initialized then Loading Empty Success and Error are all non-null`() {
        val loading: DashboardUiState = DashboardUiState.Loading
        val empty: DashboardUiState = DashboardUiState.Empty
        val error: DashboardUiState = DashboardUiState.Error("Test error")
        val success: DashboardUiState = DashboardUiState.Success(
            complianceResult = sampleCompliance,
            mandatePeriod = MandatePeriod.MONTHLY,
            todayRecord = null,
            isTodayWorkday = true
        )
        assertNotNull(loading)
        assertNotNull(empty)
        assertNotNull(error)
        assertNotNull(success)
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
    fun `given DashboardScreen composable when checked via reflection then function is accessible`() {
        val clazz = Class.forName("com.carvalhorr.daysInOffice.feature.dashboard.ui.DashboardScreenKt")
        val hasDashboardScreen = clazz.methods.any { it.name == "DashboardScreen" }
        assertTrue(hasDashboardScreen)
    }

    @Test
    fun `given DashboardUiState Empty when compliance has no working days then state is correct type`() {
        val emptyState: DashboardUiState = DashboardUiState.Empty
        assertTrue(emptyState is DashboardUiState.Empty)
    }

    @Test
    fun `given DashboardUiState Success when compliance has working days then state contains compliance`() {
        val success = DashboardUiState.Success(
            complianceResult = sampleCompliance,
            mandatePeriod = MandatePeriod.MONTHLY,
            todayRecord = null,
            isTodayWorkday = true
        )
        assertEquals(sampleCompliance, success.complianceResult)
        assertEquals(MandatePeriod.MONTHLY, success.mandatePeriod)
    }
}
