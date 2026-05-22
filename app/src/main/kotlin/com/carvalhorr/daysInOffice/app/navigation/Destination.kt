package com.carvalhorr.daysInOffice.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Onboarding : Destination("onboarding")

    sealed class Tab(route: String, val label: String) : Destination(route) {
        abstract val icon: ImageVector
    }

    data object Dashboard : Tab("dashboard", "Home") {
        override val icon: ImageVector get() = Icons.Default.Home
    }

    data object Calendar : Tab("calendar", "Calendar") {
        override val icon: ImageVector get() = Icons.Default.DateRange
    }

    data object Settings : Tab("settings", "Settings") {
        override val icon: ImageVector get() = Icons.Default.Settings
    }

    companion object {
        val bottomNavDestinations: List<Tab>
            get() = listOf(Dashboard, Calendar, Settings)
    }
}
