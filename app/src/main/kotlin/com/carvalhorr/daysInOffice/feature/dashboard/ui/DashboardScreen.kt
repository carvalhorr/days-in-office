package com.carvalhorr.daysInOffice.feature.dashboard.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.app.theme.BrandPeriodChipBg
import com.carvalhorr.daysInOffice.app.theme.BrandPeriodChipText
import com.carvalhorr.daysInOffice.core.domain.model.ComplianceResult
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.app.ui.common.EmptyState
import com.carvalhorr.daysInOffice.app.ui.common.ErrorState
import com.carvalhorr.daysInOffice.feature.dashboard.DashboardUiState
import com.carvalhorr.daysInOffice.feature.dashboard.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel,
    showDetectionPrompt: Boolean = false,
    pendingDetectionMethod: DetectionMethod? = null
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.carvalhorr.daysInOffice.R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Text("⚙️", style = MaterialTheme.typography.titleLarge)
                    }
                },
                windowInsets = WindowInsets(0)
            )
        }
    ) { paddingValues ->
        when (val s = state) {
            is DashboardUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                    CheckInCard(
                        today = LocalDate.now(),
                        currentStatus = null,
                        onCheckInOffice = viewModel::checkInAsOffice,
                        onCheckInRemote = viewModel::checkInAsRemote
                    )
                }
            }
            is DashboardUiState.Empty -> {
                EmptyState(
                    message = "No records yet — check in to get started",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is DashboardUiState.Error -> {
                ErrorState(
                    message = s.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    state = s,
                    onCheckInOffice = viewModel::checkInAsOffice,
                    onCheckInRemote = viewModel::checkInAsRemote,
                    showDetectionPrompt = showDetectionPrompt,
                    pendingDetectionMethod = pendingDetectionMethod,
                    onConfirmDetection = viewModel::confirmOfficeFromDetection,
                    onDismissDetection = viewModel::dismissDetectionPrompt,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    onCheckInOffice: () -> Unit,
    onCheckInRemote: () -> Unit,
    showDetectionPrompt: Boolean = false,
    pendingDetectionMethod: DetectionMethod? = null,
    onConfirmDetection: () -> Unit = {},
    onDismissDetection: (DetectionMethod?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val compliance = state.complianceResult
    val today = LocalDate.now()
    var showPromptCard by remember(showDetectionPrompt) { mutableStateOf(showDetectionPrompt) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (showPromptCard) {
            DetectionPromptCard(
                onConfirm = {
                    onConfirmDetection()
                    showPromptCard = false
                },
                onDismiss = {
                    onDismissDetection(pendingDetectionMethod)
                    showPromptCard = false
                }
            )
        }

        PeriodChip(periodStart = compliance.periodStart, period = state.mandatePeriod)

        ComplianceRing(
            currentPercentage = compliance.currentPercentage,
            targetPercentage = compliance.targetPercentage
        )

        StatusMessage(compliance = compliance)

        StatsStrip(compliance = compliance)

        if (state.isTodayWorkday) {
            CheckInCard(
                today = today,
                currentStatus = state.todayRecord?.status,
                onCheckInOffice = onCheckInOffice,
                onCheckInRemote = onCheckInRemote
            )
        } else {
            NonWorkdayCheckInCard(
                today = today,
                currentStatus = state.todayRecord?.status,
                onCheckInOffice = onCheckInOffice
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun NonWorkdayCheckInCard(
    today: LocalDate,
    currentStatus: DayStatus?,
    onCheckInOffice: () -> Unit
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
    val officeSelected = currentStatus == DayStatus.OFFICE
    val subtitleText = if (officeSelected) {
        "✓ Office (bonus day — counts toward your goal)"
    } else {
        "Non-workday — Office counts as a bonus toward your goal."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today — ${today.format(dayFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (officeSelected) {
                Button(
                    onClick = onCheckInOffice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorOfficeGreen)
                ) {
                    Text("🏢 Office")
                }
            } else {
                OutlinedButton(
                    onClick = onCheckInOffice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorOfficeGreen),
                    border = BorderStroke(1.dp, colorOfficeGreen)
                ) {
                    Text("🏢 Office")
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(periodStart: LocalDate, period: MandatePeriod) {
    val label = formatPeriodLabel(periodStart, period)
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandPeriodChipBg
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = BrandPeriodChipText
        )
    }
}

@Composable
private fun StatusMessage(compliance: ComplianceResult) {
    val ringColor = complianceRingColor(compliance.currentPercentage, compliance.targetPercentage)
    val targetPct = (compliance.targetPercentage * 100).toInt()
    val message = when {
        compliance.isCompliant -> "On track — meeting your $targetPct% goal"
        compliance.daysNeededToComply == 1 -> "1 more day to meet your $targetPct% goal"
        else -> "${compliance.daysNeededToComply} more days to meet your $targetPct% goal"
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = ringColor,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun StatsStrip(compliance: ComplianceResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(count = compliance.officeDays, label = "OFFICE", color = colorOfficeGreen)
        Box(modifier = Modifier.width(1.dp).height(60.dp).background(MaterialTheme.colorScheme.outlineVariant))
        StatItem(count = compliance.remoteDays, label = "REMOTE", color = Color(0xFF1565C0))
        Box(modifier = Modifier.width(1.dp).height(60.dp).background(MaterialTheme.colorScheme.outlineVariant))
        StatItem(count = compliance.unknownDays, label = "UNKNOWN", color = colorAmber)
    }
}

@Composable
private fun StatItem(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(50),
                color = color
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckInCard(
    today: LocalDate,
    currentStatus: DayStatus?,
    onCheckInOffice: () -> Unit,
    onCheckInRemote: () -> Unit
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
    val subtitleText = when (currentStatus) {
        DayStatus.OFFICE -> "✓ Checked in for the office"
        DayStatus.REMOTE -> "Marked as remote day"
        DayStatus.PTO -> "On PTO today"
        else -> "Are you in the office today?"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today — ${today.format(dayFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QuickCheckInButton(
                onCheckInOffice = onCheckInOffice,
                onCheckInRemote = onCheckInRemote,
                currentStatus = currentStatus
            )
        }
    }
}

@Composable
private fun DetectionPromptCard(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Are you at the office?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "We detected you may be at the office. Mark today as Office day?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onConfirm) {
                    Text("Yes, Office")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("No, dismiss")
                }
            }
        }
    }
}

private fun formatPeriodLabel(periodStart: LocalDate, period: MandatePeriod): String =
    when (period) {
        MandatePeriod.MONTHLY -> {
            val month = periodStart.month.name.take(3)
            "$month ${periodStart.year} · MONTHLY"
        }
        MandatePeriod.WEEKLY -> {
            val month = periodStart.month.name.take(3)
            "$month ${periodStart.year} · WEEKLY"
        }
        MandatePeriod.QUARTERLY -> {
            val quarter = (periodStart.monthValue - 1) / 3 + 1
            "Q$quarter ${periodStart.year} · QUARTERLY"
        }
        MandatePeriod.ROLLING_4_WEEKS -> "ROLLING 4 WEEKS"
    }
