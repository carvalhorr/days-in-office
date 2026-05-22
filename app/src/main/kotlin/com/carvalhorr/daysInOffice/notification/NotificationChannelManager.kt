package com.carvalhorr.daysInOffice.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_DAILY_CHECKIN = "daily_checkin"
        const val CHANNEL_WEEKLY_SUMMARY = "weekly_summary"
        const val CHANNEL_DETECTION_PROMPT = "detection_prompt"
    }

    fun createChannels() {
        val nm = context.getSystemService<NotificationManager>() ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_CHECKIN,
                "Daily Check-in Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminder to confirm your office attendance"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WEEKLY_SUMMARY,
                "Weekly Compliance Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly summary of your in-office days"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DETECTION_PROMPT,
                "Office detection prompts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Prompts to confirm when automated detection suggests you may be at the office"
            }
        )
    }
}
