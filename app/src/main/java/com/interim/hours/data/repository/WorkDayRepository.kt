package com.interim.hours.data.repository

import com.interim.hours.data.database.WorkDayDao
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkDayRepository @Inject constructor(
    private val workDayDao: WorkDayDao
) {
    fun getWorkDaysWithDetailsFlow(): Flow<List<WorkDayWithDetails>> =
        workDayDao.getWorkDaysWithDetailsFlow()

    suspend fun getWorkDayWithDetailsById(id: Int): WorkDayWithDetails? =
        workDayDao.getWorkDayWithDetailsById(id)

    fun getWorkDaysWithDetailsInRangeFlow(start: Long, end: Long): Flow<List<WorkDayWithDetails>> =
        workDayDao.getWorkDaysWithDetailsInRangeFlow(start, end)

    suspend fun saveWorkDay(workDay: WorkDay, bonuses: List<WorkDayBonus>) =
        workDayDao.saveWorkDayWithBonuses(workDay, bonuses)

    suspend fun deleteWorkDay(workDay: WorkDay) =
        workDayDao.deleteWorkDay(workDay)
}
