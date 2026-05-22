package com.carvalhorr.daysInOffice.notification

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    companion object {
        const val WORK_NAME = "daily_checkin_notification"
        val DEFAULT_NOTIFICATION_TIME: LocalTime = LocalTime.of(18, 0)
    }

    fun schedule(notificationTime: LocalTime = DEFAULT_NOTIFICATION_TIME) {
        val now = LocalDateTime.now()
        val todayAt = now.toLocalDate().atTime(notificationTime)
        val nextFire = if (now.isBefore(todayAt)) todayAt else todayAt.plusDays(1)
        val initialDelayMinutes = Duration.between(now, nextFire).toMinutes()

        val request = PeriodicWorkRequestBuilder<DailyCheckInNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
