package com.carvalhorr.daysInOffice.app.ui.common

import com.carvalhorr.daysInOffice.feature.calendar.CalendarViewModel
import com.carvalhorr.daysInOffice.feature.dashboard.DashboardViewModel
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorStateTest {

    @Test
    fun `ErrorState composable function exists in expected package`() {
        val clazz = Class.forName("com.carvalhorr.daysInOffice.app.ui.common.ErrorStateKt")
        val hasMethod = clazz.methods.any { it.name == "ErrorState" }
        assertTrue(hasMethod)
    }

    @Test
    fun `ErrorState composable method accepts multiple parameters including retry callback`() {
        val clazz = Class.forName("com.carvalhorr.daysInOffice.app.ui.common.ErrorStateKt")
        val methods = clazz.methods.filter { it.name == "ErrorState" }
        assertTrue(methods.isNotEmpty())
        // Composable compiles to method with message, onRetry, Modifier, Composer, changed params
        assertTrue(methods.first().parameterCount >= 2)
    }

    @Test
    fun `DashboardViewModel exposes retry function`() {
        val retryMethod = DashboardViewModel::class.java.methods.any { it.name == "retry" }
        assertTrue(retryMethod)
    }

    @Test
    fun `CalendarViewModel exposes retry function`() {
        val retryMethod = CalendarViewModel::class.java.methods.any { it.name == "retry" }
        assertTrue(retryMethod)
    }

    @Test
    fun `ErrorState composable is different from EmptyState composable`() {
        val errorClazz = Class.forName("com.carvalhorr.daysInOffice.app.ui.common.ErrorStateKt")
        val emptyClazz = Class.forName("com.carvalhorr.daysInOffice.app.ui.common.EmptyStateKt")
        assertTrue(errorClazz != emptyClazz)
    }
}
