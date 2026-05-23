package com.carvalhorr.daysInOffice.feature.settings.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
fun PeriodSheet(
    currentPeriod: MandatePeriod,
    currentFiscalYearStartMonth: Int,
    onSave: (MandatePeriod, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var periodDraft by remember { mutableStateOf(currentPeriod) }
    var fyStartDraft by remember { mutableIntStateOf(currentFiscalYearStartMonth.coerceIn(1, 12)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Mandate Period",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.selectableGroup()) {
                MandatePeriod.entries.forEach { period ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = period == periodDraft,
                                onClick = { periodDraft = period },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = period == periodDraft, onClick = null)
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
                    if (period == MandatePeriod.QUARTERLY && periodDraft == MandatePeriod.QUARTERLY) {
                        FiscalYearStartPicker(
                            currentMonth = fyStartDraft,
                            onMonthSelected = { fyStartDraft = it },
                            modifier = Modifier.padding(start = 40.dp, bottom = 12.dp)
                        )
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = { onSave(periodDraft, fyStartDraft) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun FiscalYearStartPicker(
    currentMonth: Int,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Fiscal year starts",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { menuOpen = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthLabel(currentMonth),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Choose month"
                    )
                }
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                (1..12).forEach { m ->
                    DropdownMenuItem(
                        text = { Text(monthLabel(m)) },
                        onClick = {
                            onMonthSelected(m)
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

internal fun monthLabel(monthValue: Int): String =
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
