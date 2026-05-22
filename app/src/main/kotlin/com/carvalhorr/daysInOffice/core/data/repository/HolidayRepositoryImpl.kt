package com.carvalhorr.daysInOffice.core.data.repository

import com.carvalhorr.daysInOffice.core.data.db.dao.HolidayDao
import com.carvalhorr.daysInOffice.core.data.db.entity.HolidayEntity
import com.carvalhorr.daysInOffice.core.domain.model.Holiday
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HolidayRepositoryImpl @Inject constructor(
    private val dao: HolidayDao
) : HolidayRepository {

    override fun getHolidays(start: LocalDate, end: LocalDate): Flow<List<Holiday>> =
        dao.getByDateRange(start, end).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertHoliday(holiday: Holiday) =
        dao.upsert(holiday.toEntity())

    override suspend fun syncFromCalendar(start: LocalDate, end: LocalDate) {
        // CalendarDataSource integration deferred to a future task
    }

    override suspend fun clearAndReplace(holidays: List<Holiday>) {
        dao.deleteAll()
        holidays.forEach { dao.upsert(it.toEntity()) }
    }

    private fun HolidayEntity.toDomain(): Holiday = Holiday(
        date = date,
        name = name,
        isPublicHoliday = isPublicHoliday,
        source = source
    )

    private fun Holiday.toEntity(): HolidayEntity = HolidayEntity(
        date = date,
        name = name,
        isPublicHoliday = isPublicHoliday,
        source = source
    )
}
