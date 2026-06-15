package com.interim.hours.di

import android.content.Context
import androidx.room.Room
import com.interim.hours.data.database.AppDatabase
import com.interim.hours.data.database.MissionDao
import com.interim.hours.data.database.WorkDayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "interim_hours_db"
        ).fallbackToDestructiveMigration() // Simple for development, can add migrations later
            .build()
    }

    @Provides
    fun provideMissionDao(database: AppDatabase): MissionDao {
        return database.missionDao()
    }

    @Provides
    fun provideWorkDayDao(database: AppDatabase): WorkDayDao {
        return database.workDayDao()
    }
}
