package com.carvalhorr.daysInOffice.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckInWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val recordOfficeDayUseCase = mockk<RecordOfficeDayUseCase>(relaxed = true)
    private val testDate = LocalDate.of(2026, 5, 18)

    @Test
    fun `given check in action when handleCheckIn then records office day with manual detection`() = runTest {
        CheckInActionReceiver.handleCheckIn(context, recordOfficeDayUseCase, testDate) {}

        coVerify { recordOfficeDayUseCase(testDate, DetectionMethod.MANUAL) }
    }

    @Test
    fun `given check in action when handleCheckIn then triggers widget refresh`() = runTest {
        var widgetRefreshed = false

        CheckInActionReceiver.handleCheckIn(context, recordOfficeDayUseCase, testDate) {
            widgetRefreshed = true
        }

        assertTrue(widgetRefreshed)
    }
}
