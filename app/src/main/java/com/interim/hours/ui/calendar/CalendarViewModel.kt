package com.interim.hours.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interim.hours.data.model.MissionWithBonuses
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.data.repository.MissionRepository
import com.interim.hours.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    val selectedMonthState = MutableStateFlow(Calendar.getInstance())

    val activeMissionsState: StateFlow<List<MissionWithBonuses>> =
        missionRepository.getMissionsWithBonusesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workDaysInMonthState: StateFlow<List<WorkDayWithDetails>> = selectedMonthState
        .flatMapLatest { cal ->
            val start = cal.clone() as Calendar
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = cal.clone() as Calendar
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            end.set(Calendar.MILLISECOND, 999)

            workDayRepository.getWorkDaysWithDetailsInRangeFlow(start.timeInMillis, end.timeInMillis)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() {
        val next = selectedMonthState.value.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        selectedMonthState.value = next
    }

    fun previousMonth() {
        val prev = selectedMonthState.value.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        selectedMonthState.value = prev
    }

    fun resetToCurrentMonth() {
        selectedMonthState.value = Calendar.getInstance()
    }

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
