package com.phms.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.phms.app.data.repository.FarmSettings
import com.phms.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: MainViewModel, navController: NavController) {
    var step by remember { mutableIntStateOf(0) }

    // Form states for farm setup
    var farmName by remember { mutableStateOf("My Commercial Pig Farm") }
    var farmerName by remember { mutableStateOf("") }
    var selectedCounty by remember { mutableStateOf("Trans Nzoia") }
    var selectedSubCounty by remember { mutableStateOf("Kiminini") }
    var selectedWard by remember { mutableStateOf("Hospital Ward") }

    // Pens state
    val defaultPenPresets = listOf(
        Pair("Farrowing Pen 1", 5),
        Pair("Weaner Barn A", 20),
        Pair("Grower Pen 1", 15),
        Pair("Finishing House 1", 15),
        Pair("Gestation / Sow Barn", 10)
    )
    val selectedPens = remember { mutableStateListOf<Pair<String, Int>>().apply { addAll(defaultPenPresets.take(3)) } }
    var customPenName by remember { mutableStateOf("") }
    var customPenCapacityStr by remember { mutableStateOf("15") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp)
        ) {
            // Header progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FARM SETUP (${step + 1}/3)",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == step) 20.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(if (i == step) Color(0xFF4CAF50) else Color(0xFF30363D))
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            val currentSettings = viewModel.farmSettings.value
                            viewModel.saveFarmSettings(
                                currentSettings.copy(
                                    isOnboarded = true,
                                    showTourOnFirstOpen = false
                                )
                            )
                            defaultPenPresets.take(3).forEach { (penName, capacity) ->
                                viewModel.createPen(name = penName, capacity = capacity, notes = "Default setup")
                            }
                            navController.navigate("dashboard") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Skip", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (step) {
                0 -> {
                    // STEP 0: Farm Mode Selection (Sample Farm vs Real Farm)
                    var isLoadingDemo by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF4CAF50).copy(alpha = 0.25f), Color.Transparent)))
                                .border(2.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(44.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Welcome to Digital Pig Farm", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Choose how you'd like to get started today:",
                                color = Color(0xFF8B949E), fontSize = 13.sp, textAlign = TextAlign.Center
                            )
                        }

                        // Option A: Explore Sample Farm
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isLoadingDemo) {
                                    isLoadingDemo = true
                                    viewModel.loadDemoFarm {
                                        viewModel.saveFarmSettings(
                                            viewModel.farmSettings.value.copy(
                                                farmName = "Demo Model Pig Farm",
                                                farmerName = "Demo Manager",
                                                farmLocation = "Kiminini, Trans Nzoia",
                                                isOnboarded = true,
                                                showTourOnFirstOpen = true
                                            )
                                        )
                                        navController.navigate("dashboard") {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.5.dp, Color(0xFF4CAF50))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                        Text("Explore Sample Farm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Surface(
                                        color = Color(0xFF1B5E20),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Recommended for Testing", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(
                                    "Pre-loaded with 35 realistic pigs across all growth stages, 5 housing pens, breeding events, feed formulations, and financial P&L records.",
                                    color = Color(0xFF8B949E),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        isLoadingDemo = true
                                        viewModel.loadDemoFarm {
                                            viewModel.saveFarmSettings(
                                                viewModel.farmSettings.value.copy(
                                                    farmName = "Demo Model Pig Farm",
                                                    farmerName = "Demo Manager",
                                                    farmLocation = "Kiminini, Trans Nzoia",
                                                    isOnboarded = true,
                                                    showTourOnFirstOpen = true
                                                )
                                            )
                                            navController.navigate("dashboard") {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isLoadingDemo
                                ) {
                                    if (isLoadingDemo) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Loading Sample Herd...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Load Sample Farm & Open App", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Option B: Real Farm Setup
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { step = 1 },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(20.dp))
                                    Text("Start Fresh with My Real Farm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    "Starts with an empty herd (0 pigs). Continue to enter your farm profile name, Kenya county location, and initial housing pens.",
                                    color = Color(0xFF8B949E),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { step = 1 },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, Color(0xFF00B4D8)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Set Up My Farm Profile →", color = Color(0xFF00B4D8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // STEP 1: Farm Profile Setup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Set Up Your Farm Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Enter your farm details so records, invoices, and PDF reports reflect your farm branding.", color = Color(0xFF8B949E), fontSize = 13.sp)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FormField("Farm Name *", farmName, { farmName = it }, placeholder = "e.g. Wekesa Commercial Pig Farm")
                                FormField("Farmer / Manager Name", farmerName, { farmerName = it }, placeholder = "e.g. Harrison Wekesa")
                                HorizontalDivider(color = Color(0xFF21262D))
                                Text("Farm Administrative Location (Kenya 47 Counties)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Select your County, Sub-County, and Ward for location branding, weather, and local market linkage:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                com.phms.app.ui.components.LocationSelector(
                                    selectedCounty = selectedCounty,
                                    selectedSubCounty = selectedSubCounty,
                                    selectedWard = selectedWard,
                                    onLocationChanged = { c, sc, w ->
                                        selectedCounty = c
                                        selectedSubCounty = sc
                                        selectedWard = w
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // STEP 2: Configure Initial Farm Pens
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Configure Initial Farm Pens", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Set up housing pens for organizing your herd by stage (Farrowing, Weaner, Grower, Finisher).", color = Color(0xFF8B949E), fontSize = 13.sp)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Selected Farm Pens (${selectedPens.size})", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                selectedPens.forEachIndexed { idx, pen ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF21262D))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Home, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                            Text(pen.first, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Cap: ${pen.second}", color = Color(0xFF8B949E), fontSize = 12.sp)
                                            IconButton(
                                                onClick = { selectedPens.removeAt(idx) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF21262D))

                                Text("Add Custom Pen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.weight(2f)) {
                                        FormField("Pen Name", customPenName, { customPenName = it }, placeholder = "e.g. Pen 6")
                                    }
                                    Box(Modifier.weight(1f)) {
                                        FormField("Capacity", customPenCapacityStr, { customPenCapacityStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (customPenName.isNotBlank()) {
                                            val cap = customPenCapacityStr.toIntOrNull() ?: 15
                                            selectedPens.add(Pair(customPenName.trim(), cap))
                                            customPenName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Pen", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back", color = Color(0xFF8B949E))
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        // Finish setup & seed initial pens
                        val currentSettings = viewModel.farmSettings.value
                        viewModel.saveFarmSettings(
                            currentSettings.copy(
                                farmName = farmName.ifBlank { "My Commercial Pig Farm" },
                                farmerName = farmerName,
                                farmLocation = listOf(selectedWard, selectedSubCounty, selectedCounty).filter { it.isNotBlank() }.joinToString(", "),
                                isOnboarded = true,
                                showTourOnFirstOpen = true
                            )
                        )
                        // Seed selected pens
                        selectedPens.forEach { (penName, capacity) ->
                            viewModel.createPen(name = penName, capacity = capacity, notes = "Configured during farm onboarding")
                        }
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(if (step < 2) "Next →" else "Complete Setup", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
