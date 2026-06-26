package com.interim.hours.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.interim.hours.data.pointing.PointingManager
import com.interim.hours.data.pointing.PointingState
import com.interim.hours.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PointingReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pointingManager: PointingManager

    companion object {
        private const val CHANNEL_ID = "pointing_reminder_channel"
        private const val NOTIFICATION_ID_SHIFT = 2001
        private const val NOTIFICATION_ID_BREAK = 2002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule active alarms on reboot
            val state = pointingManager.getPointingState()
            val startTime = pointingManager.getStartTime()
            val breakStartTime = pointingManager.getBreakStartTime()

            if (state == PointingState.WORKING && startTime > 0L) {
                val triggerTime = startTime + 10 * 3600 * 1000L // 10 hours
                PointingReminderScheduler.scheduleShiftReminder(context, triggerTime)
            } else if (state == PointingState.ON_BREAK && breakStartTime > 0L) {
                val triggerTime = breakStartTime + 2 * 3600 * 1000L // 2 hours
                PointingReminderScheduler.scheduleBreakReminder(context, triggerTime)
            }
            return
        }

        if (action == "com.interim.hours.ACTION_SHIFT_REMINDER") {
            sendNotification(
                context = context,
                notificationId = NOTIFICATION_ID_SHIFT,
                title = "Oubli de fin de journée ?",
                text = "Vous travaillez depuis plus de 10 heures. Pensez à enregistrer vos heures !"
            )
            return
        }

        if (action == "com.interim.hours.ACTION_BREAK_REMINDER") {
            sendNotification(
                context = context,
                notificationId = NOTIFICATION_ID_BREAK,
                title = "Pause prolongée ?",
                text = "Votre pause dure depuis plus de 2 heures. Reprenez votre travail ou terminez la journée !"
            )
            return
        }
    }

    private fun sendNotification(context: Context, notificationId: Int, title: String, text: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_FROM_REMINDER", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rappels de pointage"
            val descriptionText = "Rappels pour ne pas oublier d'enregistrer les fins de shift ou de pause"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
