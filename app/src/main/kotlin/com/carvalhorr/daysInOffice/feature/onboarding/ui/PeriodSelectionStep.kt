package com.carvalhorr.daysInOffice.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelectionStep(
    selectedPeriod: MandatePeriod,
    fiscalYearStartMonth: Int,
    onPeriodSelected: (MandatePeriod) -> Unit,
    onFiscalYearStartChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFiscalYearSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose your mandate period",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "How often is your in-office mandate measured?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(modifier = Modifier.selectableGroup()) {
            MandatePeriod.entries.forEach { period ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = period == selectedPeriod,
                            onClick = { onPeriodSelected(period) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = period == selectedPeriod,
                        onClick = null
                    )
                    Column {
                        Text(
                            text = period.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = period.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }
        }

        if (selectedPeriod == MandatePeriod.QUARTERLY) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = false,
                        onClick = { showFiscalYearSheet = true },
                        role = Role.Button
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fiscal year starts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = monthFullName(fiscalYearStartMonth),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap to change. Quarters cascade from this — e.g. April → Q1 is Apr–Jun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showFiscalYearSheet) {
        FiscalYearStartInlineSheet(
            currentMonth = fiscalYearStartMonth,
            onSave = { month ->
                onFiscalYearStartChanged(month)
                showFiscalYearSheet = false
            },
            onDismiss = { showFiscalYearSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiscalYearStartInlineSheet(
    currentMonth: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Fiscal year starts",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.selectableGroup()) {
                (1..12).forEach { monthValue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = monthValue == currentMonth,
                                onClick = { onSave(monthValue) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = monthValue == currentMonth, onClick = null)
                        Text(
                            text = monthFullName(monthValue),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun monthFullName(monthValue: Int): String =
    Month.of(monthValue.coerceIn(1, 12))
        .getDisplayName(TextStyle.FULL, Locale.getDefault())

private val MandatePeriod.displayName: String
    get() = when (this) {
        MandatePeriod.WEEKLY -> "Weekly"
        MandatePeriod.MONTHLY -> "Monthly"
        MandatePeriod.QUARTERLY -> "Quarterly"
        MandatePeriod.ROLLING_4_WEEKS -> "Rolling 4 Weeks"
    }

private val MandatePeriod.description: String
    get() = when (this) {
        MandatePeriod.WEEKLY -> "Measured each week"
        MandatePeriod.MONTHLY -> "Measured each calendar month"
        MandatePeriod.QUARTERLY -> "Measured each quarter"
        MandatePeriod.ROLLING_4_WEEKS -> "Measured over the last 28 days"
    }
