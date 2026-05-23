package com.carvalhorr.daysInOffice.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.carvalhorr.daysInOffice.app.MainActivity
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate

class DailyCheckInNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dayRecordRepository: DayRecordRepository,
    private val mandateConfigRepository: MandateConfigRepository,
    private val notificationChannelManager: NotificationChannelManager
) : CoroutineWorker(context, workerParams) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, workerParams: WorkerParameters): DailyCheckInNotificationWorker
    }

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        val workingDays = mandateConfigRepository.getMandateConfig().first().workingDays
        val dayRecord = dayRecordRepository.getDayRecord(today).first()
        val status = dayRecord?.status ?: DayStatus.UNKNOWN

        if (!shouldNotify(status, dayOfWeek, workingDays)) return Result.success()

        notificationChannelManager.createChannels()
        postCheckInNotification()
        return Result.success()
    }

    @Suppress("MissingPermission")
    private fun postCheckInNotification() {
        val officeIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_OFFICE
        }
        val remoteIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REMOTE
        }
        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notification = NotificationCompat.Builder(
            applicationContext, NotificationChannelManager.CHANNEL_DAILY_CHECKIN
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Did you go to the office today?")
            .setContentText("Tap to confirm your attendance")
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext, REQUEST_CODE_OPEN_APP, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                NotificationCompat.Action(
                    0, "Office",
                    PendingIntent.getBroadcast(
                        applicationContext, REQUEST_CODE_OFFICE, officeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    0, "Remote",
                    PendingIntent.getBroadcast(
                        applicationContext, REQUEST_CODE_REMOTE, remoteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_OFFICE = 100
        private const val REQUEST_CODE_REMOTE = 101
        private const val REQUEST_CODE_OPEN_APP = 102

        internal fun shouldNotify(
            status: DayStatus,
            dayOfWeek: DayOfWeek,
            workingDays: Set<DayOfWeek>
        ): Boolean {
            if (dayOfWeek !in workingDays) return false
            return status == DayStatus.UNKNOWN
        }
    }
}
