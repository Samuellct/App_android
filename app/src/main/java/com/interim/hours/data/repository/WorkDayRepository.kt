package com.interim.hours.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.interim.hours.data.database.WorkDayDao
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.widget.WorkLogWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkDayRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workDayDao: WorkDayDao
) {
    fun getWorkDaysWithDetailsFlow(): Flow<List<WorkDayWithDetails>> =
        workDayDao.getWorkDaysWithDetailsFlow()

    suspend fun getWorkDayWithDetailsById(id: Int): WorkDayWithDetails? =
        workDayDao.getWorkDayWithDetailsById(id)

    fun getWorkDaysWithDetailsInRangeFlow(start: Long, end: Long): Flow<List<WorkDayWithDetails>> =
        workDayDao.getWorkDaysWithDetailsInRangeFlow(start, end)

    suspend fun saveWorkDay(workDay: WorkDay, bonuses: List<WorkDayBonus>) {
        workDayDao.saveWorkDayWithBonuses(workDay, bonuses)
        try {
            WorkLogWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteWorkDay(workDay: WorkDay) {
        workDayDao.deleteWorkDay(workDay)
        try {
            WorkLogWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
