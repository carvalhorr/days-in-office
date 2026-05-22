package com.carvalhorr.daysInOffice.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.carvalhorr.daysInOffice.app.navigation.BottomNavBar
import com.carvalhorr.daysInOffice.app.navigation.DaysInOfficeNavHost
import com.carvalhorr.daysInOffice.app.navigation.Destination
import com.carvalhorr.daysInOffice.app.theme.DaysInOfficeTheme
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.notification.DetectionPromptActionReceiver
import com.carvalhorr.daysInOffice.notification.DetectionPromptNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var mandateConfigRepository: MandateConfigRepository

    private var showDetectionPrompt by mutableStateOf(false)
    private var pendingDetectionMethod by mutableStateOf<DetectionMethod?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        processIntent(intent)
        setContent {
            val onboardingComplete by mandateConfigRepository.getOnboardingComplete()
                .collectAsState(initial = null)

            DaysInOfficeTheme {
                when (val completed = onboardingComplete) {
                    null -> Unit
                    else -> AppRoot(
                        startDestination = if (completed) Destination.Dashboard else Destination.Onboarding,
                        showDetectionPrompt = showDetectionPrompt,
                        pendingDetectionMethod = pendingDetectionMethod
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        showDetectionPrompt = intent?.getBooleanExtra(
            DetectionPromptNotificationWorker.EXTRA_SHOW_DETECTION_PROMPT, false
        ) ?: false
        val methodName = intent?.getStringExtra(DetectionPromptActionReceiver.EXTRA_DETECTION_METHOD)
        pendingDetectionMethod = methodName?.let {
            runCatching { DetectionMethod.valueOf(it) }.getOrNull()
        }
    }
}

@Composable
private fun AppRoot(
    startDestination: Destination,
    showDetectionPrompt: Boolean = false,
    pendingDetectionMethod: DetectionMethod? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Destination.Onboarding.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { paddingValues ->
        DaysInOfficeNavHost(
            navController = navController,
            startDestination = startDestination,
            showDetectionPrompt = showDetectionPrompt,
            pendingDetectionMethod = pendingDetectionMethod,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
