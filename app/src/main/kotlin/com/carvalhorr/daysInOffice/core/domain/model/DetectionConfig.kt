package com.carvalhorr.daysInOffice.core.domain.model

data class DetectionConfig(
    val enabledMethods: Set<DetectionMethod>,
    val wifiSsid: String?,
    val geofenceLatitude: Double?,
    val geofenceLongitude: Double?,
    val geofenceRadiusMeters: Float?
)
