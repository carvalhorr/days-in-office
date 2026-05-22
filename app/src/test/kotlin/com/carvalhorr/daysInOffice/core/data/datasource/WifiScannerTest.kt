package com.carvalhorr.daysInOffice.core.data.datasource

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WifiScannerTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockWifiManager = mockk<WifiManager>(relaxed = true)
    private lateinit var scanner: WifiScanner

    @BeforeEach
    fun setup() {
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSystemService(Context.WIFI_SERVICE) } returns mockWifiManager
        scanner = WifiScanner(mockContext)
    }

    @Test
    fun `given startScan throttled when scanForSsids then returns failure`() = runTest {
        every { mockWifiManager.startScan() } returns false

        val result = scanner.scanForSsids()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("throttled") == true)
    }

    @Test
    fun `given scan succeeds when scanForSsids then returns unique non-empty ssids`() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        every { mockContext.registerReceiver(capture(receiverSlot), any()) } returns mockk()
        every { mockWifiManager.startScan() } returns true

        val result1 = mockk<ScanResult>(relaxed = true).also { it.SSID = "OfficeNetwork" }
        val result2 = mockk<ScanResult>(relaxed = true).also { it.SSID = "GuestNetwork" }
        val result3 = mockk<ScanResult>(relaxed = true).also { it.SSID = "OfficeNetwork" }
        every { mockWifiManager.scanResults } returns listOf(result1, result2, result3)

        val deferred = async { scanner.scanForSsids() }
        runCurrent()
        receiverSlot.captured.onReceive(mockContext, Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        val result = deferred.await()

        assertTrue(result.isSuccess)
        assertEquals(listOf("OfficeNetwork", "GuestNetwork"), result.getOrNull())
    }

    @Test
    fun `given scan results contain empty ssids when scanForSsids then filters them out`() = runTest {
        val receiverSlot = slot<BroadcastReceiver>()
        every { mockContext.registerReceiver(capture(receiverSlot), any()) } returns mockk()
        every { mockWifiManager.startScan() } returns true

        val result1 = mockk<ScanResult>(relaxed = true).also { it.SSID = "ValidNetwork" }
        val result2 = mockk<ScanResult>(relaxed = true).also { it.SSID = "" }
        every { mockWifiManager.scanResults } returns listOf(result1, result2)

        val deferred = async { scanner.scanForSsids() }
        runCurrent()
        receiverSlot.captured.onReceive(mockContext, Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        val result = deferred.await()

        assertTrue(result.isSuccess)
        assertEquals(listOf("ValidNetwork"), result.getOrNull())
    }

    @Test
    fun `given scan times out when scanForSsids then returns failure`() = runTest {
        every { mockContext.registerReceiver(any(), any()) } returns mockk()
        every { mockWifiManager.startScan() } returns true

        val deferred = async { scanner.scanForSsids() }
        runCurrent()
        advanceTimeBy(11_000L)

        val result = deferred.await()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("timed out") == true)
    }

    @Test
    fun `given permission denied when scanForSsids then returns failure with security exception`() = runTest {
        every { mockContext.registerReceiver(any(), any()) } returns mockk()
        every { mockWifiManager.startScan() } throws SecurityException("Permission denied")

        val result = scanner.scanForSsids()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
