package com.carvalhorr.daysInOffice.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.feature.settings.OneShotResult
import com.carvalhorr.daysInOffice.feature.settings.SettingsNavigationEvent
import com.carvalhorr.daysInOffice.feature.settings.SettingsUiState
import com.carvalhorr.daysInOffice.feature.settings.SettingsViewModel
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.GeofenceSheet
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.PeriodSheet
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.TargetSheet
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.WifiConnectedSheet
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.WifiScanSheet
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.WorkingDaysSheet
import com.carvalhorr.daysInOffice.feature.settings.ui.sheets.monthLabel
import java.time.DayOfWeek
import kotlin.math.roundToInt

private enum class SettingsSheet {
    TARGET, PERIOD, WORKING_DAYS, WIFI_CONNECTED, WIFI_SCAN, GEOFENCE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)?,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    var activeSheet by remember { mutableStateOf<SettingsSheet?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(navigationEvent) {
        if (navigationEvent is SettingsNavigationEvent.NavigateToOnboarding) {
            onNavigateToOnboarding()
            viewModel.onNavigationHandled()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.oneShotResult.collect { result ->
            val message = when (result) {
                OneShotResult.NotificationFired -> "Detection ran. Check the notification."
                OneShotResult.NoSignal -> "Detection ran. You're not at the office (no positive signal)."
                is OneShotResult.PermissionMissing -> "Cannot send notifications. Grant POST_NOTIFICATIONS in Settings."
                is OneShotResult.Error -> "Detection failed: ${result.message}"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        when (val s = state) {
            is SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is SettingsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.message)
                }
            }
            is SettingsUiState.Success -> {
                SettingsContent(
                    state = s,
                    onOpenSheet = { activeSheet = it },
                    onResetOnboarding = viewModel::resetOnboarding,
                    modifier = Modifier.padding(paddingValues)
                )

                when (activeSheet) {
                    SettingsSheet.TARGET -> TargetSheet(
                        currentPercentage = s.mandateConfig.targetPercentage,
                        onSave = { pct ->
                            viewModel.updateTargetPercentage(pct)
                            activeSheet = null
                        },
                        onDismiss = { activeSheet = null }
                    )
                    SettingsSheet.PERIOD -> PeriodSheet(
                        currentPeriod = s.mandateConfig.period,
                        currentFiscalYearStartMonth = s.mandateConfig.fiscalYearStartMonth,
                        onSave = { period, fyStart ->
                            viewModel.updatePeriodAndFiscalYearStart(period, fyStart)
                            activeSheet = null
                        },
                        onDismiss = { activeSheet = null }
                    )
                    SettingsSheet.WORKING_DAYS -> WorkingDaysSheet(
                        currentDays = s.mandateConfig.workingDays,
                        onSave = { days ->
                            viewModel.updateWorkingDays(days)
                            activeSheet = null
                        },
                        onDismiss = { activeSheet = null }
                    )
                    SettingsSheet.WIFI_CONNECTED -> WifiConnectedSheet(
                        enabled = s.detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED),
                        currentSsid = s.detectionConfig.wifiSsid,
                        onSave = { en, ssid ->
                            viewModel.updateWifiConnected(en, ssid)
                            activeSheet = null
                        },
                        onDismiss = { activeSheet = null }
                    )
                    SettingsSheet.WIFI_SCAN -> WifiScanSheet(
                        enabled = s.detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_SCAN),
                        currentSsid = s.detectionConfig.wifiSsid,
                        onSave = { en, ssid ->
                            viewModel.updateWifiScan(en, ssid)
                            activeSheet = null
                        },
                        onDismiss = { activeSheet = null }
                    )
                    SettingsSheet.GEOFENCE -> GeofenceSheet(
                        enabled = s.detectionConfig.enabledMethods.contains(DetectionMethod.GEOFENCE),
                        currentLat = s.detectionConfig.geofenceLatitude,
                        currentLng = s.detectionConfig.geofenceLongitude,
                        currentRadius = s.detectionConfig.geofenceRadiusMeters,
                        onSave = { en, lat, lng, radius ->
                            viewModel.updateGeofence(en, lat, lng, radius)
                            activeSheet = null
                        },
                        onDismiss = { activeSheet = null }
                    )
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Success,
    onOpenSheet: (SettingsSheet) -> Unit,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MandateSection(state = state, onOpenSheet = onOpenSheet)
        DetectionSection(state = state, onOpenSheet = onOpenSheet)
        SetupSection(onRerunSetupWizard = { showResetDialog = true })
        Spacer(Modifier.height(16.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Re-run setup wizard?") },
            text = { Text("This will return you to the setup wizard. Your data is kept. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetOnboarding()
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MandateSection(
    state: SettingsUiState.Success,
    onOpenSheet: (SettingsSheet) -> Unit
) {
    SettingsSection(title = "Mandate") {
        SettingsRow(
            emoji = "🎯",
            label = "Target",
            value = "${(state.mandateConfig.targetPercentage * 100).roundToInt()}%",
            onClick = { onOpenSheet(SettingsSheet.TARGET) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
        SettingsRow(
            emoji = "📅",
            label = "Period",
            value = state.mandateConfig.periodValueLabel,
            onClick = { onOpenSheet(SettingsSheet.PERIOD) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
        SettingsRow(
            emoji = "📆",
            label = "Working days",
            value = state.mandateConfig.workingDays.workingDaysLabel,
            onClick = { onOpenSheet(SettingsSheet.WORKING_DAYS) }
        )
    }
}

@Composable
private fun DetectionSection(
    state: SettingsUiState.Success,
    onOpenSheet: (SettingsSheet) -> Unit
) {
    SettingsSection(title = "Detection") {
        val wifiConnEnabled = state.detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED)
        SettingsRow(
            emoji = "📶",
            label = "Wi-Fi (connected)",
            value = if (wifiConnEnabled) "On · ${state.detectionConfig.wifiSsid ?: "no SSID"}" else "Off",
            onClick = { onOpenSheet(SettingsSheet.WIFI_CONNECTED) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
        val wifiScanEnabled = state.detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_SCAN)
        SettingsRow(
            emoji = "📡",
            label = "Wi-Fi (scan only)",
            value = if (wifiScanEnabled) "On · ${state.detectionConfig.wifiSsid ?: "no SSID"}" else "Off",
            onClick = { onOpenSheet(SettingsSheet.WIFI_SCAN) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
        val geoEnabled = state.detectionConfig.enabledMethods.contains(DetectionMethod.GEOFENCE)
        SettingsRow(
            emoji = "📍",
            label = "Geofencing",
            value = if (geoEnabled) "On" else "Off",
            onClick = { onOpenSheet(SettingsSheet.GEOFENCE) }
        )
    }
}

@Composable
private fun SetupSection(onRerunSetupWizard: () -> Unit) {
    SettingsSection(title = "Setup") {
        SettingsRow(
            emoji = "🔁",
            label = "Re-run setup wizard",
            value = "Walks you through the initial setup again. Your data is kept.",
            onClick = onRerunSetupWizard
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    emoji: String,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EmojiIconBox(emoji = emoji)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmojiIconBox(emoji: String) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val MandatePeriod.label: String
    get() = when (this) {
        MandatePeriod.WEEKLY -> "Weekly"
        MandatePeriod.MONTHLY -> "Monthly"
        MandatePeriod.QUARTERLY -> "Quarterly"
        MandatePeriod.ROLLING_4_WEEKS -> "Rolling 4 weeks"
    }

private val com.carvalhorr.daysInOffice.core.domain.model.MandateConfig.periodValueLabel: String
    get() = if (period == MandatePeriod.QUARTERLY) {
        "Quarterly · ${monthLabel(fiscalYearStartMonth)}"
    } else {
        period.label
    }

private val Set<DayOfWeek>.workingDaysLabel: String
    get() {
        val order = listOf(
            DayOfWeek.MONDAY to "Mon",
            DayOfWeek.TUESDAY to "Tue",
            DayOfWeek.WEDNESDAY to "Wed",
            DayOfWeek.THURSDAY to "Thu",
            DayOfWeek.FRIDAY to "Fri",
            DayOfWeek.SATURDAY to "Sat",
            DayOfWeek.SUNDAY to "Sun"
        )
        val selected = order.filter { (day, _) -> contains(day) }.map { it.second }
        return if (selected.isEmpty()) "None" else selected.joinToString(", ")
    }
