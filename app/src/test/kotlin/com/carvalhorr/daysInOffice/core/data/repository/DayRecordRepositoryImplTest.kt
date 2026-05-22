package com.carvalhorr.daysInOffice.core.data.repository

import app.cash.turbine.test
import com.carvalhorr.daysInOffice.core.data.db.dao.DayRecordDao
import com.carvalhorr.daysInOffice.core.data.db.entity.DayRecordEntity
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class DayRecordRepositoryImplTest {

    private val dao: DayRecordDao = mockk()
    private lateinit var repository: DayRecordRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = DayRecordRepositoryImpl(dao)
    }

    @Test
    fun `given dao emits entities when getDayRecords then flow emits mapped domain models`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val entity = DayRecordEntity(
            date = date,
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        every { dao.getByDateRange(any(), any()) } returns flowOf(listOf(entity))

        repository.getDayRecords(date, date).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals(date, result[0].date)
            assertEquals(DayStatus.OFFICE, result[0].status)
            assertEquals(DetectionMethod.MANUAL, result[0].detectionMethod)
            assertTrue(result[0].confirmedByUser)
            awaitComplete()
        }
    }

    @Test
    fun `given domain model when upsertDayRecord then dao upsert called with mapped entity`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        val record = DayRecord(
            date = date,
            status = DayStatus.OFFICE,
            detectionMethod = DetectionMethod.MANUAL,
            confirmedByUser = true
        )
        val entitySlot = slot<DayRecordEntity>()
        coEvery { dao.upsert(capture(entitySlot)) } just runs

        repository.upsertDayRecord(record)

        coVerify { dao.upsert(any()) }
        assertEquals(date, entitySlot.captured.date)
        assertEquals(DayStatus.OFFICE, entitySlot.captured.status)
        assertEquals(DetectionMethod.MANUAL, entitySlot.captured.detectionMethod)
        assertTrue(entitySlot.captured.confirmedByUser)
    }

    @Test
    fun `given dao emits null when getDayRecord then flow emits null`() = runTest {
        val date = LocalDate.of(2026, 5, 1)
        every { dao.getByDate(any()) } returns flowOf(null)

        repository.getDayRecord(date).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
