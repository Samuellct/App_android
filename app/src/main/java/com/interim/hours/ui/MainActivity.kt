package com.interim.hours.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.interim.hours.ui.navigation.AppNavigation
import com.interim.hours.ui.theme.WorkLogTheme
import com.interim.hours.ui.theme.ThemeMode
import com.interim.hours.ui.settings.SettingsViewModel
import com.interim.hours.data.pointing.PointingManager
import com.interim.hours.data.pointing.PointingState
import com.interim.hours.data.pointing.PrefillShiftData
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled silently
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let { handleIntent(it) }

        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settingsViewModel: SettingsViewModel by viewModels()
            val themeMode by settingsViewModel.appTheme.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            WorkLogTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val isFromReminder = intent.getBooleanExtra("EXTRA_FROM_REMINDER", false)
        val isPrefillShift = intent.getBooleanExtra("EXTRA_PREFILL_SHIFT", false)

        if (isPrefillShift) {
            val startTime = intent.getLongExtra("EXTRA_START_TIME", 0L)
            val endTime = intent.getLongExtra("EXTRA_END_TIME", 0L)
            val breakMin = intent.getIntExtra("EXTRA_BREAK_MINUTES", 0)
            if (startTime > 0L && endTime > 0L) {
                PointingManager.setPrefillData(
                    PrefillShiftData(
                        startTimeMillis = startTime,
                        endTimeMillis = endTime,
                        breakMinutes = breakMin
                    )
                )
            }
        } else if (isFromReminder) {
            val pointingManager = PointingManager.getInstance(applicationContext)
            val state = pointingManager.getPointingState()
            if (state != PointingState.IDLE) {
                val startTime = pointingManager.getStartTime()
                val breakStartTime = pointingManager.getBreakStartTime()
                var accumulatedBreak = pointingManager.getAccumulatedBreakMinutes()

                if (state == PointingState.ON_BREAK && breakStartTime > 0L) {
                    val elapsed = ((System.currentTimeMillis() - breakStartTime) / 60000).toInt()
                    accumulatedBreak += elapsed
                }

                if (startTime > 0L) {
                    PointingManager.setPrefillData(
                        PrefillShiftData(
                            startTimeMillis = startTime,
                            endTimeMillis = System.currentTimeMillis(),
                            breakMinutes = accumulatedBreak
                        )
                    )
                }
            }
        }
    }
}
