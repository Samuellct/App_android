package com.interim.hours.data.repository

import com.interim.hours.data.database.MissionDao
import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.MissionBonus
import com.interim.hours.data.model.MissionWithBonuses
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepository @Inject constructor(
    private val missionDao: MissionDao
) {
    fun getMissionsFlow(): Flow<List<Mission>> = missionDao.getMissionsFlow()

    fun getMissionsWithBonusesFlow(): Flow<List<MissionWithBonuses>> =
        missionDao.getMissionsWithBonusesFlow()

    suspend fun getMissionWithBonusesById(id: Int): MissionWithBonuses? =
        missionDao.getMissionWithBonusesById(id)

    suspend fun saveMission(mission: Mission, bonuses: List<MissionBonus>) =
        missionDao.saveMissionWithBonuses(mission, bonuses)

    suspend fun deleteMission(mission: Mission) =
        missionDao.deleteMission(mission)

    suspend fun updateMission(mission: Mission) =
        missionDao.updateMission(mission)
}
