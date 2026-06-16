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

    val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE missions ADD COLUMN hasWeeklyOvertime INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE missions ADD COLUMN weeklyOvertimeThreshold REAL NOT NULL DEFAULT 35.0")
            db.execSQL("ALTER TABLE missions ADD COLUMN overtimeRate1Percentage REAL NOT NULL DEFAULT 25.0")
            db.execSQL("ALTER TABLE missions ADD COLUMN overtimeRate2Percentage REAL NOT NULL DEFAULT 50.0")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "interim_hours_db"
        ).addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration() // Simple for development, can add migrations later
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
