package com.carvalhorr.daysInOffice.core.di

import android.content.ContentResolver
import android.content.Context
import com.carvalhorr.daysInOffice.core.data.datasource.LocationProvider
import com.carvalhorr.daysInOffice.core.data.datasource.WifiScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider =
        LocationProvider(context)

    @Provides
    @Singleton
    fun provideWifiScanner(@ApplicationContext context: Context): WifiScanner =
        WifiScanner(context)
}
