package com.carvalhorr.daysInOffice.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.feature.shared.ui.GeofencePicker
import com.carvalhorr.daysInOffice.feature.shared.ui.LocationDisclosureCard
import com.carvalhorr.daysInOffice.feature.shared.ui.WifiSsidPicker

@Composable
fun DetectionSetupStep(
    detectionConfig: DetectionConfig,
    onToggleMethod: (DetectionMethod) -> Unit,
    onUpdateWifiSsid: (String) -> Unit,
    onUpdateGeofence: (lat: Double, lng: Double, radius: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val showWifiSsid = detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_CONNECTED) ||
            detectionConfig.enabledMethods.contains(DetectionMethod.WIFI_SCAN)
    val showGeofence = detectionConfig.enabledMethods.contains(DetectionMethod.GEOFENCE)
    // Prominent disclosure must precede the geofence picker (and thus the
    // background-location permission request) during onboarding as well.
    var locationDisclosureAccepted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Set up detection",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Choose how the app detects when you're at the office.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val detectionMethods = listOf(
            DetectionMethod.WIFI_CONNECTED,
            DetectionMethod.WIFI_SCAN,
            DetectionMethod.GEOFENCE
        )
        detectionMethods.forEach { method ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = detectionConfig.enabledMethods.contains(method),
                    onCheckedChange = { onToggleMethod(method) }
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = method.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = method.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showWifiSsid) {
            WifiSsidPicker(
                currentSsid = detectionConfig.wifiSsid,
                onUpdate = onUpdateWifiSsid
            )
        }

        if (showGeofence) {
            if (!locationDisclosureAccepted) {
                LocationDisclosureCard(onContinue = { locationDisclosureAccepted = true })
            } else {
                GeofencePicker(
                    latitude = detectionConfig.geofenceLatitude,
                    longitude = detectionConfig.geofenceLongitude,
                    radius = detectionConfig.geofenceRadiusMeters,
                    onUpdate = onUpdateGeofence
                )
            }
        }
    }
}

private val DetectionMethod.displayName: String
    get() = when (this) {
        DetectionMethod.WIFI_CONNECTED -> "Wi-Fi Connected"
        DetectionMethod.WIFI_SCAN -> "Wi-Fi Scan"
        DetectionMethod.GEOFENCE -> "Geofencing"
        DetectionMethod.MANUAL -> "Manual"
        DetectionMethod.MANUAL_CONFIRMED_FROM_DETECTION -> "Manual (confirmed)"
    }

private val DetectionMethod.description: String
    get() = when (this) {
        DetectionMethod.WIFI_CONNECTED -> "Detects when connected to your office Wi-Fi"
        DetectionMethod.WIFI_SCAN -> "Detects when office Wi-Fi is in range"
        DetectionMethod.GEOFENCE -> "Detects when you're near the office location"
        DetectionMethod.MANUAL -> "Check in manually each day"
        DetectionMethod.MANUAL_CONFIRMED_FROM_DETECTION -> "Confirmed from detection prompt"
    }
