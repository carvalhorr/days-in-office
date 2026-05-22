package com.carvalhorr.daysInOffice.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod

@Composable
fun PeriodSelectionStep(
    selectedPeriod: MandatePeriod,
    onPeriodSelected: (MandatePeriod) -> Unit,
    modifier: Modifier = Modifier
) {
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
