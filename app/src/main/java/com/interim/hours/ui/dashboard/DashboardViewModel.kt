package com.interim.hours.ui.dashboard

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interim.hours.data.model.MissionWithBonuses
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.data.repository.MissionRepository
import com.interim.hours.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

import java.text.SimpleDateFormat
import java.util.Locale

data class MonthlyChartItem(
    val monthLabel: String,
    val hours: Double,
    val earnings: Double
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val missionRepository: MissionRepository,
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _targetType = MutableStateFlow(sharedPrefs.getString("target_type", "HOURS") ?: "HOURS")
    val targetType: StateFlow<String> = _targetType

    private val _targetValueHours = MutableStateFlow(sharedPrefs.getFloat("target_value_hours", 151.67f))
    val targetValueHours: StateFlow<Float> = _targetValueHours

    private val _targetValueEarnings = MutableStateFlow(sharedPrefs.getFloat("target_value_earnings", 1800f))
    val targetValueEarnings: StateFlow<Float> = _targetValueEarnings

    private val _chartDurationMonths = MutableStateFlow(sharedPrefs.getInt("chart_duration_months", 6))
    val chartDurationMonths: StateFlow<Int> = _chartDurationMonths

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            "target_type" -> _targetType.value = prefs.getString("target_type", "HOURS") ?: "HOURS"
            "target_value_hours" -> _targetValueHours.value = prefs.getFloat("target_value_hours", 151.67f)
            "target_value_earnings" -> _targetValueEarnings.value = prefs.getFloat("target_value_earnings", 1800f)
            "chart_duration_months" -> _chartDurationMonths.value = prefs.getInt("chart_duration_months", 6)
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    val activeMissionsState: StateFlow<List<MissionWithBonuses>> =
        missionRepository.getMissionsWithBonusesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workDaysState: StateFlow<List<WorkDayWithDetails>> =
        workDayRepository.getWorkDaysWithDetailsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chartDataState: StateFlow<List<MonthlyChartItem>> = combine(
        workDaysState,
        chartDurationMonths
    ) { workDays, durationMonths ->
        val monthsList = mutableListOf<Calendar>()
        for (i in (durationMonths - 1) downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            monthsList.add(cal)
        }

        val sdf = SimpleDateFormat("MMM", Locale.FRANCE)

        monthsList.map { monthCal ->
            val startMillis = monthCal.timeInMillis
            val nextMonthCal = (monthCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            val endMillis = nextMonthCal.timeInMillis

            val monthDays = workDays.filter { item ->
                item.workDay.dateMillis >= startMillis && item.workDay.dateMillis < endMillis
            }

            val hours = monthDays.sumOf { item ->
                val day = item.workDay
                val duration = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
                if (duration > 0.0) duration else 0.0
            }

            val earningsGross = monthDays.sumOf { item ->
                com.interim.hours.utils.SalaryCalculator.calculateEarnings(item.workDay, item.mission, item.bonuses)
            }

            val daysGroupedByWeek = monthDays.groupBy { item ->
                com.interim.hours.utils.SalaryCalculator.getYearAndWeek(item.workDay.dateMillis)
            }
            var overtimePremium = 0.0
            daysGroupedByWeek.forEach { (_, weekDaysList) ->
                val premium = com.interim.hours.utils.SalaryCalculator.calculateWeeklyOvertimePremium(weekDaysList)
                if (premium > 0.0) {
                    overtimePremium += premium
                }
            }

            val totalGross = earningsGross + overtimePremium
            val totalNet = totalGross * 0.77

            val monthLabel = sdf.format(monthCal.time).replaceFirstChar { it.uppercase() }

            MonthlyChartItem(
                monthLabel = monthLabel,
                hours = hours,
                earnings = totalNet
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class DashboardStats(
        val weeklyHours: Double = 0.0,
        val monthlyHours: Double = 0.0,
        val weeklyEarnings: Double = 0.0,
        val monthlyEarnings: Double = 0.0,
        val activeMissionsCount: Int = 0,
        val recentDays: List<WorkDayWithDetails> = emptyList(),
        val targetType: String = "HOURS",
        val targetValueHours: Float = 151.67f,
        val targetValueEarnings: Float = 1800f
    )

    val statsState: StateFlow<DashboardStats> = combine(
        activeMissionsState,
        workDaysState,
        targetType,
        targetValueHours,
        targetValueEarnings
    ) { missions, workDays, type, tHours, tEarnings ->
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
            recentDays = workDays.take(3),
            targetType = type,
            targetValueHours = tHours,
            targetValueEarnings = tEarnings
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
