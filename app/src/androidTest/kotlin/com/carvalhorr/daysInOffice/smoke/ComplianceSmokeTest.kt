package com.carvalhorr.daysInOffice.smoke

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.carvalhorr.daysInOffice.core.data.db.AppDatabase
import com.carvalhorr.daysInOffice.core.data.repository.DayRecordRepositoryImpl
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.Holiday
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.core.domain.usecase.GetComplianceUseCase
import com.carvalhorr.daysInOffice.core.domain.usecase.GetWorkingDaysUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ComplianceSmokeTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun givenAllWorkingDaysSeededAsOfficeDaysNeededToComplyIsZeroAndNonNegative() = runBlocking {
        val today = LocalDate.now()
        val start = today.minusDays(27)

        val workingDays = (0L..27L)
            .map { start.plusDays(it) }
            .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }

        val dayRecordRepo = DayRecordRepositoryImpl(db.dayRecordDao())
        workingDays.forEach { date ->
            dayRecordRepo.upsertDayRecord(
                DayRecord(
                    date = date,
                    status = DayStatus.OFFICE,
                    detectionMethod = DetectionMethod.MANUAL,
                    confirmedByUser = false
                )
            )
        }

        val mandateConfig = MandateConfig(
            targetPercentage = 0.5f,
            period = MandatePeriod.ROLLING_4_WEEKS,
            workingDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        )

        val fakeHolidayRepo = object : HolidayRepository {
            override fun getHolidays(start: LocalDate, end: LocalDate): Flow<List<Holiday>> =
                flowOf(emptyList())
            override suspend fun upsertHoliday(holiday: Holiday) {}
            override suspend fun syncFromCalendar(start: LocalDate, end: LocalDate) {}
            override suspend fun clearAndReplace(holidays: List<Holiday>) {}
        }

        val fakeMandateConfigRepo = object : MandateConfigRepository {
            override fun getMandateConfig(): Flow<MandateConfig> = flowOf(mandateConfig)
            override suspend fun saveMandateConfig(config: MandateConfig) {}
            override fun getDetectionConfig(): Flow<DetectionConfig> =
                flowOf(DetectionConfig(emptySet(), null, null, null, null))
            override suspend fun saveDetectionConfig(config: DetectionConfig) {}
            override fun getOnboardingComplete(): Flow<Boolean> = flowOf(true)
            override suspend fun saveOnboardingComplete(complete: Boolean) {}
            override fun getCalendarSyncEnabled(): Flow<Boolean> = flowOf(false)
            override suspend fun saveCalendarSyncEnabled(enabled: Boolean) {}
            override suspend fun markDetectorDismissed(method: DetectionMethod, date: LocalDate) {}
            override suspend fun isDetectorDismissedToday(method: DetectionMethod, date: LocalDate): Boolean = false
        }

        val useCase = GetComplianceUseCase(
            fakeMandateConfigRepo,
            dayRecordRepo,
            GetWorkingDaysUseCase(fakeHolidayRepo)
        )

        val result = useCase().first()

        assertTrue("daysNeededToComply must be non-negative", result.daysNeededToComply >= 0)
        assertEquals(0, result.daysNeededToComply)
        assertTrue(result.isCompliant)
        assertEquals(workingDays.size, result.officeDays)
    }
}
