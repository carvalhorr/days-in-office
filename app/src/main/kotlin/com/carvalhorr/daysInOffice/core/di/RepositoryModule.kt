package com.carvalhorr.daysInOffice.core.di

import com.carvalhorr.daysInOffice.core.data.repository.DayRecordRepositoryImpl
import com.carvalhorr.daysInOffice.core.data.repository.HolidayRepositoryImpl
import com.carvalhorr.daysInOffice.core.data.repository.MandateConfigRepositoryImpl
import com.carvalhorr.daysInOffice.core.domain.repository.DayRecordRepository
import com.carvalhorr.daysInOffice.core.domain.repository.HolidayRepository
import com.carvalhorr.daysInOffice.core.domain.repository.MandateConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindDayRecordRepository(impl: DayRecordRepositoryImpl): DayRecordRepository

    @Binds
    abstract fun bindHolidayRepository(impl: HolidayRepositoryImpl): HolidayRepository

    @Binds
    abstract fun bindMandateConfigRepository(impl: MandateConfigRepositoryImpl): MandateConfigRepository
}
