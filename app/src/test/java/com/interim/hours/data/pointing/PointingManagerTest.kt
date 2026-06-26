package com.interim.hours.data.pointing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PointingManagerTest {

    private lateinit var context: Context
    private lateinit var pointingManager: PointingManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pointingManager = PointingManager(context)
        // Clear preferences before each test
        context.getSharedPreferences("pointing_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testInitialState() {
        assertEquals(PointingState.IDLE, pointingManager.getPointingState())
        assertEquals(0L, pointingManager.getStartTime())
        assertEquals(0L, pointingManager.getBreakStartTime())
        assertEquals(0, pointingManager.getAccumulatedBreakMinutes())
    }

    @Test
    fun testStartShift() {
        val now = System.currentTimeMillis()
        pointingManager.startShift(now)
        assertEquals(PointingState.WORKING, pointingManager.getPointingState())
        assertEquals(now, pointingManager.getStartTime())
        assertEquals(0L, pointingManager.getBreakStartTime())
        assertEquals(0, pointingManager.getAccumulatedBreakMinutes())
    }

    @Test
    fun testStartAndEndBreak() {
        val shiftStart = System.currentTimeMillis()
        pointingManager.startShift(shiftStart)

        // Go on break after some time
        val breakStart = shiftStart + 60 * 1000 // 1 min later
        pointingManager.startBreak(breakStart)
        assertEquals(PointingState.ON_BREAK, pointingManager.getPointingState())
        assertEquals(breakStart, pointingManager.getBreakStartTime())

        // End break 30 minutes later
        val breakEnd = breakStart + 30 * 60 * 1000 // 30 mins break
        pointingManager.endBreak(breakEnd)
        assertEquals(PointingState.WORKING, pointingManager.getPointingState())
        assertEquals(30, pointingManager.getAccumulatedBreakMinutes())
        assertEquals(0L, pointingManager.getBreakStartTime())
    }

    @Test
    fun testEndShift() {
        val shiftStart = System.currentTimeMillis()
        pointingManager.startShift(shiftStart)

        // Go on break for 15 minutes
        val breakStart = shiftStart + 60 * 1000
        pointingManager.startBreak(breakStart)
        val breakEnd = breakStart + 15 * 60 * 1000
        pointingManager.endBreak(breakEnd)

        // End shift 8 hours after start
        val shiftEnd = shiftStart + 8 * 3600 * 1000
        val prefillData = pointingManager.endShift(shiftEnd)

        assertEquals(PointingState.IDLE, pointingManager.getPointingState())
        assertEquals(shiftStart, prefillData.startTimeMillis)
        assertEquals(shiftEnd, prefillData.endTimeMillis)
        assertEquals(15, prefillData.breakMinutes)

        // Assert pref values are cleared
        assertEquals(0L, pointingManager.getStartTime())
        assertEquals(0L, pointingManager.getBreakStartTime())
        assertEquals(0, pointingManager.getAccumulatedBreakMinutes())
    }

    @Test
    fun testEndShiftWhileOnBreak() {
        val shiftStart = System.currentTimeMillis()
        pointingManager.startShift(shiftStart)

        // Go on break for 15 minutes, then end shift directly without manually ending break
        val breakStart = shiftStart + 60 * 1000
        pointingManager.startBreak(breakStart)

        // End shift 15 minutes after break started
        val shiftEnd = breakStart + 15 * 60 * 1000
        val prefillData = pointingManager.endShift(shiftEnd)

        assertEquals(PointingState.IDLE, pointingManager.getPointingState())
        assertEquals(shiftStart, prefillData.startTimeMillis)
        assertEquals(shiftEnd, prefillData.endTimeMillis)
        assertEquals(15, prefillData.breakMinutes)
    }
}
