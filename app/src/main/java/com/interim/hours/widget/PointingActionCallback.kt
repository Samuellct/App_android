package com.interim.hours.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.interim.hours.data.pointing.PointingManager
import com.interim.hours.data.pointing.PointingState
import com.interim.hours.worker.PointingReminderScheduler
import com.interim.hours.ui.MainActivity
import androidx.glance.appwidget.updateAll

class PointingActionCallback : ActionCallback {

    companion object {
        val KEY_ACTION = ActionParameters.Key<String>("pointing_action")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[KEY_ACTION] ?: return
        val pointingManager = PointingManager.getInstance(context)
        val now = System.currentTimeMillis()

        when (action) {
            "start_shift" -> {
                pointingManager.startShift(now)
                // Schedule alarm for 10 hours from now
                PointingReminderScheduler.scheduleShiftReminder(context, now + 10 * 3600 * 1000L)
            }
            "start_break" -> {
                pointingManager.startBreak(now)
                // Cancel shift alarm and schedule break alarm for 2 hours from now
                PointingReminderScheduler.cancelShiftReminder(context)
                PointingReminderScheduler.scheduleBreakReminder(context, now + 2 * 3600 * 1000L)
            }
            "end_break" -> {
                pointingManager.endBreak(now)
                // Cancel break alarm and reschedule shift alarm
                PointingReminderScheduler.cancelBreakReminder(context)
                val startTime = pointingManager.getStartTime()
                if (startTime > 0L) {
                    PointingReminderScheduler.scheduleShiftReminder(context, startTime + 10 * 3600 * 1000L)
                }
            }
            "end_shift" -> {
                // End shift returns the pre-filled data and resets states
                val data = pointingManager.endShift(now)
                // Cancel all alarms
                PointingReminderScheduler.cancelAllReminders(context)

                // Open MainActivity with the pre-filled data
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("EXTRA_PREFILL_SHIFT", true)
                    putExtra("EXTRA_START_TIME", data.startTimeMillis)
                    putExtra("EXTRA_END_TIME", data.endTimeMillis)
                    putExtra("EXTRA_BREAK_MINUTES", data.breakMinutes)
                }
                context.startActivity(intent)
            }
        }

        // Update widgets synchronously within the action callback lifecycle
        try {
            WorkLogWidget().updateAll(context)
            WorkLogVerticalWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
