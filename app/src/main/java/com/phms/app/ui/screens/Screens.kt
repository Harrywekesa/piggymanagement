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
    val updateInfo by viewModel.appUpdateInfo.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val marketReadyCount = pigs.count { it.current_stage_id == 5L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (updateInfo.isUpdateAvailable) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        com.phms.app.data.updater.GitHubUpdateChecker.openUpdateLink(context, updateInfo)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3A4B)),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFF90E0EF), modifier = Modifier.size(24.dp))
                            Column {
                                Text("🚀 New Update Available (${updateInfo.latestVersion})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Tap to download & install the latest GitHub release.", color = Color(0xFF90E0EF), fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { com.phms.app.data.updater.GitHubUpdateChecker.openUpdateLink(context, updateInfo) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // KPI CARDS
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Farm Overview", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedButton(
                    onClick = { navController.navigate("help_center?autoTour=true") },
                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Help & Tour", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
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
    val pens by viewModel.pens.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var mainTab by remember { mutableIntStateOf(0) } // 0 = Pig Herd, 1 = Pens Directory
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    var showAddPenDialog by remember { mutableStateOf(false) }
    var targetPenForAssignment by remember { mutableStateOf<PenEntity?>(null) }

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
        Column(Modifier.fillMaxSize()) {
            // Header & Segmented Tab Row
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Herd & Pens Management", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = mainTab,
                    containerColor = Color(0xFF161B22),
                    contentColor = Color(0xFF4CAF50)
                ) {
                    Tab(
                        selected = mainTab == 0,
                        onClick = { mainTab = 0 },
                        text = { Text("🐷 Pig Herd (${pigs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = mainTab == 1,
                        onClick = { mainTab = 1 },
                        text = { Text("🏡 Pens Directory (${pens.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            if (mainTab == 0) {
                // PIG HERD LIST
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color(0xFF30363D),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                unfocusedContainerColor = Color(0xFF161B22), focusedContainerColor = Color(0xFF161B22)
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
            } else {
                // PENS DIRECTORY
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Active Farm Pens", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Button(
                                onClick = { showAddPenDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add New Pen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (pens.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Home, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No Pens Created Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Create pens to organize pigs into Nursery, Growth, Farrowing, or Finishers.", color = Color(0xFF8B949E), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { showAddPenDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Create First Pen")
                                    }
                                }
                            }
                        }
                    } else {
                        items(pens) { pen ->
                            val assignedPigs = pigs.filter { it.pen_id == pen.id }
                            val occupancyPct = (assignedPigs.size.toFloat() / pen.capacity.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                                border = BorderStroke(1.dp, Color(0xFF30363D))
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(pen.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            if (!pen.notes.isNullOrBlank()) {
                                                Text(pen.notes, color = Color(0xFF8B949E), fontSize = 11.sp)
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (assignedPigs.size >= pen.capacity) Color(0xFF3E1F1F) else Color(0xFF1B3A1B)
                                        ) {
                                            Text(
                                                "Occupancy: ${assignedPigs.size} / ${pen.capacity}",
                                                color = if (assignedPigs.size >= pen.capacity) Color(0xFFFF5252) else Color(0xFF4CAF50),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { occupancyPct },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = if (occupancyPct >= 1.0f) Color(0xFFFF5252) else if (occupancyPct >= 0.8f) Color(0xFFFFB300) else Color(0xFF4CAF50),
                                        trackColor = Color(0xFF21262D)
                                    )

                                    if (assignedPigs.isNotEmpty()) {
                                        Text("Assigned Pigs (${assignedPigs.size}):", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            assignedPigs.forEach { pig ->
                                                AssistChip(
                                                    onClick = { navController.navigate("pig_detail/${pig.id}") },
                                                    label = { Text("#${pig.tag_number} (${pig.breed.take(8)})", fontSize = 11.sp) },
                                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF21262D), labelColor = Color.White)
                                                )
                                            }
                                        }
                                    } else {
                                        Text("No pigs assigned to this pen.", color = Color(0xFF6E7681), fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { targetPenForAssignment = pen },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Pets, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Assign / Move Pig To ${pen.name}", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to add pig
        if (mainTab == 0) {
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

        // Add Pen Dialog
        if (showAddPenDialog) {
            var penName by remember { mutableStateOf("") }
            var penCapacityStr by remember { mutableStateOf("10") }
            var penNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddPenDialog = false },
                containerColor = Color(0xFF161B22),
                title = { Text("Create New Pen", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormField("Pen Name / Identifier *", penName, { penName = it }, placeholder = "e.g. Pen A1 (Nursery), Pen B2 (Farrowing)")
                        FormField("Max Capacity (Number of Pigs) *", penCapacityStr, { penCapacityStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        FormField("Notes / Facilities", penNotes, { penNotes = it }, placeholder = "e.g. Creep heater, nipple drinkers")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (penName.isBlank()) {
                                Toast.makeText(context, "Please enter pen name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.createPen(penName, penCapacityStr.toIntOrNull() ?: 10, penNotes.ifBlank { null })
                            showAddPenDialog = false
                            Toast.makeText(context, "Pen '$penName' created!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Create Pen") }
                },
                dismissButton = { OutlinedButton(onClick = { showAddPenDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) } }
            )
        }

        // Assign Pig to Pen Dialog
        targetPenForAssignment?.let { pen ->
            var selectedPigId by remember { mutableStateOf<Long?>(null) }

            AlertDialog(
                onDismissRequest = { targetPenForAssignment = null },
                containerColor = Color(0xFF161B22),
                title = { Text("Assign Pig to ${pen.name}", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Select pig from active herd:", color = Color(0xFF8B949E), fontSize = 12.sp)
                        pigs.forEach { pig ->
                            val currentPen = pens.find { it.id == pig.pen_id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPigId = pig.id }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedPigId == pig.id, onClick = { selectedPigId = pig.id })
                                Column {
                                    Text("Pig #${pig.tag_number} (${pig.breed})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Current: ${currentPen?.name ?: "Unassigned"}", color = Color(0xFF8B949E), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedPigId?.let { pigId ->
                                viewModel.assignPigToPen(pigId, pen.id)
                                Toast.makeText(context, "Pig assigned to ${pen.name}!", Toast.LENGTH_SHORT).show()
                            }
                            targetPenForAssignment = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Assign to Pen") }
                },
                dismissButton = { OutlinedButton(onClick = { targetPenForAssignment = null }) { Text("Cancel", color = Color(0xFF8B949E)) } }
            )
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
