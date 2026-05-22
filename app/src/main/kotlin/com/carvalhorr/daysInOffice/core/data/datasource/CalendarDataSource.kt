package com.carvalhorr.daysInOffice.core.data.datasource

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.carvalhorr.daysInOffice.core.domain.model.Holiday
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarDataSource @Inject constructor(
    private val contentResolver: ContentResolver,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SOURCE = "CALENDAR"
        private val PTO_KEYWORDS = listOf("pto", "vacation", "holiday", "time off", "day off", "annual leave")
    }

    suspend fun readHolidays(start: LocalDate, end: LocalDate): List<Holiday> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val holidayCalendarIds = findHolidayCalendarIds()
        return queryEvents(start, end, holidayCalendarIds)
    }

    private fun findHolidayCalendarIds(): Set<Long> {
        val ids = mutableSetOf<Long>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                if (name.contains("Holiday", ignoreCase = true)) {
                    ids.add(cursor.getLong(idIdx))
                }
            }
        }

        return ids
    }

    private fun queryEvents(
        start: LocalDate,
        end: LocalDate,
        holidayCalendarIds: Set<Long>
    ): List<Holiday> {
        val startMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_ID
        )

        val selection = "${CalendarContract.Events.DTSTART} < ? AND ${CalendarContract.Events.DTEND} > ?"
        val selectionArgs = arrayOf(endMillis.toString(), startMillis.toString())

        val holidays = mutableListOf<Holiday>()

        contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val dtStartIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val dtEndIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val calendarIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIdx) ?: continue
                val dtStart = cursor.getLong(dtStartIdx)
                val dtEnd = cursor.getLong(dtEndIdx)
                val calendarId = cursor.getLong(calendarIdIdx)

                val isHolidayCalendar = calendarId in holidayCalendarIds
                val isTitleMatch = PTO_KEYWORDS.any { keyword ->
                    title.contains(keyword, ignoreCase = true)
                }

                if (!isHolidayCalendar && !isTitleMatch) continue

                val eventStart = Instant.ofEpochMilli(dtStart).atZone(ZoneOffset.UTC).toLocalDate()
                val eventEndRaw = Instant.ofEpochMilli(dtEnd).atZone(ZoneOffset.UTC).toLocalDate()
                // dtEnd is exclusive for all-day events; collapse same-day timed events to eventStart
                val eventEnd = if (eventEndRaw.isAfter(eventStart)) eventEndRaw.minusDays(1) else eventStart

                generateSequence(eventStart) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(eventEnd) }
                    .filter { !it.isBefore(start) && !it.isAfter(end) }
                    .forEach { date ->
                        holidays.add(
                            Holiday(
                                date = date,
                                name = title,
                                isPublicHoliday = isHolidayCalendar,
                                source = SOURCE
                            )
                        )
                    }
            }
        }

        return holidays
    }
}
