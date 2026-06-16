package com.interim.hours.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interim.hours.data.model.MissionWithBonuses
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.data.repository.MissionRepository
import com.interim.hours.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    val activeMissionsState: StateFlow<List<MissionWithBonuses>> =
        missionRepository.getMissionsWithBonusesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workDaysState: StateFlow<List<WorkDayWithDetails>> =
        workDayRepository.getWorkDaysWithDetailsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class DashboardStats(
        val weeklyHours: Double = 0.0,
        val monthlyHours: Double = 0.0,
        val weeklyEarnings: Double = 0.0,
        val monthlyEarnings: Double = 0.0,
        val activeMissionsCount: Int = 0,
        val recentDays: List<WorkDayWithDetails> = emptyList()
    )

    val statsState: StateFlow<DashboardStats> = combine(
        activeMissionsState,
        workDaysState
    ) { missions, workDays ->
        val now = Calendar.getInstance()

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (now.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, -6)
            }
        }.timeInMillis

        var weeklyHours = 0.0
        var monthlyHours = 0.0
        var weeklyEarnings = 0.0
        var monthlyEarnings = 0.0

        // 1. Calculate daily earnings and hours
        workDays.forEach { item ->
            val day = item.workDay
            val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
            val cleanDuration = if (durationHours > 0.0) durationHours else 0.0
            val dayEarnings = com.interim.hours.utils.SalaryCalculator.calculateEarnings(day, item.mission, item.bonuses)

            if (day.dateMillis >= startOfMonth) {
                monthlyHours += cleanDuration
                monthlyEarnings += dayEarnings
            }

            if (day.dateMillis >= startOfWeek) {
                weeklyHours += cleanDuration
                weeklyEarnings += dayEarnings
            }
        }

        // 2. Group by calendar week to calculate weekly overtime premiums
        val daysGroupedByWeek = workDays.groupBy { item ->
            com.interim.hours.utils.SalaryCalculator.getYearAndWeek(item.workDay.dateMillis)
        }

        daysGroupedByWeek.forEach { (_, weekDaysList) ->
            val overtimePremium = com.interim.hours.utils.SalaryCalculator.calculateWeeklyOvertimePremium(weekDaysList)
            if (overtimePremium > 0.0) {
                // If any workday in this week is in the current week, add to weekly earnings
                val hasDayInCurrentWeek = weekDaysList.any { it.workDay.dateMillis >= startOfWeek }
                if (hasDayInCurrentWeek) {
                    weeklyEarnings += overtimePremium
                }

                // If any workday in this week is in the current month, add to monthly earnings
                val hasDayInCurrentMonth = weekDaysList.any { it.workDay.dateMillis >= startOfMonth }
                if (hasDayInCurrentMonth) {
                    monthlyEarnings += overtimePremium
                }
            }
        }

        DashboardStats(
            weeklyHours = weeklyHours,
            monthlyHours = monthlyHours,
            weeklyEarnings = weeklyEarnings,
            monthlyEarnings = monthlyEarnings,
            activeMissionsCount = missions.count { it.mission.isActive },
            recentDays = workDays.take(3)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun saveWorkDay(workDay: WorkDay, bonuses: List<WorkDayBonus>) {
        viewModelScope.launch {
            workDayRepository.saveWorkDay(workDay, bonuses)
        }
    }

    fun deleteWorkDay(workDay: WorkDay) {
        viewModelScope.launch {
            workDayRepository.deleteWorkDay(workDay)
        }
    }
}
