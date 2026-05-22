package com.carvalhorr.daysInOffice.core.detection

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.carvalhorr.daysInOffice.core.detection.detector.WifiScanDetector
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class WifiScanDetectorTest {

    private val mockContext = mockk<Context>()
    private val mockWifiManager = mockk<WifiManager>()

    private fun makeConfig(ssid: String?) = DetectionConfig(
        enabledMethods = setOf(DetectionMethod.WIFI_SCAN),
        wifiSsid = ssid,
        geofenceLatitude = null,
        geofenceLongitude = null,
        geofenceRadiusMeters = null
    )

    @Suppress("DEPRECATION")
    private fun makeScanResult(ssid: String): ScanResult {
        val constructor = ScanResult::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val result = constructor.newInstance()
        result.SSID = ssid
        return result
    }

    @Suppress("DEPRECATION")
    private fun setupWifiManager(scanResults: List<ScanResult>, startScanResult: Boolean = true) {
        every { mockContext.getSystemService(WifiManager::class.java) } returns mockWifiManager
        every { mockWifiManager.startScan() } returns startScanResult
        every { mockWifiManager.scanResults } returns scanResults
    }

    @Test
    fun `given scan results contain office SSID when isAtOffice then returns true`() = runTest {
        setupWifiManager(listOf(makeScanResult("OfficeWifi"), makeScanResult("OtherNetwork")))
        val detector = WifiScanDetector(mockContext)
        assertTrue(detector.isAtOffice(makeConfig("OfficeWifi")))
    }

    @Test
    fun `given scan results do not contain office SSID when isAtOffice then returns false`() = runTest {
        setupWifiManager(listOf(makeScanResult("HomeWifi"), makeScanResult("OtherNetwork")))
        val detector = WifiScanDetector(mockContext)
        assertFalse(detector.isAtOffice(makeConfig("OfficeWifi")))
    }

    @Test
    fun `given startScan returns false when isAtOffice then falls back to existing scan results`() = runTest {
        setupWifiManager(listOf(makeScanResult("OfficeWifi")), startScanResult = false)
        val detector = WifiScanDetector(mockContext)
        assertTrue(detector.isAtOffice(makeConfig("OfficeWifi")))
    }

    @Test
    fun `given config has null SSID when isAtOffice then returns false`() = runTest {
        val detector = WifiScanDetector(mockContext)
        assertFalse(detector.isAtOffice(makeConfig(null)))
    }

    @Test
    fun `given empty scan results when isAtOffice then returns false`() = runTest {
        setupWifiManager(emptyList())
        val detector = WifiScanDetector(mockContext)
        assertFalse(detector.isAtOffice(makeConfig("OfficeWifi")))
    }
}
