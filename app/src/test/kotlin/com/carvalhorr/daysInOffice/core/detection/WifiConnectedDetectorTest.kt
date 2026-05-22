package com.carvalhorr.daysInOffice.core.detection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.carvalhorr.daysInOffice.core.detection.detector.WifiConnectedDetector
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class WifiConnectedDetectorTest {

    private val mockContext = mockk<Context>()
    private val mockConnectivityManager = mockk<ConnectivityManager>()
    private val mockWifiManager = mockk<WifiManager>()

    private fun makeConfig(ssid: String?) = DetectionConfig(
        enabledMethods = setOf(DetectionMethod.WIFI_CONNECTED),
        wifiSsid = ssid,
        geofenceLatitude = null,
        geofenceLongitude = null,
        geofenceRadiusMeters = null
    )

    private fun setupApi31(connectedSsid: String?) {
        val mockNetwork = mockk<Network>()
        val mockCapabilities = mockk<NetworkCapabilities>()
        val mockWifiInfo = mockk<WifiInfo>()
        every { mockContext.getSystemService(ConnectivityManager::class.java) } returns mockConnectivityManager
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        every { mockCapabilities.transportInfo } returns mockWifiInfo
        every { mockWifiInfo.ssid } returns connectedSsid
    }

    @Test
    fun `given connected SSID matches config when isAtOffice then returns true`() = runTest {
        setupApi31("\"OfficeWifi\"")
        val detector = WifiConnectedDetector(mockContext)
        assertTrue(detector.isAtOffice(makeConfig("OfficeWifi")))
    }

    @Test
    fun `given connected SSID does not match config when isAtOffice then returns false`() = runTest {
        setupApi31("\"HomeWifi\"")
        val detector = WifiConnectedDetector(mockContext)
        assertFalse(detector.isAtOffice(makeConfig("OfficeWifi")))
    }

    @Test
    fun `given SSID wrapped in quotes when isAtOffice then quotes stripped before comparison`() = runTest {
        setupApi31("\"OfficeWifi\"")
        val detector = WifiConnectedDetector(mockContext)
        assertTrue(detector.isAtOffice(makeConfig("OfficeWifi")))
    }

    @Test
    fun `given config has null SSID when isAtOffice then returns false`() = runTest {
        val detector = WifiConnectedDetector(mockContext)
        assertFalse(detector.isAtOffice(makeConfig(null)))
    }

    @Test
    fun `given config has blank SSID when isAtOffice then returns false`() = runTest {
        val detector = WifiConnectedDetector(mockContext)
        assertFalse(detector.isAtOffice(makeConfig("   ")))
    }

    @Test
    fun `isAtOffice_returnsFalse_whenTargetSsidBlank_andLogsExplicitly`() = runTest {
        val detector = WifiConnectedDetector(mockContext)
        // null target
        assertFalse(detector.isAtOffice(makeConfig(null)))
        // blank target
        assertFalse(detector.isAtOffice(makeConfig("")))
        assertFalse(detector.isAtOffice(makeConfig("   ")))
        // No throw; getSystemService should not even be invoked
        verify(exactly = 0) { mockContext.getSystemService(ConnectivityManager::class.java) }
        verify(exactly = 0) { mockContext.getSystemService(WifiManager::class.java) }
    }

    @Test
    fun `given API level at least 31 when isAtOffice then ConnectivityManager path used`() = runTest {
        setupApi31("\"OfficeWifi\"")
        val detector = WifiConnectedDetector(mockContext)
        detector.isAtOffice(makeConfig("OfficeWifi"))
        verify { mockContext.getSystemService(ConnectivityManager::class.java) }
        verify(exactly = 0) { mockContext.getSystemService(WifiManager::class.java) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `given API level less than 31 when isAtOffice then WifiManager connectionInfo path used`() = runTest {
        val mockWifiInfo = mockk<WifiInfo>()
        every { mockContext.getSystemService(WifiManager::class.java) } returns mockWifiManager
        every { mockWifiManager.connectionInfo } returns mockWifiInfo
        every { mockWifiInfo.ssid } returns "\"OfficeWifi\""

        val detector = WifiConnectedDetector(mockContext)
        assertTrue(detector.isAtOffice(makeConfig("OfficeWifi")))
        verify { mockContext.getSystemService(WifiManager::class.java) }
        verify(exactly = 0) { mockContext.getSystemService(ConnectivityManager::class.java) }
    }
}
