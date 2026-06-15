package com.interim.hours.ui.workday

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.interim.hours.data.model.MissionWithBonuses
import com.interim.hours.data.model.WorkDay
import com.interim.hours.data.model.WorkDayBonus
import com.interim.hours.data.model.WorkDayWithDetails
import com.interim.hours.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDayDialog(
    missions: List<MissionWithBonuses>,
    onDismiss: () -> Unit,
    onSave: (WorkDay, List<WorkDayBonus>) -> Unit,
    existingWorkDayWithDetails: WorkDayWithDetails? = null,
    initialDateMillis: Long? = null
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }

    // Init state
    var selectedMissionWithBonuses by remember {
        mutableStateOf(
            existingWorkDayWithDetails?.let { day ->
                missions.find { it.mission.id == day.workDay.missionId }
            } ?: missions.firstOrNull { it.mission.isActive } ?: missions.firstOrNull()
        )
    }

    val calendar = remember {
        Calendar.getInstance().apply {
            existingWorkDayWithDetails?.let {
                timeInMillis = it.workDay.dateMillis
            }
        }
    }

    var dateMillis by remember { mutableStateOf(existingWorkDayWithDetails?.workDay?.dateMillis ?: initialDateMillis ?: calendar.timeInMillis) }

    val startCalendar = remember {
        Calendar.getInstance().apply {
            existingWorkDayWithDetails?.let {
                timeInMillis = it.workDay.startTimeMillis
            } ?: run {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
            }
        }
    }
    var startTimeMillis by remember { mutableStateOf(existingWorkDayWithDetails?.workDay?.startTimeMillis ?: startCalendar.timeInMillis) }

    val endCalendar = remember {
        Calendar.getInstance().apply {
            existingWorkDayWithDetails?.let {
                timeInMillis = it.workDay.endTimeMillis
            } ?: run {
                set(Calendar.HOUR_OF_DAY, 17)
                set(Calendar.MINUTE, 0)
            }
        }
    }
    var endTimeMillis by remember { mutableStateOf(existingWorkDayWithDetails?.workDay?.endTimeMillis ?: endCalendar.timeInMillis) }

    var breakMinutes by remember { mutableStateOf(existingWorkDayWithDetails?.workDay?.breakMinutes ?: 60) }
    var comment by remember { mutableStateOf(existingWorkDayWithDetails?.workDay?.comment ?: "") }

    // Bonuses manager
    val workdayBonuses = remember {
        mutableStateListOf<WorkDayBonus>().apply {
            existingWorkDayWithDetails?.bonuses?.let { addAll(it) } ?: run {
                // If new day, pre-populate default bonuses from the selected mission
                selectedMissionWithBonuses?.bonuses?.forEach { defaultBonus ->
                    add(WorkDayBonus(workDayId = 0, name = defaultBonus.name, amount = defaultBonus.defaultAmount))
                }
            }
        }
    }

    // Auto-update default bonuses when changing selected mission (only if creating a new workday)
    LaunchedEffect(selectedMissionWithBonuses) {
        if (existingWorkDayWithDetails == null) {
            workdayBonuses.clear()
            selectedMissionWithBonuses?.bonuses?.forEach { defaultBonus ->
                workdayBonuses.add(WorkDayBonus(workDayId = 0, name = defaultBonus.name, amount = defaultBonus.defaultAmount))
            }
        }
    }

    var newBonusName by remember { mutableStateOf("") }
    var newBonusAmount by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    // Helper functions to open Pickers
    fun openDatePicker() {
        val dCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dCal.set(Calendar.YEAR, year)
                dCal.set(Calendar.MONTH, month)
                dCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateMillis = dCal.timeInMillis
            },
            dCal.get(Calendar.YEAR),
            dCal.get(Calendar.MONTH),
            dCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePicker(isStart: Boolean) {
        val tCal = Calendar.getInstance().apply { timeInMillis = if (isStart) startTimeMillis else endTimeMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                tCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                tCal.set(Calendar.MINUTE, minute)
                tCal.set(Calendar.SECOND, 0)
                tCal.set(Calendar.MILLISECOND, 0)
                if (isStart) {
                    startTimeMillis = tCal.timeInMillis
                } else {
                    endTimeMillis = tCal.timeInMillis
                }
            },
            tCal.get(Calendar.HOUR_OF_DAY),
            tCal.get(Calendar.MINUTE),
            true
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (existingWorkDayWithDetails == null) "Saisir une Journée" else "Modifier la Journée") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { padding ->
                if (missions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Veuillez créer une mission d'abord !",
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Mission selector
                        item {
                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedMissionWithBonuses?.mission?.company ?: "Sélectionner une mission",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mission") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    missions.forEach { mWithB ->
                                        DropdownMenuItem(
                                            text = { Text("${mWithB.mission.company} (${mWithB.mission.agency})") },
                                            onClick = {
                                                selectedMissionWithBonuses = mWithB
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Date picker
                        item {
                            OutlinedButton(
                                onClick = { openDatePicker() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date de la journée", color = MaterialTheme.colorScheme.onBackground)
                                    Text(
                                        text = dateFormatter.format(Date(dateMillis)),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Times pickers
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { openTimePicker(true) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Début", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                                        Text(
                                            text = timeFormatter.format(Date(startTimeMillis)),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { openTimePicker(false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Fin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
                                        Text(
                                            text = timeFormatter.format(Date(endTimeMillis)),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Break minutes slider & quick choices
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Temps de pause (non payé)", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${breakMinutes} min",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = breakMinutes.toFloat(),
                                    onValueChange = { breakMinutes = it.toInt() },
                                    valueRange = 0f..180f,
                                    steps = 11
                                )
                                // Quick choice chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(0, 15, 30, 45, 60).forEach { mins ->
                                        SuggestionChip(
                                            onClick = { breakMinutes = mins },
                                            label = { Text("${mins}m") },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (breakMinutes == mins) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Primes / Bonuses header
                        item {
                            Divider()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Primes quotidiennes pour cette journée", style = MaterialTheme.typography.titleSmall)
                        }

                        // Add new prime custom
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newBonusName,
                                    onValueChange = { newBonusName = it },
                                    label = { Text("Nom prime") },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = newBonusAmount,
                                    onValueChange = { newBonusAmount = it },
                                    label = { Text("Montant €") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        val amt = newBonusAmount.toDoubleOrNull()
                                        if (newBonusName.isNotBlank() && amt != null) {
                                            workdayBonuses.add(
                                                WorkDayBonus(
                                                    workDayId = existingWorkDayWithDetails?.workDay?.id ?: 0,
                                                    name = newBonusName,
                                                    amount = amt
                                                )
                                            )
                                            newBonusName = ""
                                            newBonusAmount = ""
                                        }
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Ajouter prime")
                                }
                            }
                        }

                        // List of daily bonuses
                        items(workdayBonuses.toList()) { bonus ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bonus.name, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(String.format("%.2f €", bonus.amount), color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { workdayBonuses.remove(bonus) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = ErrorRed)
                                    }
                                }
                            }
                        }

                        // Comments field
                        item {
                            Divider()
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                label = { Text("Commentaire facultatif") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }

                        // Validation error message
                        if (showError && startTimeMillis >= endTimeMillis) {
                            item {
                                Text(
                                    "L'heure de fin doit être après l'heure de début !",
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Save action
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (startTimeMillis < endTimeMillis && selectedMissionWithBonuses != null) {
                                        // Standardize DateMillis to represent only date (ignoring hours/mins/secs) for clean grouping and query support
                                        val dateCalendar = Calendar.getInstance().apply {
                                            timeInMillis = dateMillis
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }

                                        val workDay = WorkDay(
                                            id = existingWorkDayWithDetails?.workDay?.id ?: 0,
                                            missionId = selectedMissionWithBonuses!!.mission.id,
                                            dateMillis = dateCalendar.timeInMillis,
                                            startTimeMillis = startTimeMillis,
                                            endTimeMillis = endTimeMillis,
                                            breakMinutes = breakMinutes,
                                            comment = comment
                                        )
                                        onSave(workDay, workdayBonuses.toList())
                                    } else {
                                        showError = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Enregistrer la journée", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
