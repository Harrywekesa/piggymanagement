package com.phms.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.phms.app.domain.engine.AutoFeedingCategoryPlan
import com.phms.app.domain.engine.SwineFeedingGuideItem
import com.phms.app.domain.engine.SwineFeedingKey
import com.phms.app.domain.engine.TodayFeedingStatus
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

fun fuzzyMatchIngredient(recipeName: String, invList: List<FeedIngredientEntity>): FeedIngredientEntity? {
    val rLower = recipeName.lowercase().trim()

    // 1. Exact match (case-insensitive)
    invList.find { it.name.trim().equals(rLower, ignoreCase = true) }?.let { return it }

    // 2. High-priority agricultural synonyms / aliases mapping
    val keywords: List<String> = when {
        rLower.contains("maize") || rLower.contains("corn") -> listOf("maize", "corn")
        rLower.contains("wheat") || rLower.contains("pollard") || rLower.contains("bran") -> listOf("pollard", "wheat", "bran")
        rLower.contains("soy") || rLower.contains("soya") -> listOf("soya", "soybean", "soy")
        rLower.contains("sunflower") || rLower.contains("cotton") || rLower.contains("cake") -> listOf("sunflower", "cotton", "cake", "seedcake")
        rLower.contains("fish") || rLower.contains("omena") || rLower.contains("dagaa") -> listOf("omena", "fishmeal", "fish", "dagaa")
        rLower.contains("oil") || rLower.contains("fat") -> listOf("oil", "fat")
        rLower.contains("lime") || rLower.contains("calcium") || rLower.contains("limestone") -> listOf("lime", "limestone", "calcium")
        rLower.contains("dcp") || rLower.contains("bone") || rLower.contains("phosphorus") -> listOf("dcp", "bone")
        rLower.contains("premix") || rLower.contains("vitamin") || rLower.contains("mineral") -> listOf("premix", "mineral", "vitamin")
        rLower.contains("salt") -> listOf("salt")
        else -> emptyList()
    }

    if (keywords.isNotEmpty()) {
        invList.find { inv ->
            val invLower = inv.name.lowercase()
            keywords.any { kw -> invLower.contains(kw) }
        }?.let { return it }
    }

    // 3. Fallback: mutual words >= 3 chars
    val tokens = rLower.replace("/", " ").replace("-", " ").replace("(", " ").replace(")", " ")
        .split(" ")
        .map { it.trim() }
        .filter { it.length >= 3 }

    return invList.find { inv ->
        val invLower = inv.name.lowercase()
        tokens.any { t -> invLower.contains(t) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val ingredients by viewModel.feedIngredients.collectAsState()
    val formulas by viewModel.formulas.collectAsState()
    val feedingLogs by viewModel.feedingLogs.collectAsState()
    val pigs by viewModel.activePigs.collectAsState()
    val pens by viewModel.pens.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val todayFeedingStatus by viewModel.todayFeedingStatus.collectAsState()
    val farmSettings by viewModel.farmSettings.collectAsState()
    val mixedBatches by viewModel.mixedBatches.collectAsState()
    val availableMixedBatches by viewModel.availableMixedBatches.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inventory, 1: Formulator, 2: Feeding Logs

    var showAddInventoryDialog by remember { mutableStateOf(false) }
    var showLogFeedingDialog by remember { mutableStateOf(false) }
    var selectedIngredientForAction by remember { mutableStateOf<com.phms.app.data.local.entity.FeedIngredientEntity?>(null) }
    var showIngredientActionDialog by remember { mutableStateOf(false) }
    var showMixBatchDialog by remember { mutableStateOf(false) }
    var showRegisterBatchDialog by remember { mutableStateOf(false) }
    var showResetFeedDialog by remember { mutableStateOf(false) }
    var showCustomMixDialog by remember { mutableStateOf(false) }

    // Feed Formulator state
    var selectedPreset by remember { mutableStateOf("Weaner/Starter") }
    var batchWeightKgStr by remember { mutableStateOf("1000") }
    var formulaDisplayUnit by remember { mutableStateOf("kg") } // "Both", "kg", "grams"

    // Recipe data class: name, description, kg per 1000 kg base
    data class Ingredient(val name: String, val role: String, val kgPerTonne: Double)
    data class FeedRecipe(val name: String, val ingredients: List<Ingredient>)

    val recipes = listOf(
        FeedRecipe("Weaner/Starter", listOf(
            Ingredient("Whole Ground Maize",   "Energy source",             600.0),
            Ingredient("Whole Ground Wheat",   "Energy & fiber",            100.0),
            Ingredient("Soybean Meal",          "Plant protein",             180.0),
            Ingredient("Sunflower Cake",        "Protein & energy",          50.0),
            Ingredient("Fishmeal/Omena",        "Animal protein & minerals",  30.0),
            Ingredient("Vegetable Oil",         "Energy (fat)",               15.0),
            Ingredient("Feed Lime/Limestone",   "Calcium source",              8.0),
            Ingredient("DCP (Bone Meal)",       "Phosphorus & calcium",        7.0),
            Ingredient("2% Pig Feed Premix",   "Vitamins & minerals",         20.0),
            Ingredient("Stock/Animal Salt",     "Electrolytes",                5.0)
        )),
        FeedRecipe("Grower", listOf(
            Ingredient("Whole Ground Maize",   "Energy source",             670.0),
            Ingredient("Whole Ground Wheat",   "Energy & fiber",            100.0),
            Ingredient("Soybean Meal",          "Plant protein",             120.0),
            Ingredient("Sunflower Cake",        "Protein & energy",          70.0),
            Ingredient("Fishmeal/Omena",        "Animal protein & minerals",  10.0),
            Ingredient("Vegetable Oil",         "Energy (fat)",                2.0),
            Ingredient("Feed Lime/Limestone",   "Calcium source",              3.0),
            Ingredient("DCP (Bone Meal)",       "Phosphorus & calcium",        2.0),
            Ingredient("2% Pig Feed Premix",   "Vitamins & minerals",         20.0),
            Ingredient("Stock/Animal Salt",     "Electrolytes",                1.0)
        )),
        FeedRecipe("Finisher", listOf(
            Ingredient("Whole Ground Maize",   "Energy source",             710.0),
            Ingredient("Whole Ground Wheat",   "Energy & fiber",            100.0),
            Ingredient("Soybean Meal",          "Plant protein",              80.0),
            Ingredient("Sunflower Cake",        "Protein & energy",          70.0),
            Ingredient("Fishmeal/Omena",        "Animal protein & minerals",  10.0),
            Ingredient("Vegetable Oil",         "Energy (fat)",                2.0),
            Ingredient("Feed Lime/Limestone",   "Calcium source",              3.0),
            Ingredient("DCP (Bone Meal)",       "Phosphorus & calcium",        2.0),
            Ingredient("2% Pig Feed Premix",   "Vitamins & minerals",         20.0),
            Ingredient("Stock/Animal Salt",     "Electrolytes",                1.0)
        ))
    )

    val currentRecipe = recipes.find { it.name == selectedPreset } ?: recipes[0]
    val batchKg = batchWeightKgStr.toDoubleOrNull() ?: 1000.0
    val baseTotal = currentRecipe.ingredients.sumOf { it.kgPerTonne }
    val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    val dayKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val yesterdayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 86400000L)) }
    val headerDateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

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
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log Feeding", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161B22),
            contentColor = Color(0xFF4CAF50)
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Inventory", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Formulator", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Feeding Logs", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) })
        }
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> { // INVENTORY
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasMixedReady = availableMixedBatches.any { it.remaining_kg > 0 }
                    if (hasMixedReady) {
                        Text("${String.format(Locale.US, "%.0f", availableMixedBatches.sumOf { it.remaining_kg })} kg mixed feed ready", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("${ingredients.count { it.stock_kg <= it.reorder_level }} low stock", color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showAddInventoryDialog = true },
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Stock", color = Color(0xFF4CAF50), fontSize = 11.sp)
                        }
                        Button(
                            onClick = { showCustomMixDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Mix", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showRegisterBatchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3A4B)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Layers, null, tint = Color(0xFF90E0EF), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("+ Batch", color = Color(0xFF90E0EF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { showResetFeedDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Feed", tint = Color(0xFFFF7043), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // All-ingredients-mixed banner
                    if (mixedBatches.any { it.remaining_kg > 0 } && ingredients.isNotEmpty() && ingredients.all { it.stock_kg <= 0.0 }) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1A237E).copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, Color(0xFF3F51B5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("🥣", fontSize = 20.sp)
                                    Column {
                                        Text("All ingredients mixed into batch", fontWeight = FontWeight.Bold, color = Color(0xFF90CAF9), fontSize = 13.sp)
                                        Text("Your ingredient stocks are at 0 — they've been combined into ${mixedBatches.count { it.remaining_kg > 0 }} batch(es) below. Use Log Feeding > Mix Batch to feed from them.", color = Color(0xFFBBDEFB), fontSize = 11.sp, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                    items(ingredients, key = { it.id }) { ing ->
                        val isLow = ing.stock_kg <= ing.reorder_level
                        val fillFraction = if (ing.reorder_level > 0) (ing.stock_kg / (ing.reorder_level * 2.5)).coerceIn(0.0, 1.0).toFloat() else 1f
                        val isZero = ing.stock_kg <= 0.01

                        val icon = when {
                            ing.name.contains("maize", ignoreCase = true) || ing.name.contains("corn", ignoreCase = true) -> Icons.Default.Grain
                            ing.name.contains("wheat", ignoreCase = true) || ing.name.contains("bran", ignoreCase = true) -> Icons.Default.Grass
                            ing.name.contains("soy", ignoreCase = true) -> Icons.Default.Eco
                            ing.name.contains("fish", ignoreCase = true) || ing.name.contains("omena", ignoreCase = true) -> Icons.Default.Water
                            ing.name.contains("lime", ignoreCase = true) || ing.name.contains("calcium", ignoreCase = true) -> Icons.Default.Science
                            ing.name.contains("oil", ignoreCase = true) -> Icons.Default.Opacity
                            else -> Icons.Default.Inventory2
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIngredientForAction = ing
                                    showIngredientActionDialog = true
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.dp, if (isLow && !isZero) Color(0xFFFF9800) else MaterialTheme.colorScheme.outline)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Avatar Icon
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isZero -> Color(0xFF1B3A1B).copy(alpha = 0.5f)
                                                    isLow -> Color(0xFF3E2723)
                                                    else -> Color(0xFF1B3A1B)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = when {
                                                isZero -> Color(0xFF81C784).copy(alpha = 0.6f)
                                                isLow -> Color(0xFFFFB74D)
                                                else -> Color(0xFF4CAF50)
                                            },
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Name & Price details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                ing.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Color.White
                                            )
                                            if (ing.type == "Commercial Premix") {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B3A1B)) {
                                                    Text(
                                                        "Premix",
                                                        color = Color(0xFF81C784),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            if (ing.price_per_bag != null && ing.bag_size_kg != null)
                                                "KSh ${String.format(Locale.US, "%.1f", ing.cost_per_kg)}/kg • KSh ${ing.price_per_bag?.toInt()}/${ing.bag_size_kg?.toInt()}kg bag"
                                            else
                                                "KSh ${String.format(Locale.US, "%.1f", ing.cost_per_kg)}/kg" + if (ing.reorder_level > 0) " • Alert: ${ing.reorder_level.toInt()}kg" else "",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8B949E)
                                        )
                                    }

                                    // Stock number & compact status badge
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "${String.format(Locale.US, "%.1f", ing.stock_kg)} kg",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = when {
                                                isZero -> Color(0xFF8B949E)
                                                isLow -> Color(0xFFFFB74D)
                                                else -> Color(0xFF4CAF50)
                                            }
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when {
                                                isZero -> Color(0xFF21262D)
                                                isLow -> Color(0xFF3E2723)
                                                else -> Color(0xFF1B3A1B)
                                            }
                                        ) {
                                            Text(
                                                when {
                                                    isZero -> "0 kg (Mixed)"
                                                    isLow -> "Low Stock"
                                                    else -> "In Stock"
                                                },
                                                color = when {
                                                    isZero -> Color(0xFF8B949E)
                                                    isLow -> Color(0xFFFFB74D)
                                                    else -> Color(0xFF81C784)
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            )
                                        }
                                    }
                                }

                                // Slim 2dp progress bar integrated at bottom edge of card
                                if (!isZero) {
                                    LinearProgressIndicator(
                                        progress = { fillFraction },
                                        modifier = Modifier.fillMaxWidth().height(2.dp),
                                        color = if (isLow) Color(0xFFFF9800) else Color(0xFF2E7D32),
                                        trackColor = Color(0xFF21262D)
                                    )
                                }
                            }
                        }
                    }

                    // ── MIXED BATCHES SECTION ───────────────────────────────────────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Mixed Batches Ready to Feed", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (mixedBatches.isNotEmpty()) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1B5E20)) {
                                        Text("${mixedBatches.count { it.remaining_kg > 0 }} available", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            TextButton(
                                onClick = { showRegisterBatchDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Register Batch", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (mixedBatches.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D24)),
                                border = BorderStroke(1.dp, Color(0xFF1E3A4B))
                            ) {
                                Row(
                                    Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("No mixed batches yet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF90E0EF))
                                        Text("Mixed ingredients yesterday? Register your combined feed batch here.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = { showRegisterBatchDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("+ Register", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(mixedBatches, key = { it.id }) { batch ->
                            val pct = if (batch.total_kg > 0) (batch.remaining_kg / batch.total_kg).toFloat().coerceIn(0f, 1f) else 0f
                            val isEmpty = batch.remaining_kg <= 0.01
                            val sdf = remember { java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault()) }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Avatar Icon
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isEmpty) Color(0xFF1B3A1B).copy(alpha = 0.5f) else Color(0xFF1B3A1B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = if (isEmpty) Color(0xFF81C784).copy(alpha = 0.6f) else Color(0xFF4CAF50),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        // Batch Name & Cost Details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    batch.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = Color.White
                                                )
                                                if (!batch.is_complete_formula) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3E2723)) {
                                                        Text("Partial", color = Color(0xFFFFB74D), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(
                                                "KSh ${String.format(Locale.US, "%.1f", batch.cost_per_kg)}/kg • ${sdf.format(java.util.Date(batch.created_date))}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF8B949E)
                                            )
                                        }

                                        // Remaining stock & delete
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    "${String.format(Locale.US, "%.1f", batch.remaining_kg)} kg",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isEmpty) Color(0xFF8B949E) else Color(0xFF4CAF50)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isEmpty) Color(0xFF21262D) else Color(0xFF1B3A4B)
                                                ) {
                                                    Text(
                                                        if (isEmpty) "Depleted" else "Ready to Feed",
                                                        color = if (isEmpty) Color(0xFF8B949E) else Color(0xFF90E0EF),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteMixedBatch(batch.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF8B949E), modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }

                                    // Slim 2dp progress bar
                                    if (!isEmpty) {
                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier.fillMaxWidth().height(2.dp),
                                            color = Color(0xFF2E7D32),
                                            trackColor = Color(0xFF21262D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> { // FORMULATOR
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.dp, Color(0xFF30363D))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Farm Feed Formulator", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Select a recipe and enter your target batch size. Ingredient quantities scale automatically.", color = Color(0xFF8B949E), fontSize = 12.sp)

                                Text("Recipe:", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    recipes.forEach { recipe ->
                                        FilterChip(
                                            selected = selectedPreset == recipe.name,
                                            onClick = { selectedPreset = recipe.name },
                                            label = { Text(recipe.name, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF1B5E20),
                                                selectedLabelColor = Color(0xFF4CAF50),
                                                containerColor = Color(0xFF21262D),
                                                labelColor = Color(0xFF8B949E)
                                            )
                                        )
                                    }
                                }

                                Text("Target Batch Size:", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("100", "250", "500", "1000").forEach { weight ->
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
                                    label = { Text("Custom Batch Size (kg)") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedLabelColor = Color(0xFF8B949E), focusedLabelColor = Color(0xFF4CAF50))
                                )
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
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${selectedPreset} — ${batchKg.toInt()} kg Batch", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Scaled from 1-tonne base formula", color = Color(0xFF8B949E), fontSize = 11.sp)
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Display Units:", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("kg", "grams", "Both").forEach { unit ->
                                                FilterChip(
                                                    selected = formulaDisplayUnit == unit,
                                                    onClick = { formulaDisplayUnit = unit },
                                                    label = { Text(if (unit == "Both") "kg & g" else unit, fontSize = 11.sp) },
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
                                }
                                HorizontalDivider(color = Color(0xFF2E7D32))

                                currentRecipe.ingredients.forEachIndexed { idx, ingredient ->
                                    val scaledKg = (ingredient.kgPerTonne / baseTotal) * batchKg
                                    val pct = (ingredient.kgPerTonne / baseTotal) * 100.0
                                    UneditableRationListItem(ingredient.name, ingredient.role, pct, scaledKg, formulaDisplayUnit)
                                }

                                HorizontalDivider(color = Color(0xFF2E7D32))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Total Output:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${batchKg.toInt()} kg (${currentRecipe.ingredients.size} ingredients)", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                // Base formula note
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0E2A1A)) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("ℹ️", fontSize = 14.sp)
                                        Text("Base recipe: ${currentRecipe.ingredients.sumOf { it.kgPerTonne }.toInt()} kg per tonne. All quantities scaled proportionally to your batch size.", color = Color(0xFF8B949E), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    // ── FEED COST CALCULATOR ─────────────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A1E00)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text("Batch Cost Estimate", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("${batchKg.toInt()} kg of $selectedPreset", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                    }

                                    val matchedCount = currentRecipe.ingredients.count { fuzzyMatchIngredient(it.name, ingredients) != null }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (matchedCount == currentRecipe.ingredients.size) Color(0xFF1B5E20) else Color(0xFF3E2723)
                                    ) {
                                        Text(
                                            "$matchedCount/${currentRecipe.ingredients.size} priced",
                                            color = if (matchedCount == currentRecipe.ingredients.size) Color(0xFF81C784) else Color(0xFFFFCC80),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text("Calculated automatically using current prices from your inventory ingredients.", color = Color(0xFF8B949E), fontSize = 11.sp)
                                HorizontalDivider(color = Color(0xFF30363D))

                                var totalCost = 0.0
                                var totalPricedKg = 0.0

                                currentRecipe.ingredients.forEach { recipeIng ->
                                    val scaledKg = (recipeIng.kgPerTonne / baseTotal) * batchKg
                                    val matchedIng = fuzzyMatchIngredient(recipeIng.name, ingredients)
                                    val pricePerKg = matchedIng?.cost_per_kg ?: 0.0
                                    val lineTotal = scaledKg * pricePerKg
                                    totalCost += lineTotal
                                    if (pricePerKg > 0) totalPricedKg += scaledKg

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(recipeIng.name, color = Color(0xFFC9D1D9), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            if (matchedIng != null) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("✓ matched: ${matchedIng.name}", color = Color(0xFF81C784), fontSize = 10.sp)
                                                    Text("• KSh ${String.format(Locale.US, "%.1f", pricePerKg)}/kg", color = Color(0xFF8B949E), fontSize = 10.sp)
                                                    if (matchedIng.stock_kg > 0) {
                                                        Text("• ${String.format(Locale.US, "%.0f", matchedIng.stock_kg)}kg stock", color = Color(0xFF64B5F6), fontSize = 10.sp)
                                                    }
                                                }
                                            } else {
                                                Text("Optional / unstocked additive", color = Color(0xFF8B949E), fontSize = 10.sp)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                if (pricePerKg > 0) "KSh ${String.format(Locale.US, "%,.0f", lineTotal)}" else "KSh 0",
                                                color = if (pricePerKg > 0) Color(0xFFFFD54F) else Color(0xFF6E7681),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text("${String.format(Locale.US, "%.1f", scaledKg)} kg", color = Color(0xFF8B949E), fontSize = 10.sp)
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF30363D))

                                // Totals summary
                                val costPerKg = if (totalPricedKg > 0) totalCost / totalPricedKg else (if (batchKg > 0) totalCost / batchKg else 0.0)
                                val costPer50kgBag = costPerKg * 50.0

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Total Estimated Cost:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("KSh ${String.format(Locale.US, "%,.0f", totalCost)}", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Estimated Cost per kg:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                        Text(if (costPerKg > 0) "KSh ${String.format(Locale.US, "%.2f", costPerKg)}/kg" else "—", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("50 kg Bag Equivalent:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                        Text(if (costPer50kgBag > 0) "KSh ${String.format(Locale.US, "%,.0f", costPer50kgBag)} / 50kg bag" else "—", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (totalCost == 0.0) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E1500), modifier = Modifier.fillMaxWidth()) {
                                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                                            Text("Set ingredient prices in the Inventory tab to see live cost estimates.", color = Color(0xFFFFCC80), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── MIX THIS BATCH BUTTON CARD ───────────────────────────────────────
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    Text("Mix This Batch Now", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Text(
                                    "Combine your ${selectedPreset} ingredients into a ready-to-feed mixed batch of ${batchKg.toInt()} kg. Each ingredient's stock will be deducted proportionally.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                                )
                                Button(
                                    onClick = { showMixBatchDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mix ${batchKg.toInt()} kg of ${selectedPreset}", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showRegisterBatchDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90CAF9)),
                                    border = BorderStroke(1.dp, Color(0xFF1E88E5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Already Mixed? Register Pre-Mixed Batch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }


            2 -> { // FEEDING LOGS
                val context = LocalContext.current
                val groupedFeedingLogs = remember(feedingLogs) {
                    feedingLogs.groupBy { dayKeyFormat.format(Date(it.date)) }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Auto Daily Feeding Card ──────────────────────────────────────
                    item {
                        val status = todayFeedingStatus
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF238636))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Auto Daily Feeding",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Enabled/disabled badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (farmSettings.autoFeedingEnabled) Color(0xFF1B5E20) else Color(0xFF3D2B2B)
                                    ) {
                                        Text(
                                            if (farmSettings.autoFeedingEnabled) "AUTO ON" else "AUTO OFF",
                                            color = if (farmSettings.autoFeedingEnabled) Color(0xFF81C784) else Color(0xFFEF9A9A),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))

                                if (status == null) {
                                    Text(
                                        "Loading feeding status...",
                                        color = Color(0xFF8B949E),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    // Today's summary row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Already fed badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (status.isLoggedToday) Color(0xFF1B5E20) else Color(0xFF1B3A4B),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    if (status.isLoggedToday) "✓ Fed Today" else "Not Fed Yet",
                                                    color = if (status.isLoggedToday) Color(0xFF81C784) else Color(0xFF56CCF2),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (status.totalKgLoggedToday > 0.0) {
                                                    Text(
                                                        "${String.format("%.1f", status.totalKgLoggedToday)} kg total",
                                                        color = Color(0xFFC9D1D9),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        // Pigs count badge
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF21262D),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    "${status.totalPigsFedToday} pigs",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text("in ${status.plans.size} categories", color = Color(0xFF8B949E), fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    // Per-category breakdown
                                    if (status.plans.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "Today's Feeding Plan",
                                            color = Color(0xFF8B949E),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        status.plans.forEach { plan ->
                                            val ingName = plan.matchedIngredient?.name
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Category dot
                                                Box(
                                                    Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF4CAF50))
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        plan.categoryName,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        "${plan.pigCount} pigs × ${String.format("%.2f", plan.rationPerPigKg)} kg",
                                                        color = Color(0xFF8B949E),
                                                        fontSize = 10.sp
                                                    )
                                                    if (ingName != null) {
                                                        Text(
                                                            "Feed: $ingName",
                                                            color = Color(0xFF56CCF2),
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "${String.format("%.1f", plan.totalFeedKg)} kg",
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Divider(color = Color(0xFF21262D), thickness = 0.5.dp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Feed All Now button
                                Button(
                                    onClick = {
                                        viewModel.triggerManualDailyFeeding()
                                        Toast.makeText(context, "Daily feeding logged & stock deducted!", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (todayFeedingStatus?.isLoggedToday == true) Color(0xFF21262D) else Color(0xFF238636)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (todayFeedingStatus?.isLoggedToday == true) "Re-Feed (Override)" else "Feed All Now (1 Tap)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                // Log Past Days button
                                var showBacklogPicker by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { showBacklogPicker = !showBacklogPicker },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (showBacklogPicker) "Hide Past Days" else "Log a Past Day (Backlog)",
                                        color = Color(0xFF8B949E),
                                        fontSize = 12.sp
                                    )
                                }

                                if (showBacklogPicker) {
                                    Spacer(Modifier.height(8.dp))
                                    val backlogDateFormat = remember { java.text.SimpleDateFormat("EEE, d MMM yyyy", java.util.Locale.getDefault()) }
                                    val today = remember {
                                        java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }
                                    val pastDays = remember {
                                        (1..30).map { daysAgo ->
                                            today - daysAgo * 24 * 60 * 60 * 1000L
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Select a missed day to back-log:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                        pastDays.forEach { dayMs ->
                                            // Check if this day already has logs in feedingLogs
                                            val dayEnd = dayMs + 24 * 60 * 60 * 1000L
                                            val alreadyLogged = feedingLogs.any { log ->
                                                log.date >= dayMs && log.date < dayEnd && log.feeding_time == "Daily Ration"
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (alreadyLogged) Color(0xFF1B3A1B) else Color(0xFF21262D))
                                                    .clickable {
                                                        viewModel.triggerBacklogFeeding(dayMs)
                                                        Toast.makeText(
                                                            context,
                                                            "${backlogDateFormat.format(java.util.Date(dayMs))} feeding logged!",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    backlogDateFormat.format(java.util.Date(dayMs)),
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (alreadyLogged) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B5E20)) {
                                                        Text("✓ Logged", color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                    }
                                                } else {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Feeding Log History ──────────────────────────────────────────
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Feeding History (${feedingLogs.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (feedingLogs.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showResetFeedDialog = true },
                                        border = BorderStroke(1.dp, Color(0xFF8C1D1D)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear Logs", color = Color(0xFFFF5252), fontSize = 11.sp)
                                    }
                                }
                                Button(
                                    onClick = { showLogFeedingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Log Feeding", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (feedingLogs.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                                Text("No daily feeding logs yet. Tap 'Log Feeding' to track daily consumption.", color = Color(0xFF6E7681), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        groupedFeedingLogs.forEach { (dayKey, dayLogs) ->
                            val firstDate = dayLogs.firstOrNull()?.date ?: System.currentTimeMillis()
                            val dateLabel = when (dayKey) {
                                todayKey -> "Today • ${headerDateFormat.format(Date(firstDate))}"
                                yesterdayKey -> "Yesterday • ${headerDateFormat.format(Date(firstDate))}"
                                else -> headerDateFormat.format(Date(firstDate))
                            }
                            val totalDayKg = dayLogs.sumOf { it.quantity_kg }
                            val totalDayCost = dayLogs.sumOf { log ->
                                val mb = mixedBatches.find { it.id == log.mixed_batch_id }
                                val ing = ingredients.find { it.id == log.ingredient_id }
                                val unitCost = mb?.cost_per_kg ?: ing?.cost_per_kg ?: 0.0
                                log.quantity_kg * unitCost
                            }

                            item(key = "day_header_$dayKey") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF161B22),
                                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(13.dp))
                                            Text(dateLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B5E20)) {
                                                val kgStr = if (totalDayKg % 1.0 == 0.0) "${totalDayKg.toInt()} kg" else String.format(Locale.US, "%.1f kg", totalDayKg)
                                                Text(kgStr, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                            if (totalDayCost > 0) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E2415)) {
                                                    Text("KSh ${totalDayCost.toInt()}", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(dayLogs, key = { it.id }) { log ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.deleteFeedingLog(log.id)
                                            true
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF8C1D1D)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(modifier = Modifier.padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                ) {
                                    val mixedBatch = mixedBatches.find { it.id == log.mixed_batch_id }
                                    val ing = ingredients.find { it.id == log.ingredient_id }
                                    val targetIcon: androidx.compose.ui.graphics.vector.ImageVector = when (log.target_scope) {
                                        "Single Pig" -> Icons.Default.Pets
                                        "Pen" -> Icons.Default.Home
                                        "Category" -> Icons.Default.Groups
                                        else -> Icons.Default.Pets
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
                                    val feedTitle = mixedBatch?.name ?: ing?.name ?: log.feed_type

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp,
                                            if (log.feeding_time == "Daily Ration") Color(0xFF1B3A1B) else Color(0xFF30363D))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                                                    Icon(targetIcon, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            feedTitle,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        if (mixedBatch != null || log.mixed_batch_id != null) {
                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B3A4B)) {
                                                                Text("Mixed Batch", color = Color(0xFF90E0EF), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                            }
                                                        } else if (ing != null) {
                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E2415)) {
                                                                Text("Raw Stock", color = Color(0xFFFFB74D), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                            }
                                                        }
                                                        if (log.feeding_time == "Daily Ration") {
                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0D2818)) {
                                                                Text("AUTO", color = Color(0xFF4CAF50), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                            }
                                                        }
                                                    }
                                                    Text("${log.feeding_time} • $targetTitle • ${log.num_pigs} pig(s)", color = Color(0xFF8B949E), fontSize = 12.sp)
                                                    Text(timeFormat.format(Date(log.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Column(horizontalAlignment = Alignment.End) {
                                                    val qtyFormatted = if (log.quantity_kg % 1.0 == 0.0) {
                                                        "${log.quantity_kg.toInt()} kg"
                                                    } else {
                                                        String.format(Locale.US, "%.1f kg", log.quantity_kg)
                                                    }
                                                    Text(qtyFormatted, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    val unitCost = mixedBatch?.cost_per_kg ?: ing?.cost_per_kg ?: 0.0
                                                    val cost = log.quantity_kg * unitCost
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
                                                        log.notes ?: "",
                                                        color = Color(0xFFC9D1D9),
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } // end SwipeToDismissBox
                            }
                        }
                    }
                }
            }
        }
    }

// -- INGREDIENT ACTION DIALOG (Restock / Update Price / Edit & Set Stock / Delete) --
    if (showIngredientActionDialog && selectedIngredientForAction != null) {
        val ing = selectedIngredientForAction!!
        var actionMode by remember { mutableStateOf("") } // "" | "restock" | "price" | "edit" | "delete"
        var addKgStr by remember { mutableStateOf("") }
        var totalCostStr by remember { mutableStateOf("") }
        var newCostPerKgStr by remember { mutableStateOf(ing.cost_per_kg.toString()) }
        var newPricePerBagStr by remember { mutableStateOf(ing.price_per_bag?.toString() ?: "") }

        // States for Edit Details & Direct Stock Setting
        var editNameStr by remember(ing) { mutableStateOf(ing.name) }
        var editStockKgStr by remember(ing) { mutableStateOf(String.format(Locale.US, "%.1f", ing.stock_kg)) }
        var editCostPerKgStr by remember(ing) { mutableStateOf(ing.cost_per_kg.toString()) }
        var editReorderLevelStr by remember(ing) { mutableStateOf(ing.reorder_level.toString()) }

        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showIngredientActionDialog = false; actionMode = "" },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column {
                    Text(
                        when (actionMode) {
                            "edit" -> "Edit Ingredient & Set Stock"
                            "delete" -> "Delete Ingredient"
                            "restock" -> "Restock ${ing.name}"
                            "price" -> "Update Price: ${ing.name}"
                            else -> ing.name
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (actionMode.isEmpty()) {
                        Text(
                            if (ing.price_per_bag != null) "KSh ${ing.price_per_bag?.toInt()}/bag (${ing.bag_size_kg?.toInt()}kg) • Current Stock: ${String.format(Locale.US, "%.1f", ing.stock_kg)} kg"
                            else "KSh ${ing.cost_per_kg}/kg • Current Stock: ${String.format(Locale.US, "%.1f", ing.stock_kg)} kg",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (actionMode.isEmpty()) {
                        Text("Choose an action for this ingredient:", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)

                        // 1. Restock (+kg)
                        Button(
                            onClick = { actionMode = "restock" },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add More Stock (+kg)", fontWeight = FontWeight.Bold)
                        }

                        // 2. Edit Details & Set Stock (Direct Override)
                        OutlinedButton(
                            onClick = { actionMode = "edit" },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF64B5F6)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Details & Set Exact Stock", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                        }

                        // 3. Update Price
                        OutlinedButton(
                            onClick = { actionMode = "price" },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Update Price Only", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }

                        // 4. Delete Ingredient
                        OutlinedButton(
                            onClick = { actionMode = "delete" },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFE57373)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Ingredient", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                        }
                    } else if (actionMode == "restock") {
                        Text("Add Stock (+kg to inventory)", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = addKgStr,
                            onValueChange = { addKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Quantity to Add (kg)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        val addKg = addKgStr.toDoubleOrNull() ?: 0.0
                        val autoCost = addKg * ing.cost_per_kg
                        OutlinedTextField(
                            value = totalCostStr,
                            onValueChange = { totalCostStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = {
                                Text(
                                    if (autoCost > 0) "Total Cost (KSh) — defaults to KSh ${autoCost.toInt()}"
                                    else "Total Cost (KSh) — optional"
                                )
                            },
                            placeholder = { if (autoCost > 0) Text("${autoCost.toInt()}") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (addKg > 0) {
                            Text("New stock will be: ${String.format(Locale.US, "%.1f", ing.stock_kg + addKg)} kg", color = Color(0xFF81C784), fontSize = 12.sp)
                        }
                    } else if (actionMode == "price") {
                        Text("Update Price", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = newCostPerKgStr,
                            onValueChange = { newCostPerKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Price per kg (KSh)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (ing.price_per_bag != null) {
                            OutlinedTextField(
                                value = newPricePerBagStr,
                                onValueChange = { newPricePerBagStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Price per Bag (KSh)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    } else if (actionMode == "edit") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0D47A1).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, Color(0xFF1E88E5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Directly set the exact stock in kilograms or rename the ingredient. You can use this to correct deductions or set exact amounts.",
                                fontSize = 11.sp,
                                color = Color(0xFF90CAF9),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        OutlinedTextField(
                            value = editNameStr,
                            onValueChange = { editNameStr = it },
                            label = { Text("Ingredient Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF64B5F6),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = editStockKgStr,
                            onValueChange = { editStockKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Current Stock (kg)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF64B5F6),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editCostPerKgStr,
                                onValueChange = { editCostPerKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Cost/kg (KSh)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF64B5F6),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            OutlinedTextField(
                                value = editReorderLevelStr,
                                onValueChange = { editReorderLevelStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Alert Level (kg)") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF64B5F6),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    } else if (actionMode == "delete") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFB71C1C).copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Color(0xFFE53935)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚠️ Confirm Deletion", fontWeight = FontWeight.Bold, color = Color(0xFFFF8A80), fontSize = 13.sp)
                                Text("Are you sure you want to delete ${ing.name} from inventory? This cannot be undone.", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (actionMode.isNotEmpty()) {
                    Button(
                        onClick = {
                            when (actionMode) {
                                "restock" -> {
                                    val addKg = addKgStr.toDoubleOrNull() ?: 0.0
                                    if (addKg > 0) {
                                        val enteredCost = totalCostStr.toDoubleOrNull()
                                        val finalCost = if (enteredCost != null && enteredCost > 0.0) enteredCost else (addKg * ing.cost_per_kg)
                                        viewModel.restockIngredient(ing.id, addKg, finalCost)
                                        Toast.makeText(context, "+${addKg}kg added to ${ing.name}", Toast.LENGTH_SHORT).show()
                                        showIngredientActionDialog = false
                                        actionMode = ""
                                    }
                                }
                                "price" -> {
                                    val newCpk = newCostPerKgStr.toDoubleOrNull()
                                    if (newCpk != null && newCpk > 0) {
                                        viewModel.updateIngredientPrice(ing.id, newCpk, newPricePerBagStr.toDoubleOrNull())
                                        Toast.makeText(context, "${ing.name} price updated!", Toast.LENGTH_SHORT).show()
                                        showIngredientActionDialog = false
                                        actionMode = ""
                                    }
                                }
                                "edit" -> {
                                    val newName = editNameStr.trim()
                                    val newStock = editStockKgStr.toDoubleOrNull() ?: ing.stock_kg
                                    val newCost = editCostPerKgStr.toDoubleOrNull() ?: ing.cost_per_kg
                                    val newReorder = editReorderLevelStr.toDoubleOrNull() ?: ing.reorder_level
                                    if (newName.isNotBlank()) {
                                        viewModel.updateFeedIngredient(
                                            ing.copy(
                                                name = newName,
                                                stock_kg = newStock,
                                                cost_per_kg = newCost,
                                                reorder_level = newReorder
                                            )
                                        )
                                        Toast.makeText(context, "$newName updated! Stock set to ${newStock} kg", Toast.LENGTH_SHORT).show()
                                        showIngredientActionDialog = false
                                        actionMode = ""
                                    }
                                }
                                "delete" -> {
                                    viewModel.deleteFeedIngredient(ing.id)
                                    Toast.makeText(context, "${ing.name} removed from inventory", Toast.LENGTH_SHORT).show()
                                    showIngredientActionDialog = false
                                    actionMode = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (actionMode) {
                                "delete" -> Color(0xFFD32F2F)
                                "edit" -> Color(0xFF1976D2)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    ) {
                        Text(
                            when (actionMode) {
                                "restock" -> "Add Stock"
                                "price" -> "Save Price"
                                "edit" -> "Save Changes & Stock"
                                "delete" -> "Confirm Delete"
                                else -> "Save"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (actionMode.isEmpty()) { showIngredientActionDialog = false }
                        else { actionMode = "" }
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) { Text(if (actionMode.isEmpty()) "Close" else "Back", color = Color(0xFF8B949E)) }
            }
        )
    }

    // ── REGISTER PRE-MIXED BATCH DIALOG ──────────────────────────────────────
    if (showRegisterBatchDialog) {
        val formulaOptions = listOf("Grower", "Weaner/Starter", "Finisher", "Custom Mix")
        var selectedFormula by remember { mutableStateOf("Grower") }
        var batchNameStr by remember { mutableStateOf("Grower Farm Mix") }
        var totalKgStr by remember { mutableStateOf("100.0") }
        var costPerKgStr by remember { mutableStateOf("50.0") }
        var notesStr by remember { mutableStateOf("") }
        val context = LocalContext.current

        val totalKg = totalKgStr.toDoubleOrNull() ?: 0.0
        val costPerKg = costPerKgStr.toDoubleOrNull() ?: 0.0
        val totalCost = totalKg * costPerKg

        AlertDialog(
            onDismissRequest = { showRegisterBatchDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Layers, null, tint = Color(0xFF90CAF9))
                    Text("Register Pre-Mixed Batch", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Record a feed batch that was already mixed physically (e.g. mixed yesterday). This creates a ready-to-feed batch without deducting raw ingredients.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Recipe category chips
                    Text("Formula / Feed Stage", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        formulaOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedFormula == opt,
                                onClick = {
                                    selectedFormula = opt
                                    batchNameStr = "$opt Farm Mix"
                                },
                                label = { Text(opt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B3A4B),
                                    selectedLabelColor = Color(0xFF90E0EF)
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = batchNameStr,
                        onValueChange = { batchNameStr = it },
                        label = { Text("Batch Name / Label") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = totalKgStr,
                            onValueChange = { totalKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Total Weight (kg)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        OutlinedTextField(
                            value = costPerKgStr,
                            onValueChange = { costPerKgStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Cost / kg (KSh)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1B281B),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Batch Cost:", color = Color(0xFF81C784), fontSize = 12.sp)
                            Text("KSh ${String.format(Locale.US, "%,.0f", totalCost)}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 14.sp)
                        }
                    }

                    OutlinedTextField(
                        value = notesStr,
                        onValueChange = { notesStr = it },
                        label = { Text("Notes (optional, e.g. mixed by hand)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (batchNameStr.isNotBlank() && totalKg > 0) {
                            viewModel.registerPreMixedBatch(
                                name = batchNameStr.trim(),
                                formulaName = selectedFormula,
                                totalKg = totalKg,
                                costPerKg = costPerKg,
                                notes = notesStr.ifBlank { null }
                            ) {
                                Toast.makeText(context, "Registered $batchNameStr (${totalKg.toInt()} kg) ready to feed!", Toast.LENGTH_SHORT).show()
                            }
                            showRegisterBatchDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    enabled = batchNameStr.isNotBlank() && totalKg > 0
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Register Batch", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRegisterBatchDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

// ── Mix Batch Dialog ─────────────────────────────────────────────────────
    if (showMixBatchDialog) {
        // Build mix items from the current recipe + inventory using fuzzy matching
        val recipeIngItems = currentRecipe.ingredients.map { recipeIng ->
            val scaledKg = (recipeIng.kgPerTonne / baseTotal) * batchKg
            val matched = fuzzyMatchIngredient(recipeIng.name, ingredients)
            com.phms.app.data.repository.PHMSRepository.IngredientMixItem(
                ingredientId = matched?.id,
                name = recipeIng.name,
                kgNeeded = scaledKg,
                costPerKg = matched?.cost_per_kg ?: 0.0,
                include = matched != null
            )
        }
        // User-toggled inclusion state
        val includedState = remember(selectedPreset, batchKg) {
            recipeIngItems.map { it.ingredientId != null }.toMutableStateList()
        }

        AlertDialog(
            onDismissRequest = { showMixBatchDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Mix ${batchKg.toInt()} kg of ${selectedPreset}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Review each ingredient. Uncheck any you don't have on hand. Included items will be deducted from stock.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    recipeIngItems.forEachIndexed { idx, item ->
                        val matchedIng = fuzzyMatchIngredient(item.name, ingredients)
                        val stockKg = matchedIng?.stock_kg ?: 0.0
                        val inInventory = matchedIng != null
                        val hasEnough = inInventory && stockKg >= item.kgNeeded
                        val chipBg = when {
                            !inInventory -> Color(0xFF21262D)
                            !hasEnough -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val chipText = when {
                            !inInventory -> "Optional / Unstocked"
                            !hasEnough -> "${String.format("%.1f", stockKg)} kg (need ${String.format("%.1f", item.kgNeeded)} kg)"
                            else -> "${String.format("%.1f", stockKg)} kg avail"
                        }
                        val chipColor = when {
                            !inInventory -> Color(0xFF8B949E)
                            !hasEnough -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> Color(0xFF4CAF50)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (includedState[idx]) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (hasEnough && includedState[idx]) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = includedState[idx],
                                    onCheckedChange = { v -> includedState[idx] = v },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), uncheckedColor = MaterialTheme.colorScheme.outlineVariant)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Need: ${String.format("%.2f", item.kgNeeded)} kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = chipBg) {
                                    Text(chipText, color = chipColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val includedItems = recipeIngItems.filterIndexed { i, _ -> includedState.getOrElse(i) { false } }
                    val skippedItems = recipeIngItems.filterIndexed { i, _ -> !includedState.getOrElse(i) { false } }
                    val actualKgMix = includedItems.sumOf { it.kgNeeded }
                    val totalCostMix = includedItems.sumOf { it.kgNeeded * it.costPerKg }
                    val costPerKgMix = if (actualKgMix > 0) totalCostMix / actualKgMix else 0.0
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Batch Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            Text("Actual yield: ${String.format("%.1f", actualKgMix)} kg (${includedItems.size}/${recipeIngItems.size} ingredients)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text("Est. cost: KSh ${totalCostMix.toInt()} at KSh ${String.format("%.1f", costPerKgMix)}/kg", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            if (skippedItems.isNotEmpty()) {
                                Text("Skipped: ${skippedItems.map { it.name }.joinToString(", ")}", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val includedForConfirm = recipeIngItems.filterIndexed { i, _ -> includedState.getOrElse(i) { false } }
                Button(
                    onClick = {
                        if (includedForConfirm.isNotEmpty()) {
                            val finalItems = recipeIngItems.mapIndexed { i, item -> item.copy(include = includedState.getOrElse(i) { false }) }
                            viewModel.mixBatch(
                                formulaName = selectedPreset,
                                totalBatchKg = batchKg,
                                items = finalItems
                            )
                            showMixBatchDialog = false
                        }
                    },
                    enabled = recipeIngItems.indices.any { i -> includedState.getOrElse(i) { false } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Confirm & Mix Batch", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMixBatchDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    
    if (showCustomMixDialog) {
        var customBatchName by remember { mutableStateOf("Farm Formulated Mix") }
        val inputKgMap = remember(ingredients) {
            mutableStateMapOf<Long, String>().apply {
                ingredients.forEach { this[it.id] = if (it.stock_kg > 0) String.format(Locale.US, "%.1f", it.stock_kg) else "0.0" }
            }
        }

        val parsedItems = ingredients.map { ing ->
            val kg = inputKgMap[ing.id]?.toDoubleOrNull() ?: 0.0
            val cost = kg * ing.cost_per_kg
            Triple(ing, kg, cost)
        }

        val totalKgMix = parsedItems.sumOf { it.second }
        val totalCostMix = parsedItems.sumOf { it.third }
        val costPerKgMix = if (totalKgMix > 0) totalCostMix / totalKgMix else 0.0

        AlertDialog(
            onDismissRequest = { showCustomMixDialog = false },
            containerColor = Color(0xFF161B22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF4CAF50))
                    Text("Mix Custom Batch from Inventory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Enter the kg to mix from each of your in-stock ingredients. Stock will be deducted and a new mixed batch created.",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = customBatchName,
                        onValueChange = { customBatchName = it },
                        label = { Text("Batch Name / Label", color = Color(0xFF8B949E)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF30363D)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Inventory Ingredients (${ingredients.size})", color = Color(0xFFC9D1D9), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        TextButton(
                            onClick = {
                                ingredients.forEach { ing ->
                                    inputKgMap[ing.id] = String.format(Locale.US, "%.1f", ing.stock_kg)
                                }
                            }
                        ) {
                            Text("Mix All Available Stock", color = Color(0xFF58A6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF30363D))

                    ingredients.forEach { ing ->
                        val curKgStr = inputKgMap[ing.id] ?: "0.0"
                        val kgVal = curKgStr.toDoubleOrNull() ?: 0.0
                        val lineCost = kgVal * ing.cost_per_kg

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0D1117),
                            border = BorderStroke(1.dp, if (kgVal > 0) Color(0xFF238636) else Color(0xFF30363D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ing.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "${String.format(Locale.US, "%.1f", ing.stock_kg)} kg avail • KSh ${String.format(Locale.US, "%.1f", ing.cost_per_kg)}/kg",
                                        color = Color(0xFF8B949E),
                                        fontSize = 11.sp
                                    )
                                    if (kgVal > 0) {
                                        Text(
                                            "Subtotal: KSh ${String.format(Locale.US, "%,.0f", lineCost)}",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = curKgStr,
                                    onValueChange = { inputKgMap[ing.id] = it },
                                    label = { Text("Kg", fontSize = 10.sp) },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.width(90.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4CAF50),
                                        unfocusedBorderColor = Color(0xFF30363D)
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF30363D))

                    // Summary Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1B3A4B),
                        border = BorderStroke(1.dp, Color(0xFF0077B6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Batch Cost & Weight Summary", fontWeight = FontWeight.Bold, color = Color(0xFF90E0EF), fontSize = 13.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Mixed Weight:", color = Color.White, fontSize = 12.sp)
                                Text("${String.format(Locale.US, "%.1f", totalKgMix)} kg", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Batch Cost:", color = Color.White, fontSize = 12.sp)
                                Text("KSh ${String.format(Locale.US, "%,.0f", totalCostMix)}", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cost per Kg:", color = Color.White, fontSize = 12.sp)
                                Text("KSh ${String.format(Locale.US, "%.1f", costPerKgMix)} / kg", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val context = LocalContext.current
                Button(
                    onClick = {
                        if (totalKgMix > 0) {
                            val mixItems = parsedItems
                                .filter { it.second > 0 }
                                .map { (ing, kg, _) ->
                                    com.phms.app.data.repository.PHMSRepository.IngredientMixItem(
                                        ingredientId = ing.id,
                                        name = ing.name,
                                        kgNeeded = kg,
                                        costPerKg = ing.cost_per_kg,
                                        include = true
                                    )
                                }
                            viewModel.mixBatch(
                                formulaName = customBatchName.ifBlank { "Custom Farm Mix" },
                                totalBatchKg = totalKgMix,
                                items = mixItems,
                                batchLabel = customBatchName.ifBlank { "Custom Farm Mix (${totalKgMix.toInt()} kg)" }
                            )
                            Toast.makeText(context, "Mixed ${String.format(Locale.US, "%.1f", totalKgMix)} kg batch successfully!", Toast.LENGTH_SHORT).show()
                            showCustomMixDialog = false
                        }
                    },
                    enabled = totalKgMix > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirm & Mix Batch (${String.format(Locale.US, "%.1f", totalKgMix)} kg)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomMixDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }

    if (showResetFeedDialog) {
        var clearLogsChecked by remember { mutableStateOf(true) }
        var clearBatchesChecked by remember { mutableStateOf(true) }
        var resetIngredientStockChecked by remember { mutableStateOf(false) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showResetFeedDialog = false },
            containerColor = Color(0xFF161B22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFFF5252))
                    Text("Reset Feed Records", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E1C1C),
                        border = BorderStroke(1.dp, Color(0xFF8C1D1D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "This will clean up jumbled feed records so you can set your exact feed stock and pricing. Herd data (pigs, pens, breeding, weights, health) is NEVER touched.",
                            color = Color(0xFFFFCCBA),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearLogsChecked,
                            onCheckedChange = { clearLogsChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE53935))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All Feeding Logs", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearBatchesChecked,
                            onCheckedChange = { clearBatchesChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE53935))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All Mixed Batches", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = resetIngredientStockChecked,
                            onCheckedChange = { resetIngredientStockChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE53935))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Raw Ingredient Stock to 0 kg", color = Color.White, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearFeedData(
                            clearLogs = clearLogsChecked,
                            clearBatches = clearBatchesChecked,
                            clearIngredients = resetIngredientStockChecked
                        )
                        Toast.makeText(context, "Feed records cleared successfully", Toast.LENGTH_SHORT).show()
                        showResetFeedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("Confirm Reset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetFeedDialog = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }
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

        var feedingDateOption by remember { mutableStateOf("Today") }
        var feedingTime by remember { mutableStateOf("Morning (07:00 AM)") }
        var feedType by remember { mutableStateOf("Commercial Pellets/Mash") }
        var selectedIngId by remember { mutableStateOf<Long?>(ingredients.firstOrNull()?.id ?: -1L) }

        var feedSource by remember { mutableStateOf(if (availableMixedBatches.isNotEmpty()) "Mixed Batch" else "Ingredient") } // "Ingredient" or "Mixed Batch"
        var selectedMixedBatchId by remember { mutableStateOf<Long?>(availableMixedBatches.firstOrNull()?.id) }

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

        val recGuide = remember(targetScope, selectedCategory, selectedPigId) {
            when (targetScope) {
                "Single Pig" -> {
                    val p = pigs.find { it.id == selectedPigId }
                    val ageW = if (p != null) ((System.currentTimeMillis() - p.birth_date) / (1000L * 60 * 60 * 24 * 7)).toInt() else null
                    SwineFeedingKey.getRecommendation(weightKg = null, ageWeeks = ageW, categoryName = null)
                }
                "Category" -> SwineFeedingKey.getRecommendation(weightKg = null, ageWeeks = null, categoryName = selectedCategory)
                else -> SwineFeedingKey.getRecommendation(weightKg = null, ageWeeks = null, categoryName = "Growers")
            }
        }

        AlertDialog(
            onDismissRequest = { showLogFeedingDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Log Daily Feeding & Deduct Stock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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

                    // STEP 3: Feed Source & Stock Deduction
                    Text("2. Feed Source & Stock Deduction", color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    // Feed Source toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Ingredient", "Mixed Batch").forEach { src ->
                            val hasData = if (src == "Mixed Batch") availableMixedBatches.isNotEmpty() else true
                            FilterChip(
                                selected = feedSource == src,
                                onClick = { if (hasData) feedSource = src },
                                label = { Text(if (src == "Mixed Batch") "Mix Batch" else "Single Ingredient", fontSize = 11.sp) },
                                enabled = hasData,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (src == "Mixed Batch") Color(0xFF1B3A4B) else Color(0xFF1B5E20),
                                    selectedLabelColor = if (src == "Mixed Batch") Color(0xFF90E0EF) else Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (feedSource == "Mixed Batch") {
                        if (availableMixedBatches.isEmpty()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                                Text("No mixed batches available. Go to the Formulator tab to mix a batch first.", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                            }
                        } else {
                            DropdownSelector(
                                label = "Select Mixed Batch",
                                options = availableMixedBatches.map { b -> b.id to "${b.name} (${String.format("%.1f", b.remaining_kg)} kg left)" },
                                selectedId = selectedMixedBatchId ?: availableMixedBatches.first().id,
                                onSelect = { selectedMixedBatchId = it }
                            )
                            val chosenBatch = availableMixedBatches.find { it.id == selectedMixedBatchId }
                            if (chosenBatch != null) {
                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("KSh ${String.format("%.1f", chosenBatch.cost_per_kg)}/kg x ${String.format("%.1f", totalFeedKg)} kg = KSh ${String.format("%.0f", chosenBatch.cost_per_kg * totalFeedKg)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Text("${String.format("%.1f", chosenBatch.remaining_kg)} kg remaining in batch", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        if (!chosenBatch.is_complete_formula) {
                                            Text("Incomplete mix — some ingredients were skipped", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Warning: mixed batches exist but user chose single ingredient mode
                        if (availableMixedBatches.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF3E2723),
                                border = BorderStroke(1.dp, Color(0xFFFF6F00)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Text(
                                        "You have ${availableMixedBatches.size} mixed batch(es) ready. Using Single Ingredient mode will deduct from raw ingredient stock. Switch to Mix Batch mode above to feed from your pre-mixed stock.",
                                        color = Color(0xFFFFCC80),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
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
                    }

                    Text("Feeding Date", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Today", "Yesterday").forEach { dOpt ->
                            FilterChip(
                                selected = feedingDateOption == dOpt,
                                onClick = { feedingDateOption = dOpt },
                                label = { Text(if (dOpt == "Today") "Today (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())})" else "Yesterday (${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86400000L))})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B5E20),
                                    selectedLabelColor = Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
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

                    // Feeding Key Recommendation helper banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1B281B),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Key Guide: ${recGuide.category}", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${recGuide.dailyFeedRange} • ${recGuide.feedType}", color = Color.White, fontSize = 11.sp)
                                Text("${recGuide.dailyMeals} • ${recGuide.crudeProtein}", color = Color(0xFF8B949E), fontSize = 10.sp)
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = { amountPerPigStr = recGuide.dailyFeedRationKg.toString() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Use ${recGuide.dailyFeedRationKg} kg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

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
                            val chosenBatch = availableMixedBatches.find { it.id == selectedMixedBatchId } ?: availableMixedBatches.firstOrNull()
                            val finalFeedType = if (feedSource == "Mixed Batch") {
                                chosenBatch?.name ?: "Mixed Batch"
                            } else if (feedType == "Raw Ingredient") {
                                chosenIng?.name ?: "Raw Ingredient"
                            } else {
                                feedType
                            }

                            val cal = Calendar.getInstance()
                            if (feedingDateOption == "Yesterday") {
                                cal.add(Calendar.DAY_OF_YEAR, -1)
                            }
                            when {
                                feedingTime.contains("Morning") -> { cal.set(Calendar.HOUR_OF_DAY, 7); cal.set(Calendar.MINUTE, 0) }
                                feedingTime.contains("Afternoon") || feedingTime.contains("12:00") -> { cal.set(Calendar.HOUR_OF_DAY, 12); cal.set(Calendar.MINUTE, 0) }
                                feedingTime.contains("Evening") || feedingTime.contains("05:00") -> { cal.set(Calendar.HOUR_OF_DAY, 17); cal.set(Calendar.MINUTE, 0) }
                                else -> {}
                            }

                            viewModel.logFeedingAndDeductStock(
                                penId = if (targetScope == "Pen") selectedPenId else null,
                                batchId = null,
                                pigId = if (targetScope == "Single Pig") selectedPigId else null,
                                ingredientId = if (feedSource == "Ingredient" && selectedIngId != null && selectedIngId != -1L) selectedIngId else null,
                                feedingTime = feedingTime,
                                feedType = finalFeedType,
                                quantityPerPigKg = amountPerPig,
                                numPigs = numPigs,
                                targetScope = targetScope,
                                category = if (targetScope == "Category") selectedCategory else null,
                                mixedBatchId = if (feedSource == "Mixed Batch") (chosenBatch?.id) else null,
                                notes = notes.ifBlank { null },
                                date = cal.timeInMillis
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
                    "Auto-Calc",
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
                        val pctText = if (percentage < 1.0) String.format(Locale.getDefault(), "%.1f%%", percentage) else "${percentage.toInt()}%"
                        Text(pctText, color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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

@Composable
fun SwineFeedingKeyTab(pigs: List<PigEntity>) {
    var calcWeightStr by remember { mutableStateOf("") }
    var calcAgeStr by remember { mutableStateOf("") }
    var calcCategory by remember { mutableStateOf<String?>("Growers") }
    var herdCountStr by remember { mutableStateOf("10") }

    val weightVal = calcWeightStr.toDoubleOrNull()
    val ageVal = calcAgeStr.toIntOrNull()
    val rec = remember(weightVal, ageVal, calcCategory) {
        SwineFeedingKey.getRecommendation(weightVal, ageVal, calcCategory)
    }

    val herdCount = herdCountStr.toIntOrNull() ?: 1
    val totalHerdDailyKg = rec.dailyFeedRationKg * herdCount
    val bags70kg = totalHerdDailyKg / 70.0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Interactive Recommendation Calculator Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, Color(0xFF2E7D32))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1B5E20)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Feed Requirement Calculator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Interactive guide by weight, age, or category", color = Color(0xFF8B949E), fontSize = 11.sp)
                            }
                        }
                    }

                    // Category Filter Chips
                    Text("Select Pig Category / Stage:", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Piglets", "Weaners", "Growers", "Finishers", "Market Ready",
                            "Pregnant Sow", "Lactating Sow", "Gilt", "Boar"
                        ).forEach { cat ->
                            FilterChip(
                                selected = calcCategory == cat,
                                onClick = {
                                    calcCategory = if (calcCategory == cat) null else cat
                                },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1B5E20),
                                    selectedLabelColor = Color(0xFF4CAF50),
                                    containerColor = Color(0xFF21262D),
                                    labelColor = Color(0xFF8B949E)
                                )
                            )
                        }
                    }

                    // Weight and Age overrides
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            FormField(
                                label = "Weight (kg, optional)",
                                value = calcWeightStr,
                                onValueChange = {
                                    calcWeightStr = it.filter { c -> c.isDigit() || c == '.' }
                                    if (it.isNotBlank()) calcCategory = null
                                },
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                placeholder = "e.g. 45"
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            FormField(
                                label = "Age (weeks, optional)",
                                value = calcAgeStr,
                                onValueChange = {
                                    calcAgeStr = it.filter { c -> c.isDigit() }
                                    if (it.isNotBlank()) calcCategory = null
                                },
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                placeholder = "e.g. 14"
                            )
                        }
                    }

                    // Result box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF122316),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.category, color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Typical: ${rec.weightRange} • ${rec.ageRange}", color = Color(0xFF8B949E), fontSize = 11.sp)
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1B5E20)) {
                                    Text(
                                        "${rec.dailyFeedRationKg} kg / day",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFF1F3D24))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Feed Type:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                Text(rec.feedType, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Crude Protein (CP):", color = Color(0xFF8B949E), fontSize = 11.sp)
                                Text(rec.crudeProtein, color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Feeding Schedule:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                Text(rec.dailyMeals, color = Color.White, fontSize = 11.sp)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Water Requirement:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                Text(rec.waterReqLiters, color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF192A1D)) {
                                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                                    Text(rec.managementGuide, color = Color(0xFFC9D1D9), fontSize = 11.sp)
                                }
                            }

                            // Batch / Herd Daily Feed projection
                            HorizontalDivider(color = Color(0xFF1F3D24))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Calculate for herd size:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("5", "10", "20", "50").forEach { presetCount ->
                                        FilterChip(
                                            selected = herdCountStr == presetCount,
                                            onClick = { herdCountStr = presetCount },
                                            label = { Text("$presetCount pigs", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF2E7D32),
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFF1F242C),
                                                labelColor = Color(0xFF8B949E)
                                            )
                                        )
                                    }
                                }
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0E170E)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$herdCount pigs daily requirement:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${String.format("%.1f", totalHerdDailyKg)} kg / day (~${String.format("%.2f", bags70kg)} 70kg bags)",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section header for full master table
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Standard Swine Feeding Key Table",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF161B22)) {
                    Text(
                        "${SwineFeedingKey.table.size} Growth Categories",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. Full Table of Categories
        items(SwineFeedingKey.table) { item ->
            SwineFeedingGuideCard(item)
        }
    }
}

@Composable
fun SwineFeedingGuideCard(item: SwineFeedingGuideItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Weight: ${item.weightRange} • Age: ${item.ageRange}", color = Color(0xFF8B949E), fontSize = 11.sp)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1B5E20)
                ) {
                    Text(
                        "${item.dailyFeedRationKg} kg/day",
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF21262D))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Feed Type & CP:", color = Color(0xFF8B949E), fontSize = 11.sp)
                Text("${item.feedType} (${item.crudeProtein})", color = Color(0xFFC9D1D9), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily Ration Range:", color = Color(0xFF8B949E), fontSize = 11.sp)
                Text(item.dailyFeedRange, color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Meals & Water:", color = Color(0xFF8B949E), fontSize = 11.sp)
                Text("${item.dailyMeals} • ${item.waterReqLiters}", color = Color(0xFF64B5F6), fontSize = 11.sp)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF21262D)
            ) {
                Text(
                    item.managementGuide,
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

