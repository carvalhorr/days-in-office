package com.carvalhorr.daysInOffice.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun MandateSetupStep(
    targetPercentage: Float,
    workingDays: Set<DayOfWeek>,
    onPercentageChange: (Float) -> Unit,
    onWorkingDaysChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Set your in-office target",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "How often does your company require you to be in the office?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${(targetPercentage * 100).roundToInt()}%",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )
            val daysPerWeek = targetPercentage * workingDays.size
            val avgDaysText = if (daysPerWeek == daysPerWeek.roundToInt().toFloat()) {
                val n = daysPerWeek.roundToInt()
                "$n day${if (n == 1) "" else "s"} per week"
            } else {
                "${"%.1f".format(daysPerWeek)} days per week"
            }
            Text(
                text = avgDaysText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // 5% increments: 0,5,10,...,100 = 21 values, 19 intermediate steps
        Slider(
            value = targetPercentage,
            onValueChange = { value ->
                val rounded = (value * 20).roundToInt() / 20f
                onPercentageChange(rounded)
            },
            valueRange = 0f..1f,
            steps = 19,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Working days",
            style = MaterialTheme.typography.titleSmall
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
                val isSelected = workingDays.contains(day)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newDays = workingDays.toMutableSet()
                        if (isSelected) newDays.remove(day) else newDays.add(day)
                        onWorkingDaysChange(newDays)
                    },
                    label = { Text(label) },
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
            }
        }
    }
}
