package com.carvalhorr.daysInOffice.feature.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus

private val colorRemoteBlue = Color(0xFF1565C0)

@Composable
fun QuickCheckInButton(
    onCheckInOffice: () -> Unit,
    onCheckInRemote: () -> Unit,
    currentStatus: DayStatus?,
    modifier: Modifier = Modifier
) {
    val officeSelected = currentStatus == DayStatus.OFFICE
    val remoteSelected = currentStatus == DayStatus.REMOTE

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (officeSelected) {
            Button(
                onClick = onCheckInOffice,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colorOfficeGreen)
            ) {
                Text("🏢 Office")
            }
        } else {
            OutlinedButton(
                onClick = onCheckInOffice,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorOfficeGreen),
                border = BorderStroke(1.dp, colorOfficeGreen)
            ) {
                Text("🏢 Office")
            }
        }

        if (remoteSelected) {
            Button(
                onClick = onCheckInRemote,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = colorRemoteBlue)
            ) {
                Text("🏠 Remote")
            }
        } else {
            OutlinedButton(
                onClick = onCheckInRemote,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorRemoteBlue),
                border = BorderStroke(1.dp, colorRemoteBlue)
            ) {
                Text("🏠 Remote")
            }
        }
    }
}
