package com.interim.hours.ui.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.MissionBonus
import com.interim.hours.data.model.MissionWithBonuses
import com.interim.hours.data.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val missionRepository: MissionRepository
) : ViewModel() {

    val missionsState: StateFlow<List<MissionWithBonuses>> =
        missionRepository.getMissionsWithBonusesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveMission(mission: Mission, bonuses: List<MissionBonus>) {
        viewModelScope.launch {
            missionRepository.saveMission(mission, bonuses)
        }
    }

    fun toggleMissionActive(mission: Mission) {
        viewModelScope.launch {
            missionRepository.updateMission(mission.copy(isActive = !mission.isActive))
        }
    }

    fun deleteMission(mission: Mission) {
        viewModelScope.launch {
            missionRepository.deleteMission(mission)
        }
    }
}
