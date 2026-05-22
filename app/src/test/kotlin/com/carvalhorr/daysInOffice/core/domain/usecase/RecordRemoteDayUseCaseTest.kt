package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RecordRemoteDayUseCaseTest {

    private val dayRecordRepository: DayRecordRepository = mockk()
    private lateinit var useCase: RecordRemoteDayUseCase

    @BeforeEach
    fun setUp() {
        useCase = RecordRemoteDayUseCase(dayRecordRepository)
    }

    @Test
    fun `given no existing record when invoked then REMOTE record created with confirmedByUser true`() = runTest {
        val date = LocalDate.of(2026, 5, 15)
        val slot = slot<DayRecord>()
        coEvery { dayRecordRepository.upsertDayRecord(capture(slot)) } just runs

        useCase(date)

        coVerify(exactly = 1) { dayRecordRepository.upsertDayRecord(any()) }
        assertEquals(DayStatus.REMOTE, slot.captured.status)
        assertEquals(date, slot.captured.date)
        assertTrue(slot.captured.confirmedByUser)
    }

    @Test
    fun `given confirmed OFFICE record when invoked then record becomes REMOTE`() = runTest {
        val date = LocalDate.of(2026, 5, 15)
        val slot = slot<DayRecord>()
        coEvery { dayRecordRepository.upsertDayRecord(capture(slot)) } just runs

        useCase(date)

        coVerify(exactly = 1) { dayRecordRepository.upsertDayRecord(any()) }
        assertEquals(DayStatus.REMOTE, slot.captured.status)
        assertTrue(slot.captured.confirmedByUser)
    }
}
