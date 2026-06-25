package com.interim.hours.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.MissionBonus
import com.interim.hours.data.model.MissionWithBonuses
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    @Query("SELECT * FROM missions ORDER BY isActive DESC, company ASC")
    fun getMissionsFlow(): Flow<List<Mission>>

    @Transaction
    @Query("SELECT * FROM missions ORDER BY isActive DESC, company ASC")
    fun getMissionsWithBonusesFlow(): Flow<List<MissionWithBonuses>>

    @Transaction
    @Query("SELECT * FROM missions WHERE id = :id")
    suspend fun getMissionWithBonusesById(id: Int): MissionWithBonuses?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: Mission): Long

    @Update
    suspend fun updateMission(mission: Mission)

    @Delete
    suspend fun deleteMission(mission: Mission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissionBonus(bonus: MissionBonus)

    @Query("DELETE FROM mission_bonuses WHERE missionId = :missionId")
    suspend fun deleteMissionBonusesByMissionId(missionId: Int)

    @Transaction
    suspend fun saveMissionWithBonuses(mission: Mission, bonuses: List<MissionBonus>): Int {
        val missionId = if (mission.id == 0) {
            insertMission(mission).toInt()
        } else {
            updateMission(mission)
            deleteMissionBonusesByMissionId(mission.id)
            mission.id
        }
        bonuses.forEach { bonus ->
            insertMissionBonus(bonus.copy(missionId = missionId))
        }
        return missionId
    }
}
