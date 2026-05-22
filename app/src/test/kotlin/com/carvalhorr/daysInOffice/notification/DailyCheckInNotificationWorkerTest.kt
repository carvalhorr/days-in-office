package com.carvalhorr.daysInOffice.notification

import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class DailyCheckInNotificationWorkerTest {

    private val defaultWorkingDays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    @Test
    fun `given UNKNOWN status on working day when shouldNotify then returns true`() {
        assertTrue(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.UNKNOWN, DayOfWeek.MONDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given HOLIDAY status on working day when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.HOLIDAY, DayOfWeek.MONDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given PTO status on working day when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.PTO, DayOfWeek.MONDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given UNKNOWN status on Saturday when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.UNKNOWN, DayOfWeek.SATURDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given UNKNOWN status on Sunday when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.UNKNOWN, DayOfWeek.SUNDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given OFFICE status on working day when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.OFFICE, DayOfWeek.MONDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given REMOTE status on working day when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.REMOTE, DayOfWeek.MONDAY, defaultWorkingDays
            )
        )
    }

    @Test
    fun `given UNKNOWN status but day not in configured working days when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.UNKNOWN, DayOfWeek.MONDAY, setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
            )
        )
    }

    @Test
    fun `given WEEKEND status on Saturday when shouldNotify then returns false`() {
        assertFalse(
            DailyCheckInNotificationWorker.shouldNotify(
                DayStatus.WEEKEND, DayOfWeek.SATURDAY, defaultWorkingDays
            )
        )
    }
}
