package com.carvalhorr.daysInOffice.feature.settings.ui.sheets

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.carvalhorr.daysInOffice.core.permissions.AppPermission
import com.carvalhorr.daysInOffice.core.permissions.rememberPermissionRequester
import com.carvalhorr.daysInOffice.feature.shared.ui.GeofencePicker
import com.carvalhorr.daysInOffice.feature.shared.ui.LocationDisclosureCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceSheet(
    enabled: Boolean,
    currentLat: Double?,
    currentLng: Double?,
    currentRadius: Float?,
    onSave: (Boolean, Double?, Double?, Float?) -> Unit,
    onDismiss: () -> Unit
) {
    var draftEnabled by remember { mutableStateOf(enabled) }
    var draftGeofenceLat by remember { mutableStateOf(currentLat) }
    var draftGeofenceLng by remember { mutableStateOf(currentLng) }
    var draftGeofenceRadius by remember { mutableStateOf(currentRadius ?: 100f) }
    val context = LocalContext.current
    val permissionRequester = rememberPermissionRequester()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasBackgroundLocation by remember { mutableStateOf(false) }
    // Google Play prominent-disclosure gate: the user must acknowledge how
    // background location is used before the app can request the permission.
    // Skipped once the permission is already granted.
    var disclosureAccepted by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                hasBackgroundLocation = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        hasBackgroundLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                text = "Geofencing",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Detects when your device is within a set radius of the office location.",
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
                            // Only request notifications now if no prominent
                            // disclosure will be shown. Otherwise defer until the
                            // user taps Continue, so the notification system prompt
                            // never appears on top of the disclosure.
                            if (newValue && hasBackgroundLocation) {
                                coroutineScope.launch {
                                    permissionRequester.request(AppPermission.NOTIFICATIONS)
                                }
                            }
                        }
                    )
                }
                if (draftEnabled && !hasBackgroundLocation && !disclosureAccepted) {
                    Spacer(Modifier.height(8.dp))
                    LocationDisclosureCard(
                        onContinue = {
                            disclosureAccepted = true
                            coroutineScope.launch {
                                permissionRequester.request(AppPermission.NOTIFICATIONS)
                            }
                        }
                    )
                } else if (draftEnabled) {
                    GeofencePicker(
                        latitude = draftGeofenceLat,
                        longitude = draftGeofenceLng,
                        radius = draftGeofenceRadius,
                        onUpdate = { lat, lng, r ->
                            draftGeofenceLat = lat
                            draftGeofenceLng = lng
                            draftGeofenceRadius = r
                        }
                    )
                    if (!hasBackgroundLocation) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Background location is not granted. Detection will only work while the app is open.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                ) {
                                    Text("Grant", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
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
                    onClick = {
                        if (draftEnabled) {
                            onSave(true, draftGeofenceLat, draftGeofenceLng, draftGeofenceRadius)
                        } else {
                            onSave(false, null, null, null)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}
