package com.carvalhorr.daysInOffice.smoke

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.carvalhorr.daysInOffice.core.data.db.AppDatabase
import com.carvalhorr.daysInOffice.core.data.repository.DayRecordRepositoryImpl
import com.carvalhorr.daysInOffice.core.detection.Detector
import com.carvalhorr.daysInOffice.core.detection.DetectionOrchestrator
import com.carvalhorr.daysInOffice.core.detection.worker.DayDetectionWorker
import com.carvalhorr.daysInOffice.core.domain.model.DetectionConfig
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import com.carvalhorr.daysInOffice.notification.DetectionPromptNotificationWorker
import com.carvalhorr.daysInOffice.notification.NotificationChannelManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class WorkerSchedulingSmokeTest {

    @Before
    fun initWorkManager() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testWorkerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                if (workerClassName != DayDetectionWorker::class.java.name) return null
                val db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
                val dayRecordRepo = DayRecordRepositoryImpl(db.dayRecordDao())
                val fakeMandateRepo = object : MandateConfigRepository {
                    override fun getMandateConfig(): Flow<MandateConfig> = flowOf(
                        MandateConfig(
                            0.6f, MandatePeriod.ROLLING_4_WEEKS, setOf(
                                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                            )
                        )
                    )
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
                val notificationWorker = DetectionPromptNotificationWorker(
                    appContext, NotificationChannelManager(appContext)
                )
                val orchestrator = DetectionOrchestrator(
                    emptyMap<DetectionMethod, Detector>(),
                    fakeMandateRepo,
                    dayRecordRepo,
                    notificationWorker,
                    appContext
                )
                return DayDetectionWorker(appContext, workerParameters, orchestrator)
            }
        }
        // WorkManager may have been initialised early by the App Startup library using
        // DaysInOfficeApp's Configuration.Provider. Clear the static singleton so that
        // our WorkManager.initialize() call below installs the test factory.
        val candidates = mutableListOf<Class<*>>(WorkManager::class.java)
        runCatching { candidates.add(Class.forName("androidx.work.impl.WorkManagerImpl")) }
        for (cls in candidates) {
            for (name in arrayOf("sDelegatedInstance", "sDefaultInstance", "sDelegatingInstance")) {
                runCatching {
                    val f = cls.getDeclaredField(name)
                    f.isAccessible = true
                    f.set(null, null)
                }
            }
        }

        try {
            WorkManager.initialize(
                context,
                Configuration.Builder().setWorkerFactory(testWorkerFactory).build()
            )
        } catch (_: IllegalStateException) {
            // Singleton clear did not fully reset — proceed with existing instance.
        }
    }

    @Test
    fun shouldDetect_returnsTrueForWeekdayDuringWorkHours() {
        // 2026-05-11 is a Monday
        val monday = LocalDate.of(2026, 5, 11)
        assertTrue(DayDetectionWorker.shouldDetect(monday, LocalTime.of(9, 0)))
    }

    @Test
    fun shouldDetect_returnsFalseForSaturday() {
        // 2026-05-09 is a Saturday
        val saturday = LocalDate.of(2026, 5, 9)
        assertFalse(DayDetectionWorker.shouldDetect(saturday, LocalTime.of(9, 0)))
    }

    @Test
    fun shouldDetect_returnsFalseForSunday() {
        // 2026-05-10 is a Sunday
        val sunday = LocalDate.of(2026, 5, 10)
        assertFalse(DayDetectionWorker.shouldDetect(sunday, LocalTime.of(9, 0)))
    }

    @Test
    fun shouldDetect_returnsFalseBeforeWorkHours() {
        val monday = LocalDate.of(2026, 5, 11)
        assertFalse(DayDetectionWorker.shouldDetect(monday, LocalTime.of(6, 30)))
    }

    @Test
    fun whenDayDetectionWorkerEnqueuedItCompletesWithResultSuccess() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val workManager = WorkManager.getInstance(context)

        val request = OneTimeWorkRequestBuilder<DayDetectionWorker>().build()
        workManager.enqueue(request).result.get()

        val deadline = System.currentTimeMillis() + 15_000L
        var info: WorkInfo? = null
        while (System.currentTimeMillis() < deadline) {
            info = workManager.getWorkInfoById(request.id).get()
            if (info?.state?.isFinished == true) break
            delay(200)
        }

        assertNotNull("Worker did not complete within timeout", info)
        assertEquals(WorkInfo.State.SUCCEEDED, info?.state)
    }
}
