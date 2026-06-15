package com.interim.hours.utils

import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
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
}
