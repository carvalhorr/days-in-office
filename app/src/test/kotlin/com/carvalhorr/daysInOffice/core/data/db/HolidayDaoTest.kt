package com.carvalhorr.daysInOffice.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.carvalhorr.daysInOffice.core.data.db.dao.HolidayDao
import com.carvalhorr.daysInOffice.core.data.db.entity.HolidayEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HolidayDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: HolidayDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.holidayDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given holidays when getByDateRange then correct holidays returned`() = runTest {
        val holidays = listOf(
            HolidayEntity(LocalDate.of(2024, 1, 1), "New Year's Day", true, "MANUAL"),
            HolidayEntity(LocalDate.of(2024, 7, 4), "Independence Day", true, "MANUAL"),
            HolidayEntity(LocalDate.of(2024, 12, 25), "Christmas", true, "MANUAL")
        )
        holidays.forEach { dao.upsert(it) }

        val result = dao.getByDateRange(
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 7, 4)
        ).first()

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2024, 1, 1), result[0].date)
        assertEquals(LocalDate.of(2024, 7, 4), result[1].date)
    }

    @Test
    fun `given holidays when deleteAll then db is empty`() = runTest {
        val holidays = listOf(
            HolidayEntity(LocalDate.of(2024, 1, 1), "New Year's Day", true, "MANUAL"),
            HolidayEntity(LocalDate.of(2024, 7, 4), "Independence Day", true, "MANUAL")
        )
        holidays.forEach { dao.upsert(it) }

        dao.deleteAll()

        val result = dao.getByDateRange(
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 12, 31)
        ).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given holidays when deleteByDateRange then only records outside range remain`() = runTest {
        val holidays = listOf(
            HolidayEntity(LocalDate.of(2024, 1, 1), "New Year's Day", true, "MANUAL"),
            HolidayEntity(LocalDate.of(2024, 7, 4), "Independence Day", true, "MANUAL"),
            HolidayEntity(LocalDate.of(2024, 12, 25), "Christmas", true, "MANUAL")
        )
        holidays.forEach { dao.upsert(it) }

        dao.deleteByDateRange(
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 7, 31)
        )

        val result = dao.getByDateRange(
            start = LocalDate.of(2024, 1, 1),
            end = LocalDate.of(2024, 12, 31)
        ).first()
        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2024, 12, 25), result[0].date)
    }
}
