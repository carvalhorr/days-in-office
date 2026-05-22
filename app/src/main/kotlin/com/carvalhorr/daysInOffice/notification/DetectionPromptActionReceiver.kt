package com.carvalhorr.daysInOffice.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class DetectionPromptActionReceiver : BroadcastReceiver() {

    @Inject lateinit var recordOfficeDayUseCase: RecordOfficeDayUseCase
    @Inject lateinit var mandateConfigRepository: MandateConfigRepository

    companion object {
        const val ACTION_CONFIRM_OFFICE = "com.carvalhorr.daysInOffice.ACTION_CONFIRM_OFFICE"
        const val ACTION_DISMISS = "com.carvalhorr.daysInOffice.ACTION_DISMISS_DETECTION"
        const val EXTRA_DETECTION_METHOD = "detection_method"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val methodName = intent.getStringExtra(EXTRA_DETECTION_METHOD) ?: return
        val method = runCatching { DetectionMethod.valueOf(methodName) }.getOrNull() ?: return
        val notificationManager = NotificationManagerCompat.from(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAction(intent.action, LocalDate.now(), method, notificationManager)
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handleAction(
        action: String?,
        date: LocalDate,
        method: DetectionMethod,
        notificationManager: NotificationManagerCompat
    ) {
        when (action) {
            ACTION_CONFIRM_OFFICE -> {
                recordOfficeDayUseCase(date, DetectionMethod.MANUAL_CONFIRMED_FROM_DETECTION)
                notificationManager.cancel(DetectionPromptNotificationWorker.NOTIFICATION_ID)
            }
            ACTION_DISMISS -> {
                mandateConfigRepository.markDetectorDismissed(method, date)
                notificationManager.cancel(DetectionPromptNotificationWorker.NOTIFICATION_ID)
            }
        }
    }
}
