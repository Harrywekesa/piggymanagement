package com.phms.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.data.local.entity.PigEntity
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BreedingScreen(viewModel: MainViewModel) {
    val pigs by viewModel.activePigs.collectAsState()
    val activePregnancies by viewModel.activePregnancies.collectAsState()
    val farrowingRecords by viewModel.farrowingRecords.collectAsState()

    val sows = pigs.filter { it.sex == "F" }
    val boars = pigs.filter { it.sex == "M" }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Pregnancies, 1: Herd & Mating, 2: Farrowing Log

    var showMatingDialog by remember { mutableStateOf(false) }
    var showFarrowingDialog by remember { mutableStateOf(false) }
    var showGiltHeatDialog by remember { mutableStateOf(false) }
    var showGiltServiceDialog by remember { mutableStateOf(false) }
    var selectedSowForMating by remember { mutableStateOf<PigEntity?>(null) }
    var selectedSowForFarrowing by remember { mutableStateOf<PigEntity?>(null) }

    val femalePigs = pigs.filter { it.sex == "F" && it.status == "Active" }
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Breeding & Reproduction", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Gestation tracking, AI mating & gilt heat alerts", fontSize = 12.sp, color = Color(0xFF8B949E))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showGiltHeatDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD81B60)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🔥 Record Gilt Heat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showGiltServiceDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🐖 Record Gilt Served", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        selectedSowForMating = sows.firstOrNull()
                        showMatingDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = sows.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sow Mating", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashKpiCard("Active Pregnancies", "${activePregnancies.size}", "Gestating Sows", Icons.Default.ChildCare, Color(0xFFE91E8C), Modifier.weight(1f))
            DashKpiCard("Breeding Sows", "${sows.size}", "Active Females", Icons.Default.Female, Color(0xFF4CAF50), Modifier.weight(1f))
            DashKpiCard("Total Farrowings", "${farrowingRecords.size}", "Historical Litters", Icons.Default.FormatListNumbered, Color(0xFF2196F3), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161B22),
            contentColor = Color(0xFF4CAF50)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active Gestations (${activePregnancies.size})", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Herd (${sows.size} Sow / ${boars.size} Boar)", fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Farrowing Log (${farrowingRecords.size})", fontSize = 12.sp) })
        }
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // ACTIVE PREGNANCIES TAB
                if (activePregnancies.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No active pregnancies logged.", color = Color(0xFF6E7681), fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Click 'Record Mating' above to start tracking gestation for a sow.", color = Color(0xFF484F58), fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(activePregnancies) { preg ->
                            val sow = sows.find { it.id == preg.sow_id }
                            val totalGestationMs = 114L * 24 * 60 * 60 * 1000
                            val elapsedMs = (now - preg.insemination_date).coerceAtLeast(0L)
                            val progress = (elapsedMs.toFloat() / totalGestationMs.toFloat()).coerceIn(0f, 1f)
                            val daysRemaining = ((preg.expected_farrowing_date - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0L)
                            val daysIn = (elapsedMs / (1000 * 60 * 60 * 24)).coerceIn(0L, 114L)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF880E4F)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🐖", fontSize = 20.sp)
                                            }
                                            Column {
                                                Text("Sow Tag #${sow?.tag_number ?: preg.sow_id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("Inseminated: ${dateFormat.format(Date(preg.insemination_date))}", color = Color(0xFF8B949E), fontSize = 12.sp)
                                            }
                                        }
                                        Surface(shape = RoundedCornerShape(8.dp), color = if (daysRemaining <= 5) Color(0xFFD32F2F) else Color(0xFF1B5E20)) {
                                            Text(
                                                if (daysRemaining == 0L) "DUE NOW!" else "$daysRemaining days to farrow",
                                                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Gestation Progress ($daysIn / 114 days)", color = Color(0xFF8B949E), fontSize = 12.sp)
                                        Text("Due: ${dateFormat.format(Date(preg.expected_farrowing_date))}", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFFE91E8C),
                                        trackColor = Color(0xFF21262D),
                                    )

                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = {
                                                selectedSowForFarrowing = sow
                                                showFarrowingDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Record Farrowing", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // HERD & PEDIGREE TAB
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Breeding Females (Sows)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (sows.isEmpty()) {
                        item { Text("No active sows registered in herd.", color = Color(0xFF6E7681), fontSize = 13.sp) }
                    } else {
                        items(sows) { sow ->
                            val offsprings = pigs.filter { it.dam_id == sow.id }
                            val activePreg = activePregnancies.find { it.sow_id == sow.id }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("🐖", fontSize = 20.sp)
                                            Column {
                                                Text("Tag #${sow.tag_number}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(sow.breed, color = Color(0xFF8B949E), fontSize = 12.sp)
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (activePreg != null) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF880E4F)) {
                                                    Text("PREGNANT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    selectedSowForMating = sow
                                                    showMatingDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Mating", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    if (offsprings.isNotEmpty()) {
                                        Text("Offspring Count: ${offsprings.size} pigs", color = Color(0xFF8B949E), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Breeding Males (Boars)", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (boars.isEmpty()) {
                        item { Text("No active boars registered in herd.", color = Color(0xFF6E7681), fontSize = 13.sp) }
                    } else {
                        items(boars) { boar ->
                            val offsprings = pigs.filter { it.sire_id == boar.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🐗", fontSize = 20.sp)
                                        Column {
                                            Text("Boar Tag #${boar.tag_number}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(boar.breed, color = Color(0xFF8B949E), fontSize = 12.sp)
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0D47A1)) {
                                        Text("${offsprings.size} Offspring", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // FARROWING HISTORY LOG
                if (farrowingRecords.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No farrowing records recorded yet.", color = Color(0xFF6E7681), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(farrowingRecords) { rec ->
                            val sow = sows.find { it.id == rec.sow_id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Sow Tag #${sow?.tag_number ?: rec.sow_id}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(dateFormat.format(Date(rec.farrowing_date)), color = Color(0xFF8B949E), fontSize = 12.sp)
                                    }
                                    HorizontalDivider(color = Color(0xFF21262D))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Born Alive: ${rec.born_alive}", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Total Born: ${rec.total_born}", color = Color(0xFFC9D1D9), fontSize = 12.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Stillborn: ${rec.stillborn} | Mummified: ${rec.mummified}", color = Color(0xFFE57373), fontSize = 12.sp)
                                            Text("Avg Weight: ${String.format("%.2f", rec.avg_birth_weight)} kg", color = Color(0xFF64B5F6), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // RECORD MATING DIALOG
    if (showMatingDialog) {
        var selectedSow by remember { mutableStateOf(selectedSowForMating ?: sows.firstOrNull()) }
        var selectedBoar by remember { mutableStateOf(boars.firstOrNull()) }
        var matingType by remember { mutableStateOf("Natural") } // "Natural" or "AI"
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showMatingDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Record Mating / Insemination", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownSelector(
                        label = "Select Sow *",
                        options = sows.map { it.id to "Tag #${it.tag_number} (${it.breed})" },
                        selectedId = selectedSow?.id,
                        onSelect = { id -> selectedSow = sows.find { it.id == id } }
                    )

                    Text("Service Type", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = matingType == "Natural",
                            onClick = { matingType = "Natural" },
                            label = { Text("Natural Service") }
                        )
                        FilterChip(
                            selected = matingType == "AI",
                            onClick = { matingType = "AI" },
                            label = { Text("Artificial Insemination (AI)") }
                        )
                    }

                    if (matingType == "Natural" && boars.isNotEmpty()) {
                        DropdownSelector(
                            label = "Select Boar",
                            options = boars.map { it.id to "Boar #${it.tag_number} (${it.breed})" },
                            selectedId = selectedBoar?.id,
                            onSelect = { id -> selectedBoar = boars.find { it.id == id } }
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Straw Batch No.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sow = selectedSow
                        if (sow != null) {
                            viewModel.recordMating(
                                sowId = sow.id,
                                boarId = if (matingType == "Natural") selectedBoar?.id else null,
                                type = matingType,
                                notes = notes.ifBlank { null }
                            )
                            showMatingDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = selectedSow != null
                ) {
                    Text("Start Gestation Tracking")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMatingDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }

    // RECORD FARROWING DIALOG
    if (showFarrowingDialog) {
        val sow = selectedSowForFarrowing ?: sows.firstOrNull()
        var totalBornStr by remember { mutableStateOf("10") }
        var bornAliveStr by remember { mutableStateOf("9") }
        var stillbornStr by remember { mutableStateOf("1") }
        var mummifiedStr by remember { mutableStateOf("0") }
        var avgWeightStr by remember { mutableStateOf("1.4") }

        AlertDialog(
            onDismissRequest = { showFarrowingDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Record Farrowing — Sow Tag #${sow?.tag_number ?: ""}", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalBornStr, onValueChange = { totalBornStr = it },
                            label = { Text("Total Born") }, modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = bornAliveStr, onValueChange = { bornAliveStr = it },
                            label = { Text("Born Alive") }, modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stillbornStr, onValueChange = { stillbornStr = it },
                            label = { Text("Stillborn") }, modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = mummifiedStr, onValueChange = { mummifiedStr = it },
                            label = { Text("Mummified") }, modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    OutlinedTextField(
                        value = avgWeightStr, onValueChange = { avgWeightStr = it },
                        label = { Text("Avg Birth Weight (kg)") }, modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sowObj = sow
                        if (sowObj != null) {
                            viewModel.recordFarrowing(
                                sowId = sowObj.id,
                                totalBorn = totalBornStr.toIntOrNull() ?: 0,
                                bornAlive = bornAliveStr.toIntOrNull() ?: 0,
                                stillborn = stillbornStr.toIntOrNull() ?: 0,
                                mummified = mummifiedStr.toIntOrNull() ?: 0,
                                avgBirthWeight = avgWeightStr.toDoubleOrNull() ?: 1.2
                            )
                            showFarrowingDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Save Farrowing Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFarrowingDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }

    // RECORD GILT HEAT DIALOG
    if (showGiltHeatDialog) {
        var selectedGilt by remember { mutableStateOf(femalePigs.firstOrNull()) }
        var standingHeat by remember { mutableStateOf(true) }
        var symptoms by remember { mutableStateOf("Standing Reflex, Vulva Swelling") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGiltHeatDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("🔥 Record Gilt Heat Observation", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a female pig/gilt to record heat symptoms. The app will auto-alert you in 21 days for the next heat cycle if not served.", color = Color(0xFF8B949E), fontSize = 12.sp)

                    DropdownSelector(
                        label = "Select Female Pig / Gilt *",
                        options = femalePigs.map { it.id to "Tag #${it.tag_number} (${it.breed})" },
                        selectedId = selectedGilt?.id,
                        onSelect = { id -> selectedGilt = femalePigs.find { it.id == id } }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = standingHeat,
                            onCheckedChange = { standingHeat = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD81B60))
                        )
                        Text("Standing Reflex Observed (Ready for Mating)", color = Color.White, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { symptoms = it },
                        label = { Text("Symptoms / Heat Signs") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Observations") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gilt = selectedGilt
                        if (gilt != null) {
                            viewModel.recordGiltHeat(gilt.id, standingHeat, symptoms, notes.ifBlank { null })
                            showGiltHeatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD81B60)),
                    enabled = selectedGilt != null
                ) {
                    Text("Save Heat & Set 21-Day Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiltHeatDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }

    // RECORD GILT SERVED DIALOG
    if (showGiltServiceDialog) {
        var selectedGilt by remember { mutableStateOf(femalePigs.firstOrNull()) }
        var selectedBoar by remember { mutableStateOf(boars.firstOrNull()) }
        var serviceType by remember { mutableStateOf("Natural Service") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showGiltServiceDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("🐖 Record Gilt Served / Mating Event", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Record mating event for a gilt. Starts 114-day gestation tracking and schedules Day 110 & Day 114 farrowing alerts.", color = Color(0xFF8B949E), fontSize = 12.sp)

                    DropdownSelector(
                        label = "Select Gilt / Female Pig *",
                        options = femalePigs.map { it.id to "Tag #${it.tag_number} (${it.breed})" },
                        selectedId = selectedGilt?.id,
                        onSelect = { id -> selectedGilt = femalePigs.find { it.id == id } }
                    )

                    Text("Service Type", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = serviceType == "Natural Service",
                            onClick = { serviceType = "Natural Service" },
                            label = { Text("Natural") }
                        )
                        FilterChip(
                            selected = serviceType == "Artificial Insemination (AI)",
                            onClick = { serviceType = "Artificial Insemination (AI)" },
                            label = { Text("AI Straw") }
                        )
                    }

                    if (serviceType == "Natural Service" && boars.isNotEmpty()) {
                        DropdownSelector(
                            label = "Select Boar",
                            options = boars.map { it.id to "Boar #${it.tag_number} (${it.breed})" },
                            selectedId = selectedBoar?.id,
                            onSelect = { id -> selectedBoar = boars.find { it.id == id } }
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("AI Straw Batch / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gilt = selectedGilt
                        if (gilt != null) {
                            viewModel.recordGiltService(
                                pigId = gilt.id,
                                boarId = if (serviceType == "Natural Service") selectedBoar?.id else null,
                                serviceType = serviceType,
                                notes = notes.ifBlank { null }
                            )
                            showGiltServiceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = selectedGilt != null
                ) {
                    Text("Record Service & Start 114-Day Gestation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiltServiceDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }
}
