package com.carvalhorr.daysInOffice.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class CheckInActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CheckInWidgetEntryPoint::class.java
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleCheckIn(
                    context,
                    entryPoint.recordOfficeDayUseCase(),
                    LocalDate.now()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        internal suspend fun handleCheckIn(
            context: Context,
            recordOfficeDayUseCase: RecordOfficeDayUseCase,
            date: LocalDate,
            widgetRefresh: suspend (Context) -> Unit = { ctx -> updateWidget(ctx) }
        ) {
            recordOfficeDayUseCase(date, DetectionMethod.MANUAL)
            widgetRefresh(context)
        }

        internal suspend fun updateWidget(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(CheckInWidget::class.java).forEach { id ->
                CheckInWidget().update(context, id)
            }
        }
    }
}
