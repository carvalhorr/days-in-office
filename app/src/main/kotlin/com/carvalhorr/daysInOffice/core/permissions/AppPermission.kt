package com.carvalhorr.daysInOffice.core.permissions

import android.Manifest

enum class AppPermission(
    val manifestPermission: String,
    val rationaleText: String,
    val deniedPermanentlyText: String
) {
    FINE_LOCATION(
        manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
        rationaleText = "Location access is needed to detect when you're at the office using Wi-Fi or geofence.",
        deniedPermanentlyText = "Location permission is required for office detection. Please enable it in app settings."
    ),
    BACKGROUND_LOCATION(
        manifestPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        rationaleText = "Background location access allows the app to automatically detect office visits throughout the day.",
        deniedPermanentlyText = "Background location permission is required for automatic detection. Please enable it in app settings."
    ),
    NOTIFICATIONS(
        manifestPermission = "android.permission.POST_NOTIFICATIONS",
        rationaleText = "Notifications allow the app to remind you to check in on office days.",
        deniedPermanentlyText = "Notification permission is required for daily check-in reminders. Please enable it in app settings."
    ),
    NEARBY_WIFI_DEVICES(
        manifestPermission = "android.permission.NEARBY_WIFI_DEVICES",
        rationaleText = "Nearby Wi-Fi devices permission is needed to scan for Wi-Fi networks on Android 13+.",
        deniedPermanentlyText = "Nearby Wi-Fi devices permission is required for Wi-Fi scanning. Please enable it in app settings."
    )
}
