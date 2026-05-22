package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecordOfficeDayUseCaseTest {

    private val dayRecordRepository: DayRecordRepository = mockk()
    private lateinit var useCase: RecordOfficeDayUseCase

    @BeforeEach
    fun setUp() {
        useCase = RecordOfficeDayUseCase(dayRecordRepository)
    }

    @Test
    fun `given no existing record when MANUAL invoked then OFFICE record created with confirmedByUser true`() = runTest {
        val date = LocalDate.of(2026, 5, 15)
        val slot = slot<DayRecord>()
        coEvery { dayRecordRepository.upsertDayRecord(capture(slot)) } just runs

        useCase(date, DetectionMethod.MANUAL)

        coVerify(exactly = 1) { dayRecordRepository.upsertDayRecord(any()) }
        assertEquals(DayStatus.OFFICE, slot.captured.status)
        assertEquals(date, slot.captured.date)
        assertTrue(slot.captured.confirmedByUser)
    }

    @Test
    fun `given confirmed REMOTE record when MANUAL office invoked then record becomes OFFICE`() = runTest {
        val date = LocalDate.of(2026, 5, 15)
        val confirmedRemote = DayRecord(
            date = date,
            status = DayStatus.REMOTE,
            detectionMethod = null,
            confirmedByUser = true
        )
        every { dayRecordRepository.getDayRecord(date) } returns flowOf(confirmedRemote)
        val slot = slot<DayRecord>()
        coEvery { dayRecordRepository.upsertDayRecord(capture(slot)) } just runs

        useCase(date, DetectionMethod.MANUAL)

        coVerify(exactly = 1) { dayRecordRepository.upsertDayRecord(any()) }
        assertEquals(DayStatus.OFFICE, slot.captured.status)
        assertTrue(slot.captured.confirmedByUser)
    }

    @Test
    fun `given confirmed record when automated detection invoked then record NOT updated`() = runTest {
        val date = LocalDate.of(2026, 5, 15)
        val confirmedRecord = DayRecord(
            date = date,
            status = DayStatus.REMOTE,
            detectionMethod = null,
            confirmedByUser = true
        )
        every { dayRecordRepository.getDayRecord(date) } returns flowOf(confirmedRecord)

        useCase(date, DetectionMethod.WIFI_CONNECTED)

        coVerify(exactly = 0) { dayRecordRepository.upsertDayRecord(any()) }
    }

    @Test
    fun `given unconfirmed REMOTE record when invoked then record updated to OFFICE`() = runTest {
        val date = LocalDate.of(2026, 5, 15)
        val unconfirmedRecord = DayRecord(
            date = date,
            status = DayStatus.REMOTE,
            detectionMethod = null,
            confirmedByUser = false
        )
        every { dayRecordRepository.getDayRecord(date) } returns flowOf(unconfirmedRecord)
        val slot = slot<DayRecord>()
        coEvery { dayRecordRepository.upsertDayRecord(capture(slot)) } just runs

        useCase(date, DetectionMethod.WIFI_CONNECTED)

        coVerify(exactly = 1) { dayRecordRepository.upsertDayRecord(any()) }
        assertEquals(DayStatus.OFFICE, slot.captured.status)
    }
}
