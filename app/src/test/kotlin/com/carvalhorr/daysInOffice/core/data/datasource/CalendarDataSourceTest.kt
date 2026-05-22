package com.carvalhorr.daysInOffice.core.data.datasource

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CalendarDataSourceTest {

    private lateinit var context: Application
    private val mockContentResolver = mockk<ContentResolver>()
    private lateinit var dataSource: CalendarDataSource

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context).grantPermissions(android.Manifest.permission.READ_CALENDAR)
        dataSource = CalendarDataSource(mockContentResolver, context)
    }

    @Test
    fun `given READ_CALENDAR permission not granted when readHolidays then empty list returned`() {
        shadowOf(context).denyPermissions(android.Manifest.permission.READ_CALENDAR)

        val result = runBlocking {
            dataSource.readHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        }

        assertTrue(result.isEmpty())
        verify(exactly = 0) { mockContentResolver.query(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given ContentResolver returns PTO event when readHolidays then Holiday with isPublicHoliday=false returned`() {
        val date = LocalDate.of(2026, 1, 5)
        val dtStart = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dtEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returnsMany listOf(
            emptyCalendarCursor(),
            eventCursor("Annual Leave", dtStart, dtEnd, calendarId = 999L)
        )

        val result = runBlocking {
            dataSource.readHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        }

        assertEquals(1, result.size)
        assertFalse(result[0].isPublicHoliday)
        assertEquals(date, result[0].date)
        assertEquals("CALENDAR", result[0].source)
    }

    @Test
    fun `given ContentResolver returns event from holiday calendar when readHolidays then Holiday with isPublicHoliday=true returned`() {
        val date = LocalDate.of(2026, 1, 1)
        val dtStart = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dtEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returnsMany listOf(
            calendarCursor(id = 42L, name = "Holidays in United States"),
            eventCursor("New Year's Day", dtStart, dtEnd, calendarId = 42L)
        )

        val result = runBlocking {
            dataSource.readHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        }

        assertEquals(1, result.size)
        assertTrue(result[0].isPublicHoliday)
        assertEquals(date, result[0].date)
    }

    @Test
    fun `given multi-day PTO event spanning 3 days when readHolidays then 3 Holiday objects returned`() {
        val eventStart = LocalDate.of(2026, 1, 5)
        val dtStart = eventStart.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        // all-day events: dtEnd is midnight of the day after the last day
        val dtEnd = eventStart.plusDays(3).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        every { mockContentResolver.query(any(), any(), any(), any(), any()) } returnsMany listOf(
            emptyCalendarCursor(),
            eventCursor("PTO", dtStart, dtEnd, calendarId = 999L)
        )

        val result = runBlocking {
            dataSource.readHolidays(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))
        }

        assertEquals(3, result.size)
        assertEquals(LocalDate.of(2026, 1, 5), result[0].date)
        assertEquals(LocalDate.of(2026, 1, 6), result[1].date)
        assertEquals(LocalDate.of(2026, 1, 7), result[2].date)
        assertTrue(result.all { !it.isPublicHoliday })
    }

    private fun emptyCalendarCursor(): Cursor = mockk<Cursor>(relaxed = true).also {
        every { it.moveToNext() } returns false
        every { it.close() } just Runs
    }

    private fun calendarCursor(id: Long, name: String): Cursor = mockk<Cursor>(relaxed = true).also {
        every { it.getColumnIndexOrThrow(CalendarContract.Calendars._ID) } returns 0
        every { it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME) } returns 1
        every { it.moveToNext() } returnsMany listOf(true, false)
        every { it.getLong(0) } returns id
        every { it.getString(1) } returns name
        every { it.close() } just Runs
    }

    private fun eventCursor(title: String, dtStart: Long, dtEnd: Long, calendarId: Long): Cursor =
        mockk<Cursor>(relaxed = true).also {
            every { it.getColumnIndexOrThrow(CalendarContract.Events.TITLE) } returns 0
            every { it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART) } returns 1
            every { it.getColumnIndexOrThrow(CalendarContract.Events.DTEND) } returns 2
            every { it.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID) } returns 3
            every { it.moveToNext() } returnsMany listOf(true, false)
            every { it.getString(0) } returns title
            every { it.getLong(1) } returns dtStart
            every { it.getLong(2) } returns dtEnd
            every { it.getLong(3) } returns calendarId
            every { it.close() } just Runs
        }
}
