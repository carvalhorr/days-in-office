package com.carvalhorr.daysInOffice.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.feature.calendar.CalendarViewModel
import com.carvalhorr.daysInOffice.feature.calendar.ui.CalendarScreen
import com.carvalhorr.daysInOffice.feature.dashboard.DashboardViewModel
import com.carvalhorr.daysInOffice.feature.dashboard.ui.DashboardScreen
import com.carvalhorr.daysInOffice.feature.onboarding.OnboardingViewModel
import com.carvalhorr.daysInOffice.feature.onboarding.ui.OnboardingScreen
import com.carvalhorr.daysInOffice.feature.settings.SettingsViewModel
import com.carvalhorr.daysInOffice.feature.settings.ui.SettingsScreen

@Composable
fun DaysInOfficeNavHost(
    navController: NavHostController,
    startDestination: Destination,
    showDetectionPrompt: Boolean = false,
    pendingDetectionMethod: DetectionMethod? = null,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(Destination.Onboarding.route) {
            val activity = LocalContext.current as ComponentActivity
            val viewModel: OnboardingViewModel = viewModel(viewModelStoreOwner = activity)
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        composable(Destination.Dashboard.route) {
            val activity = LocalContext.current as ComponentActivity
            val viewModel: DashboardViewModel = viewModel(viewModelStoreOwner = activity)
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate(Destination.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                viewModel = viewModel,
                showDetectionPrompt = showDetectionPrompt,
                pendingDetectionMethod = pendingDetectionMethod
            )
        }
        composable(Destination.Calendar.route) {
            val activity = LocalContext.current as ComponentActivity
            val viewModel: CalendarViewModel = viewModel(viewModelStoreOwner = activity)
            CalendarScreen(viewModel = viewModel)
        }
        composable(Destination.Settings.route) {
            val activity = LocalContext.current as ComponentActivity
            val viewModel: SettingsViewModel = viewModel(viewModelStoreOwner = activity)
            SettingsScreen(
                onNavigateBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null,
                onNavigateToOnboarding = {
                    navController.navigate(Destination.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
    }
}
