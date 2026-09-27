package com.phms.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthScreen(viewModel: MainViewModel) {
    val healthEvents by viewModel.allHealthEvents.collectAsState()
    val pigs by viewModel.activePigs.collectAsState()
    var showGroupHealthDialog by remember { mutableStateOf(false) }

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

    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Health & Veterinary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Medical logs, disease & servicing records", fontSize = 11.sp, color = Color(0xFF8B949E))
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
        Spacer(Modifier.height(12.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)), border = BorderStroke(1.dp, Color(0xFFFF5252))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Block, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                Column {
                    Text("Withdrawal Period Monitor", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Pigs under drug withdrawal are auto-blocked from sales.", color = Color(0xFF8B949E), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Text("Recent Health Events (${healthEvents.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(healthEvents) { event ->
                HealthEventRow(event, dateFormat)
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
            containerColor = Color(0xFF161B22),
            title = { Text("Log Health / Vet Event", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Target Scope Dropdown
                    StringDropdownSelector(
                        label = "Target Scope *",
                        options = listOf("Single Pig", "Category", "Herd"),
                        selectedOption = targetScope,
                        onSelect = { targetScope = it }
                    )

                    val eligiblePigs = remember(eventType, pigs) {
                        if (eventType == "Gilt/Sow Serviced") pigs.filter { it.sex.equals("F", true) } else pigs
                    }
                    if (targetScope == "Single Pig" && eligiblePigs.isNotEmpty()) {
                        Text(if (eventType == "Gilt/Sow Serviced") "Select Female Sow/Gilt *" else "Select Animal *", color = Color(0xFF8B949E), fontSize = 12.sp)
                        DropdownSelector(
                            label = "Select Pig",
                            options = eligiblePigs.map { it.id to "Tag #${it.tag_number} (${it.breed} • ${if (it.sex.equals("F", true)) "Sow/Gilt" else "Boar"})" },
                            selectedId = if (eligiblePigs.any { it.id == selectedPigId }) selectedPigId!! else eligiblePigs.first().id,
                            onSelect = { selectedPigId = it }
                        )
                    } else if (targetScope == "Single Pig" && eventType == "Gilt/Sow Serviced" && eligiblePigs.isEmpty()) {
                        Text("⚠️ No female pigs (Sows / Gilts) in active herd to service.", color = Color(0xFFFF9800), fontSize = 12.sp)
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
                        Text("Servicing Boar (Optional for AI)", color = Color(0xFF8B949E), fontSize = 12.sp)
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
                        val finalDisease = if (isCustomDisease) customDiseaseText.ifBlank { "Unspecified Disease" } else diseaseName
                        if (product.isNotBlank()) {
                            val targetPig = if (targetScope == "Single Pig") selectedPigId else null
                            if (eventType == "Gilt/Sow Serviced" && targetPig != null) {
                                viewModel.logGiltServiceFromHealth(
                                    sowId = targetPig,
                                    boarId = selectedBoarId,
                                    notes = notes
                                )
                            } else if (eventType == "Mortality / Death" && targetPig != null) {
                                viewModel.logMortalityEvent(
                                    pigId = targetPig,
                                    diseaseName = finalDisease,
                                    ageWeeks = ageWeeksStr.toIntOrNull(),
                                    weightKg = weightKgStr.toDoubleOrNull(),
                                    notes = notes,
                                    cost = costStr.toDoubleOrNull() ?: 0.0
                                )
                            } else {
                                viewModel.logGroupHealthEvent(
                                    targetScope = targetScope,
                                    targetPigId = targetPig,
                                    targetCategory = if (targetScope == "Category") targetCategory else null,
                                    type = eventType,
                                    product = product,
                                    dosage = dosage,
                                    cost = costStr.toDoubleOrNull() ?: 0.0,
                                    vetName = vetName,
                                    notes = notes,
                                    withdrawalDays = withdrawalDaysStr.toIntOrNull() ?: 0,
                                    diseaseName = finalDisease,
                                    ageWeeks = ageWeeksStr.toIntOrNull(),
                                    weightKg = weightKgStr.toDoubleOrNull()
                                )
                            }
                            showGroupHealthDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Save Event") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGroupHealthDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) }
            }
        )
    }
}
