package com.carvalhorr.daysInOffice.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordRemoteDayUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var recordOfficeDayUseCase: RecordOfficeDayUseCase
    @Inject lateinit var recordRemoteDayUseCase: RecordRemoteDayUseCase

    companion object {
        const val ACTION_OFFICE = "com.carvalhorr.daysInOffice.ACTION_OFFICE"
        const val ACTION_REMOTE = "com.carvalhorr.daysInOffice.ACTION_REMOTE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val today = LocalDate.now()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_OFFICE -> recordOfficeDayUseCase(today, DetectionMethod.MANUAL)
                    ACTION_REMOTE -> recordRemoteDayUseCase(today)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
