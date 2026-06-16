package com.interim.hours.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDayDao {

    @Transaction
    @Query("SELECT * FROM work_days ORDER BY dateMillis DESC, startTimeMillis DESC")
    fun getWorkDaysWithDetailsFlow(): Flow<List<WorkDayWithDetails>>

    @Transaction
    @Query("SELECT * FROM work_days ORDER BY dateMillis DESC, startTimeMillis DESC")
    suspend fun getWorkDaysWithDetails(): List<WorkDayWithDetails>

    @Transaction
    @Query("SELECT * FROM work_days WHERE id = :id")
    suspend fun getWorkDayWithDetailsById(id: Int): WorkDayWithDetails?

    @Transaction
    @Query("SELECT * FROM work_days WHERE dateMillis >= :start AND dateMillis <= :end ORDER BY dateMillis ASC, startTimeMillis ASC")
    fun getWorkDaysWithDetailsInRangeFlow(start: Long, end: Long): Flow<List<WorkDayWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDay): Long

    @Update
    suspend fun updateWorkDay(workDay: WorkDay)

    @Delete
    suspend fun deleteWorkDay(workDay: WorkDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDayBonus(bonus: WorkDayBonus)

    @Query("DELETE FROM work_day_bonuses WHERE workDayId = :workDayId")
    suspend fun deleteWorkDayBonusesByWorkDayId(workDayId: Int)

    @Transaction
    suspend fun saveWorkDayWithBonuses(workDay: WorkDay, bonuses: List<WorkDayBonus>) {
        val workDayId = if (workDay.id == 0) {
            insertWorkDay(workDay).toInt()
        } else {
            updateWorkDay(workDay)
            deleteWorkDayBonusesByWorkDayId(workDay.id)
            workDay.id
        }
        bonuses.forEach { bonus ->
            insertWorkDayBonus(bonus.copy(workDayId = workDayId))
        }
    }
}
