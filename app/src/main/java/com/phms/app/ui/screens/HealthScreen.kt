package com.phms.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.data.local.entity.AlertEntity
import com.phms.app.data.local.entity.HealthEventEntity
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val healthEvents: List<HealthEventEntity> by viewModel.allHealthEvents.collectAsState()
    val pigs by viewModel.activePigs.collectAsState()
    val allPigs by viewModel.allPigs.collectAsState()
    val allAlerts: List<AlertEntity> by viewModel.allAlerts.collectAsState()
    var showGroupHealthDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog form state
    var targetScope by remember { mutableStateOf("Single Pig") }
    var selectedPigId by remember { mutableStateOf<Long?>(null) }
    var targetCategory by remember { mutableStateOf("Piglets") }
    var eventType by remember { mutableStateOf("Vaccination") }
    var product by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var withdrawalDaysStr by remember { mutableStateOf("0") }
    var costStr by remember { mutableStateOf("") }
    var vetName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var repeatIntervalDaysStr by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val now = System.currentTimeMillis()

    // Build upcoming schedule: recurring events + future-dated alerts
    val scheduleItems = remember(healthEvents, allAlerts, now) {
        val items = mutableListOf<ScheduleItem>()

        // 1) Recurring health events — compute next due date
        val scheduleTypes = setOf("Vaccination", "Deworming", "Vitamin")
        healthEvents
            .filter { it.repeat_interval_days > 0 && it.type in scheduleTypes }
            .forEach { ev ->
                val nextDue = ev.date + ev.repeat_interval_days * 86_400_000L
                val pigTag = allPigs.find { p -> p.id == ev.pig_id }?.tag_number ?: "Herd"
                items.add(
                    ScheduleItem(
                        type = ev.type,
                        label = ev.product,
                        pigTag = "Tag #$pigTag",
                        dueDateMs = nextDue,
                        intervalDays = ev.repeat_interval_days,
                        source = "recurring"
                    )
                )
            }

        // 2) Future-dated alerts for vaccination/deworming types (not yet visible in active alerts)
        val alertTypes = setOf("Vaccination", "Deworming", "Vitamin", "Heat Check", "Farrowing")
        allAlerts
            .filter { alert ->
                alert.status == "Active" &&
                alert.type in alertTypes
            }
            .forEach { alert ->
                val pigTag = alert.related_pig_id?.let { id ->
                    allPigs.find { p -> p.id == id }?.tag_number?.let { "Tag #$it" }
                } ?: "Herd"
                items.add(
                    ScheduleItem(
                        type = alert.type,
                        label = alert.message,
                        pigTag = pigTag,
                        dueDateMs = alert.created_date,
                        intervalDays = 0,
                        source = "alert"
                    )
                )
            }

        items.sortedBy { it.dueDateMs }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Health & Veterinary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Medical logs, disease & servicing records", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = {
                    if (selectedPigId == null && pigs.isNotEmpty()) selectedPigId = pigs.first().id
                    showGroupHealthDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.MedicalServices, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log Health Event", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)), border = BorderStroke(1.dp, Color(0xFFFF5252))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Block, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                Column {
                    Text("Withdrawal Period Monitor", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Pigs under drug withdrawal are auto-blocked from sales.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Tab bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color(0xFF4CAF50),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF4CAF50)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Event Log (${healthEvents.size})", fontSize = 12.sp) },
                selectedContentColor = Color(0xFF4CAF50),
                unselectedContentColor = Color(0xFF8B949E)
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Schedule (${scheduleItems.size})", fontSize = 12.sp) },
                selectedContentColor = Color(0xFF4CAF50),
                unselectedContentColor = Color(0xFF8B949E)
            )
        }
        Spacer(Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // --- EVENT LOG TAB ---
                if (healthEvents.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.MedicalServices, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                            Text("No Health Events Logged", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Tap 'Log Health Event' above to record vaccinations, treatments, or vet checkups.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(healthEvents) { event ->
                            HealthEventRow(event, dateFormat)
                        }
                    }
                }
            }

            1 -> {
                // --- SCHEDULE TAB ---
                if (scheduleItems.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                            Text("No Upcoming Schedule", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Log a recurring vaccination or deworming event to see upcoming reminders here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scheduleItems) { item ->
                            ScheduleItemCard(item, dateFormat, now)
                        }
                    }
                }
            }
        }
    }

    if (showGroupHealthDialog) {
        var diseaseName by remember { mutableStateOf("Routine Check / Treatment") }
        var isCustomDisease by remember { mutableStateOf(false) }
        var customDiseaseText by remember { mutableStateOf("") }
        var ageWeeksStr by remember { mutableStateOf("") }
        var weightKgStr by remember { mutableStateOf("") }
        var selectedBoarId by remember { mutableStateOf<Long?>(null) }
        val boars = pigs.filter { it.sex.equals("M", true) }
        val eligiblePigs = remember(eventType, pigs) {
            if (eventType == "Gilt/Sow Serviced") pigs.filter { it.sex.equals("F", true) } else pigs
        }

        // Pre-fill age if single pig selected
        LaunchedEffect(selectedPigId) {
            val selectedPig = pigs.find { it.id == selectedPigId }
            if (selectedPig != null) {
                val ageMs = System.currentTimeMillis() - selectedPig.birth_date
                val weeks = (ageMs / (1000L * 60 * 60 * 24 * 7)).toInt()
                if (ageWeeksStr.isBlank()) ageWeeksStr = maxOf(1, weeks).toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showGroupHealthDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Log Health / Vet Event", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Target Scope Dropdown
                    StringDropdownSelector(
                        label = "Target Scope *",
                        options = listOf("Single Pig", "Category", "Herd"),
                        selectedOption = targetScope,
                        onSelect = { targetScope = it }
                    )

                    if (targetScope == "Single Pig" && eligiblePigs.isNotEmpty()) {
                        Text(if (eventType == "Gilt/Sow Serviced") "Select Female Sow/Gilt *" else "Select Animal *", color = Color(0xFF8B949E), fontSize = 12.sp)
                        DropdownSelector(
                            label = "Select Pig",
                            options = eligiblePigs.map { it.id to "Tag #${it.tag_number} (${it.breed} • ${if (it.sex.equals("F", true)) "Sow/Gilt" else "Boar"})" },
                            selectedId = if (eligiblePigs.any { it.id == selectedPigId }) selectedPigId!! else eligiblePigs.first().id,
                            onSelect = { selectedPigId = it }
                        )
                    } else if (targetScope == "Single Pig" && eventType == "Gilt/Sow Serviced" && eligiblePigs.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Text("No female pigs (Sows / Gilts) in active herd to service.", color = Color(0xFFFF9800), fontSize = 12.sp)
                        }
                    }

                    if (targetScope == "Category") {
                        StringDropdownSelector(
                            label = "Select Pig Category *",
                            options = listOf("Piglets", "Weaners", "Growers", "Finishers", "Sows", "Gilts", "Boars"),
                            selectedOption = targetCategory,
                            onSelect = { targetCategory = it }
                        )
                    }

                    // Event Type Dropdown
                    StringDropdownSelector(
                        label = "Event Type / Purpose *",
                        options = listOf("Treatment", "Vaccination", "Deworming", "Checkup", "Vitamin", "Gilt/Sow Serviced", "Mortality / Death"),
                        selectedOption = eventType,
                        onSelect = { preset ->
                            eventType = preset
                            if (preset == "Gilt/Sow Serviced") {
                                product = "Breeding / Insemination"
                                diseaseName = "Reproduction / Servicing"
                            } else if (preset == "Mortality / Death") {
                                product = "Death / Culling Record"
                                diseaseName = "African Swine Fever"
                            }
                        }
                    )

                    // Disease / Diagnosis Dropdown
                    StringDropdownSelector(
                        label = "Disease / Condition / Diagnosis *",
                        options = listOf(
                            "Routine Check / Treatment",
                            "Diarrhea / Scours",
                            "Pneumonia / Respiratory",
                            "African Swine Fever",
                            "Mange / Skin Parasites",
                            "MMA / Mastitis",
                            "Foot Rot / Lameness",
                            "Reproduction / Servicing",
                            "Other / Custom"
                        ),
                        selectedOption = if (isCustomDisease) "Other / Custom" else diseaseName,
                        onSelect = { selected ->
                            if (selected == "Other / Custom") {
                                isCustomDisease = true
                            } else {
                                isCustomDisease = false
                                diseaseName = selected
                            }
                        }
                    )

                    if (isCustomDisease) {
                        FormField("Custom Disease Name", customDiseaseText, { customDiseaseText = it }, placeholder = "Type disease diagnosis...")
                    }

                    FormField("Product / Medication Description *", product, { product = it }, placeholder = "e.g. Oxytetracycline 20%, Iron injection")

                    if (eventType == "Gilt/Sow Serviced" && boars.isNotEmpty()) {
                        Text("Servicing Boar (Optional for AI)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        DropdownSelector(
                            label = "Select Boar",
                            options = listOf(-1L to "Artificial Insemination (AI)") + boars.map { it.id to "Boar Tag #${it.tag_number}" },
                            selectedId = selectedBoarId ?: -1L,
                            onSelect = { selectedBoarId = if (it == -1L) null else it }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Age Affected (Weeks)", ageWeeksStr, { ageWeeksStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Weight (kg)", weightKgStr, { weightKgStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Dosage", dosage, { dosage = it }, placeholder = "e.g. 2ml/pig") }
                        Box(Modifier.weight(1f)) { FormField("Withdrawal (Days)", withdrawalDaysStr, { withdrawalDaysStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }

                    // Repeat interval — shown only for Deworming, Vaccination, Vitamin
                    if (eventType in listOf("Deworming", "Vaccination", "Vitamin")) {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = Color(0xFF0E2420)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Default.Refresh,
                                        null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Repeat Schedule (optional)", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                FormField(
                                    label = "Repeat Every (Days)",
                                    value = repeatIntervalDaysStr,
                                    onValueChange = { repeatIntervalDaysStr = it.filter { c -> c.isDigit() } },
                                    placeholder = "e.g. 90 for quarterly deworming",
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                                if (repeatIntervalDaysStr.isNotBlank() && (repeatIntervalDaysStr.toIntOrNull() ?: 0) > 0) {
                                    Text(
                                        "🔁 An alert will be created in ${repeatIntervalDaysStr} days reminding you to administer ${eventType.lowercase()} again.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Total Cost (KSh)", costStr, { costStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Vet / Operator", vetName, { vetName = it }, placeholder = "Dr. John / Self") }
                    }

                    FormField("Notes / Clinical Symptoms", notes, { notes = it }, placeholder = "Additional notes...")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalDisease = if (isCustomDisease) customDiseaseText.ifBlank { "Routine Check / Treatment" } else diseaseName
                        val finalProduct = product.ifBlank { "$eventType - $finalDisease" }
                        val effectivePigId = if (targetScope == "Single Pig") {
                            if (eligiblePigs.any { it.id == selectedPigId }) selectedPigId else eligiblePigs.firstOrNull()?.id
                        } else null

                        if (targetScope == "Single Pig" && eligiblePigs.isEmpty()) {
                            Toast.makeText(context, "No pigs available in herd for this event", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (eventType == "Gilt/Sow Serviced" && effectivePigId != null) {
                            viewModel.logGiltServiceFromHealth(
                                sowId = effectivePigId,
                                boarId = selectedBoarId,
                                notes = notes
                            )
                        } else if (eventType == "Mortality / Death" && effectivePigId != null) {
                            viewModel.logMortalityEvent(
                                pigId = effectivePigId,
                                diseaseName = finalDisease,
                                ageWeeks = ageWeeksStr.toIntOrNull(),
                                weightKg = weightKgStr.toDoubleOrNull(),
                                notes = notes,
                                cost = costStr.toDoubleOrNull() ?: 0.0
                            )
                        } else {
                            viewModel.logGroupHealthEvent(
                                targetScope = targetScope,
                                targetPigId = effectivePigId,
                                targetCategory = if (targetScope == "Category") targetCategory else null,
                                type = eventType,
                                product = finalProduct,
                                dosage = dosage.ifBlank { "Standard Dose" },
                                cost = costStr.toDoubleOrNull() ?: 0.0,
                                vetName = vetName.ifBlank { "Farm Operator" },
                                notes = notes,
                                withdrawalDays = withdrawalDaysStr.toIntOrNull() ?: 0,
                                diseaseName = finalDisease,
                                ageWeeks = ageWeeksStr.toIntOrNull(),
                                weightKg = weightKgStr.toDoubleOrNull(),
                                repeatIntervalDays = repeatIntervalDaysStr.toIntOrNull() ?: 0
                            )
                        }
                        Toast.makeText(context, "Health event recorded successfully", Toast.LENGTH_SHORT).show()
                        showGroupHealthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Save Event") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGroupHealthDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

// Data class for schedule tab items
data class ScheduleItem(
    val type: String,
    val label: String,
    val pigTag: String,
    val dueDateMs: Long,
    val intervalDays: Int,
    val source: String // "recurring" or "alert"
)

@Composable
fun ScheduleItemCard(item: ScheduleItem, dateFormat: SimpleDateFormat, now: Long) {
    val daysUntil = ((item.dueDateMs - now) / 86_400_000L).toInt()
    val isPast = daysUntil < 0
    val isUrgent = daysUntil in 0..3
    val isSoon = daysUntil in 4..7

    val borderColor = when {
        isPast -> Color(0xFFFF5252)
        isUrgent -> Color(0xFFFF9800)
        isSoon -> Color(0xFFFFEB3B)
        else -> Color(0xFF4CAF50)
    }
    val bgColor = when {
        isPast -> Color(0xFF1A0A0A)
        isUrgent -> Color(0xFF1A1200)
        isSoon -> Color(0xFF1A1600)
        else -> Color(0xFF0D1F14)
    }
    val statusText = when {
        isPast -> "OVERDUE by ${-daysUntil}d"
        daysUntil == 0 -> "DUE TODAY"
        isUrgent -> "Due in ${daysUntil}d"
        else -> "Due in ${daysUntil}d"
    }
    val statusColor = when {
        isPast -> Color(0xFFFF5252)
        isUrgent -> Color(0xFFFF9800)
        isSoon -> Color(0xFFFFEB3B)
        else -> Color(0xFF4CAF50)
    }

    val typeIcon = when (item.type) {
        "Vaccination" -> Icons.Default.Vaccines
        "Deworming" -> Icons.Default.BugReport
        "Vitamin" -> Icons.Default.LocalPharmacy
        "Heat Check" -> Icons.Default.Favorite
        "Farrowing" -> Icons.Default.ChildFriendly
        else -> Icons.Default.MedicalServices
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(typeIcon, null, tint = borderColor, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(item.type, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(item.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2)
                Text("🐷 ${item.pigTag}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                if (item.intervalDays > 0) {
                    Text("🔁 Every ${item.intervalDays} days", color = Color(0xFF4CAF50), fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(dateFormat.format(Date(item.dueDateMs)), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.18f)) {
                    Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}
