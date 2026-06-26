package com.interim.hours.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.work.*
import com.interim.hours.data.repository.MissionRepository
import com.interim.hours.data.repository.WorkDayRepository
import com.interim.hours.worker.ReminderScheduler
import com.interim.hours.ui.theme.ThemeMode
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
    private val workDayRepository: WorkDayRepository,
    private val missionRepository: MissionRepository,
    private val appDatabase: com.interim.hours.data.database.AppDatabase
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            "app_theme" -> {
                val themeStr = prefs.getString("app_theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
                _appTheme.value = ThemeMode.valueOf(themeStr)
            }
            "notifications_enabled" -> {
                _notificationsEnabled.value = prefs.getBoolean("notifications_enabled", true)
            }
            "notification_hour" -> {
                _notificationHour.value = prefs.getInt("notification_hour", 19)
            }
            "notification_minute" -> {
                _notificationMinute.value = prefs.getInt("notification_minute", 0)
            }
            "target_type" -> {
                _targetType.value = prefs.getString("target_type", "HOURS") ?: "HOURS"
            }
            "target_value_hours" -> {
                _targetValueHours.value = prefs.getFloat("target_value_hours", 151.67f)
            }
            "target_value_earnings" -> {
                _targetValueEarnings.value = prefs.getFloat("target_value_earnings", 1800f)
            }
            "has_completed_onboarding" -> {
                _hasCompletedOnboarding.value = prefs.getBoolean("has_completed_onboarding", false)
            }
            "chart_duration_months" -> {
                _chartDurationMonths.value = prefs.getInt("chart_duration_months", 6)
            }
            "google_backup_enabled" -> {
                _googleBackupEnabled.value = prefs.getBoolean("google_backup_enabled", true)
            }
        }
    }

    private val _chartDurationMonths = MutableStateFlow(sharedPrefs.getInt("chart_duration_months", 6))
    val chartDurationMonths: StateFlow<Int> = _chartDurationMonths

    fun setChartDurationMonths(months: Int) {
        sharedPrefs.edit().putInt("chart_duration_months", months).apply()
        _chartDurationMonths.value = months
    }

    private val _appTheme = MutableStateFlow(
        ThemeMode.valueOf(sharedPrefs.getString("app_theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val appTheme: StateFlow<ThemeMode> = _appTheme

    fun setAppTheme(theme: ThemeMode) {
        sharedPrefs.edit().putString("app_theme", theme.name).apply()
        _appTheme.value = theme
    }

    private val _targetType = MutableStateFlow(sharedPrefs.getString("target_type", "HOURS") ?: "HOURS")
    val targetType: StateFlow<String> = _targetType

    private val _targetValueHours = MutableStateFlow(sharedPrefs.getFloat("target_value_hours", 151.67f))
    val targetValueHours: StateFlow<Float> = _targetValueHours

    private val _targetValueEarnings = MutableStateFlow(sharedPrefs.getFloat("target_value_earnings", 1800f))
    val targetValueEarnings: StateFlow<Float> = _targetValueEarnings

    fun setTargetType(type: String) {
        sharedPrefs.edit().putString("target_type", type).apply()
        _targetType.value = type
    }

    fun setTargetValueHours(value: Float) {
        sharedPrefs.edit().putFloat("target_value_hours", value).apply()
        _targetValueHours.value = value
    }

    fun setTargetValueEarnings(value: Float) {
        sharedPrefs.edit().putFloat("target_value_earnings", value).apply()
        _targetValueEarnings.value = value
    }

    private val _hasCompletedOnboarding = MutableStateFlow(sharedPrefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    private val _googleBackupEnabled = MutableStateFlow(sharedPrefs.getBoolean("google_backup_enabled", true))
    val googleBackupEnabled: StateFlow<Boolean> = _googleBackupEnabled

    fun setGoogleBackupEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("google_backup_enabled", enabled).apply()
        _googleBackupEnabled.value = enabled
    }

    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _notificationHour = MutableStateFlow(sharedPrefs.getInt("notification_hour", 19))
    val notificationHour: StateFlow<Int> = _notificationHour

    private val _notificationMinute = MutableStateFlow(sharedPrefs.getInt("notification_minute", 0))
    val notificationMinute: StateFlow<Int> = _notificationMinute

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener)
        // Schedule reminder on startup if enabled
        if (_notificationsEnabled.value) {
            scheduleReminder(_notificationHour.value, _notificationMinute.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener)
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
        ReminderScheduler.scheduleReminder(context, hour, minute)
    }

    private fun cancelReminder() {
        ReminderScheduler.cancelReminder(context)
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

    fun exportBackupJson(uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val missionsWithBonuses = missionRepository.getMissionsWithBonusesFlow().first()
                val workDaysWithDetails = workDayRepository.getWorkDaysWithDetailsFlow().first()

                val rootJson = org.json.JSONObject()
                rootJson.put("version", 1)

                // Missions
                val missionsArray = org.json.JSONArray()
                missionsWithBonuses.forEach { item ->
                    val mission = item.mission
                    val mJson = org.json.JSONObject().apply {
                        put("syncId", mission.syncId)
                        put("company", mission.company)
                        put("agency", mission.agency)
                        put("hourlyRate", mission.hourlyRate)
                        put("siteAddress", mission.siteAddress)
                        put("colorHex", mission.colorHex)
                        put("isActive", mission.isActive)
                        put("nightStartHour", mission.nightStartHour)
                        put("nightEndHour", mission.nightEndHour)
                        put("nightRatePercentage", mission.nightRatePercentage)
                        put("hasIfmIccp", mission.hasIfmIccp)
                        put("hasWeeklyOvertime", mission.hasWeeklyOvertime)
                        put("weeklyOvertimeThreshold", mission.weeklyOvertimeThreshold)
                        put("overtimeRate1Percentage", mission.overtimeRate1Percentage)
                        put("overtimeRate2Percentage", mission.overtimeRate2Percentage)
                    }

                    val bonusesArray = org.json.JSONArray()
                    item.bonuses.forEach { bonus ->
                        val bJson = org.json.JSONObject().apply {
                            put("name", bonus.name)
                            put("defaultAmount", bonus.defaultAmount)
                        }
                        bonusesArray.put(bJson)
                    }
                    mJson.put("bonuses", bonusesArray)
                    missionsArray.put(mJson)
                }
                rootJson.put("missions", missionsArray)

                // WorkDays
                val workDaysArray = org.json.JSONArray()
                workDaysWithDetails.forEach { item ->
                    val day = item.workDay
                    val dJson = org.json.JSONObject().apply {
                        put("syncId", day.syncId)
                        put("missionSyncId", item.mission.syncId)
                        put("dateMillis", day.dateMillis)
                        put("startTimeMillis", day.startTimeMillis)
                        put("endTimeMillis", day.endTimeMillis)
                        put("breakMinutes", day.breakMinutes)
                        put("comment", day.comment)
                    }

                    val bonusesArray = org.json.JSONArray()
                    item.bonuses.forEach { bonus ->
                        val bJson = org.json.JSONObject().apply {
                            put("name", bonus.name)
                            put("amount", bonus.amount)
                        }
                        bonusesArray.put(bJson)
                    }
                    dJson.put("bonuses", bonusesArray)
                    workDaysArray.put(dJson)
                }
                rootJson.put("workDays", workDaysArray)

                // Write to Uri
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun importBackupJson(uri: android.net.Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("Impossible de lire le flux d'entrée")

                val rootJson = org.json.JSONObject(jsonString)

                val missionsArray = rootJson.optJSONArray("missions") ?: org.json.JSONArray()
                val workDaysArray = rootJson.optJSONArray("workDays") ?: org.json.JSONArray()

                // Limit checks for safety against DoS/bloat
                if (missionsArray.length() > 200 || workDaysArray.length() > 2000) {
                    throw Exception("Le fichier de sauvegarde dépasse les limites autorisées (max 200 missions et 2000 journées).")
                }

                var importedMissions = 0
                var importedWorkDays = 0

                // Keep track of syncId to new generated mission ID mapping
                val missionSyncIdToIdMap = mutableMapOf<String, Int>()

                // Regular expression to check hex color validity
                val colorHexPattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$".toRegex()

                // Perform all database operations inside a single atomic transaction
                appDatabase.withTransaction {
                    // First, read all existing missions to avoid inserting duplicates if they already exist
                    val existingMissions = appDatabase.missionDao().getMissionsWithBonusesFlow().first()
                    val existingMissionsMap = existingMissions.associateBy { it.mission.syncId }

                    // Insert/Update missions
                    for (i in 0 until missionsArray.length()) {
                        val mJson = missionsArray.getJSONObject(i)
                        
                        val company = mJson.optString("company", "").trim()
                        val agency = mJson.optString("agency", "").trim()
                        if (company.isEmpty() || agency.isEmpty()) {
                            continue // Skip invalid missions missing main text details
                        }

                        val syncId = mJson.optString("syncId", java.util.UUID.randomUUID().toString())
                        val existing = existingMissionsMap[syncId]
                        
                        val colorHex = mJson.optString("colorHex", "#6366F1")
                        val cleanColorHex = if (colorHexPattern.matches(colorHex)) colorHex else "#6366F1"

                        val mission = com.interim.hours.data.model.Mission(
                            id = existing?.mission?.id ?: 0,
                            syncId = syncId,
                            company = company,
                            agency = agency,
                            hourlyRate = mJson.optDouble("hourlyRate", 12.31).coerceIn(0.0, 500.0),
                            siteAddress = mJson.optString("siteAddress", ""),
                            colorHex = cleanColorHex,
                            isActive = mJson.optBoolean("isActive", true),
                            nightStartHour = mJson.optInt("nightStartHour", 21).coerceIn(0, 23),
                            nightEndHour = mJson.optInt("nightEndHour", 6).coerceIn(0, 23),
                            nightRatePercentage = mJson.optDouble("nightRatePercentage", 0.0).coerceIn(0.0, 200.0),
                            hasIfmIccp = mJson.optBoolean("hasIfmIccp", true),
                            hasWeeklyOvertime = mJson.optBoolean("hasWeeklyOvertime", true),
                            weeklyOvertimeThreshold = mJson.optDouble("weeklyOvertimeThreshold", 35.0).coerceIn(10.0, 100.0),
                            overtimeRate1Percentage = mJson.optDouble("overtimeRate1Percentage", 25.0).coerceIn(0.0, 200.0),
                            overtimeRate2Percentage = mJson.optDouble("overtimeRate2Percentage", 50.0).coerceIn(0.0, 200.0)
                        )

                        val bonusesArray = mJson.optJSONArray("bonuses") ?: org.json.JSONArray()
                        val bonuses = mutableListOf<com.interim.hours.data.model.MissionBonus>()
                        for (j in 0 until bonusesArray.length()) {
                            val bJson = bonusesArray.getJSONObject(j)
                            val name = bJson.optString("name", "").trim()
                            if (name.isNotEmpty()) {
                                bonuses.add(
                                    com.interim.hours.data.model.MissionBonus(
                                        missionId = mission.id,
                                        name = name,
                                        defaultAmount = bJson.optDouble("defaultAmount", 0.0).coerceIn(0.0, 1000.0)
                                    )
                                )
                            }
                        }

                        // Save the mission to database and retrieve the generated/updated ID directly
                        val dbId = appDatabase.missionDao().saveMissionWithBonuses(mission, bonuses)
                        missionSyncIdToIdMap[syncId] = dbId
                        importedMissions++
                    }

                    // Read existing work days to update/prevent duplicates
                    val existingWorkDays = appDatabase.workDayDao().getWorkDaysWithDetails()
                    val existingWorkDaysMap = existingWorkDays.associateBy { it.workDay.syncId }

                    // Insert/Update workdays
                    for (i in 0 until workDaysArray.length()) {
                        val dJson = workDaysArray.getJSONObject(i)
                        val syncId = dJson.optString("syncId", java.util.UUID.randomUUID().toString())
                        val missionSyncId = dJson.getString("missionSyncId")
                        
                        val dbMissionId = missionSyncIdToIdMap[missionSyncId]
                            ?: existingMissions.find { it.mission.syncId == missionSyncId }?.mission?.id
                            ?: continue // Skip if mission doesn't exist or couldn't be resolved

                        val dateMillis = dJson.optLong("dateMillis", 0L)
                        val startTimeMillis = dJson.optLong("startTimeMillis", 0L)
                        val endTimeMillis = dJson.optLong("endTimeMillis", 0L)
                        if (dateMillis <= 0L || startTimeMillis <= 0L || endTimeMillis <= 0L) {
                            continue // Skip entry if dates are completely corrupted/empty
                        }

                        val durationDiff = endTimeMillis - startTimeMillis
                        if (durationDiff <= 0L || durationDiff > 36 * 3600 * 1000L) {
                            continue // Skip entry with corrupted timeframe (prevents ANR and infinite loops)
                        }

                        val existing = existingWorkDaysMap[syncId]

                        val workDay = com.interim.hours.data.model.WorkDay(
                            id = existing?.workDay?.id ?: 0,
                            missionId = dbMissionId,
                            dateMillis = dateMillis,
                            startTimeMillis = startTimeMillis,
                            endTimeMillis = endTimeMillis,
                            breakMinutes = dJson.optInt("breakMinutes", 0).coerceIn(0, 1440),
                            comment = dJson.optString("comment", ""),
                            syncId = syncId
                        )

                        val bonusesArray = dJson.optJSONArray("bonuses") ?: org.json.JSONArray()
                        val bonuses = mutableListOf<com.interim.hours.data.model.WorkDayBonus>()
                        for (j in 0 until bonusesArray.length()) {
                            val bJson = bonusesArray.getJSONObject(j)
                            val name = bJson.optString("name", "").trim()
                            if (name.isNotEmpty()) {
                                bonuses.add(
                                    com.interim.hours.data.model.WorkDayBonus(
                                        workDayId = workDay.id,
                                        name = name,
                                        amount = bJson.optDouble("amount", 0.0).coerceIn(0.0, 1000.0)
                                    )
                                )
                            }
                        }

                        appDatabase.workDayDao().saveWorkDayWithBonuses(workDay, bonuses)
                        importedWorkDays++
                    }
                }

                // Update Widget only once after all imports are safely committed
                com.interim.hours.widget.WidgetUpdater.triggerUpdate(context)

                onComplete(true, "Importation réussie : $importedMissions missions et $importedWorkDays journées importées.")
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Erreur lors de l'importation : ${e.localizedMessage}")
            }
        }
    }
}
