package com.carvalhorr.daysInOffice.feature.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.app.theme.dayStatusColor
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    dayRecord: DayRecord,
    onDismiss: () -> Unit,
    onOverrideStatus: (LocalDate, DayStatus) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = dayRecord.date.format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy", Locale.getDefault())
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.dayStatusColor(dayRecord.status)
                ) {}
                Text(
                    text = dayRecord.status.name
                        .lowercase()
                        .replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (dayRecord.detectionMethod != null) {
                Text(
                    text = "Detection: ${dayRecord.detectionMethod.name.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Button(
                onClick = { onOverrideStatus(dayRecord.date, DayStatus.OFFICE) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Office")
            }

            OutlinedButton(
                onClick = { onOverrideStatus(dayRecord.date, DayStatus.REMOTE) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Remote")
            }

            OutlinedButton(
                onClick = { onOverrideStatus(dayRecord.date, DayStatus.PTO) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as PTO")
            }
        }
    }
}
