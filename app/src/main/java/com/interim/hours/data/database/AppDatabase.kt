package com.interim.hours.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.MissionBonus
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus

@Database(
    entities = [
        Mission::class,
        MissionBonus::class,
        WorkDay::class,
        WorkDayBonus::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun missionDao(): MissionDao
    abstract fun workDayDao(): WorkDayDao
}
