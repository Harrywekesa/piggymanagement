package com.phms.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.data.backup.BackupResult
import com.phms.app.ui.components.LocationSelector
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val farmSettings by viewModel.farmSettings.collectAsState()

    var isEditingFarmProfile by remember { mutableStateOf(false) }

    // Editable state
    var farmName by remember(farmSettings.farmName) { mutableStateOf(farmSettings.farmName) }
    var farmerName by remember(farmSettings.farmerName) { mutableStateOf(farmSettings.farmerName) }
    var farmLocation by remember(farmSettings.farmLocation) { mutableStateOf(farmSettings.farmLocation) }
    var farmCounty by remember { mutableStateOf("Mombasa") }
    var farmSubCounty by remember { mutableStateOf("Nyali") }
    var farmWard by remember { mutableStateOf("Frere Town") }
    var currencySymbol by remember(farmSettings.currencySymbol) { mutableStateOf(farmSettings.currencySymbol) }
    var alertVaccination by remember(farmSettings.alertVaccination) { mutableStateOf(farmSettings.alertVaccination) }
    var alertFarrowing by remember(farmSettings.alertFarrowing) { mutableStateOf(farmSettings.alertFarrowing) }
    var alertLowFeed by remember(farmSettings.alertLowFeed) { mutableStateOf(farmSettings.alertLowFeed) }
    var alertPromotion by remember(farmSettings.alertPromotion) { mutableStateOf(farmSettings.alertPromotion) }
    var alertWithdrawal by remember(farmSettings.alertWithdrawal) { mutableStateOf(farmSettings.alertWithdrawal) }

    val backupStatus by viewModel.backupStatus.collectAsState()
    var showRestoreConfirmDialog by remember { mutableStateOf<Uri?>(null) }
    var isProcessingBackup by remember { mutableStateOf(false) }

    LaunchedEffect(backupStatus) {
        backupStatus?.let { status ->
            isProcessingBackup = false
            when (status) {
                is BackupResult.Success -> {
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                }
                is BackupResult.Error -> {
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                }
            }
            viewModel.clearBackupStatus()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            isProcessingBackup = true
            viewModel.exportBackup(context, it)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            showRestoreConfirmDialog = it
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) }

        // Farm Profile (View Mode vs Edit Mode)
        item {
            SectionCard("Farm Profile & Location") {
                if (!isEditingFarmProfile) {
                    // View Mode: Read-only info card
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(farmSettings.farmName.ifBlank { "My Pig Farm" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Owner: ${farmSettings.farmerName.ifBlank { "Farmer" }}", color = Color(0xFF8B949E), fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = { isEditingFarmProfile = true },
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("✏️ Edit Profile", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = Color(0xFF21262D))
                        Text("📍 Location: ${if (farmSettings.farmLocation.isNotBlank()) farmSettings.farmLocation else "$farmWard, $farmSubCounty, $farmCounty"}", color = Color(0xFF81C784), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Currency: ${farmSettings.currencySymbol}", color = Color(0xFF8B949E), fontSize = 12.sp)
                    }
                } else {
                    // Edit Mode: Form inputs with LocationSelector
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormField("Farm Name *", farmName, { farmName = it })
                        FormField("Farmer's Name *", farmerName, { farmerName = it })

                        Text("Farm Administrative Location (Kenya)", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        LocationSelector(
                            selectedCounty = farmCounty,
                            selectedSubCounty = farmSubCounty,
                            selectedWard = farmWard,
                            onLocationChanged = { c, sc, w ->
                                farmCounty = c
                                farmSubCounty = sc
                                farmWard = w
                                farmLocation = "$w, $sc, $c"
                            }
                        )

                        FormField("Custom Location Notes", farmLocation, { farmLocation = it }, placeholder = "e.g. Kitale, Trans Nzoia")
                        FormField("Currency Symbol", currencySymbol, { currencySymbol = it })

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isEditingFarmProfile = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Cancel", color = Color(0xFF8B949E)) }

                            Button(
                                onClick = {
                                    viewModel.saveFarmSettings(
                                        farmSettings.copy(
                                            farmName = farmName,
                                            farmerName = farmerName,
                                            farmLocation = farmLocation.ifBlank { "$farmWard, $farmSubCounty, $farmCounty" },
                                            currencySymbol = currencySymbol
                                        )
                                    )
                                    isEditingFarmProfile = false
                                    Toast.makeText(context, "Farm profile updated!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Save Profile") }
                        }
                    }
                }
            }
        }

        // Alert Preferences
        item {
            SectionCard("Alert Notifications") {
                AlertToggle("Vaccination Due", alertVaccination) {
                    alertVaccination = it
                    viewModel.saveFarmSettings(farmSettings.copy(alertVaccination = it))
                }
                AlertToggle("Farrowing Due", alertFarrowing) {
                    alertFarrowing = it
                    viewModel.saveFarmSettings(farmSettings.copy(alertFarrowing = it))
                }
                AlertToggle("Low Feed Stock", alertLowFeed) {
                    alertLowFeed = it
                    viewModel.saveFarmSettings(farmSettings.copy(alertLowFeed = it))
                }
                AlertToggle("Stage Promotion Ready", alertPromotion) {
                    alertPromotion = it
                    viewModel.saveFarmSettings(farmSettings.copy(alertPromotion = it))
                }
                AlertToggle("Withdrawal Period Active", alertWithdrawal) {
                    alertWithdrawal = it
                    viewModel.saveFarmSettings(farmSettings.copy(alertWithdrawal = it))
                }
            }
        }

        // Data Backup & Restore
        item {
            SectionCard("Data Backup & Restore") {
                Text(
                    "Export your entire farm database (pigs, pens, weight, breeding, feed, and financial records) into a JSON backup file, or restore from a previously saved backup.",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("phms_backup_$timestamp.json")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isProcessingBackup
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isProcessingBackup
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore Backup", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isProcessingBackup) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF4CAF50), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Processing backup...", color = Color(0xFF8B949E), fontSize = 12.sp)
                    }
                }
            }
        }

        // About & Developer Details & Feedback
        item {
            val appVersionName = remember(context) {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }
            }

            SectionCard("About & Developer Info") {
                Text("Pig Health & Management System (PHMS)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("App Version v$appVersionName", color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Lead Developer: Harrison Wekesa", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Contact / WhatsApp: +254 791 496 057", color = Color(0xFF8B949E), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Developed by Harrison Wekesa to empower commercial pig farmers with data-driven herd analytics, feed formulation, and reproduction tracking.", color = Color(0xFFC9D1D9), fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(14.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://wa.me/254791496057")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp: +254 791 496 057", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WhatsApp", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.saveFarmSettings(farmSettings.copy(showTourOnFirstOpen = true))
                            Toast.makeText(context, "Guided Tour re-enabled for next app open!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Replay Tour", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showRestoreConfirmDialog != null) {
        val restoreUri = showRestoreConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
            title = { Text("Restore Farm Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Restoring will replace the current farm records with the data from the selected backup file.\n\nAre you sure you want to proceed?",
                    color = Color(0xFFC9D1D9),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = restoreUri
                        showRestoreConfirmDialog = null
                        isProcessingBackup = true
                        viewModel.importBackup(context, uri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Restore & Replace", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlertToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color(0xFF6E7681),
                uncheckedTrackColor = Color(0xFF21262D)
            )
        )
    }
}
