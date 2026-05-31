package com.carvalhorr.daysInOffice.feature.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Google Play "prominent disclosure" for background location, rendered inline
 * wherever the user opts into a location-based detection feature (geofencing).
 *
 * Play policy requires that, before the app requests location while it intends
 * to use it in the background, the user sees an in-app statement of what data
 * is collected and that collection continues when the app is closed or not in
 * use — and takes an affirmative action ([onContinue]) before the system
 * permission prompt appears.
 *
 * See: https://support.google.com/googleplay/android-developer/answer/9799150
 */
@Composable
fun LocationDisclosureCard(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Location access",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Days in Office collects location data to detect when you " +
                    "arrive at your office, even when the app is closed or not in " +
                    "use. This lets the app prompt you to confirm office days " +
                    "automatically.\n\nYour location is used only on this device to " +
                    "trigger those check-in prompts. It is never recorded without " +
                    "your confirmation and is never shared. You can turn this off " +
                    "anytime in Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.align(Alignment.End)
            ) { Text("Continue") }
        }
    }
}
