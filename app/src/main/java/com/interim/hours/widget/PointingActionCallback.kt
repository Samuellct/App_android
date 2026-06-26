package com.interim.hours.widget

import android.content.Context
import android.content.Intent
import android.util.Log
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
        Log.d("PointingActionCallback", "onAction triggered for action=$action at now=$now")

        try {
            when (action) {
                "start_shift" -> {
                    pointingManager.startShift(now)
                    try {
                        PointingReminderScheduler.scheduleShiftReminder(context, now + 10 * 3600 * 1000L)
                    } catch (e: Exception) {
                        Log.e("PointingActionCallback", "Failed to schedule shift reminder: ${e.message}", e)
                    }
                }
                "start_break" -> {
                    pointingManager.startBreak(now)
                    try {
                        PointingReminderScheduler.cancelShiftReminder(context)
                        PointingReminderScheduler.scheduleBreakReminder(context, now + 2 * 3600 * 1000L)
                    } catch (e: Exception) {
                        Log.e("PointingActionCallback", "Failed to schedule break reminder: ${e.message}", e)
                    }
                }
                "end_break" -> {
                    pointingManager.endBreak(now)
                    try {
                        PointingReminderScheduler.cancelBreakReminder(context)
                        val startTime = pointingManager.getStartTime()
                        if (startTime > 0L) {
                            PointingReminderScheduler.scheduleShiftReminder(context, startTime + 10 * 3600 * 1000L)
                        }
                    } catch (e: Exception) {
                        Log.e("PointingActionCallback", "Failed to reschedule shift reminder: ${e.message}", e)
                    }
                }
                "end_shift" -> {
                    val data = pointingManager.endShift(now)
                    try {
                        PointingReminderScheduler.cancelAllReminders(context)
                    } catch (e: Exception) {
                        Log.e("PointingActionCallback", "Failed to cancel reminders: ${e.message}", e)
                    }

                    // Update widgets synchronously *before* starting MainActivity to avoid delay due to activity startup transition
                    try {
                        WorkLogWidget().updateAll(context)
                        WorkLogVerticalWidget().updateAll(context)
                        Log.d("PointingActionCallback", "Widgets updated synchronously prior to Activity start")
                    } catch (e: Exception) {
                        Log.e("PointingActionCallback", "Failed to update widgets: ${e.message}", e)
                    }

                    // Open MainActivity with prefilled data
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("EXTRA_PREFILL_SHIFT", true)
                        putExtra("EXTRA_START_TIME", data.startTimeMillis)
                        putExtra("EXTRA_END_TIME", data.endTimeMillis)
                        putExtra("EXTRA_BREAK_MINUTES", data.breakMinutes)
                    }
                    context.startActivity(intent)
                    return // Prevent running the trailing updateAll below
                }
            }
        } catch (e: Exception) {
            Log.e("PointingActionCallback", "Error processing action: ${e.message}", e)
        }

        // Update widgets synchronously within the action callback lifecycle
        try {
            WorkLogWidget().updateAll(context)
            WorkLogVerticalWidget().updateAll(context)
            Log.d("PointingActionCallback", "Widgets updated at the end of onAction")
        } catch (e: Exception) {
            Log.e("PointingActionCallback", "Failed to update widgets: ${e.message}", e)
        }
    }
}
