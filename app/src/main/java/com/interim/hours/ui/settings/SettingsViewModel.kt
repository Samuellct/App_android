package com.interim.hours.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.interim.hours.data.repository.WorkDayRepository
import com.interim.hours.worker.ReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _notificationHour = MutableStateFlow(sharedPrefs.getInt("notification_hour", 19))
    val notificationHour: StateFlow<Int> = _notificationHour

    private val _notificationMinute = MutableStateFlow(sharedPrefs.getInt("notification_minute", 0))
    val notificationMinute: StateFlow<Int> = _notificationMinute

    init {
        // Schedule reminder on startup if enabled
        if (_notificationsEnabled.value) {
            scheduleReminder(_notificationHour.value, _notificationMinute.value)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
        if (enabled) {
            scheduleReminder(_notificationHour.value, _notificationMinute.value)
        } else {
            cancelReminder()
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        sharedPrefs.edit()
            .putInt("notification_hour", hour)
            .putInt("notification_minute", minute)
            .apply()
        _notificationHour.value = hour
        _notificationMinute.value = minute

        if (_notificationsEnabled.value) {
            scheduleReminder(hour, minute)
        }
    }

    private fun scheduleReminder(hour: Int, minute: Int) {
        val workManager = WorkManager.getInstance(context)

        // Calculate initial delay
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        // Build periodic request
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyReminderWork",
            ExistingPeriodicWorkPolicy.UPDATE, // updates schedule with new time
            reminderRequest
        )
    }

    private fun cancelReminder() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("DailyReminderWork")
    }

    fun exportToCSV(onShareIntentReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val workDays = workDayRepository.getWorkDaysWithDetailsFlow().first()
            val csvBuilder = StringBuilder()

            // CSV Header
            csvBuilder.append("Date,Mission,Agence,Taux Horaire,Heures Travaillées,Primes,Commentaire\n")

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            workDays.forEach { item ->
                val day = item.workDay
                val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
                val cleanDuration = if (durationHours > 0.0) durationHours else 0.0
                val bonusesText = item.bonuses.joinToString(";") { "${it.name}:${it.amount}€" }

                csvBuilder.append(dateFormat.format(Date(day.dateMillis))).append(",")
                csvBuilder.append("\"").append(item.mission.company.replace("\"", "\"\"")).append("\",")
                csvBuilder.append("\"").append(item.mission.agency.replace("\"", "\"\"")).append("\",")
                csvBuilder.append(item.mission.hourlyRate).append(",")
                csvBuilder.append(String.format(Locale.US, "%.2f", cleanDuration)).append(",")
                csvBuilder.append("\"").append(bonusesText).append("\",")
                csvBuilder.append("\"").append(day.comment.replace("\"", "\"\"")).append("\"\n")
            }

            // Create share intent for CSV text
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Export Heures d'Intérim")
                putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
            }
            onShareIntentReady(Intent.createChooser(intent, "Partager l'export CSV"))
        }
    }
}
