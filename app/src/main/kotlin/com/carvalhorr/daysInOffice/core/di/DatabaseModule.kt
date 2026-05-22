package com.carvalhorr.daysInOffice.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.WorkManager
import com.carvalhorr.daysInOffice.core.data.db.AppDatabase
import com.carvalhorr.daysInOffice.core.data.db.dao.DayRecordDao
import com.carvalhorr.daysInOffice.core.data.db.dao.HolidayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "days_in_office.db").build()

    @Provides
    fun provideDayRecordDao(db: AppDatabase): DayRecordDao = db.dayRecordDao()

    @Provides
    fun provideHolidayDao(db: AppDatabase): HolidayDao = db.holidayDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appDataStore

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
