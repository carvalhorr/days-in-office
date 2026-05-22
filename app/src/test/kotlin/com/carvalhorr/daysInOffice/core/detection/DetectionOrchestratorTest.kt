package com.carvalhorr.daysInOffice.core.detection

import android.content.Context
import android.util.Log
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.notification.DetectionPromptNotificationWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetectionOrchestratorTest {

    private val mockContext = mockk<Context>()
    private val mockConfigRepository = mockk<MandateConfigRepository>()
    private val mockDayRecordRepository = mockk<DayRecordRepository>()
    private val mockNotificationWorker = mockk<DetectionPromptNotificationWorker>()
    private val mockWifiConnectedDetector = mockk<Detector>()
    private val mockWifiScanDetector = mockk<Detector>()

    private lateinit var orchestrator: DetectionOrchestrator

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { mockDayRecordRepository.getDayRecord(any()) } returns flowOf(null)
        coEvery { mockConfigRepository.isDetectorDismissedToday(any(), any()) } returns false
        every { mockNotificationWorker.postPromptNotification(any()) } returns true
        orchestrator = DetectionOrchestrator(
            detectors = mapOf(
                DetectionMethod.WIFI_CONNECTED to mockWifiConnectedDetector,
                DetectionMethod.WIFI_SCAN to mockWifiScanDetector
            ),
            configRepository = mockConfigRepository,
            dayRecordRepository = mockDayRecordRepository,
            detectionPromptNotificationWorker = mockNotificationWorker,
            context = mockContext
        )
    }

    private fun configWith(vararg methods: DetectionMethod) = DetectionConfig(
        enabledMethods = setOf(*methods),
        wifiSsid = "OfficeWifi",
        geofenceLatitude = null,
        geofenceLongitude = null,
        geofenceRadiusMeters = null
    )

    @Test
    fun `given detector available and at office and clean state when runDetection then fires detection prompt notification`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 1) { mockNotificationWorker.postPromptNotification(DetectionMethod.WIFI_CONNECTED) }
    }

    @Test
    fun `given today already has confirmed record when runDetection then no notification fired`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        val confirmedRecord = DayRecord(
            date = date,
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = true
        )
        every { mockDayRecordRepository.getDayRecord(date) } returns flowOf(confirmedRecord)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given detector dismissed today when runDetection then no notification fired`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns true
        coEvery { mockConfigRepository.isDetectorDismissedToday(DetectionMethod.WIFI_CONNECTED, date) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given detector not at office when runDetection then no notification fired`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns false

        orchestrator.runDetection(date)

        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given detector unavailable when runDetection then isAtOffice not called`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns false

        orchestrator.runDetection(date)

        coVerify(exactly = 0) { mockWifiConnectedDetector.isAtOffice(any()) }
        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given enabled method has no matching detector when runDetection then skips it`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.GEOFENCE))

        orchestrator.runDetection(date)

        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given no enabled methods when runDetection then no notification fired`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith())

        orchestrator.runDetection(date)

        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given first detector returns true when runDetection then second detector not queried and notification fired`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(
            configWith(DetectionMethod.WIFI_CONNECTED, DetectionMethod.WIFI_SCAN)
        )
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns true
        coEvery { mockWifiScanDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiScanDetector.isAtOffice(any()) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 1) { mockNotificationWorker.postPromptNotification(DetectionMethod.WIFI_CONNECTED) }
        coVerify(exactly = 0) { mockWifiScanDetector.isAtOffice(any()) }
    }

    @Test
    fun `given first detector returns false and second returns true when runDetection then notification fired for second`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(
            configWith(DetectionMethod.WIFI_CONNECTED, DetectionMethod.WIFI_SCAN)
        )
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns false
        coEvery { mockWifiScanDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiScanDetector.isAtOffice(any()) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 1) { mockNotificationWorker.postPromptNotification(DetectionMethod.WIFI_SCAN) }
    }

    @Test
    fun `given geofence config with persisted coordinates when runDetection then fires notification for geofence`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        val mockGeofenceDetector = mockk<Detector>()
        val orchestratorWithGeofence = DetectionOrchestrator(
            detectors = mapOf(DetectionMethod.GEOFENCE to mockGeofenceDetector),
            configRepository = mockConfigRepository,
            dayRecordRepository = mockDayRecordRepository,
            detectionPromptNotificationWorker = mockNotificationWorker,
            context = mockContext
        )
        val configWithGeofence = DetectionConfig(
            enabledMethods = setOf(DetectionMethod.GEOFENCE),
            wifiSsid = null,
            geofenceLatitude = 37.7749,
            geofenceLongitude = -122.4194,
            geofenceRadiusMeters = 200f
        )
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWithGeofence)
        coEvery { mockGeofenceDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockGeofenceDetector.isAtOffice(any()) } returns true

        orchestratorWithGeofence.runDetection(date)

        verify(exactly = 1) { mockNotificationWorker.postPromptNotification(DetectionMethod.GEOFENCE) }
    }

    @Test
    fun `given confirmed record with remote status when runDetection then no notification fired`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        val confirmedRemote = DayRecord(
            date = date,
            status = DayStatus.REMOTE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = true
        )
        every { mockDayRecordRepository.getDayRecord(date) } returns flowOf(confirmedRemote)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 0) { mockNotificationWorker.postPromptNotification(any()) }
    }

    @Test
    fun `given unconfirmed record when runDetection then notification fires`() = runTest {
        val date = LocalDate.of(2026, 5, 18)
        val unconfirmedRecord = DayRecord(
            date = date,
            status = DayStatus.UNKNOWN,
            detectionMethod = null,
            confirmedByUser = false
        )
        every { mockDayRecordRepository.getDayRecord(date) } returns flowOf(unconfirmedRecord)
        every { mockConfigRepository.getDetectionConfig() } returns flowOf(configWith(DetectionMethod.WIFI_CONNECTED))
        coEvery { mockWifiConnectedDetector.isAvailable(mockContext, any()) } returns true
        coEvery { mockWifiConnectedDetector.isAtOffice(any()) } returns true

        orchestrator.runDetection(date)

        verify(exactly = 1) { mockNotificationWorker.postPromptNotification(DetectionMethod.WIFI_CONNECTED) }
    }
}
