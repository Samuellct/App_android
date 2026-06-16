package com.interim.hours.ui.missions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalContext
import com.interim.hours.data.model.Mission
import com.interim.hours.data.model.MissionBonus
import com.interim.hours.ui.theme.ErrorRed

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray
    }
}

private val MISSION_COLORS = listOf(
    "#6366F1", // Indigo
    "#10B981", // Emerald
    "#3B82F6", // Blue
    "#EC4899", // Pink
    "#F59E0B"  // Amber
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MissionsScreen(
    viewModel: MissionsViewModel,
    modifier: Modifier = Modifier
) {
    val missionsWithBonuses by viewModel.missionsState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedMissionWithBonuses by remember { mutableStateOf<com.interim.hours.data.model.MissionWithBonuses?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    val activeMissions = missionsWithBonuses.filter { it.mission.isActive }
    val archivedMissions = missionsWithBonuses.filter { !it.mission.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Missions d'intérim", fontWeight = FontWeight.Bold) },
                actions = {
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            showArchived = !showArchived
                            android.widget.Toast.makeText(
                                context,
                                if (showArchived) "Missions archivées affichées" else "Missions archivées masquées",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = if (showArchived) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showArchived) "Masquer archives" else "Missions archivées",
                            style = MaterialTheme.typography.labelMedium
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
                onClick = {
                    selectedMissionWithBonuses = null
                    showDialog = true
                },
                icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                text = { Text("Nouvelle Mission") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeMissions.isEmpty() && (!showArchived || archivedMissions.isEmpty())) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.WorkOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Aucune mission active",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "Ajoutez une mission pour commencer à saisir vos heures.",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(activeMissions, key = { it.mission.id }) { item ->
                    MissionCard(
                        missionWithBonuses = item,
                        onEdit = {
                            selectedMissionWithBonuses = item
                            showDialog = true
                        },
                        onToggleActive = { viewModel.toggleMissionActive(item.mission) },
                        onDelete = { viewModel.deleteMission(item.mission) }
                    )
                }

                if (showArchived && archivedMissions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Missions Archivées",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(archivedMissions, key = { it.mission.id }) { item ->
                        MissionCard(
                            missionWithBonuses = item,
                            onEdit = {
                                selectedMissionWithBonuses = item
                                showDialog = true
                            },
                            onToggleActive = { viewModel.toggleMissionActive(item.mission) },
                            onDelete = { viewModel.deleteMission(item.mission) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showDialog) {
        MissionEditDialog(
            missionWithBonuses = selectedMissionWithBonuses,
            onDismiss = { showDialog = false },
            onSave = { mission, bonuses ->
                viewModel.saveMission(mission, bonuses)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionCard(
    missionWithBonuses: com.interim.hours.data.model.MissionWithBonuses,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mission = missionWithBonuses.mission
    val bonuses = missionWithBonuses.bonuses
    val color = mission.colorHex.toColor()
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (mission.isActive) color.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = mission.company,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (mission.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Agence: ${mission.agency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Plus")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (mission.isActive) "Archiver" else "Restaurer") },
                                onClick = {
                                    onToggleActive()
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (mission.isActive) Icons.Default.Archive else Icons.Default.Unarchive,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Supprimer", color = ErrorRed) },
                                onClick = {
                                    onDelete()
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ErrorRed
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Taux horaire", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format("%.2f € / h", mission.hourlyRate),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (mission.siteAddress.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    ) {
                        Text("Lieu de mission", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = mission.siteAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (bonuses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Primes par jour:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bonuses.forEach { bonus ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text("${bonus.name} : ${String.format("%.2f €", bonus.defaultAmount)}") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionEditDialog(
    missionWithBonuses: com.interim.hours.data.model.MissionWithBonuses?,
    onDismiss: () -> Unit,
    onSave: (Mission, List<MissionBonus>) -> Unit
) {
    var company by remember { mutableStateOf(missionWithBonuses?.mission?.company ?: "") }
    var agency by remember { mutableStateOf(missionWithBonuses?.mission?.agency ?: "") }
    var hourlyRate by remember { mutableStateOf(missionWithBonuses?.mission?.hourlyRate?.toString() ?: "") }
    var siteAddress by remember { mutableStateOf(missionWithBonuses?.mission?.siteAddress ?: "") }
    var selectedColor by remember { mutableStateOf(missionWithBonuses?.mission?.colorHex ?: MISSION_COLORS.first()) }

    // Night work and contract compensation parameters
    var nightStartHour by remember { mutableStateOf(missionWithBonuses?.mission?.nightStartHour?.toString() ?: "21") }
    var nightEndHour by remember { mutableStateOf(missionWithBonuses?.mission?.nightEndHour?.toString() ?: "6") }
    var nightRatePercentage by remember { mutableStateOf(missionWithBonuses?.mission?.nightRatePercentage?.toString() ?: "0.0") }
    var hasIfmIccp by remember { mutableStateOf(missionWithBonuses?.mission?.hasIfmIccp ?: true) }
    
    var hasWeeklyOvertime by remember { mutableStateOf(missionWithBonuses?.mission?.hasWeeklyOvertime ?: true) }
    var weeklyOvertimeThreshold by remember { mutableStateOf(missionWithBonuses?.mission?.weeklyOvertimeThreshold?.toString() ?: "35") }
    var overtimeRate1Percentage by remember { mutableStateOf(missionWithBonuses?.mission?.overtimeRate1Percentage?.toString() ?: "25.0") }
    var overtimeRate2Percentage by remember { mutableStateOf(missionWithBonuses?.mission?.overtimeRate2Percentage?.toString() ?: "50.0") }
    
    var showCustomColorPicker by remember { mutableStateOf(false) }

    // Bonuses manager
    val defaultBonuses = remember {
        mutableStateListOf<MissionBonus>().apply {
            missionWithBonuses?.bonuses?.let { addAll(it) }
        }
    }
    var newBonusName by remember { mutableStateOf("") }
    var newBonusAmount by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

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
                        title = { Text(if (missionWithBonuses == null) "Nouvelle Mission" else "Modifier Mission") },
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Entreprise / Client") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = showError && company.isBlank()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = agency,
                            onValueChange = { agency = it },
                            label = { Text("Agence d'intérim") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = showError && agency.isBlank()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = hourlyRate,
                            onValueChange = { hourlyRate = it },
                            label = { Text("Taux horaire brut (€/heure)") },
                            placeholder = { Text("12.31 (SMIC)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = showError && hourlyRate.isNotEmpty() && hourlyRate.toDoubleOrNull() == null
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = siteAddress,
                            onValueChange = { siteAddress = it },
                            label = { Text("Adresse du site (Lieu)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Indemnités fin contrat (IFM/ICCP)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Calcule automatiquement 10% IFM + 10% ICCP (+21% brut).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasIfmIccp,
                                onCheckedChange = { hasIfmIccp = it }
                            )
                        }
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Paramètres heures de nuit", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = nightStartHour,
                                onValueChange = { nightStartHour = it },
                                label = { Text("Début nuit (H)") },
                                placeholder = { Text("21") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = nightEndHour,
                                onValueChange = { nightEndHour = it },
                                label = { Text("Fin nuit (H)") },
                                placeholder = { Text("6") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = nightRatePercentage,
                            onValueChange = { nightRatePercentage = it },
                            label = { Text("Majoration nuit (%)") },
                            placeholder = { Text("20.0") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Heures supplémentaires hebdo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Calcule les majorations hebdos (au-delà de 35h).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasWeeklyOvertime,
                                onCheckedChange = { hasWeeklyOvertime = it }
                            )
                        }
                    }

                    if (hasWeeklyOvertime) {
                        item {
                            OutlinedTextField(
                                value = weeklyOvertimeThreshold,
                                onValueChange = { weeklyOvertimeThreshold = it },
                                label = { Text("Seuil heures sup. (heures/semaine)") },
                                placeholder = { Text("35") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = overtimeRate1Percentage,
                                    onValueChange = { overtimeRate1Percentage = it },
                                    label = { Text("Majoration Taux 1 (%)") },
                                    placeholder = { Text("25.0") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = overtimeRate2Percentage,
                                    onValueChange = { overtimeRate2Percentage = it },
                                    label = { Text("Majoration Taux 2 (%)") },
                                    placeholder = { Text("50.0") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }

                    item {
                        Text("Couleur de la mission", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MISSION_COLORS.forEach { hex ->
                                val color = hex.toColor()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == hex) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                            
                            val isCustomSelected = selectedColor !in MISSION_COLORS
                            val customColorRepresentation = if (isCustomSelected) selectedColor.toColor() else Color.LightGray
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(customColorRepresentation)
                                    .border(
                                        width = if (isCustomSelected) 3.dp else 1.dp,
                                        color = if (isCustomSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { showCustomColorPicker = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Couleur personnalisée",
                                    tint = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Primes quotidiennes par défaut", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ces primes seront automatiquement pré-remplies lors de la saisie de vos journées.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Add prime input
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newBonusName,
                                onValueChange = { newBonusName = it },
                                label = { Text("Nom (ex: Panier)") },
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
                                    val amount = newBonusAmount.toDoubleOrNull()
                                    if (newBonusName.isNotBlank() && amount != null) {
                                        defaultBonuses.add(
                                            MissionBonus(
                                                missionId = missionWithBonuses?.mission?.id ?: 0,
                                                name = newBonusName,
                                                defaultAmount = amount
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

                    // Listed default bonuses
                    items(defaultBonuses.toList()) { bonus ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bonus.name, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(String.format("%.2f €", bonus.defaultAmount), color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { defaultBonuses.remove(bonus) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = ErrorRed)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val rate = if (hourlyRate.isBlank()) 12.31 else hourlyRate.toDoubleOrNull()
                                val nightStart = nightStartHour.toIntOrNull() ?: 21
                                val nightEnd = nightEndHour.toIntOrNull() ?: 6
                                val nightRate = nightRatePercentage.toDoubleOrNull() ?: 0.0
                                val threshold = weeklyOvertimeThreshold.toDoubleOrNull() ?: 35.0
                                val rate1 = overtimeRate1Percentage.toDoubleOrNull() ?: 25.0
                                val rate2 = overtimeRate2Percentage.toDoubleOrNull() ?: 50.0

                                if (company.isNotBlank() && agency.isNotBlank() && rate != null) {
                                    val mission = Mission(
                                        id = missionWithBonuses?.mission?.id ?: 0,
                                        company = company,
                                        agency = agency,
                                        hourlyRate = rate,
                                        siteAddress = siteAddress,
                                        colorHex = selectedColor,
                                        isActive = missionWithBonuses?.mission?.isActive ?: true,
                                        syncId = missionWithBonuses?.mission?.syncId ?: java.util.UUID.randomUUID().toString(),
                                        nightStartHour = nightStart,
                                        nightEndHour = nightEnd,
                                        nightRatePercentage = nightRate,
                                        hasIfmIccp = hasIfmIccp,
                                        hasWeeklyOvertime = hasWeeklyOvertime,
                                        weeklyOvertimeThreshold = threshold,
                                        overtimeRate1Percentage = rate1,
                                        overtimeRate2Percentage = rate2
                                    )
                                    onSave(mission, defaultBonuses.toList())
                                } else {
                                    showError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Enregistrer la mission", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedColor,
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { hex ->
                selectedColor = hex
                showCustomColorPicker = false
            }
        )
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialColor = try {
        android.graphics.Color.parseColor(initialColorHex)
    } catch (e: Exception) {
        android.graphics.Color.GRAY
    }

    var r by remember { mutableStateOf(android.graphics.Color.red(initialColor)) }
    var g by remember { mutableStateOf(android.graphics.Color.green(initialColor)) }
    var b by remember { mutableStateOf(android.graphics.Color.blue(initialColor)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Couleur Personnalisée") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(r, g, b))
                        .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                )

                val hexString = String.format("#%02X%02X%02X", r, g, b)
                Text(hexString, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Column {
                    Text("Rouge ($r)", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = r.toFloat(),
                        onValueChange = { r = it.toInt() },
                        valueRange = 0f..255f
                    )
                }
                Column {
                    Text("Vert ($g)", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = g.toFloat(),
                        onValueChange = { g = it.toInt() },
                        valueRange = 0f..255f
                    )
                }
                Column {
                    Text("Bleu ($b)", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = b.toFloat(),
                        onValueChange = { b = it.toInt() },
                        valueRange = 0f..255f
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalHex = String.format("#%02X%02X%02X", r, g, b)
                    onColorSelected(finalHex)
                }
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
