package com.phms.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.phms.app.data.local.entity.*
import com.phms.app.data.repository.FarmSettings
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// 1. DASHBOARD SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val pigs by viewModel.activePigs.collectAsState()
    val alerts by viewModel.activeAlerts.collectAsState()
    val criticalAlerts by viewModel.criticalAlerts.collectAsState()
    val pnl by viewModel.pnlSummary.collectAsState()
    val marketReadyCount = pigs.count { it.current_stage_id == 5L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI CARDS
        item {
            Text("Farm Overview", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashKpiCard("Total Herd", "${pigs.size}", "Active Pigs", Icons.Default.Pets, Color(0xFF4CAF50), Modifier.weight(1f))
                DashKpiCard("Market Ready", "$marketReadyCount", "90kg+ Target", Icons.Default.ShoppingCart, Color(0xFFFFB300), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashKpiCard(
                    "Active Alerts", "${alerts.size}", "Action Required",
                    Icons.Default.Notifications,
                    if (alerts.isNotEmpty()) Color(0xFFFF5252) else Color(0xFF4CAF50),
                    Modifier.weight(1f)
                )
                DashKpiCard(
                    "Est. Net Profit", "KSh ${pnl?.netProfit?.toInt() ?: 0}",
                    "30-Day", Icons.Default.TrendingUp, Color(0xFF4CAF50), Modifier.weight(1f)
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                            Text("30-Day Financial P&L", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                        Text(
                            "Net: KSh ${String.format("%.0f", pnl?.netProfit ?: 0.0)}",
                            fontWeight = FontWeight.Bold,
                            color = if ((pnl?.netProfit ?: 0.0) >= 0) Color(0xFF81C784) else Color(0xFFE57373),
                            fontSize = 14.sp
                        )
                    }
                    HorizontalDivider(color = Color(0xFF21262D))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Revenue: KSh ${String.format("%.0f", pnl?.totalRevenue ?: 0.0)}", color = Color(0xFF81C784), fontSize = 11.sp)
                            Text("Feed: KSh ${String.format("%.0f", pnl?.feedCost ?: 0.0)}", color = Color(0xFFE57373), fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Health: KSh ${String.format("%.0f", pnl?.healthCost ?: 0.0)}", color = Color(0xFFE57373), fontSize = 11.sp)
                            Text("Overheads: KSh ${String.format("%.0f", pnl?.otherExpensesCost ?: 0.0)}", color = Color(0xFFE57373), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // QUICK ACTIONS
        item {
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashQuickAction("Pigs", Icons.Default.Pets, { navController.navigate("pigs") }, Modifier.weight(1f))
                DashQuickAction("Feed", Icons.Default.Grass, { navController.navigate("feed") }, Modifier.weight(1f))
                DashQuickAction("Health", Icons.Default.LocalHospital, { navController.navigate("health") }, Modifier.weight(1f))
                DashQuickAction("Market", Icons.Default.ShoppingCart, { navController.navigate("market") }, Modifier.weight(1f))
            }
        }

        // ALERTS PREVIEW
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Alerts (${alerts.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = { navController.navigate("alerts") }) {
                    Text("View All", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (alerts.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                        Text("All caught up! No active alerts.", color = Color(0xFF8B949E))
                    }
                }
            }
        } else {
            items(alerts.take(4)) { alert ->
                AlertItemCard(alert = alert, onDone = { viewModel.markAlertDone(alert.id) })
            }
        }

        // RECENT PIGS PREVIEW
        item { Text("Recent Pigs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        items(pigs.take(3)) { pig ->
            CompactPigRow(pig = pig, onClick = { navController.navigate("pig_detail/${pig.id}") })
        }
        if (pigs.isNotEmpty()) {
            item {
                TextButton(onClick = { navController.navigate("pigs") }, modifier = Modifier.fillMaxWidth()) {
                    Text("View All ${pigs.size} Pigs →", color = Color(0xFF4CAF50))
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun DashKpiCard(title: String, value: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF6E7681))
        }
    }
}

@Composable
fun DashQuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF1B5E20)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
            Text(label, fontSize = 11.sp, color = Color(0xFFE6EDF3), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CompactPigRow(pig: PigEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                if (pig.photo_path != null) {
                    AsyncImage(model = pig.photo_path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(pig.tag_number.take(3), color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("#${pig.tag_number}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${pig.breed} • ${if (pig.sex == "M") "Boar" else "Sow"}", color = Color(0xFF8B949E), fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF30363D), modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. PIGS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PigsScreen(viewModel: MainViewModel, navController: NavController) {
    val pigs by viewModel.activePigs.collectAsState()
    val stages by viewModel.stages.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredPigs = pigs.filter { pig ->
        (searchQuery.isEmpty() ||
                pig.tag_number.contains(searchQuery, ignoreCase = true) ||
                pig.breed.contains(searchQuery, ignoreCase = true)) &&
                (selectedFilter == "All" ||
                        (selectedFilter == "Boar" && pig.sex == "M") ||
                        (selectedFilter == "Sow" && pig.sex == "F") ||
                        (selectedFilter == "Market Ready" && pig.current_stage_id == 5L))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Pig Herd", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("${pigs.size} active • ${filteredPigs.size} shown", color = Color(0xFF8B949E), fontSize = 13.sp)
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by tag or breed...", color = Color(0xFF6E7681)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF6E7681)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedContainerColor = Color(0xFF161B22),
                        focusedContainerColor = Color(0xFF161B22)
                    )
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Sow", "Boar", "Market Ready").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1B5E20),
                                selectedLabelColor = Color(0xFF4CAF50),
                                containerColor = Color(0xFF161B22),
                                labelColor = Color(0xFF8B949E)
                            )
                        )
                    }
                }
            }
            if (filteredPigs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No pigs match your search.", color = Color(0xFF6E7681), textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(filteredPigs) { pig ->
                    val stage = stages.find { it.id == pig.current_stage_id }
                    PigListCard(pig = pig, stageName = stage?.name ?: "Piglet", onClick = {
                        navController.navigate("pig_detail/${pig.id}")
                    })
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("add_edit_pig/-1") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, "Add Pig")
        }
    }
}

@Composable
fun PigListCard(pig: PigEntity, stageName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo or Tag Avatar
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
                        )
                    )
                    .border(2.dp, Color(0xFF4CAF50), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (pig.photo_path != null) {
                    AsyncImage(
                        model = pig.photo_path,
                        contentDescription = "Pig photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐷", fontSize = 18.sp)
                        Text(
                            pig.tag_number.take(4),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Tag #${pig.tag_number}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(
                    "${pig.breed} • ${if (pig.sex == "M") "Boar" else "Sow"}",
                    fontSize = 13.sp, color = Color(0xFF8B949E)
                )
                Text(pig.source, fontSize = 11.sp, color = Color(0xFF6E7681))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1B5E20)) {
                    Text(
                        stageName,
                        color = Color(0xFF4CAF50),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF30363D), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. ALERTS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AlertsScreen(viewModel: MainViewModel) {
    val alerts by viewModel.activeAlerts.collectAsState()
    var filterPriority by remember { mutableStateOf("All") }

    val filteredAlerts = alerts.filter {
        filterPriority == "All" || it.priority == filterPriority
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Alerts Inbox", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Critical", "High", "Medium", "Low").forEach { p ->
                    FilterChip(
                        selected = filterPriority == p,
                        onClick = { filterPriority = p },
                        label = { Text(p, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (p) {
                                "Critical" -> Color(0xFF7B1F1F)
                                "High" -> Color(0xFF7B4F00)
                                else -> Color(0xFF1B5E20)
                            },
                            selectedLabelColor = when (p) {
                                "Critical" -> Color(0xFFFF5252)
                                "High" -> Color(0xFFFFB300)
                                else -> Color(0xFF4CAF50)
                            },
                            containerColor = Color(0xFF161B22),
                            labelColor = Color(0xFF8B949E)
                        )
                    )
                }
            }
        }
        if (filteredAlerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✅", fontSize = 40.sp)
                    Text("All clear! No alerts here.", color = Color(0xFF8B949E), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertItemCard(alert = alert, onDone = { viewModel.markAlertDone(alert.id) })
                }
            }
        }
    }
}

@Composable
fun AlertItemCard(alert: AlertEntity, onDone: () -> Unit) {
    val (bg, border, textColor) = when (alert.priority) {
        "Critical" -> Triple(Color(0xFF1A0A0A), Color(0xFFFF5252), Color(0xFFFF5252))
        "High" -> Triple(Color(0xFF1A1400), Color(0xFFFFB300), Color(0xFFFFB300))
        else -> Triple(Color(0xFF161B22), Color(0xFF30363D), Color(0xFF4CAF50))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, border)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(textColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alert.type.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(alert.message, fontSize = 13.sp, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDone, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Check, "Done", tint = Color(0xFF4CAF50))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. FEED SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val ingredients by viewModel.feedIngredients.collectAsState()
    val formulas by viewModel.formulas.collectAsState()
    val feedingLogs by viewModel.feedingLogs.collectAsState()
    val pigs by viewModel.activePigs.collectAsState()
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                                        Text("🥣", fontSize = 20.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ing?.name ?: "Feed Mix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${log.feeding_time} • ${log.num_pigs} pig(s)", color = Color(0xFF8B949E), fontSize = 12.sp)
                                        Text(dateFormat.format(Date(log.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${log.quantity_kg} kg", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("KSh ${(log.quantity_kg * (ing?.cost_per_kg ?: 0.0)).toInt()}", color = Color(0xFF81C784), fontSize = 11.sp)
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
        var bagSizeStr by remember { mutableStateOf("70") }
        var bagPriceStr by remember { mutableStateOf("3200") }
        var totalStockStr by remember { mutableStateOf("140") }
        var costPerKgStr by remember { mutableStateOf("45") }
        var reorderLevelStr by remember { mutableStateOf("50") }
        var stageCategoryExpanded by remember { mutableStateOf(false) }
        var selectedStageCategory by remember { mutableStateOf("Grower") }
        val stageCategories = listOf("Creep/Starter", "Weaner", "Grower", "Finisher", "Sow/Gilt", "All Stages")

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
                    FormField("Feed / Brand Name *", name, { name = it }, placeholder = "e.g. Pembe Pig Finisher 70kg Bag")
                    if (type == "Commercial Premix") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { FormField("Bag Size (kg)", bagSizeStr, { bagSizeStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                            Box(Modifier.weight(1f)) { FormField("Price / Bag (KSh)", bagPriceStr, { bagPriceStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        }
                        // Stage category picker
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
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color(0xFF30363D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
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
                        FormField("Cost per kg (KSh)", costPerKgStr, { costPerKgStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Total Stock (kg)", totalStockStr, { totalStockStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Reorder Level (kg)", reorderLevelStr, { reorderLevelStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val bSize = bagSizeStr.toDoubleOrNull()
                            val bPrice = bagPriceStr.toDoubleOrNull()
                            val calculatedCostPerKg = if (type == "Commercial Premix" && bSize != null && bPrice != null && bSize > 0) bPrice / bSize else (costPerKgStr.toDoubleOrNull() ?: 45.0)
                            viewModel.addFeedIngredient(
                                name = name,
                                stockKg = totalStockStr.toDoubleOrNull() ?: 50.0,
                                costPerKg = calculatedCostPerKg,
                                reorderLevel = reorderLevelStr.toDoubleOrNull() ?: 20.0,
                                type = type,
                                brandName = if (type == "Commercial Premix") name else null,
                                bagSizeKg = if (type == "Commercial Premix") bSize else null,
                                pricePerBag = if (type == "Commercial Premix") bPrice else null,
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
        var feedingTime by remember { mutableStateOf("Morning (07:00 AM)") }
        var selectedIngId by remember { mutableStateOf(ingredients.firstOrNull()?.id ?: 1L) }
        var amountPerPigStr by remember { mutableStateOf("2.0") }
        var numPigsStr by remember { mutableStateOf("${pigs.size.coerceAtLeast(1)}") }

        val amountPerPig = amountPerPigStr.toDoubleOrNull() ?: 0.0
        val numPigs = numPigsStr.toIntOrNull() ?: 0
        val totalFeedKg = amountPerPig * numPigs

        AlertDialog(
            onDismissRequest = { showLogFeedingDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Log Daily Feeding & Deduct Stock", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Feeding Time", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Morning (07:00 AM)", "Afternoon (12:00 PM)", "Evening (05:00 PM)").forEach { ft ->
                            FilterChip(
                                selected = feedingTime == ft,
                                onClick = { feedingTime = ft },
                                label = { Text(ft, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1B5E20), selectedLabelColor = Color(0xFF4CAF50))
                            )
                        }
                    }
                    if (ingredients.isNotEmpty()) {
                        Text("Select Feed Stock / Bag", color = Color(0xFF8B949E), fontSize = 12.sp)
                        DropdownSelector(
                            label = "Feed Stock",
                            options = ingredients.map { it.id to "${it.name} (${String.format("%.1f", it.stock_kg)}kg avail)" },
                            selectedId = selectedIngId,
                            onSelect = { selectedIngId = it ?: 1L }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Amount / Pig (kg)", amountPerPigStr, { amountPerPigStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Number of Pigs", numPigsStr, { numPigsStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF1B3A1B)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Feed Output:", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${String.format("%.1f", totalFeedKg)} kg", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (totalFeedKg > 0) {
                            viewModel.logFeedingAndDeductStock(
                                penId = null,
                                batchId = null,
                                pigId = null,
                                ingredientId = selectedIngId,
                                feedingTime = feedingTime,
                                feedType = "Formula Ration",
                                quantityPerPigKg = amountPerPig,
                                numPigs = numPigs
                            )
                            showLogFeedingDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Confirm & Deduct Stock") }
            },
            dismissButton = { OutlinedButton(onClick = { showLogFeedingDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) } }
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

// ─────────────────────────────────────────────────────────────────────────────
// 5. HEALTH SCREEN
// ─────────────────────────────────────────────────────────────────────────────
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Health & Veterinary", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = {
                    if (selectedPigId == null && pigs.isNotEmpty()) selectedPigId = pigs.first().id
                    showGroupHealthDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.MedicalServices, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Log Event", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        AlertDialog(
            onDismissRequest = { showGroupHealthDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Log Health Event", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Target Scope", color = Color(0xFF8B949E), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Single Pig", "Category", "Herd").forEach { scope ->
                            FilterChip(
                                selected = targetScope == scope,
                                onClick = { targetScope = scope },
                                label = { Text(scope, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1B5E20), selectedLabelColor = Color(0xFF4CAF50))
                            )
                        }
                    }
                    if (targetScope == "Single Pig" && pigs.isNotEmpty()) {
                        Text("Select Animal *", color = Color(0xFF8B949E), fontSize = 12.sp)
                        DropdownSelector(
                            label = "Select Pig",
                            options = pigs.map { it.id to "Tag #${it.tag_number} (${it.breed})" },
                            selectedId = selectedPigId ?: pigs.first().id,
                            onSelect = { selectedPigId = it }
                        )
                    }
                    if (targetScope == "Category") {
                        Text("Category / Group", color = Color(0xFF8B949E), fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Piglets", "Weaners", "Growers", "Finishers", "Sows", "Boars").forEach { cat ->
                                FilterChip(
                                    selected = targetCategory == cat,
                                    onClick = { targetCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1B5E20), selectedLabelColor = Color(0xFF4CAF50))
                                )
                            }
                        }
                    }
                    FormField("Event Type (e.g. Deworming, Vaccine)", eventType, { eventType = it })
                    FormField("Product / Medicine Name *", product, { product = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Dosage", dosage, { dosage = it }, placeholder = "e.g. 2ml/pig") }
                        Box(Modifier.weight(1f)) { FormField("Withdrawal (Days)", withdrawalDaysStr, { withdrawalDaysStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Total Cost (KSh)", costStr, { costStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Vet / Operator", vetName, { vetName = it }) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (product.isNotBlank()) {
                            viewModel.logGroupHealthEvent(
                                targetScope = targetScope,
                                targetPigId = if (targetScope == "Single Pig") selectedPigId else null,
                                targetCategory = if (targetScope == "Category") targetCategory else null,
                                type = eventType,
                                product = product,
                                dosage = dosage,
                                cost = costStr.toDoubleOrNull() ?: 0.0,
                                vetName = vetName,
                                notes = notes,
                                withdrawalDays = withdrawalDaysStr.toIntOrNull() ?: 0
                            )
                            showGroupHealthDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Save Health Log") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGroupHealthDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. BREEDING SCREEN
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
// 7. MARKET SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MarketScreen(viewModel: MainViewModel, onStartSale: () -> Unit) {
    val pigs by viewModel.activePigs.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val marketPigs = pigs.filter { it.current_stage_id == 5L && it.status == "Active" }
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    var selectedCountyFilter by remember { mutableStateOf("All Counties") }
    var buyerSearchQuery by remember { mutableStateOf("") }

    val filteredBuyers = buyers.filter { buyer ->
        (selectedCountyFilter == "All Counties" || buyer.county.equals(selectedCountyFilter, ignoreCase = true) || buyer.location?.contains(selectedCountyFilter, ignoreCase = true) == true) &&
        (buyerSearchQuery.isBlank() || buyer.name.contains(buyerSearchQuery, ignoreCase = true) || buyer.phone.contains(buyerSearchQuery) || buyer.location?.contains(buyerSearchQuery, ignoreCase = true) == true)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Market & Sales", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (marketPigs.isNotEmpty()) Color(0xFF1B3A1B) else Color(0xFF161B22)),
                border = BorderStroke(1.dp, if (marketPigs.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFF30363D))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (marketPigs.isNotEmpty()) "📣 MARKET ALERT: ${marketPigs.size} Finisher Pig(s) Market Ready!" else "Market Status Normal",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                        Text(
                            if (marketPigs.isNotEmpty()) "Pigs have reached ~90kg target weight. Ready for buyers & abattoir sales." else "No pigs currently at finisher market stage.",
                            color = Color(0xFF8B949E), fontSize = 11.sp
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = onStartSale,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Record New Sale to Buyer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Text("Market-Ready Stock (${marketPigs.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        items(marketPigs) { pig ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF21262D)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pig.photo_path != null) {
                            AsyncImage(model = pig.photo_path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(pig.tag_number.take(3), color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("#${pig.tag_number}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${pig.breed} • Est. 95kg", color = Color(0xFF8B949E), fontSize = 12.sp)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1B5E20)) {
                        Text("READY", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Buyers Directory — Search by Location", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                OutlinedTextField(
                    value = buyerSearchQuery,
                    onValueChange = { buyerSearchQuery = it },
                    placeholder = { Text("Search buyer name, location, phone...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF8B949E)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Text("Filter by County:", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All Counties", "Mombasa", "Nakuru", "Nairobi", "Kiambu", "Uasin Gishu", "Kakamega", "Kisumu", "Kilifi").forEach { county ->
                        FilterChip(
                            selected = selectedCountyFilter == county,
                            onClick = { selectedCountyFilter = county },
                            label = { Text(county, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1B5E20),
                                selectedLabelColor = Color(0xFF4CAF50),
                                containerColor = Color(0xFF21262D),
                                labelColor = Color(0xFF8B949E)
                            )
                        )
                    }
                }
            }
        }

        if (filteredBuyers.isEmpty()) {
            item {
                Text("No buyers found matching location filter.", color = Color(0xFF6E7681), fontSize = 13.sp)
            }
        } else {
            items(filteredBuyers) { buyer ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(buyer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val locText = listOfNotNull(buyer.ward, buyer.sub_county, buyer.county ?: buyer.location).filter { it.isNotBlank() }.joinToString(", ")
                            Text(if (locText.isNotBlank()) "📍 $locText" else "📍 Location N/A", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${buyer.type} • ${buyer.phone}", color = Color(0xFF8B949E), fontSize = 11.sp)
                        }
                        if (!buyer.phone.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${buyer.phone}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) { e.printStackTrace() }
                                },
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Call Buyer", color = Color(0xFF4CAF50), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        if (sales.isNotEmpty()) {
            item { Text("Recent Sales (${sales.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
            items(sales.take(10)) { sale ->
                val buyer = buyers.find { it.id == sale.buyer_id }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(buyer?.name ?: "Unknown Buyer", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${sale.total_weight}kg @ KSh ${sale.price_per_kg}/kg", color = Color(0xFF8B949E), fontSize = 12.sp)
                            Text(dateFormat.format(Date(sale.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("KSh ${String.format("%,.0f", sale.total_amount)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            val (psColor, psLabel) = when (sale.payment_status) {
                                "Paid" -> Pair(Color(0xFF4CAF50), "Paid")
                                "Partial" -> Pair(Color(0xFFFFB300), "Partial")
                                else -> Pair(Color(0xFFFF5252), "Pending")
                            }
                            Text(psLabel, color = psColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. REPORTS SCREEN — FILTERABLE & SEARCHABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ReportsHubScreen(viewModel: MainViewModel) {
    val pnl by viewModel.pnlSummary.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRange by remember { mutableStateOf("This Month") }
    var searchQuery by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    // Date range filter
    val now = System.currentTimeMillis()
    val startMs = when (selectedRange) {
        "This Month" -> now - TimeUnit.DAYS.toMillis(30)
        "3 Months" -> now - TimeUnit.DAYS.toMillis(90)
        "6 Months" -> now - TimeUnit.DAYS.toMillis(180)
        else -> 0L
    }

    LaunchedEffect(startMs) { viewModel.loadPnL(startMs) }

    val filteredSales = sales.filter { s ->
        s.date >= startMs && (searchQuery.isEmpty() ||
                buyers.find { it.id == s.buyer_id }?.name?.contains(searchQuery, ignoreCase = true) == true ||
                s.total_amount.toString().contains(searchQuery) ||
                dateFormat.format(Date(s.date)).contains(searchQuery, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        // Header + Filters
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Financial Reports", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("This Month", "3 Months", "6 Months", "All Time").forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1B5E20),
                            selectedLabelColor = Color(0xFF4CAF50),
                            containerColor = Color(0xFF161B22),
                            labelColor = Color(0xFF8B949E)
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by buyer, date, amount...", color = Color(0xFF6E7681)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF6E7681)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedContainerColor = Color(0xFF161B22),
                    focusedContainerColor = Color(0xFF161B22)
                )
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161B22),
            contentColor = Color(0xFF4CAF50),
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        ) {
            listOf("P&L Summary", "Sales Ledger").forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = {
                    Text(title, fontSize = 13.sp, color = if (selectedTab == i) Color(0xFF4CAF50) else Color(0xFF8B949E))
                })
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (selectedTab) {
                0 -> {
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Profit & Loss — $selectedRange", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                HorizontalDivider(color = Color(0xFF21262D))
                                PnLRow("Sales Revenue", "KSh ${pnl?.totalRevenue?.toInt() ?: 0}", positive = true)
                                PnLRow("Feed Expenses", "– KSh ${pnl?.feedCost?.toInt() ?: 0}", positive = false)
                                PnLRow("Health & Vet", "– KSh ${pnl?.healthCost?.toInt() ?: 0}", positive = false)
                                PnLRow("Labor (Est.)", "– KSh ${pnl?.estimatedLaborCost?.toInt() ?: 0}", positive = false)
                                HorizontalDivider(color = Color(0xFF30363D))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("NET PROFIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "KSh ${pnl?.netProfit?.toInt() ?: 0}",
                                        color = if ((pnl?.netProfit ?: 0.0) >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold, fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = {
                                val pnlVal = pnl
                                if (pnlVal != null) {
                                    val file = com.phms.app.domain.reporting.PdfReportGenerator.generatePnLReportPdf(context, pnlVal)
                                    if (file != null) {
                                        Toast.makeText(context, "PDF saved: ${file.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export as PDF", color = Color.White)
                        }
                    }
                }
                1 -> {
                    if (filteredSales.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No sales in this period.", color = Color(0xFF6E7681)) } }
                    } else {
                        items(filteredSales) { sale ->
                            val buyer = buyers.find { it.id == sale.buyer_id }
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF1B5E20)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(buyer?.name ?: "Unknown", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("${sale.total_weight}kg • KSh ${sale.price_per_kg}/kg", color = Color(0xFF8B949E), fontSize = 12.sp)
                                        Text(dateFormat.format(Date(sale.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("KSh ${String.format("%,.0f", sale.total_amount)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        val statusColor = when (sale.payment_status) {
                                            "Paid" -> Color(0xFF4CAF50)
                                            "Partial" -> Color(0xFFFFB300)
                                            else -> Color(0xFFFF5252)
                                        }
                                        Text(sale.payment_status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun PnLRow(label: String, value: String, positive: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF8B949E), fontSize = 14.sp)
        Text(value, color = if (positive) Color(0xFF4CAF50) else Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. SETTINGS SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val farmSettings by viewModel.farmSettings.collectAsState()
    var isSeeding by remember { mutableStateOf(false) }

    // Editable state
    var farmName by remember(farmSettings.farmName) { mutableStateOf(farmSettings.farmName) }
    var farmerName by remember(farmSettings.farmerName) { mutableStateOf(farmSettings.farmerName) }
    var farmLocation by remember(farmSettings.farmLocation) { mutableStateOf(farmSettings.farmLocation) }
    var currencySymbol by remember(farmSettings.currencySymbol) { mutableStateOf(farmSettings.currencySymbol) }
    var alertVaccination by remember(farmSettings.alertVaccination) { mutableStateOf(farmSettings.alertVaccination) }
    var alertFarrowing by remember(farmSettings.alertFarrowing) { mutableStateOf(farmSettings.alertFarrowing) }
    var alertLowFeed by remember(farmSettings.alertLowFeed) { mutableStateOf(farmSettings.alertLowFeed) }
    var alertPromotion by remember(farmSettings.alertPromotion) { mutableStateOf(farmSettings.alertPromotion) }
    var alertWithdrawal by remember(farmSettings.alertWithdrawal) { mutableStateOf(farmSettings.alertWithdrawal) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White) }

        // Farm Profile
        item {
            SectionCard("Farm Profile") {
                FormField("Farm Name", farmName, { farmName = it })
                Spacer(Modifier.height(12.dp))
                FormField("Farmer's Name", farmerName, { farmerName = it })
                Spacer(Modifier.height(12.dp))
                FormField("Farm Location", farmLocation, { farmLocation = it }, placeholder = "e.g. Kitale, Trans Nzoia")
                Spacer(Modifier.height(12.dp))
                FormField("Currency Symbol", currencySymbol, { currencySymbol = it })
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.saveFarmSettings(
                            farmSettings.copy(
                                farmName = farmName,
                                farmerName = farmerName,
                                farmLocation = farmLocation,
                                currencySymbol = currencySymbol
                            )
                        )
                        Toast.makeText(context, "Farm profile saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Save Profile") }
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

        // Demo data
        item {
            SectionCard("Data Management") {
                Text("Populate with realistic demo farm data for testing.", color = Color(0xFF8B949E), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        isSeeding = true
                        viewModel.seedDemoData {
                            isSeeding = false
                            Toast.makeText(context, "Demo data seeded successfully!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSeeding,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSeeding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF4CAF50), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isSeeding) "Seeding..." else "Seed Demo Data", color = Color(0xFF4CAF50))
                }
                Spacer(Modifier.height(8.dp))
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

// End of Screens.kt
