package com.carvalhorr.daysInOffice.core.detection.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.carvalhorr.daysInOffice.core.detection.DetectionOrchestrator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

private const val TAG = "Detection"

class DayDetectionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val detectionOrchestrator: DetectionOrchestrator
) : CoroutineWorker(context, workerParams) {

    @AssistedFactory
    interface Factory {
        fun create(context: Context, workerParams: WorkerParameters): DayDetectionWorker
    }

    override suspend fun doWork(): Result {
        val forceRun = inputData.getBoolean("force_run", false)
        val today = LocalDate.now()
        val now = LocalTime.now()
        val willRun = shouldRun(forceRun, today, now)
        Log.i(TAG, "DayDetectionWorker.doWork: entry (forceRun=$forceRun, shouldRun=$willRun)")
        if (!willRun) return Result.success()
        Log.i(TAG, "DayDetectionWorker.doWork: calling orchestrator")
        val outcome = detectionOrchestrator.runDetection(today)
        Log.i(TAG, "DayDetectionWorker.doWork: orchestrator returned $outcome")
        return Result.success(workDataOf("outcome" to outcome.name))
    }

    companion object {
        const val WORK_NAME = "day_detection"
        private val WORK_START = LocalTime.of(7, 0)
        private val WORK_END = LocalTime.of(19, 0)

        /**
         * Detection runs 7 days a week within working hours (07:00–19:00).
         * Day-of-week is intentionally NOT gated: a user who goes to the
         * office on a Saturday should get the same auto-detection prompt
         * as any other day; if they confirm, that day is recorded as
         * bonus credit toward the mandate. Users who don't go in on
         * weekends never see a prompt anyway because the detector
         * `isAtOffice` check returns false.
         */
        internal fun shouldDetect(date: LocalDate, time: LocalTime): Boolean {
            return !time.isBefore(WORK_START) && !time.isAfter(WORK_END)
        }

        internal fun shouldRun(
            forceRun: Boolean,
            date: LocalDate,
            time: LocalTime
        ): Boolean = forceRun || shouldDetect(date, time)

        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<DayDetectionWorker>(2, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
