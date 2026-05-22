package com.carvalhorr.daysInOffice.core.detection.detector

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.util.Log
import com.carvalhorr.daysInOffice.core.detection.Detector
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig

private const val TAG = "Detection"

class WifiScanDetector(
    private val context: Context
) : Detector {

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override suspend fun isAtOffice(config: DetectionConfig): Boolean {
        val targetSsid = config.wifiSsid
        if (targetSsid.isNullOrBlank()) {
            Log.i(TAG, "WifiScanDetector.isAtOffice: target=null/blank, returning false")
            return false
        }

        val wifiManager = context.getSystemService(WifiManager::class.java)
        wifiManager.startScan()

        val results = wifiManager.scanResults
        Log.i(TAG, "WifiScanDetector.isAtOffice: scan results count=${results.size}, target=$targetSsid")
        val match = results.any { result ->
            result.SSID.removeSurrounding("\"").equals(targetSsid, ignoreCase = true)
        }
        Log.i(TAG, "WifiScanDetector.isAtOffice: match=$match")
        return match
    }

    override suspend fun isAvailable(context: Context, config: DetectionConfig): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
}
