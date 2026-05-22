package com.carvalhorr.daysInOffice.widget

import android.content.Context
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import com.carvalhorr.daysInOffice.app.MainActivity
import com.carvalhorr.daysInOffice.core.domain.usecase.GetComplianceUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CheckInWidgetEntryPoint {
    fun getComplianceUseCase(): GetComplianceUseCase
    fun recordOfficeDayUseCase(): RecordOfficeDayUseCase
}

class CheckInWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val percentage = getCompliancePercentage(context)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().clickable(
                        actionStartActivity<MainActivity>()
                    ),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(text = "${(percentage * 100).toInt()}%")
                    Button(
                        text = "Check In",
                        onClick = actionSendBroadcast<CheckInActionReceiver>()
                    )
                }
            }
        }
    }

    private suspend fun getCompliancePercentage(context: Context): Float {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                CheckInWidgetEntryPoint::class.java
            )
            entryPoint.getComplianceUseCase().invoke().first().currentPercentage
        } catch (e: Exception) {
            0f
        }
    }
}
