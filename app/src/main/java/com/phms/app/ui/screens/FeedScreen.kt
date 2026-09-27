package com.phms.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.data.local.entity.*
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val ingredients by viewModel.feedIngredients.collectAsState()
    val formulas by viewModel.formulas.collectAsState()
    val feedingLogs by viewModel.feedingLogs.collectAsState()
    val pigs by viewModel.activePigs.collectAsState()
    val pens by viewModel.pens.collectAsState()
    val stages by viewModel.stages.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inventory, 1: Formulator, 2: Feeding Logs

    var showAddInventoryDialog by remember { mutableStateOf(false) }
    var showLogFeedingDialog by remember { mutableStateOf(false) }

    // Feed Formulator state
    var selectedPreset by remember { mutableStateOf("Grower Mash") }
    var batchWeightKgStr by remember { mutableStateOf("100") }
    var maizePct by remember { mutableStateOf("58") }
    var wheatBranPct by remember { mutableStateOf("20") }
    var soybeanPct by remember { mutableStateOf("14") }
    var fishMealPct by remember { mutableStateOf("6") }
    var premixPct by remember { mutableStateOf("2") }
    var formulaDisplayUnit by remember { mutableStateOf("Both") } // "Both", "kg", "grams"

    fun applyPreset(preset: String) {
        selectedPreset = preset
        when (preset) {
            "Creep Starter" -> { maizePct = "55"; wheatBranPct = "5"; soybeanPct = "25"; fishMealPct = "12"; premixPct = "3" }
            "Weaner Starter" -> { maizePct = "52"; wheatBranPct = "18"; soybeanPct = "20"; fishMealPct = "7"; premixPct = "3" }
            "Grower Mash" -> { maizePct = "58"; wheatBranPct = "20"; soybeanPct = "14"; fishMealPct = "6"; premixPct = "2" }
            "Finisher Meal" -> { maizePct = "62"; wheatBranPct = "22"; soybeanPct = "10"; fishMealPct = "4"; premixPct = "2" }
            "Sow & Weaner" -> { maizePct = "54"; wheatBranPct = "22"; soybeanPct = "16"; fishMealPct = "6"; premixPct = "2" }
        }
    }

    val batchKg = batchWeightKgStr.toDoubleOrNull() ?: 100.0
    val mPct = maizePct.toDoubleOrNull() ?: 0.0
    val wPct = wheatBranPct.toDoubleOrNull() ?: 0.0
    val sPct = soybeanPct.toDoubleOrNull() ?: 0.0
    val fPct = fishMealPct.toDoubleOrNull() ?: 0.0
    val pPct = premixPct.toDoubleOrNull() ?: 0.0
    val totalPct = mPct + wPct + sPct + fPct + pPct
    val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Feed & Nutrition", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = { showLogFeedingDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("🥣 Log Feeding", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161B22),
            contentColor = Color(0xFF4CAF50)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Inventory", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Formulator 🌾", fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Feeding Logs 📋", fontSize = 12.sp) })
        }
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> { // INVENTORY
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${ingredients.count { it.stock_kg <= it.reorder_level }} low stock", color = Color(0xFFFF5252), fontSize = 13.sp)
                    OutlinedButton(
                        onClick = { showAddInventoryDialog = true },
                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Feed Stock / Bag", color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ingredients) { ing ->
                        val isLow = ing.stock_kg <= ing.reorder_level
                        val fillFraction = (ing.stock_kg / (ing.reorder_level * 3)).coerceIn(0.0, 1.0).toFloat()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = if (isLow) BorderStroke(1.dp, Color(0xFFFF5252)) else null
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(ing.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                            if (ing.type == "Commercial Premix") {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1B3A1B)) {
                                                    Text("Shop Premix", color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                                if (ing.stage_category != null) {
                                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1A237E)) {
                                                        Text(ing.stage_category, color = Color(0xFF82B1FF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            if (ing.price_per_bag != null)
                                                "KSh ${ing.price_per_bag?.toInt()}/bag (${ing.bag_size_kg?.toInt()}kg) • KSh ${ing.cost_per_kg}/kg"
                                            else "KSh ${ing.cost_per_kg}/kg",
                                            fontSize = 12.sp, color = Color(0xFF8B949E)
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = if (isLow) Color(0xFF7B1F1F) else Color(0xFF1B5E20)) {
                                        Text(
                                            "${String.format("%.1f", ing.stock_kg)} kg",
                                            color = if (isLow) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { fillFraction },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (isLow) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                    trackColor = Color(0xFF21262D)
                                )
                                if (isLow) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                        Text("LOW STOCK — Reorder at ${ing.reorder_level}kg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> { // FORMULATOR
                var showCustomRatioInputs by remember { mutableStateOf(false) }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Balanced Feed Formulation", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Select a formula preset and target batch weight. Calculations adjust automatically in kg and grams.", color = Color(0xFF8B949E), fontSize = 12.sp)

                                Text("Formula Presets:", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Creep Starter", "Weaner Starter", "Grower Mash", "Finisher Meal", "Sow & Weaner").forEach { preset ->
                                        FilterChip(
                                            selected = selectedPreset == preset,
                                            onClick = { applyPreset(preset) },
                                            label = { Text(preset, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF1B5E20),
                                                selectedLabelColor = Color(0xFF4CAF50),
                                                containerColor = Color(0xFF21262D),
                                                labelColor = Color(0xFF8B949E)
                                            )
                                        )
                                    }
                                }

                                Text("Target Batch Output:", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("50", "100", "200", "500", "1000").forEach { weight ->
                                        FilterChip(
                                            selected = batchWeightKgStr == weight,
                                            onClick = { batchWeightKgStr = weight },
                                            label = { Text("${weight} kg", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF1B5E20),
                                                selectedLabelColor = Color(0xFF4CAF50),
                                                containerColor = Color(0xFF21262D),
                                                labelColor = Color(0xFF8B949E)
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = batchWeightKgStr,
                                    onValueChange = { input -> batchWeightKgStr = input.filter { it.isDigit() || it == '.' } },
                                    label = { Text("Target Batch Output (kg)") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                )

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Customize Ratio Percentages", color = Color(0xFF8B949E), fontSize = 12.sp)
                                    TextButton(onClick = { showCustomRatioInputs = !showCustomRatioInputs }) {
                                        Text(if (showCustomRatioInputs) "Hide Custom Editor ▴" else "Edit Ratios ▾", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (showCustomRatioInputs) {
                                    Text("Ingredient Ratios (%) — Total must equal 100%", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1f)) { FormField("Maize Meal %", maizePct, { maizePct = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                                        Box(Modifier.weight(1f)) { FormField("Wheat Bran %", wheatBranPct, { wheatBranPct = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1f)) { FormField("Soybean %", soybeanPct, { soybeanPct = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                                        Box(Modifier.weight(1f)) { FormField("Fish Meal %", fishMealPct, { fishMealPct = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                                    }
                                    FormField("Premix & Salt %", premixPct, { premixPct = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B281B)),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Ingredient Rations List (${batchKg.toInt()} kg Batch)", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Uneditable calculated measurements (${selectedPreset})", color = Color(0xFF8B949E), fontSize = 11.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Both", "kg", "grams").forEach { unit ->
                                            FilterChip(
                                                selected = formulaDisplayUnit == unit,
                                                onClick = { formulaDisplayUnit = unit },
                                                label = { Text(if (unit == "Both") "kg & g" else unit, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF2E7D32),
                                                    selectedLabelColor = Color.White,
                                                    containerColor = Color(0xFF161B22),
                                                    labelColor = Color(0xFF8B949E)
                                                )
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF2E7D32))

                                UneditableRationListItem("Maize Meal", "Energy source", mPct, mPct / 100.0 * batchKg, formulaDisplayUnit)
                                UneditableRationListItem("Wheat Bran", "Fiber & digestion", wPct, wPct / 100.0 * batchKg, formulaDisplayUnit)
                                UneditableRationListItem("Soybean Meal", "Plant protein", sPct, sPct / 100.0 * batchKg, formulaDisplayUnit)
                                UneditableRationListItem("Fish Meal", "Animal protein & minerals", fPct, fPct / 100.0 * batchKg, formulaDisplayUnit)
                                UneditableRationListItem("Premix & Salt", "Vitamins & minerals", pPct, pPct / 100.0 * batchKg, formulaDisplayUnit)

                                HorizontalDivider(color = Color(0xFF2E7D32))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Total Mix Ratio:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${totalPct.toInt()}% (${batchKg.toInt()} kg total output)", color = if (totalPct == 100.0) Color(0xFF4CAF50) else Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            2 -> { // FEEDING LOGS
                if (feedingLogs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No daily feeding logs yet. Tap 'Log Feeding' to track daily consumption.", color = Color(0xFF6E7681), textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(feedingLogs) { log ->
                            val ing = ingredients.find { it.id == log.ingredient_id }
                            val targetIcon = when (log.target_scope) {
                                "Single Pig" -> "🐖"
                                "Pen" -> "📦"
                                "Category" -> "🐗"
                                else -> "🐷"
                            }
                            val targetTitle = when (log.target_scope) {
                                "Single Pig" -> {
                                    val pig = pigs.find { it.id == log.pig_id }
                                    "Pig #${pig?.tag_number ?: log.pig_id ?: "N/A"}"
                                }
                                "Pen" -> {
                                    val pen = pens.find { it.id == log.pen_id }
                                    "Pen: ${pen?.name ?: "Pen #${log.pen_id}"}"
                                }
                                "Category" -> "Category: ${log.category ?: "Herd"}"
                                else -> "Full Herd (${log.num_pigs} pigs)"
                            }
                            val feedTitle = ing?.name ?: log.feed_type

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                                            Text(targetIcon, fontSize = 20.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(feedTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B3A1B)) {
                                                    Text(log.target_scope, color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text("${log.feeding_time} • $targetTitle • ${log.num_pigs} pig(s)", color = Color(0xFF8B949E), fontSize = 12.sp)
                                            Text(dateFormat.format(Date(log.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${log.quantity_kg} kg", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            val cost = log.quantity_kg * (ing?.cost_per_kg ?: 0.0)
                                            if (cost > 0) {
                                                Text("KSh ${cost.toInt()}", color = Color(0xFF81C784), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                    if (!log.notes.isNullOrBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF21262D)
                                        ) {
                                            Text(
                                                "📝 ${log.notes}",
                                                color = Color(0xFFC9D1D9),
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
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

    // Modal Dialog: Add Inventory / Shop Premix Bag
    if (showAddInventoryDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Raw Ingredient") } // "Raw Ingredient" or "Commercial Premix"
        var numBagsStr by remember { mutableStateOf("2") }
        var bagSizeStr by remember { mutableStateOf("70") }
        var bagPriceStr by remember { mutableStateOf("3200") }
        var rawStockStr by remember { mutableStateOf("100") }
        var rawCostPerKgStr by remember { mutableStateOf("45") }
        var reorderLevelStr by remember { mutableStateOf("50") }
        var stageCategoryExpanded by remember { mutableStateOf(false) }
        var selectedStageCategory by remember { mutableStateOf("Grower") }
        val stageCategories = listOf("Creep/Starter", "Weaner", "Grower", "Finisher", "Sow/Gilt", "All Stages")

        val numBags = numBagsStr.toIntOrNull() ?: 1
        val bagSize = bagSizeStr.toDoubleOrNull() ?: 70.0
        val bagPrice = bagPriceStr.toDoubleOrNull() ?: 3200.0

        val computedStockKg = if (type == "Commercial Premix") numBags * bagSize else (rawStockStr.toDoubleOrNull() ?: 50.0)
        val computedCostPerKg = if (type == "Commercial Premix") (if (bagSize > 0) bagPrice / bagSize else 0.0) else (rawCostPerKgStr.toDoubleOrNull() ?: 45.0)

        AlertDialog(
            onDismissRequest = { showAddInventoryDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Add Feed Inventory / Bag", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Raw Ingredient", "Commercial Premix").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1B5E20), selectedLabelColor = Color(0xFF4CAF50))
                            )
                        }
                    }
                    FormField("Feed / Brand Name *", name, { name = it }, placeholder = if (type == "Commercial Premix") "e.g. Pembe Pig Finisher 70kg Bag" else "e.g. Maize Meal")
                    if (type == "Commercial Premix") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { FormField("Number of Bags", numBagsStr, { numBagsStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                            Box(Modifier.weight(1f)) { FormField("Bag Size (kg)", bagSizeStr, { bagSizeStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        }
                        FormField("Price / Bag (KSh)", bagPriceStr, { bagPriceStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)

                        // Auto-calculated read-only total stock & cost display
                        OutlinedTextField(
                            value = "${String.format("%.1f", computedStockKg)} kg (${numBags} bags × ${bagSize.toInt()}kg)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Total Stock (kg)", color = Color(0xFF81C784), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E7D32), unfocusedBorderColor = Color(0xFF2E7D32),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF162316), unfocusedContainerColor = Color(0xFF162316)
                            )
                        )
                        OutlinedTextField(
                            value = "KSh ${String.format("%.2f", computedCostPerKg)} / kg",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Cost per kg (KSh)", color = Color(0xFF81C784), fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2E7D32), unfocusedBorderColor = Color(0xFF2E7D32),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF162316), unfocusedContainerColor = Color(0xFF162316)
                            )
                        )

                        Text("Pig Stage Category", color = Color(0xFF8B949E), fontSize = 12.sp)
                        ExposedDropdownMenuBox(
                            expanded = stageCategoryExpanded,
                            onExpandedChange = { stageCategoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedStageCategory,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageCategoryExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color(0xFF30363D),
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = stageCategoryExpanded,
                                onDismissRequest = { stageCategoryExpanded = false }
                            ) {
                                stageCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = Color.White) },
                                        onClick = { selectedStageCategory = cat; stageCategoryExpanded = false }
                                    )
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { FormField("Cost per kg (KSh)", rawCostPerKgStr, { rawCostPerKgStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                            Box(Modifier.weight(1f)) { FormField("Total Stock (kg)", rawStockStr, { rawStockStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        }
                    }
                    FormField("Reorder Alert Level (kg)", reorderLevelStr, { reorderLevelStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addFeedIngredient(
                                name = name,
                                stockKg = computedStockKg,
                                costPerKg = computedCostPerKg,
                                reorderLevel = reorderLevelStr.toDoubleOrNull() ?: 20.0,
                                type = type,
                                brandName = if (type == "Commercial Premix") name else null,
                                bagSizeKg = if (type == "Commercial Premix") bagSize else null,
                                pricePerBag = if (type == "Commercial Premix") bagPrice else null,
                                stageCategory = if (type == "Commercial Premix") selectedStageCategory else null
                            )
                            showAddInventoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Add Stock") }
            },
            dismissButton = { OutlinedButton(onClick = { showAddInventoryDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) } }
        )
    }

    // Modal Dialog: Log Daily Feeding & Deduct Stock
    if (showLogFeedingDialog) {
        var targetScope by remember { mutableStateOf("Full Herd") } // "Full Herd", "Category", "Pen", "Single Pig"
        var selectedCategory by remember { mutableStateOf("Growers") }
        var selectedPenId by remember { mutableStateOf<Long?>(pens.firstOrNull()?.id) }
        var selectedPigId by remember { mutableStateOf<Long?>(pigs.firstOrNull()?.id) }

        var feedingTime by remember { mutableStateOf("Morning (07:00 AM)") }
        var feedType by remember { mutableStateOf("Commercial Pellets/Mash") }
        var selectedIngId by remember { mutableStateOf<Long?>(ingredients.firstOrNull()?.id ?: -1L) }

        var amountPerPigStr by remember { mutableStateOf("2.0") }
        var numPigsStr by remember { mutableStateOf("${pigs.count { it.status == "Active" }.coerceAtLeast(1)}") }
        var notes by remember { mutableStateOf("") }

        fun updatePigCountForScope(scope: String, cat: String, pId: Long?, singleId: Long?) {
            when (scope) {
                "Full Herd" -> {
                    val count = pigs.count { it.status == "Active" }
                    numPigsStr = count.coerceAtLeast(1).toString()
                }
                "Category" -> {
                    val count = pigs.count { pig ->
                        pig.status == "Active" && when (cat) {
                            "Piglets" -> pig.current_stage_id == 1L
                            "Weaners" -> pig.current_stage_id == 2L
                            "Growers" -> pig.current_stage_id == 3L
                            "Finishers" -> pig.current_stage_id in listOf(4L, 5L)
                            "Sows" -> pig.sex == "F" && pig.current_stage_id >= 4L
                            "Gilts" -> pig.sex == "F" && pig.current_stage_id in listOf(2L, 3L)
                            "Boars" -> pig.sex == "M" && pig.current_stage_id >= 3L
                            else -> true
                        }
                    }
                    numPigsStr = count.coerceAtLeast(1).toString()
                }
                "Pen" -> {
                    val count = pigs.count { it.pen_id == pId && it.status == "Active" }
                    numPigsStr = count.coerceAtLeast(1).toString()
                }
                "Single Pig" -> {
                    numPigsStr = "1"
                }
            }
        }

        val amountPerPig = amountPerPigStr.toDoubleOrNull() ?: 0.0
        val numPigs = numPigsStr.toIntOrNull() ?: 0
        val totalFeedKg = amountPerPig * numPigs
        val chosenIng = ingredients.find { it.id == selectedIngId }

        AlertDialog(
            onDismissRequest = { showLogFeedingDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("🥣 Log Daily Feeding & Deduct Stock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // STEP 1: Target Scope
                    Text("1. Target Scope (Who is being fed?)", color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Full Herd", "Category", "Pen", "Single Pig").forEach { scope ->
                            FilterChip(
                                selected = targetScope == scope,
                                onClick = {
                                    targetScope = scope
                                    updatePigCountForScope(scope, selectedCategory, selectedPenId, selectedPigId)
                                },
                                label = { Text(scope, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B5E20),
                                    selectedLabelColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }

                    // STEP 2: Target Selection Details
                    when (targetScope) {
                        "Category" -> {
                            val categories = listOf("Piglets", "Weaners", "Growers", "Finishers", "Sows", "Gilts", "Boars")
                            Text("Select Biological Category", color = Color(0xFF8B949E), fontSize = 12.sp)
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = {
                                            selectedCategory = cat
                                            updatePigCountForScope("Category", cat, selectedPenId, selectedPigId)
                                        },
                                        label = { Text(cat, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0077B6),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        "Pen" -> {
                            if (pens.isNotEmpty()) {
                                DropdownSelector(
                                    label = "Select Pen Location",
                                    options = pens.map { pen ->
                                        val pCount = pigs.count { it.pen_id == pen.id && it.status == "Active" }
                                        pen.id to "${pen.name} ($pCount active pigs)"
                                    },
                                    selectedId = selectedPenId ?: pens.first().id,
                                    onSelect = {
                                        selectedPenId = it
                                        updatePigCountForScope("Pen", selectedCategory, it, selectedPigId)
                                    }
                                )
                            } else {
                                Text("No pens registered yet. Go to pens setup to add pens.", color = Color(0xFFFF5252), fontSize = 12.sp)
                            }
                        }
                        "Single Pig" -> {
                            val activePigs = pigs.filter { it.status == "Active" }
                            if (activePigs.isNotEmpty()) {
                                DropdownSelector(
                                    label = "Select Specific Pig",
                                    options = activePigs.map { pig ->
                                        pig.id to "#${pig.tag_number} • ${pig.breed} (${pig.sex})"
                                    },
                                    selectedId = selectedPigId ?: activePigs.first().id,
                                    onSelect = {
                                        selectedPigId = it
                                        updatePigCountForScope("Single Pig", selectedCategory, selectedPenId, it)
                                    }
                                )
                            } else {
                                Text("No active pigs available.", color = Color(0xFFFF5252), fontSize = 12.sp)
                            }
                        }
                        else -> {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF21262D)) {
                                Text(
                                    "Feeding the entire herd (${pigs.count { it.status == "Active" }} active pigs total).",
                                    color = Color(0xFF8B949E),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF21262D))

                    // STEP 3: Feed Selection & Time
                    Text("2. Feed Type & Inventory Deduction", color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    val feedTypeOptions = listOf("Commercial Pellets/Mash", "Farm-Mixed Formula", "Creep Feed", "Green Fodder / Swill", "Raw Ingredient")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        feedTypeOptions.forEach { ft ->
                            FilterChip(
                                selected = feedType == ft,
                                onClick = { feedType = ft },
                                label = { Text(ft, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B3A4B),
                                    selectedLabelColor = Color(0xFF90E0EF)
                                )
                            )
                        }
                    }

                    if (ingredients.isNotEmpty()) {
                        DropdownSelector(
                            label = "Deduct from Stock (Optional)",
                            options = listOf(-1L to "None / Unstocked Feed") + ingredients.map { it.id to "${it.name} (${String.format("%.1f", it.stock_kg)}kg avail)" },
                            selectedId = selectedIngId ?: -1L,
                            onSelect = { selectedIngId = it }
                        )
                    }

                    Text("Feeding Time", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Morning (07:00 AM)", "Afternoon (12:00 PM)", "Evening (05:00 PM)", "Ad-hoc").forEach { ft ->
                            FilterChip(
                                selected = feedingTime == ft,
                                onClick = { feedingTime = ft },
                                label = { Text(ft, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B5E20),
                                    selectedLabelColor = Color(0xFF4CAF50)
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF21262D))

                    // STEP 4: Quantity & Live Calculation
                    Text("3. Quantity & Calculations", color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            FormField("Kg / Pig", amountPerPigStr, { amountPerPigStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        }
                        Box(Modifier.weight(1f)) {
                            FormField(
                                label = if (targetScope == "Single Pig") "Number of Pigs (Fixed)" else "Number of Pigs",
                                value = numPigsStr,
                                onValueChange = { if (targetScope != "Single Pig") numPigsStr = it },
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        }
                    }

                    // Live Total Feed Box
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF1B3A1B)) {
                        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Feed Output:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${String.format("%.1f", totalFeedKg)} kg", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            if (chosenIng != null && chosenIng.cost_per_kg > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Feed Cost:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                    Text("KSh ${(totalFeedKg * chosenIng.cost_per_kg).toInt()} (KSh ${chosenIng.cost_per_kg.toInt()}/kg)", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    FormField(
                        label = "Notes / Clinical Observations (Optional)",
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "e.g. Added vitamin premix, good appetite"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (totalFeedKg > 0) {
                            viewModel.logFeedingAndDeductStock(
                                penId = if (targetScope == "Pen") selectedPenId else null,
                                batchId = null,
                                pigId = if (targetScope == "Single Pig") selectedPigId else null,
                                ingredientId = if (selectedIngId != null && selectedIngId != -1L) selectedIngId else null,
                                feedingTime = feedingTime,
                                feedType = feedType,
                                quantityPerPigKg = amountPerPig,
                                numPigs = numPigs,
                                targetScope = targetScope,
                                category = if (targetScope == "Category") selectedCategory else null,
                                notes = notes.ifBlank { null }
                            )
                            showLogFeedingDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Confirm & Deduct Stock", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogFeedingDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) }
            }
        )
    }
}

@Composable
fun FormulaRow(label: String, kg: String, cost: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFFE6EDF3), fontSize = 13.sp)
        Text("$kg • $cost", color = Color(0xFF81C784), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun UneditableMeasurementBox(
    label: String,
    percentage: Double,
    calculatedKg: Double,
    displayUnit: String
) {
    val grams = calculatedKg * 1000.0
    val valueText = when (displayUnit) {
        "grams" -> "${String.format("%,.0f", grams)} g"
        "kg" -> "${String.format("%.2f", calculatedKg)} kg"
        else -> "${String.format("%.2f", calculatedKg)} kg   |   ${String.format("%,.0f", grams)} g"
    }

    OutlinedTextField(
        value = valueText,
        onValueChange = {},
        readOnly = true,
        label = { Text("$label (${percentage.toInt()}%)", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        trailingIcon = {
            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B5E20)) {
                Text(
                    "🔒 Auto-Calc",
                    color = Color(0xFF81C784),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2E7D32),
            unfocusedBorderColor = Color(0xFF2E7D32),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color(0xFF81C784),
            unfocusedLabelColor = Color(0xFF81C784),
            focusedContainerColor = Color(0xFF162316),
            unfocusedContainerColor = Color(0xFF162316)
        )
    )
}

@Composable
fun UneditableRationListItem(
    name: String,
    category: String,
    percentage: Double,
    calculatedKg: Double,
    displayUnit: String
) {
    val grams = calculatedKg * 1000.0
    val valueText = when (displayUnit) {
        "grams" -> "${String.format("%,.0f", grams)} g"
        "kg" -> "${String.format("%.2f", calculatedKg)} kg"
        else -> "${String.format("%.2f", calculatedKg)} kg (${String.format("%,.0f", grams)} g)"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF162316),
        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B5E20)) {
                        Text("${percentage.toInt()}%", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(category, color = Color(0xFF8B949E), fontSize = 11.sp)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0E170E),
                border = BorderStroke(1.dp, Color(0xFF1B5E20))
            ) {
                Text(
                    valueText,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
