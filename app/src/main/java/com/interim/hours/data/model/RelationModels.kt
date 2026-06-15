package com.interim.hours.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MissionWithBonuses(
    @Embedded val mission: Mission,
    @Relation(
        parentColumn = "id",
        entityColumn = "missionId"
    )
    val bonuses: List<MissionBonus>
)

data class WorkDayWithDetails(
    @Embedded val workDay: WorkDay,
    @Relation(
        parentColumn = "missionId",
        entityColumn = "id"
    )
    val mission: Mission,
    @Relation(
        parentColumn = "id",
        entityColumn = "workDayId"
    )
    val bonuses: List<WorkDayBonus>
)
