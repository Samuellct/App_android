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

    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedWorkDayForEdit by remember { mutableStateOf<WorkDayWithDetails?>(null) }

    var showPdfOptionsDialog by remember { mutableStateOf(false) }
    var selectedMonthForPdf by remember { mutableStateOf<String?>(null) }
    var selectedDaysForPdf by remember { mutableStateOf<List<WorkDayWithDetails>>(emptyList()) }
    var pendingFileToSave by remember { mutableStateOf<java.io.File?>(null) }

    val saveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val destUri = result.data?.data
            val sourceFile = pendingFileToSave
            if (destUri != null && sourceFile != null) {
                try {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    android.widget.Toast.makeText(context, "Fichier enregistré avec succès", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Erreur lors de l'enregistrement: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        pendingFileToSave = null
    }

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
                                        selectedMonthForPdf = monthStr
                                        selectedDaysForPdf = daysList
                                        showPdfOptionsDialog = true
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

    if (showPdfOptionsDialog) {
        val monthStr = selectedMonthForPdf ?: ""
        val daysList = selectedDaysForPdf
        AlertDialog(
            onDismissRequest = { showPdfOptionsDialog = false },
            title = { Text("Options d'export PDF", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Relevé d'heures pour $monthStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            showPdfOptionsDialog = false
                            val file = com.interim.hours.utils.PdfExporter.generatePdfFile(context, monthStr, daysList)
                            if (file != null) {
                                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Work Log - Relevé d'heures $monthStr")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(shareIntent, "Partager le relevé PDF")
                                context.startActivity(chooser)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager le relevé PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            showPdfOptionsDialog = false
                            val file = com.interim.hours.utils.PdfExporter.generatePdfFile(context, monthStr, daysList)
                            if (file != null) {
                                pendingFileToSave = file
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_TITLE, "work_log_${monthStr.lowercase().replace(" ", "_")}.pdf")
                                }
                                saveLauncher.launch(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enregistrer dans vos fichiers")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPdfOptionsDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}
