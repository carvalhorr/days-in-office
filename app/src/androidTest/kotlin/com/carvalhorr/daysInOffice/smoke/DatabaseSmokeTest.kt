package com.carvalhorr.daysInOffice.smoke

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.carvalhorr.daysInOffice.core.data.db.AppDatabase
import com.carvalhorr.daysInOffice.core.data.db.entity.DayRecordEntity
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DatabaseSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("smoke.db")
        db = Room.databaseBuilder(context, AppDatabase::class.java, "smoke.db").build()
    }

    @After
    fun tearDown() {
        db.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase("smoke.db")
    }

    @Test
    fun givenEntityWhenInsertedThenCanBeReadBack() = runBlocking {
        val entity = DayRecordEntity(
            date = LocalDate.of(2026, 5, 15),
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = false,
            createdAt = Instant.ofEpochMilli(1_000_000L),
            updatedAt = Instant.ofEpochMilli(1_000_000L)
        )

        db.dayRecordDao().upsert(entity)
        val result = db.dayRecordDao().getByDate(LocalDate.of(2026, 5, 15)).first()

        assertNotNull(result)
        assertEquals(entity.date, result?.date)
        assertEquals(entity.status, result?.status)
        assertEquals(entity.detectionMethod, result?.detectionMethod)
        assertEquals(entity.confirmedByUser, result?.confirmedByUser)
    }
}
