package com.carvalhorr.daysInOffice.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.carvalhorr.daysInOffice.core.data.db.dao.DayRecordDao
import com.carvalhorr.daysInOffice.core.data.db.entity.DayRecordEntity
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayRecordDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DayRecordDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.dayRecordDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `given empty db when upsert then getByDate returns record`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val record = DayRecordEntity(
            date = date,
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.WIFI_CONNECTED,
            confirmedByUser = false,
            createdAt = Instant.ofEpochMilli(1000),
            updatedAt = Instant.ofEpochMilli(1000)
        )

        dao.upsert(record)

        val result = dao.getByDate(date).first()
        assertEquals(record, result)
    }

    @Test
    fun `given existing record when upsert with same date then record is updated`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val original = DayRecordEntity(
            date = date,
            status = DayStatus.OFFICE,
            detectionMethod = null,
            confirmedByUser = false,
            createdAt = Instant.ofEpochMilli(1000),
            updatedAt = Instant.ofEpochMilli(1000)
        )
        dao.upsert(original)

        val updated = original.copy(status = DayStatus.REMOTE, updatedAt = Instant.ofEpochMilli(2000))
        dao.upsert(updated)

        val result = dao.getByDate(date).first()
        assertEquals(DayStatus.REMOTE, result?.status)
        assertEquals(Instant.ofEpochMilli(2000), result?.updatedAt)
    }

    @Test
    fun `given multiple records when getByDateRange then only records in range returned`() = runTest {
        val base = Instant.ofEpochMilli(0)
        val records = listOf(
            DayRecordEntity(LocalDate.of(2024, 1, 10), DayStatus.OFFICE, null, false, base, base),
            DayRecordEntity(LocalDate.of(2024, 1, 15), DayStatus.REMOTE, null, false, base, base),
            DayRecordEntity(LocalDate.of(2024, 1, 20), DayStatus.OFFICE, null, false, base, base),
            DayRecordEntity(LocalDate.of(2024, 1, 25), DayStatus.UNKNOWN, null, false, base, base)
        )
        records.forEach { dao.upsert(it) }

        val result = dao.getByDateRange(
            start = LocalDate.of(2024, 1, 14),
            end = LocalDate.of(2024, 1, 21)
        ).first()

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2024, 1, 15), result[0].date)
        assertEquals(LocalDate.of(2024, 1, 20), result[1].date)
    }

    @Test
    fun `given record when delete then getByDate returns null`() = runTest {
        val date = LocalDate.of(2024, 1, 15)
        val record = DayRecordEntity(
            date = date,
            status = DayStatus.OFFICE,
            detectionMethod = null,
            confirmedByUser = false,
            createdAt = Instant.ofEpochMilli(0),
            updatedAt = Instant.ofEpochMilli(0)
        )
        dao.upsert(record)

        dao.delete(date)

        val result = dao.getByDate(date).first()
        assertNull(result)
    }
}
