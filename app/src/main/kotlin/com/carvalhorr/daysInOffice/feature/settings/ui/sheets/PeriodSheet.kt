package com.carvalhorr.daysInOffice.feature.settings.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSheet(
    currentPeriod: MandatePeriod,
    onSave: (MandatePeriod) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(currentPeriod) }

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
                                selected = period == draft,
                                onClick = { draft = period },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(selected = period == draft, onClick = null)
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
                    onClick = { onSave(draft) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

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
