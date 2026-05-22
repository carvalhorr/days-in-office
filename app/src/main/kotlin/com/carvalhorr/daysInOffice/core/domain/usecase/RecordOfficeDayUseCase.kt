package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.model.DetectionMethod
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class RecordOfficeDayUseCase @Inject constructor(
    private val dayRecordRepository: DayRecordRepository
) {
    suspend operator fun invoke(date: LocalDate, method: DetectionMethod) {
        val isManual = method == DetectionMethod.MANUAL || method == DetectionMethod.MANUAL_CONFIRMED_FROM_DETECTION
        if (!isManual) {
            val existing = dayRecordRepository.getDayRecord(date).first()
            if (existing?.confirmedByUser == true) return
        }
        dayRecordRepository.upsertDayRecord(
            DayRecord(
                date = date,
                status = DayStatus.OFFICE,
                detectionMethod = method,
                confirmedByUser = isManual
            )
        )
    }
}
