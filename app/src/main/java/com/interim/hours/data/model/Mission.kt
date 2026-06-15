package com.interim.hours.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class Mission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val company: String,
    val agency: String,
    val hourlyRate: Double,
    val siteAddress: String,
    val colorHex: String,
    val isActive: Boolean = true,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val nightStartHour: Int = 21,
    val nightEndHour: Int = 6,
    val nightRatePercentage: Double = 0.0,
    val hasIfmIccp: Boolean = true
)

@Entity(
    tableName = "mission_bonuses",
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
data class MissionBonus(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val missionId: Int,
    val name: String,
    val defaultAmount: Double
)
