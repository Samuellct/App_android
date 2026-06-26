package com.interim.hours.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object PointingReminderScheduler {
    private const val REQUEST_CODE_SHIFT = 2001
    private const val REQUEST_CODE_BREAK = 2002

    fun scheduleShiftReminder(context: Context, triggerTimeMillis: Long) {
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
    }

    fun scheduleBreakReminder(context: Context, triggerTimeMillis: Long) {
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
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelShiftReminder(context: Context) {
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
        }
    }

    fun cancelBreakReminder(context: Context) {
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
        }
    }

    fun cancelAllReminders(context: Context) {
        cancelShiftReminder(context)
        cancelBreakReminder(context)
    }
}
