package com.interim.hours.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interim.hours.data.model.MissionWithBonuses
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.data.repository.MissionRepository
import com.interim.hours.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    val activeMissionsState: StateFlow<List<MissionWithBonuses>> =
        missionRepository.getMissionsWithBonusesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queryState = MutableStateFlow("")
    val selectedMissionFilterState = MutableStateFlow<Int?>(null)

    val workDaysWithDetailsState: StateFlow<List<WorkDayWithDetails>> =
        workDayRepository.getWorkDaysWithDetailsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredWorkDaysState: StateFlow<List<WorkDayWithDetails>> = combine(
        workDaysWithDetailsState,
        queryState,
        selectedMissionFilterState
    ) { workDays, query, missionId ->
        workDays.filter { item ->
            val matchesMission = missionId == null || item.workDay.missionId == missionId

            val matchesQuery = query.isBlank() ||
                    item.mission.company.contains(query, ignoreCase = true) ||
                    item.mission.agency.contains(query, ignoreCase = true) ||
                    item.workDay.comment.contains(query, ignoreCase = true) ||
                    item.bonuses.any { it.name.contains(query, ignoreCase = true) }

            matchesMission && matchesQuery
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(newQuery: String) {
        queryState.value = newQuery
    }

    fun selectMissionFilter(missionId: Int?) {
        selectedMissionFilterState.value = missionId
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
