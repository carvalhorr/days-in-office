package com.carvalhorr.daysInOffice.feature.settings.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingDaysSheet(
    currentDays: Set<DayOfWeek>,
    onSave: (Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(currentDays) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Working Days",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Select which days count toward your mandate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val dayLabels = listOf(
                    DayOfWeek.MONDAY to "M",
                    DayOfWeek.TUESDAY to "T",
                    DayOfWeek.WEDNESDAY to "W",
                    DayOfWeek.THURSDAY to "T",
                    DayOfWeek.FRIDAY to "F",
                    DayOfWeek.SATURDAY to "S",
                    DayOfWeek.SUNDAY to "S"
                )
                dayLabels.forEach { (day, label) ->
                    val selected = draft.contains(day)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            draft = if (selected) draft - day else draft + day
                        },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
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
