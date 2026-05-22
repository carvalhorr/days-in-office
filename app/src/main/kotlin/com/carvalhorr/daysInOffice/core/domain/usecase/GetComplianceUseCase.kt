package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.ComplianceResult
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.MandateConfig
import com.carvalhorr.daysInOffice.core.domain.model.MandatePeriod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetComplianceUseCase @Inject constructor(
    private val mandateConfigRepository: MandateConfigRepository,
    private val dayRecordRepository: DayRecordRepository,
    private val getWorkingDaysUseCase: GetWorkingDaysUseCase
) {
    operator fun invoke(): Flow<ComplianceResult> =
        mandateConfigRepository.getMandateConfig().flatMapLatest { config ->
            val today = LocalDate.now()
            val (start, end) = getPeriodBounds(config.period, today)
            dayRecordRepository.getDayRecords(start, end).map { records ->
                val workingDays = getWorkingDaysUseCase(start, end)
                buildResult(config, records, workingDays, start, end)
            }
        }

    private fun getPeriodBounds(period: MandatePeriod, today: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            MandatePeriod.WEEKLY -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                start to end
            }
            MandatePeriod.MONTHLY -> {
                val start = today.withDayOfMonth(1)
                val end = today.with(TemporalAdjusters.lastDayOfMonth())
                start to end
            }
            MandatePeriod.QUARTERLY -> {
                val quarter = (today.monthValue - 1) / 3
                val startMonth = quarter * 3 + 1
                val start = LocalDate.of(today.year, startMonth, 1)
                val end = start.plusMonths(3).minusDays(1)
                start to end
            }
            MandatePeriod.ROLLING_4_WEEKS -> {
                val start = today.minusDays(27)
                val end = today
                start to end
            }
        }

    private fun buildResult(
        config: MandateConfig,
        records: List<DayRecord>,
        workingDays: List<LocalDate>,
        start: LocalDate,
        end: LocalDate
    ): ComplianceResult {
        val recordMap = records.associateBy { it.date }
        val userExcludedDays = records
            .filter { it.status == DayStatus.PTO || it.status == DayStatus.HOLIDAY }
            .map { it.date }
            .toSet()
        val effectiveWorkingDays = workingDays.filter { it !in userExcludedDays }

        val officeDays = effectiveWorkingDays.count { recordMap[it]?.status == DayStatus.OFFICE }
        val remoteDays = effectiveWorkingDays.count { recordMap[it]?.status == DayStatus.REMOTE }
        val unknownDays = effectiveWorkingDays.count {
            recordMap[it] == null || recordMap[it]?.status == DayStatus.UNKNOWN
        }
        return ComplianceResult(
            periodStart = start,
            periodEnd = end,
            totalWorkingDays = effectiveWorkingDays.size,
            officeDays = officeDays,
            remoteDays = remoteDays,
            unknownDays = unknownDays,
            targetPercentage = config.targetPercentage
        )
    }
}
