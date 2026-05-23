package com.carvalhorr.daysInOffice.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.feature.onboarding.OnboardingNavigationEvent
import com.carvalhorr.daysInOffice.feature.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.navigationEvent) {
        if (state.navigationEvent is OnboardingNavigationEvent.NavigateToDashboard) {
            viewModel.onNavigationHandled()
            onOnboardingComplete()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Setup") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressDots(
                totalSteps = OnboardingViewModel.TOTAL_STEPS,
                currentStep = state.currentStep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                when (state.currentStep) {
                    0 -> MandateSetupStep(
                        targetPercentage = state.mandateConfig.targetPercentage,
                        workingDays = state.mandateConfig.workingDays,
                        onPercentageChange = viewModel::updateMandatePercentage,
                        onWorkingDaysChange = viewModel::updateWorkingDays
                    )
                    1 -> PeriodSelectionStep(
                        selectedPeriod = state.mandateConfig.period,
                        fiscalYearStartMonth = state.mandateConfig.fiscalYearStartMonth,
                        onPeriodSelected = viewModel::updatePeriod,
                        onFiscalYearStartChanged = viewModel::updateFiscalYearStartMonth
                    )
                    2 -> DetectionSetupStep(
                        detectionConfig = state.detectionConfig,
                        onToggleMethod = viewModel::toggleDetectionMethod,
                        onUpdateWifiSsid = viewModel::updateWifiSsid,
                        onUpdateGeofence = viewModel::updateGeofence
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.currentStep > 0) {
                    OutlinedButton(
                        onClick = viewModel::back,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (state.currentStep == OnboardingViewModel.TOTAL_STEPS - 1) {
                            viewModel.complete()
                        } else {
                            viewModel.next()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (state.currentStep == OnboardingViewModel.TOTAL_STEPS - 1) "Finish" else "Next"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isActive) 10.dp else 8.dp),
                shape = MaterialTheme.shapes.small,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            ) {}
        }
    }
}
