package com.interim.hours.utils

import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import java.util.Calendar
import java.util.Locale

object SalaryCalculator {

    fun calculateNightHours(
        startTimeMillis: Long,
        endTimeMillis: Long,
        nightStartHour: Int,
        nightEndHour: Int
    ): Double {
        if (startTimeMillis >= endTimeMillis) return 0.0
        // Safeguard to prevent extremely large loops causing ANRs on corrupt data
        if (endTimeMillis - startTimeMillis > 36 * 3600 * 1000L) return 0.0

        val calendar = Calendar.getInstance()
        var nightMinutes = 0
        var currentMillis = startTimeMillis

        while (currentMillis < endTimeMillis) {
            calendar.timeInMillis = currentMillis
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            val isNight = if (nightStartHour > nightEndHour) {
                // Spans across midnight, e.g., 21:00 to 06:00
                hour >= nightStartHour || hour < nightEndHour
            } else {
                // Within the same day, e.g., 01:00 to 05:00
                hour in nightStartHour until nightEndHour
            }
            
            if (isNight) {
                nightMinutes++
            }
            currentMillis += 60_000 // increment by 1 minute
        }
        return nightMinutes / 60.0
    }

    fun calculateEarnings(
        workDay: WorkDay,
        mission: Mission,
        bonuses: List<WorkDayBonus>
    ): Double {
        val durationHours = (workDay.endTimeMillis - workDay.startTimeMillis - workDay.breakMinutes * 60000.0) / 3600000.0
        val cleanDuration = if (durationHours > 0.0) durationHours else 0.0

        val baseWages = cleanDuration * mission.hourlyRate

        // Calculate night hours
        val nightHours = if (mission.nightRatePercentage > 0.0) {
            calculateNightHours(
                workDay.startTimeMillis,
                workDay.endTimeMillis,
                mission.nightStartHour,
                mission.nightEndHour
            )
        } else {
            0.0
        }

        // Apply break deduction to night hours to be conservative
        val cleanNightHours = minOf(nightHours, cleanDuration)
        val nightPremium = cleanNightHours * mission.hourlyRate * (mission.nightRatePercentage / 100.0)

        var totalWages = baseWages + nightPremium

        // Apply French legal compensations (10% IFM + 10% ICCP)
        if (mission.hasIfmIccp) {
            val ifm = totalWages * 0.10
            val iccp = (totalWages + ifm) * 0.10
            totalWages += ifm + iccp
        }

        val bonusesSum = bonuses.sumOf { it.amount }
        return totalWages + bonusesSum
    }

    fun getYearAndWeek(dateMillis: Long): String {
        val cal = Calendar.getInstance(Locale.FRANCE)
        cal.timeInMillis = dateMillis
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val month = cal.get(Calendar.MONTH)
        var adjustedYear = year
        if (month == Calendar.DECEMBER && week == 1) {
            adjustedYear = year + 1
        } else if (month == Calendar.JANUARY && week >= 52) {
            adjustedYear = year - 1
        }
        return "$adjustedYear-W$week"
    }

    fun calculateWeeklyOvertimePremium(
        weekDays: List<WorkDayWithDetails>
    ): Double {
        if (weekDays.isEmpty()) return 0.0
        val mission = weekDays.first().mission
        if (!mission.hasWeeklyOvertime) return 0.0

        var totalHours = 0.0
        weekDays.forEach { item ->
            val day = item.workDay
            val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
            if (durationHours > 0.0) {
                totalHours += durationHours
            }
        }

        if (totalHours <= mission.weeklyOvertimeThreshold) return 0.0

        val overtimeHours = totalHours - mission.weeklyOvertimeThreshold
        val tier1Hours = minOf(overtimeHours, 8.0)
        val tier2Hours = maxOf(0.0, overtimeHours - 8.0)

        val premiumBrut = (tier1Hours * mission.hourlyRate * (mission.overtimeRate1Percentage / 100.0)) +
                (tier2Hours * mission.hourlyRate * (mission.overtimeRate2Percentage / 100.0))

        var totalPremium = premiumBrut
        if (mission.hasIfmIccp) {
            val ifm = premiumBrut * 0.10
            val iccp = (premiumBrut + ifm) * 0.10
            totalPremium += ifm + iccp
        }

        return totalPremium
    }
}
