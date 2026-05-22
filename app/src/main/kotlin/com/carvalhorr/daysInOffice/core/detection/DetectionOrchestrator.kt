package com.carvalhorr.daysInOffice.core.detection

import android.content.Context
import android.util.Log
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.notification.DetectionPromptNotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "Detection"

enum class DetectionOutcome { NOTIFICATION_FIRED, NO_SIGNAL, ALREADY_CONFIRMED, PERMISSION_DENIED }

class DetectionOrchestrator @Inject constructor(
    private val detectors: Map<DetectionMethod, @JvmSuppressWildcards Detector>,
    private val configRepository: MandateConfigRepository,
    private val dayRecordRepository: DayRecordRepository,
    private val detectionPromptNotificationWorker: DetectionPromptNotificationWorker,
    @ApplicationContext private val context: Context
) {
    suspend fun runDetection(date: LocalDate): DetectionOutcome {
        Log.i(TAG, "DetectionOrchestrator.runDetection: entry (date=$date)")
        val todayRecord = dayRecordRepository.getDayRecord(date).first()
        val todayConfirmed = todayRecord?.confirmedByUser == true
        Log.i(TAG, "DetectionOrchestrator.runDetection: todayConfirmed=$todayConfirmed")
        if (todayConfirmed) return DetectionOutcome.ALREADY_CONFIRMED

        val config = configRepository.getDetectionConfig().first()
        for (method in config.enabledMethods) {
            val detector = detectors[method]
            val available = detector?.isAvailable(context, config) ?: false
            val atOffice = if (available && detector != null) detector.isAtOffice(config) else false
            val suppressed = if (available && atOffice) configRepository.isDetectorDismissedToday(method, date) else false
            val action = when {
                detector == null -> "no binding"
                !available -> "skipped (unavailable)"
                !atOffice -> "no signal"
                suppressed -> "suppressed"
                else -> "firing notification"
            }
            Log.i(TAG, "DetectionOrchestrator.runDetection: $method: isAvailable=$available, isAtOffice=$atOffice, suppressed=$suppressed → $action")
            if (detector != null && available && atOffice && !suppressed) {
                val fired = detectionPromptNotificationWorker.postPromptNotification(method)
                return if (fired) DetectionOutcome.NOTIFICATION_FIRED else DetectionOutcome.PERMISSION_DENIED
            }
        }
        return DetectionOutcome.NO_SIGNAL
    }
}
