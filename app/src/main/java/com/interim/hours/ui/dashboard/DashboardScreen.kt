package com.interim.hours.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.ui.missions.toColor
import com.interim.hours.ui.theme.GradientPurpleIndigo
import com.interim.hours.ui.theme.SuccessGreen
import com.interim.hours.ui.theme.GradientTealGreen
import com.interim.hours.ui.workday.WorkDayDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit
) {
    val stats by viewModel.statsState.collectAsState()
    val activeMissions by viewModel.activeMissionsState.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableau de bord", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Réglages",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                icon = { Icon(Icons.Default.MoreTime, contentDescription = null) },
                text = { Text("Saisir Journée") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main stats overview: Hours and Earnings (Row of Cards)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard(
                        title = "Mois en cours",
                        value = String.format("%.1f h", stats.monthlyHours),
                        subValue = "Semaine: ${String.format("%.1f h", stats.weeklyHours)}",
                        icon = Icons.Default.Schedule,
                        gradient = GradientPurpleIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Revenu mensuel (Brut)",
                        value = String.format("%.2f €", stats.monthlyEarnings),
                        subValue = "Net: ~${String.format("%.2f €", stats.monthlyEarnings * 0.77)} | Sem: ${String.format("%.1f € B", stats.weeklyEarnings)}",
                        icon = Icons.Default.Euro,
                        gradient = GradientTealGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Monthly progress target graphic
            item {
                MonthlyProgressCard(
                    stats = stats
                )
            }

            // Active missions card helper
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Work,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Missions actives",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stats.activeMissionsCount} mission(s) configurée(s) actuellement",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent activity list
            item {
                Text(
                    text = "Dernières saisies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (stats.recentDays.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Aucune journée saisie pour l'instant.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(stats.recentDays, key = { it.workDay.id }) { item ->
                    RecentDayItem(dayWithDetails = item)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showLogDialog) {
        WorkDayDialog(
            missions = activeMissions,
            onDismiss = { showLogDialog = false },
            onSave = { workDay, bonuses ->
                viewModel.saveWorkDay(workDay, bonuses)
                showLogDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(140.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subValue,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyProgressCard(
    stats: DashboardViewModel.DashboardStats,
    modifier: Modifier = Modifier
) {
    val isHours = stats.targetType == "HOURS"
    val target = if (isHours) stats.targetValueHours.toDouble() else stats.targetValueEarnings.toDouble()
    val value = if (isHours) stats.monthlyHours else stats.monthlyEarnings

    val progress = if (target > 0.0) (value / target).coerceIn(0.0, 1.0).toFloat() else 0f
    val percentage = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Objectif Mensuel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHours) {
                        "Cible : ${String.format(Locale.FRANCE, "%.1f", target)} h"
                    } else {
                        "Cible : ${String.format(Locale.FRANCE, "%.2f", target)} € (Net estimé)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isHours) {
                        "${String.format(Locale.FRANCE, "%.1f", value)}h sur ${String.format(Locale.FRANCE, "%.1f", target)}h réalisées"
                    } else {
                        val netValue = value * 0.77
                        "${String.format(Locale.FRANCE, "%.2f", netValue)}€ sur ${String.format(Locale.FRANCE, "%.2f", target)}€ net estimés"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Premium Compose Canvas Progress Arc/Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.surfaceVariant
                
                val finalProgress = if (isHours) {
                    progress
                } else {
                    if (target > 0.0) ((value * 0.77) / target).coerceIn(0.0, 1.0).toFloat() else 0f
                }
                val finalPercentage = (finalProgress * 100).toInt()

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = outlineColor,
                        style = Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = finalProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${finalPercentage}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RecentDayItem(
    dayWithDetails: WorkDayWithDetails,
    modifier: Modifier = Modifier
) {
    val day = dayWithDetails.workDay
    val mission = dayWithDetails.mission
    val color = mission.colorHex.toColor()
    val dateFormatter = remember { SimpleDateFormat("EEE dd MMMM", Locale.FRANCE) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }

    val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
    val cleanDuration = if (durationHours > 0.0) durationHours else 0.0
    val earnings = com.interim.hours.utils.SalaryCalculator.calculateEarnings(dayWithDetails.workDay, mission, dayWithDetails.bonuses)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = dateFormatter.format(Date(day.dateMillis)).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${mission.company} • ${timeFormatter.format(Date(day.startTimeMillis))} - ${timeFormatter.format(Date(day.endTimeMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.2f € Brut", earnings),
                    fontWeight = FontWeight.Black,
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = String.format("~%.2f € Net", earnings * 0.77),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.1f", cleanDuration)}h worked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
