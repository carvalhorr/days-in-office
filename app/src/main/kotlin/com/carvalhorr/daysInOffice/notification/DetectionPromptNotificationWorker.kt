package com.carvalhorr.daysInOffice.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.carvalhorr.daysInOffice.app.MainActivity
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Detection"

@Singleton
class DetectionPromptNotificationWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationChannelManager: NotificationChannelManager
) {
    @SuppressLint("MissingPermission")
    fun postPromptNotification(detectionMethod: DetectionMethod): Boolean {
        notificationChannelManager.createChannels()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "DetectionPromptNotificationWorker.postPromptNotification: POST_NOTIFICATIONS not granted; aborting")
                return false
            }
        }

        Log.i(TAG, "DetectionPromptNotificationWorker.postPromptNotification: posting for $detectionMethod")

        val confirmIntent = Intent(context, DetectionPromptActionReceiver::class.java).apply {
            action = DetectionPromptActionReceiver.ACTION_CONFIRM_OFFICE
            putExtra(DetectionPromptActionReceiver.EXTRA_DETECTION_METHOD, detectionMethod.name)
        }
        val dismissIntent = Intent(context, DetectionPromptActionReceiver::class.java).apply {
            action = DetectionPromptActionReceiver.ACTION_DISMISS
            putExtra(DetectionPromptActionReceiver.EXTRA_DETECTION_METHOD, detectionMethod.name)
        }
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_DETECTION_PROMPT, true)
            putExtra(DetectionPromptActionReceiver.EXTRA_DETECTION_METHOD, detectionMethod.name)
        }

        val notification = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_DETECTION_PROMPT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Are you at the office?")
            .setContentText("We detected you may be at the office. Mark today as Office day?")
            .setContentIntent(
                PendingIntent.getActivity(
                    context, RC_OPEN_APP, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                NotificationCompat.Action(
                    0, "Yes, Office",
                    PendingIntent.getBroadcast(
                        context, RC_CONFIRM, confirmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    0, "No, dismiss",
                    PendingIntent.getBroadcast(
                        context, RC_DISMISS, dismissIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val EXTRA_SHOW_DETECTION_PROMPT = "show_detection_prompt"
        private const val RC_OPEN_APP = 200
        private const val RC_CONFIRM = 201
        private const val RC_DISMISS = 202
    }
}
