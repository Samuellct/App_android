package com.interim.hours.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.ui.theme.ErrorRed
import com.interim.hours.ui.theme.SuccessGreen
import com.interim.hours.ui.missions.toColor
import com.interim.hours.ui.workday.WorkDayDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonthState.collectAsState()
    val workDays by viewModel.workDaysInMonthState.collectAsState()
    val activeMissions by viewModel.activeMissionsState.collectAsState()

    var selectedDayCalendar by remember { mutableStateOf<Calendar?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedWorkDayForEdit by remember { mutableStateOf<WorkDayWithDetails?>(null) }

    val monthName = remember(selectedMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.FRANCE)
        sdf.format(selectedMonth.time).replaceFirstChar { it.uppercase() }
    }

    // Set selectedDay default to today if it's the current month, or the 1st of the month otherwise
    LaunchedEffect(selectedMonth) {
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) &&
            today.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR)
        ) {
            selectedDayCalendar = today
        } else {
            selectedDayCalendar = (selectedMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        }
    }

    // Group workdays by day of month
    val workDaysByDay = remember(workDays) {
        workDays.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.workDay.dateMillis }
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    // Selected day's workdays list
    val selectedDayWorkDays = remember(selectedDayCalendar, workDaysByDay) {
        selectedDayCalendar?.get(Calendar.DAY_OF_MONTH)?.let { dayNum ->
            workDaysByDay[dayNum] ?: emptyList()
        } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendrier", fontWeight = FontWeight.Bold) },
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
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.resetToCurrentMonth() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "Aujourd'hui",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday labels
            Row(modifier = Modifier.fillMaxWidth()) {
                val days = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
                days.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            CalendarGrid(
                selectedMonth = selectedMonth,
                workDaysByDay = workDaysByDay,
                selectedDayCalendar = selectedDayCalendar,
                onDayClick = { selectedDayCalendar = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider()

            // Selected day's list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDayCalendar?.let {
                        val sdf = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
                        sdf.format(it.time).replaceFirstChar { char -> char.uppercase() }
                    } ?: "Détails",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                if (selectedDayWorkDays.isEmpty() && activeMissions.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedWorkDayForEdit = null
                            showEditDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Saisir")
                    }
                }
            }

            // Workdays list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (selectedDayWorkDays.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Pas d'heures enregistrées pour ce jour.",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(selectedDayWorkDays, key = { it.workDay.id }) { item ->
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
            existingWorkDayWithDetails = selectedWorkDayForEdit,
            initialDateMillis = selectedDayCalendar?.timeInMillis
        )
    }
}

@Composable
fun CalendarGrid(
    selectedMonth: Calendar,
    workDaysByDay: Map<Int, List<WorkDayWithDetails>>,
    selectedDayCalendar: Calendar?,
    onDayClick: (Calendar) -> Unit
) {
    val tempCal = remember(selectedMonth) {
        (selectedMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    }
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Sunday=1, Monday=2, ..., Saturday=7
    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
    // Offset for Monday-start (0 = Monday, ..., 6 = Sunday)
    val dayOffset = (firstDayOfWeek + 5) % 7

    val totalCells = daysInMonth + dayOffset
    val totalRows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until totalRows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - dayOffset + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val dayCal = (selectedMonth.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dayNum)
                            }
                            val isSelected = selectedDayCalendar?.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                    selectedDayCalendar.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) &&
                                    selectedDayCalendar.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR)

                            val dayWorkDays = workDaysByDay[dayNum] ?: emptyList()
                            val indicatorColors = dayWorkDays.map { it.mission.colorHex.toColor() }.distinct()

                            DayCell(
                                dayNumber = dayNum,
                                isSelected = isSelected,
                                indicatorColors = indicatorColors,
                                onClick = { onDayClick(dayCal) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    indicatorColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.1f
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = dayNumber.toString(),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            if (indicatorColors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    indicatorColors.take(3).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayWorkItem(
    dayWithDetails: WorkDayWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val day = dayWithDetails.workDay
    val mission = dayWithDetails.mission
    val color = mission.colorHex.toColor()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }

    val durationHours = (day.endTimeMillis - day.startTimeMillis - day.breakMinutes * 60000.0) / 3600000.0
    val cleanDuration = if (durationHours > 0.0) durationHours else 0.0
    val earnings = com.interim.hours.utils.SalaryCalculator.calculateEarnings(day, mission, dayWithDetails.bonuses)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = mission.company,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${timeFormatter.format(Date(day.startTimeMillis))} - ${timeFormatter.format(Date(day.endTimeMillis))} (Pause: ${day.breakMinutes}m)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", modifier = Modifier.size(18.dp), tint = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${String.format("%.1f", cleanDuration)} h travaillées",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (day.comment.isNotEmpty()) {
                        Text(
                            text = "Note: ${day.comment}",
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
                    if (dayWithDetails.bonuses.isNotEmpty()) {
                        Text(
                            text = "Primes: +${String.format("%.2f €", dayWithDetails.bonuses.sumOf { it.amount })}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
