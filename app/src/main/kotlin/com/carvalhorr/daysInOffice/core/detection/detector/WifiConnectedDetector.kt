package com.carvalhorr.daysInOffice.core.detection.detector

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.carvalhorr.daysInOffice.core.detection.Detector
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig

private const val TAG = "Detection"

class WifiConnectedDetector(
    private val context: Context
) : Detector {

    override suspend fun isAtOffice(config: DetectionConfig): Boolean {
        val targetSsid = config.wifiSsid
        if (targetSsid.isNullOrBlank()) {
            Log.i(TAG, "WifiConnectedDetector.isAtOffice: target=null/blank, returning false")
            return false
        }

        val connectedSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            (cm.getNetworkCapabilities(cm.activeNetwork)?.transportInfo as? WifiInfo)?.ssid
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(WifiManager::class.java).connectionInfo.ssid
        }

        val stripped = connectedSsid?.removeSurrounding("\"")
        if (stripped == null) {
            Log.i(TAG, "WifiConnectedDetector.isAtOffice: target=$targetSsid, connected=$connectedSsid, stripped=null, match=false")
            return false
        }
        val match = stripped.equals(targetSsid, ignoreCase = true)
        Log.i(TAG, "WifiConnectedDetector.isAtOffice: target=$targetSsid, connected=$connectedSsid, stripped=$stripped, match=$match")
        return match
    }

    override suspend fun isAvailable(context: Context, config: DetectionConfig): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
}
