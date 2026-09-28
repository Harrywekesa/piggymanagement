package com.phms.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
