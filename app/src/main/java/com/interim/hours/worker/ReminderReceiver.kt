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
import com.interim.hours.data.repository.WorkDayRepository
import com.interim.hours.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workDayRepository: WorkDayRepository

    companion object {
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PREFS_NAME = "settings_prefs"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = sharedPrefs.getBoolean("notifications_enabled", true)
            if (enabled) {
                val hour = sharedPrefs.getInt("notification_hour", 19)
                val minute = sharedPrefs.getInt("notification_minute", 0)
                ReminderScheduler.scheduleReminder(context, hour, minute)
            }
            return
        }

        if (action == "com.interim.hours.ACTION_SHOW_REMINDER") {
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val todayEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val workDaysToday = workDayRepository.getWorkDaysWithDetailsInRangeFlow(todayStart, todayEnd).first()

                    if (workDaysToday.isEmpty()) {
                        sendNotification(context)
                    }

                    // Reschedule exact alarm for tomorrow
                    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val enabled = sharedPrefs.getBoolean("notifications_enabled", true)
                    if (enabled) {
                        val hour = sharedPrefs.getInt("notification_hour", 19)
                        val minute = sharedPrefs.getInt("notification_minute", 0)
                        ReminderScheduler.scheduleReminder(context, hour, minute)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun sendNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Saisie des Heures")
            .setContentText("Vous n'avez pas encore saisi vos heures d'intérim aujourd'hui. Pensez à le faire !")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rappels quotidiens"
            val descriptionText = "Rappels pour enregistrer les heures de la journée"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
