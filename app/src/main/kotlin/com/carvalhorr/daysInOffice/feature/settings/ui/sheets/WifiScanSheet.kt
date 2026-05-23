package com.carvalhorr.daysInOffice.feature.settings.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import com.carvalhorr.daysInOffice.feature.shared.ui.WifiSsidPicker
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import android.os.Build
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.core.permissions.AppPermission
import com.carvalhorr.daysInOffice.core.permissions.rememberPermissionRequester
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScanSheet(
    enabled: Boolean,
    currentSsid: String?,
    onSave: (Boolean, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var draftEnabled by remember { mutableStateOf(enabled) }
    var draftSsid by remember { mutableStateOf(currentSsid ?: "") }
    val permissionRequester = rememberPermissionRequester()
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Wi-Fi (Scan Only)",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Detects when the office Wi-Fi network is visible, even if not connected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = draftEnabled,
                        onCheckedChange = { newValue ->
                            draftEnabled = newValue
                            if (newValue) {
                                coroutineScope.launch {
                                    permissionRequester.request(AppPermission.NOTIFICATIONS)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionRequester.request(AppPermission.NEARBY_WIFI_DEVICES)
                                    }
                                }
                            }
                        }
                    )
                }
                if (draftEnabled) {
                    WifiSsidPicker(
                        currentSsid = draftSsid,
                        onUpdate = { draftSsid = it }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = { onSave(draftEnabled, draftSsid.ifBlank { null }) },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}
