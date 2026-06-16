package com.interim.hours.ui.settings

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.interim.hours.ui.theme.ThemeMode
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notificationHour by viewModel.notificationHour.collectAsState()
    val notificationMinute by viewModel.notificationMinute.collectAsState()

    val timeFormatter = remember { DecimalFormat("00") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackupJson(it) { success ->
                if (success) {
                    android.widget.Toast.makeText(context, "Données exportées avec succès !", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Échec de l'exportation des données", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackupJson(it) { success, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                viewModel.setNotificationTime(hourOfDay, minute)
            },
            notificationHour,
            notificationMinute,
            true
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Notifications & Rappels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rappel quotidien", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Recevoir une notification si vous oubliez de saisir vos heures.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }

                    if (notificationsEnabled) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openTimePicker() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Heure de notification", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${timeFormatter.format(notificationHour)}:${timeFormatter.format(notificationMinute)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Apparence / Thème section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Apparence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Thème de l'application", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val currentTheme by viewModel.appTheme.collectAsState()
                    val themeLabels = mapOf(
                        ThemeMode.SYSTEM to "Système",
                        ThemeMode.LIGHT to "Clair",
                        ThemeMode.DARK to "Sombre"
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = themeLabels[currentTheme] ?: "Système",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ThemeMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(themeLabels[mode] ?: mode.name) },
                                    onClick = {
                                        viewModel.setAppTheme(mode)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Objectifs Mensuels Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Objectif Mensuel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Type d'objectif", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val currentTargetType by viewModel.targetType.collectAsState()
                    val targetValueHours by viewModel.targetValueHours.collectAsState()
                    val targetValueEarnings by viewModel.targetValueEarnings.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilterChip(
                            selected = currentTargetType == "HOURS",
                            onClick = { viewModel.setTargetType("HOURS") },
                            label = { Text("Heures") }
                        )
                        FilterChip(
                            selected = currentTargetType == "EARNINGS",
                            onClick = { viewModel.setTargetType("EARNINGS") },
                            label = { Text("Gains nets (€)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentTargetType == "HOURS") {
                        var hoursText by remember(targetValueHours) { mutableStateOf(targetValueHours.toString()) }
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { newValue ->
                                hoursText = newValue
                                newValue.toFloatOrNull()?.let {
                                    viewModel.setTargetValueHours(it)
                                }
                            },
                            label = { Text("Objectif d'heures (h)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        var earningsText by remember(targetValueEarnings) { mutableStateOf(targetValueEarnings.toString()) }
                        OutlinedTextField(
                            value = earningsText,
                            onValueChange = { newValue ->
                                earningsText = newValue
                                newValue.toFloatOrNull()?.let {
                                    viewModel.setTargetValueEarnings(it)
                                }
                            },
                            label = { Text("Objectif de gains nets (€)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Local Backup Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Sauvegarder ses données", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Exportez ou importez vos données locales au format JSON pour les sauvegarder ou les transférer vers un autre appareil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportLauncher.launch("work_log_backup.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exporter", style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importer", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        "Exportez vos heures et primes au format CSV (compatible Excel).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.exportToCSV { intent ->
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager en CSV")
                    }
                }
            }

            // Privacy policy section
            var showPrivacyDialog by remember { mutableStateOf(false) }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Confidentialité", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Vos données sont stockées à 100% localement sur votre téléphone. L'application ne collecte aucune donnée personnelle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Voir la Politique de Confidentialité")
                    }
                }
            }

            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = { Text("Politique de Confidentialité", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Work Log - Protection de la vie privée\n" +
                                        "Dernière mise à jour : 16 juin 2026\n\n" +
                                        "L'application Work Log fonctionne de manière 100% locale (offline-first).",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "1. Pas de transmission réseau :\n" +
                                        "L'application ne collecte, ne transmet et ne partage aucune donnée personnelle avec des serveurs tiers. Tout reste stocké sur votre téléphone.\n\n" +
                                        "2. Stockage local :\n" +
                                        "Vos heures de travail, vos missions et vos primes sont sauvegardées de façon strictement confidentielle dans la base de données interne de votre appareil.\n\n" +
                                        "3. Sauvegarde Google Auto Backup :\n" +
                                        "Si configurée, vos données locales sont chiffrées et sauvegardées de manière sécurisée sur votre espace personnel Google Drive pour être restaurées en cas de réinstallation. Aucun tiers n'a accès à ces données.\n\n" +
                                        "4. Permissions :\n" +
                                        "- Notifications : Envoyées uniquement pour le rappel journalier de saisie.\n" +
                                        "- Vibreur : Retour haptique de validation.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text("Fermer")
                        }
                    },
                    dismissButton = {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        TextButton(
                            onClick = {
                                uriHandler.openUri("https://github.com/Samuellct/App_android/blob/main/PRIVACY_POLICY.md")
                            }
                        ) {
                            Text("Ouvrir en ligne")
                        }
                    }
                )
            }

            // Info section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("À propos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Work Log V1.0\nDéveloppé pour simplifier le suivi d'activité des intérimaires.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
