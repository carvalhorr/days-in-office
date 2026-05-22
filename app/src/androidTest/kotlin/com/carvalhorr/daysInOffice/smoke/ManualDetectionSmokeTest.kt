package com.carvalhorr.daysInOffice.smoke

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.carvalhorr.daysInOffice.core.data.db.AppDatabase
import com.carvalhorr.daysInOffice.core.data.repository.DayRecordRepositoryImpl
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.usecase.RecordOfficeDayUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ManualDetectionSmokeTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DayRecordRepositoryImpl
    private lateinit var useCase: RecordOfficeDayUseCase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = DayRecordRepositoryImpl(db.dayRecordDao())
        useCase = RecordOfficeDayUseCase(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun whenOfficeDayRecordedManuallyThenOfficeRecordIsPersisted() = runBlocking {
        val date = LocalDate.now()

        useCase(date, DetectionMethod.MANUAL)

        val record = repository.getDayRecord(date).first()

        assertNotNull(record)
        assertEquals(DayStatus.OFFICE, record?.status)
        assertEquals(DetectionMethod.MANUAL, record?.detectionMethod)
        assertEquals(false, record?.confirmedByUser)
    }

    @Test
    fun whenUserConfirmedRecordExistsThenManualDetectionDoesNotOverwriteIt() = runBlocking {
        val date = LocalDate.now()

        // Record a confirmed remote day
        repository.upsertDayRecord(
            com.carvalhorr.daysInOffice.core.domain.model.DayRecord(
                date = date,
                status = DayStatus.REMOTE,
                detectionMethod = null,
                confirmedByUser = true
            )
        )

        // Attempt to overwrite with OFFICE via use case
        useCase(date, DetectionMethod.MANUAL)

        val record = repository.getDayRecord(date).first()
        assertEquals(DayStatus.REMOTE, record?.status)
    }
}
