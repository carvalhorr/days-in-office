package com.carvalhorr.daysInOffice.notification

import androidx.core.app.NotificationManagerCompat
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetectionPromptActionReceiverTest {

    private val mockRecordOfficeDayUseCase = mockk<RecordOfficeDayUseCase>()
    private val mockMandateConfigRepository = mockk<MandateConfigRepository>()
    private val mockNotificationManager = mockk<NotificationManagerCompat>(relaxed = true)

    private lateinit var receiver: DetectionPromptActionReceiver

    @BeforeEach
    fun setUp() {
        receiver = DetectionPromptActionReceiver().also {
            it.recordOfficeDayUseCase = mockRecordOfficeDayUseCase
            it.mandateConfigRepository = mockMandateConfigRepository
        }
    }

    @Test
    fun `given CONFIRM_OFFICE action when handleAction then records office day with MANUAL_CONFIRMED_FROM_DETECTION`() = runTest {
        val date = LocalDate.of(2026, 5, 20)
        coJustRun { mockRecordOfficeDayUseCase(any(), any()) }

        receiver.handleAction(
            DetectionPromptActionReceiver.ACTION_CONFIRM_OFFICE,
            date,
            DetectionMethod.WIFI_CONNECTED,
            mockNotificationManager
        )

        coVerify(exactly = 1) {
            mockRecordOfficeDayUseCase(date, DetectionMethod.MANUAL_CONFIRMED_FROM_DETECTION)
        }
    }

    @Test
    fun `given CONFIRM_OFFICE action when handleAction then does not write suppression`() = runTest {
        val date = LocalDate.of(2026, 5, 20)
        coJustRun { mockRecordOfficeDayUseCase(any(), any()) }

        receiver.handleAction(
            DetectionPromptActionReceiver.ACTION_CONFIRM_OFFICE,
            date,
            DetectionMethod.WIFI_CONNECTED,
            mockNotificationManager
        )

        coVerify(exactly = 0) { mockMandateConfigRepository.markDetectorDismissed(any(), any()) }
    }

    @Test
    fun `given DISMISS action when handleAction then marks detector dismissed`() = runTest {
        val date = LocalDate.of(2026, 5, 20)
        coJustRun { mockMandateConfigRepository.markDetectorDismissed(any(), any()) }

        receiver.handleAction(
            DetectionPromptActionReceiver.ACTION_DISMISS,
            date,
            DetectionMethod.WIFI_CONNECTED,
            mockNotificationManager
        )

        coVerify(exactly = 1) {
            mockMandateConfigRepository.markDetectorDismissed(DetectionMethod.WIFI_CONNECTED, date)
        }
    }

    @Test
    fun `given DISMISS action when handleAction then does not write office day record`() = runTest {
        val date = LocalDate.of(2026, 5, 20)
        coJustRun { mockMandateConfigRepository.markDetectorDismissed(any(), any()) }

        receiver.handleAction(
            DetectionPromptActionReceiver.ACTION_DISMISS,
            date,
            DetectionMethod.WIFI_CONNECTED,
            mockNotificationManager
        )

        coVerify(exactly = 0) { mockRecordOfficeDayUseCase(any(), any()) }
    }

    @Test
    fun `given DISMISS for geofence when handleAction then suppresses geofence not wifi`() = runTest {
        val date = LocalDate.of(2026, 5, 20)
        coJustRun { mockMandateConfigRepository.markDetectorDismissed(any(), any()) }

        receiver.handleAction(
            DetectionPromptActionReceiver.ACTION_DISMISS,
            date,
            DetectionMethod.GEOFENCE,
            mockNotificationManager
        )

        coVerify(exactly = 1) {
            mockMandateConfigRepository.markDetectorDismissed(DetectionMethod.GEOFENCE, date)
        }
        coVerify(exactly = 0) {
            mockMandateConfigRepository.markDetectorDismissed(DetectionMethod.WIFI_CONNECTED, date)
        }
    }

    @Test
    fun `given null action when handleAction then neither use case nor suppression called`() = runTest {
        val date = LocalDate.of(2026, 5, 20)

        receiver.handleAction(
            null,
            date,
            DetectionMethod.WIFI_CONNECTED,
            mockNotificationManager
        )

        coVerify(exactly = 0) { mockRecordOfficeDayUseCase(any(), any()) }
        coVerify(exactly = 0) { mockMandateConfigRepository.markDetectorDismissed(any(), any()) }
    }
}
