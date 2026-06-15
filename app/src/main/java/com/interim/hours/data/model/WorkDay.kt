package com.interim.hours.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_days",
    foreignKeys = [
        ForeignKey(
            entity = Mission::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["missionId"])]
)
data class WorkDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val missionId: Int,
    val dateMillis: Long,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val breakMinutes: Int,
    val comment: String,
    val syncId: String = java.util.UUID.randomUUID().toString()
)

@Entity(
    tableName = "work_day_bonuses",
    foreignKeys = [
        ForeignKey(
            entity = WorkDay::class,
            parentColumns = ["id"],
            childColumns = ["workDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workDayId"])]
)
data class WorkDayBonus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workDayId: Int,
    val name: String,
    val amount: Double
)
