package com.carvalhorr.daysInOffice.core.domain.usecase

import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.model.DayStatus
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class RecordRemoteDayUseCase @Inject constructor(
    private val dayRecordRepository: DayRecordRepository
) {
    suspend operator fun invoke(date: LocalDate) {
        dayRecordRepository.upsertDayRecord(
            DayRecord(
                date = date,
                status = DayStatus.REMOTE,
                detectionMethod = null,
                confirmedByUser = true
            )
        )
    }
}
