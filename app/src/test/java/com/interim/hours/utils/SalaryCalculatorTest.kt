package com.interim.hours.utils

import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SalaryCalculatorTest {

    @Test
    fun testCalculateNightHours() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JUNE, 16, 20, 0, 0)
        val startTime = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 16, 23, 0, 0)
        val endTime = cal.timeInMillis

        val nightHours = SalaryCalculator.calculateNightHours(
            startTime,
            endTime,
            21,
            6
        )

        assertEquals(2.0, nightHours, 0.001)
    }

    @Test
    fun testCalculateEarningsWithoutIfmIccp() {
        val mission = Mission(
            id = 1,
            company = "Test Company",
            agency = "Test Agency",
            hourlyRate = 10.0,
            siteAddress = "Site",
            colorHex = "#FF0000",
            nightStartHour = 21,
            nightEndHour = 6,
            nightRatePercentage = 20.0,
            hasIfmIccp = false
        )

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JUNE, 16, 18, 0, 0)
        val startTime = cal.timeInMillis
        cal.set(2026, Calendar.JUNE, 16, 22, 0, 0)
        val endTime = cal.timeInMillis

        val workDay = WorkDay(
            id = 1,
            missionId = 1,
            dateMillis = startTime,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            breakMinutes = 30,
            comment = ""
        )

        val earnings = SalaryCalculator.calculateEarnings(workDay, mission, emptyList())
        assertEquals(37.0, earnings, 0.001)
    }

    @Test
    fun testCalculateWeeklyOvertimePremium() {
        val mission = Mission(
            id = 1,
            company = "Test Company",
            agency = "Test Agency",
            hourlyRate = 12.0,
            siteAddress = "Site",
            colorHex = "#FF0000",
            hasIfmIccp = false,
            hasWeeklyOvertime = true,
            weeklyOvertimeThreshold = 35.0,
            overtimeRate1Percentage = 25.0,
            overtimeRate2Percentage = 50.0
        )

        val days = mutableListOf<WorkDayWithDetails>()
        val cal = Calendar.getInstance()
        for (i in 0 until 5) {
            cal.set(2026, Calendar.JUNE, 15 + i, 8, 0, 0)
            val start = cal.timeInMillis
            cal.set(2026, Calendar.JUNE, 15 + i, 17, 0, 0)
            val end = cal.timeInMillis

            val workDay = WorkDay(
                id = i + 1,
                missionId = 1,
                dateMillis = start,
                startTimeMillis = start,
                endTimeMillis = end,
                breakMinutes = 0,
                comment = ""
            )
            days.add(WorkDayWithDetails(workDay, mission, emptyList()))
        }

        val premium = SalaryCalculator.calculateWeeklyOvertimePremium(days)
        assertEquals(36.0, premium, 0.001)
    }
}
