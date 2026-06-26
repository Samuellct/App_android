package com.interim.hours.data.pointing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PointingState {
    IDLE,
    WORKING,
    ON_BREAK
}

data class PrefillShiftData(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val breakMinutes: Int
)

class PointingManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pointing_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STATE = "pointing_state"
        private const val KEY_START_TIME = "start_time_millis"
        private const val KEY_BREAK_START_TIME = "break_start_time_millis"
        private const val KEY_ACCUMULATED_BREAK = "accumulated_break_minutes"

        @Volatile
        private var INSTANCE: PointingManager? = null

        fun getInstance(context: Context): PointingManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PointingManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private val _prefillState = MutableStateFlow<PrefillShiftData?>(null)
        val prefillState: StateFlow<PrefillShiftData?> = _prefillState.asStateFlow()

        fun setPrefillData(data: PrefillShiftData?) {
            _prefillState.value = data
        }
    }

    fun getPointingState(): PointingState {
        val stateStr = prefs.getString(KEY_STATE, PointingState.IDLE.name) ?: PointingState.IDLE.name
        return try {
            PointingState.valueOf(stateStr)
        } catch (e: Exception) {
            PointingState.IDLE
        }
    }

    fun getStartTime(): Long = prefs.getLong(KEY_START_TIME, 0L)
    fun getBreakStartTime(): Long = prefs.getLong(KEY_BREAK_START_TIME, 0L)
    fun getAccumulatedBreakMinutes(): Int = prefs.getInt(KEY_ACCUMULATED_BREAK, 0)

    fun startShift(timestamp: Long) {
        Log.d("PointingManager", "startShift called at $timestamp")
        prefs.edit()
            .putString(KEY_STATE, PointingState.WORKING.name)
            .putLong(KEY_START_TIME, timestamp)
            .putLong(KEY_BREAK_START_TIME, 0L)
            .putInt(KEY_ACCUMULATED_BREAK, 0)
            .commit()
    }

    fun startBreak(timestamp: Long) {
        Log.d("PointingManager", "startBreak called at $timestamp")
        prefs.edit()
            .putString(KEY_STATE, PointingState.ON_BREAK.name)
            .putLong(KEY_BREAK_START_TIME, timestamp)
            .commit()
    }

    fun endBreak(timestamp: Long) {
        Log.d("PointingManager", "endBreak called at $timestamp")
        val breakStart = getBreakStartTime()
        if (breakStart > 0L && timestamp > breakStart) {
            val elapsedMinutes = ((timestamp - breakStart) / 60000).toInt()
            val totalBreak = getAccumulatedBreakMinutes() + elapsedMinutes
            Log.d("PointingManager", "endBreak: accumulated break minutes = $totalBreak")
            prefs.edit()
                .putString(KEY_STATE, PointingState.WORKING.name)
                .putInt(KEY_ACCUMULATED_BREAK, totalBreak)
                .putLong(KEY_BREAK_START_TIME, 0L)
                .commit()
        } else {
            prefs.edit()
                .putString(KEY_STATE, PointingState.WORKING.name)
                .putLong(KEY_BREAK_START_TIME, 0L)
                .commit()
        }
    }

    fun endShift(timestamp: Long): PrefillShiftData {
        val state = getPointingState()
        val startTime = getStartTime()
        var accumulatedBreak = getAccumulatedBreakMinutes()
        Log.d("PointingManager", "endShift called: state=$state, startTime=$startTime, accumulatedBreak=$accumulatedBreak")

        if (state == PointingState.ON_BREAK) {
            val breakStart = getBreakStartTime()
            if (breakStart > 0L && timestamp > breakStart) {
                val elapsedMinutes = ((timestamp - breakStart) / 60000).toInt()
                accumulatedBreak += elapsedMinutes
            }
        }

        val start = if (startTime > 0L) startTime else timestamp
        val end = timestamp

        // Reset state
        prefs.edit()
            .putString(KEY_STATE, PointingState.IDLE.name)
            .putLong(KEY_START_TIME, 0L)
            .putLong(KEY_BREAK_START_TIME, 0L)
            .putInt(KEY_ACCUMULATED_BREAK, 0)
            .commit()

        Log.d("PointingManager", "endShift completed: start=$start, end=$end, breakMinutes=$accumulatedBreak")
        return PrefillShiftData(
            startTimeMillis = start,
            endTimeMillis = end,
            breakMinutes = accumulatedBreak
        )
    }
}
