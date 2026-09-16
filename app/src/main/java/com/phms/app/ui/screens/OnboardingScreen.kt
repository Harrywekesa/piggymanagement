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
    var location by remember { mutableStateOf("") }

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
            }

            Spacer(Modifier.height(16.dp))

            when (step) {
                0 -> {
                    // STEP 0: Feature Overview
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF4CAF50).copy(alpha = 0.3f), Color.Transparent)))
                                .border(2.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐖", fontSize = 56.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Welcome to Digital Pig Farm Manager", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "An offline-first application for precision pig husbandry, health tracking, least-cost feed formulation (kg/g), market linkage, and P&L financial analytics.",
                            color = Color(0xFF8B949E), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FormField("Farm Name *", farmName, { farmName = it }, placeholder = "e.g. Wekesa Commercial Pig Farm")
                                FormField("Farmer / Manager Name", farmerName, { farmerName = it }, placeholder = "e.g. Harrison Wekesa")
                                FormField("Location / County", location, { location = it }, placeholder = "e.g. Kitale, Trans Nzoia")
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
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
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
                                farmLocation = location,
                                isOnboarded = true,
                                showTourOnFirstOpen = true
                            )
                        )
                        // Seed selected pens
                        selectedPens.forEach { (penName, capacity) ->
                            viewModel.createPen(name = penName, capacity = capacity, notes = "Configured during farm onboarding")
                        }
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(if (step < 2) "Next →" else "Complete Setup 🎉", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
