package com.interim.hours.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object PointingReminderScheduler {
    private const val REQUEST_CODE_SHIFT = 2001
    private const val REQUEST_CODE_BREAK = 2002

    fun scheduleShiftReminder(context: Context, triggerTimeMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PointingReminderReceiver::class.java).apply {
                action = "com.interim.hours.ACTION_SHIFT_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SHIFT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(alarmManager, triggerTimeMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e("PointingReminderScheduler", "Failed to schedule shift reminder: ${e.message}", e)
        }
    }

    fun scheduleBreakReminder(context: Context, triggerTimeMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PointingReminderReceiver::class.java).apply {
                action = "com.interim.hours.ACTION_BREAK_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BREAK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(alarmManager, triggerTimeMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e("PointingReminderScheduler", "Failed to schedule break reminder: ${e.message}", e)
        }
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d("PointingReminderScheduler", "Scheduled exact alarm at $triggerTimeMillis")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                    Log.d("PointingReminderScheduler", "Scheduled inexact alarm at $triggerTimeMillis (no permission)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
                Log.d("PointingReminderScheduler", "Scheduled exact alarm at $triggerTimeMillis (pre-S)")
            }
        } catch (e: SecurityException) {
            Log.w("PointingReminderScheduler", "SecurityException scheduling exact alarm: ${e.message}. Falling back to inexact alarm.", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e("PointingReminderScheduler", "Failed to schedule fallback alarm: ${ex.message}", ex)
            }
        } catch (e: Exception) {
            Log.e("PointingReminderScheduler", "Exception scheduling alarm: ${e.message}", e)
        }
    }

    fun cancelShiftReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PointingReminderReceiver::class.java).apply {
                action = "com.interim.hours.ACTION_SHIFT_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SHIFT,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("PointingReminderScheduler", "Cancelled shift reminder")
            }
        } catch (e: Exception) {
            Log.e("PointingReminderScheduler", "Error cancelling shift reminder: ${e.message}", e)
        }
    }

    fun cancelBreakReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PointingReminderReceiver::class.java).apply {
                action = "com.interim.hours.ACTION_BREAK_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BREAK,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("PointingReminderScheduler", "Cancelled break reminder")
            }
        } catch (e: Exception) {
            Log.e("PointingReminderScheduler", "Error cancelling break reminder: ${e.message}", e)
        }
    }

    fun cancelAllReminders(context: Context) {
        cancelShiftReminder(context)
        cancelBreakReminder(context)
    }
}
