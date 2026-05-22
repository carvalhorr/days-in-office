package com.carvalhorr.daysInOffice.core.detection

import com.carvalhorr.daysInOffice.core.detection.worker.DayDetectionWorker
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class DayDetectionWorkerTest {

    @Test
    fun `given Monday at 9am when shouldDetect then returns true`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertTrue(DayDetectionWorker.shouldDetect(monday, LocalTime.of(9, 0)))
    }

    @Test
    fun `given Friday at 6pm when shouldDetect then returns true`() {
        val friday = LocalDate.of(2026, 5, 22)
        assertTrue(DayDetectionWorker.shouldDetect(friday, LocalTime.of(18, 0)))
    }

    @Test
    fun `given Saturday at 9am when shouldDetect then returns false`() {
        val saturday = LocalDate.of(2026, 5, 17)
        assertFalse(DayDetectionWorker.shouldDetect(saturday, LocalTime.of(9, 0)))
    }

    @Test
    fun `given Sunday at 9am when shouldDetect then returns false`() {
        val sunday = LocalDate.of(2026, 5, 16)
        assertFalse(DayDetectionWorker.shouldDetect(sunday, LocalTime.of(9, 0)))
    }

    @Test
    fun `given weekday at exactly 7am when shouldDetect then returns true`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertTrue(DayDetectionWorker.shouldDetect(monday, LocalTime.of(7, 0)))
    }

    @Test
    fun `given weekday at exactly 7pm when shouldDetect then returns true`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertTrue(DayDetectionWorker.shouldDetect(monday, LocalTime.of(19, 0)))
    }

    @Test
    fun `given weekday one minute before 7am when shouldDetect then returns false`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertFalse(DayDetectionWorker.shouldDetect(monday, LocalTime.of(6, 59)))
    }

    @Test
    fun `given weekday one minute after 7pm when shouldDetect then returns false`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertFalse(DayDetectionWorker.shouldDetect(monday, LocalTime.of(19, 1)))
    }

    @Test
    fun `given Wednesday at noon when shouldDetect then returns true`() {
        val wednesday = LocalDate.of(2026, 5, 20)
        assertTrue(DayDetectionWorker.shouldDetect(wednesday, LocalTime.of(12, 0)))
    }

    @Test
    fun `given force_run true when Saturday at 10pm then shouldRun returns true`() {
        val saturday = LocalDate.of(2026, 5, 16)
        assertTrue(DayDetectionWorker.shouldRun(true, saturday, LocalTime.of(22, 0)))
    }

    @Test
    fun `given force_run false when Saturday at 10pm then shouldRun returns false`() {
        val saturday = LocalDate.of(2026, 5, 16)
        assertFalse(DayDetectionWorker.shouldRun(false, saturday, LocalTime.of(22, 0)))
    }

    @Test
    fun `given force_run true when weekday before 7am then shouldRun returns true`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertTrue(DayDetectionWorker.shouldRun(true, monday, LocalTime.of(6, 0)))
    }

    @Test
    fun `given force_run false when weekday in working hours then shouldRun returns true`() {
        val monday = LocalDate.of(2026, 5, 18)
        assertTrue(DayDetectionWorker.shouldRun(false, monday, LocalTime.of(9, 0)))
    }
}
