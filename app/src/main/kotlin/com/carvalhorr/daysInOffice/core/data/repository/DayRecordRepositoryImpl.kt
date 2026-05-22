package com.carvalhorr.daysInOffice.core.data.repository

import com.carvalhorr.daysInOffice.core.data.db.dao.DayRecordDao
import com.carvalhorr.daysInOffice.core.data.db.entity.DayRecordEntity
import com.carvalhorr.daysInOffice.core.domain.model.DayRecord
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayRecordRepositoryImpl @Inject constructor(
    private val dao: DayRecordDao
) : DayRecordRepository {

    override fun getDayRecords(start: LocalDate, end: LocalDate): Flow<List<DayRecord>> =
        dao.getByDateRange(start, end).map { entities -> entities.map { it.toDomain() } }

    override fun getDayRecord(date: LocalDate): Flow<DayRecord?> =
        dao.getByDate(date).map { it?.toDomain() }

    override suspend fun upsertDayRecord(record: DayRecord) =
        dao.upsert(record.toEntity())

    override suspend fun deleteDayRecord(date: LocalDate) =
        dao.delete(date)

    private fun DayRecordEntity.toDomain(): DayRecord = DayRecord(
        date = date,
        status = status,
        detectionMethod = detectionMethod,
        confirmedByUser = confirmedByUser
    )

    private fun DayRecord.toEntity(): DayRecordEntity = DayRecordEntity(
        date = date,
        status = status,
        detectionMethod = detectionMethod,
        confirmedByUser = confirmedByUser,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
