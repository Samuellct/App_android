package com.interim.hours.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.ui.calendar.CalendarDayWorkItem
import com.interim.hours.ui.missions.toColor
import com.interim.hours.ui.workday.WorkDayDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.queryState.collectAsState()
    val selectedMissionFilter by viewModel.selectedMissionFilterState.collectAsState()
    val filteredWorkDays by viewModel.filteredWorkDaysState.collectAsState()
    val activeMissions by viewModel.activeMissionsState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedWorkDayForEdit by remember { mutableStateOf<WorkDayWithDetails?>(null) }

    // Helper to format month groupings (e.g. "Juin 2026")
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.FRANCE) }

    // Group workdays by month string
    val groupedWorkDays = remember(filteredWorkDays) {
        filteredWorkDays.groupBy { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.workDay.dateMillis }
            monthYearFormat.format(cal.time).replaceFirstChar { it.uppercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique des Heures", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text("Rechercher un commentaire, agence...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mission filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedMissionFilter == null,
                        onClick = { viewModel.selectMissionFilter(null) },
                        label = { Text("Toutes") }
                    )
                }
                items(activeMissions, key = { it.mission.id }) { item ->
                    val isSelected = selectedMissionFilter == item.mission.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectMissionFilter(if (isSelected) null else item.mission.id) },
                        label = { Text(item.mission.company) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.mission.colorHex.toColor())
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List grouped by month
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (groupedWorkDays.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Aucun historique ne correspond aux critères.",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    groupedWorkDays.forEach { (monthStr, daysList) ->
                        item {
                            val context = LocalContext.current
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = monthStr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        com.interim.hours.utils.PdfExporter.exportMonthlyPdf(
                                            context = context,
                                            monthStr = monthStr,
                                            workDays = daysList
                                        ) { intent ->
                                            val chooser = Intent.createChooser(intent, "Partager le relevé PDF")
                                            context.startActivity(chooser)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Relevé PDF", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        items(daysList, key = { it.workDay.id }) { item ->
                            CalendarDayWorkItem(
                                dayWithDetails = item,
                                onEdit = {
                                    selectedWorkDayForEdit = item
                                    showEditDialog = true
                                },
                                onDelete = { viewModel.deleteWorkDay(item.workDay) }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showEditDialog) {
        WorkDayDialog(
            missions = activeMissions,
            onDismiss = { showEditDialog = false },
            onSave = { workDay, bonuses ->
                viewModel.saveWorkDay(workDay, bonuses)
                showEditDialog = false
            },
            existingWorkDayWithDetails = selectedWorkDayForEdit
        )
    }
}
