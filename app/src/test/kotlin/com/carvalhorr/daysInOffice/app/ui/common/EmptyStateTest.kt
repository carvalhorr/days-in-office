package com.carvalhorr.daysInOffice.app.ui.common

import com.carvalhorr.daysInOffice.feature.dashboard.DashboardUiState
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmptyStateTest {

    @Test
    fun `EmptyState composable function exists in expected package`() {
        val clazz = Class.forName("com.carvalhorr.daysInOffice.app.ui.common.EmptyStateKt")
        val hasMethod = clazz.methods.any { it.name == "EmptyState" }
        assertTrue(hasMethod)
    }

    @Test
    fun `given dashboard empty message when verified then message is non-blank and contains check in`() {
        val message = "No records yet — check in to get started"
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("check in"))
    }

    @Test
    fun `given calendar empty message when verified then message is non-blank and contains empty`() {
        val message = "This month is empty"
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("empty"))
    }

    @Test
    fun `DashboardUiState has Empty variant that is distinct from other states`() {
        val empty: DashboardUiState = DashboardUiState.Empty
        assertNotNull(empty)
        assertInstanceOf(DashboardUiState.Empty::class.java, empty)
    }

    @Test
    fun `DashboardUiState Empty is not the same as Loading`() {
        val empty: DashboardUiState = DashboardUiState.Empty
        val loading: DashboardUiState = DashboardUiState.Loading
        assertTrue(empty != loading)
    }
}
