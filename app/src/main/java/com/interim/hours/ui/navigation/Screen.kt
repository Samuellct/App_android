package com.interim.hours.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Calendar : Screen("calendar", "Calendrier", Icons.Default.CalendarMonth)
    object Missions : Screen("missions", "Missions", Icons.Default.Work)
    object History : Screen("history", "Historique", Icons.Default.History)
    object Settings : Screen("settings", "Réglages", Icons.Default.Settings)
}
