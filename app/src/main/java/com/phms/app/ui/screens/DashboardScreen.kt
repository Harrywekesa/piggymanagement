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
import androidx.compose.material.icons.automirrored.filled.*
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
    val pnl by viewModel.pnlSummary.collectAsState()
    val updateInfo by viewModel.appUpdateInfo.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val marketReadyCount = remember(pigs) { pigs.count { it.current_stage_id == 5L } }

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
                                Text("New Update Available (${updateInfo.latestVersion})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
        // HERO SECTION HEADER
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
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Help & Tour", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── 1. DOMINANT HERO CARD (Herd Overview & Key Metrics) ───────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Hero Row: Icon Badge, Big Number, and Quick Add Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A3E22)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Text("Total Active Herd", color = Color(0xFF8B949E), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${pigs.size}", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("pigs", fontSize = 15.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { navController.navigate("add_edit_pig/-1") },
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Pig", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF21262D))

                    // Embedded Sub-Metrics Row with clear card outlines
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroSubMetricCard(
                            label = "Market Ready",
                            value = "$marketReadyCount",
                            subtitle = "90kg+ Target",
                            icon = Icons.Default.ShoppingCart,
                            iconTint = Color(0xFFFFB74D),
                            iconBg = Color(0xFF3E2D1A),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("market") }
                        )

                        HeroSubMetricCard(
                            label = "Active Alerts",
                            value = "${alerts.size}",
                            subtitle = if (alerts.isNotEmpty()) "Action needed" else "All clear",
                            icon = Icons.Default.Notifications,
                            iconTint = if (alerts.isNotEmpty()) Color(0xFFFF5252) else Color(0xFF81C784),
                            iconBg = if (alerts.isNotEmpty()) Color(0xFF3E1A1A) else Color(0xFF1A3E22),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("alerts") }
                        )

                        HeroSubMetricCard(
                            label = "30-Day Net",
                            value = "KSh ${(pnl?.netProfit ?: 0.0).toInt()}",
                            subtitle = if ((pnl?.netProfit ?: 0.0) >= 0) "Profitable" else "Deficit",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = if ((pnl?.netProfit ?: 0.0) >= 0) Color(0xFF81C784) else Color(0xFFE57373),
                            iconBg = Color(0xFF1A2B3E),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("reports") }
                        )
                    }
                }
            }
        }

        // ── 2. FINANCIAL P&L CARD (Clear Outline) ──────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A3E22)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                            }
                            Text("30-Day Financial P&L", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if ((pnl?.netProfit ?: 0.0) >= 0) Color(0xFF1B3E22) else Color(0xFF3E1A1A),
                            border = BorderStroke(1.dp, if ((pnl?.netProfit ?: 0.0) >= 0) Color(0xFF2E7D32) else Color(0xFF8C1D1D))
                        ) {
                            Text(
                                "Net: KSh ${pnl?.netProfit?.toInt() ?: 0}",
                                fontWeight = FontWeight.Bold,
                                color = if ((pnl?.netProfit ?: 0.0) >= 0) Color(0xFF81C784) else Color(0xFFE57373),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFF21262D))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Revenue: KSh ${pnl?.totalRevenue?.toInt() ?: 0}", color = Color(0xFF81C784), fontSize = 11.sp)
                            Text("Feed: KSh ${pnl?.feedCost?.toInt() ?: 0}", color = Color(0xFFE57373), fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Health: KSh ${pnl?.healthCost?.toInt() ?: 0}", color = Color(0xFFE57373), fontSize = 11.sp)
                            Text("Overheads: KSh ${pnl?.otherExpensesCost?.toInt() ?: 0}", color = Color(0xFFE57373), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── 3. QUICK ACTIONS (Module Card Design with Outlines) ────────────────
        item {
            Text("Quick Actions", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashQuickAction("Pigs", Icons.Default.Pets, Color(0xFF81C784), Color(0xFF1A3E22), { navController.navigate("pigs") }, Modifier.weight(1f))
                DashQuickAction("Feed", Icons.Default.Grass, Color(0xFFFFD54F), Color(0xFF3E3A1A), { navController.navigate("feed") }, Modifier.weight(1f))
                DashQuickAction("Health", Icons.Default.LocalHospital, Color(0xFFE57373), Color(0xFF3E1A1A), { navController.navigate("health") }, Modifier.weight(1f))
                DashQuickAction("Market", Icons.Default.ShoppingCart, Color(0xFFFFB74D), Color(0xFF3E2D1A), { navController.navigate("market") }, Modifier.weight(1f))
            }
        }

        // ── 4. ALERTS PREVIEW ──────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Alerts (${alerts.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = { navController.navigate("alerts") }) {
                    Text("View All", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                        Text("All caught up! No active alerts.", color = Color(0xFF8B949E))
                    }
                }
            }
        } else {
            items(alerts.take(4)) { alert ->
                AlertItemCard(
                    alert = alert,
                    onDone = { viewModel.markAlertDone(alert.id) },
                    onNavigateToPig = { pigId -> navController.navigate("pig_detail/$pigId") }
                )
            }
        }

        // ── 5. RECENT PIGS PREVIEW ─────────────────────────────────────────────
        item { Text("Recent Pigs", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        items(pigs.take(3)) { pig ->
            CompactPigRow(pig = pig, onClick = { navController.navigate("pig_detail/${pig.id}") })
        }
        if (pigs.isNotEmpty()) {
            item {
                TextButton(onClick = { navController.navigate("pigs") }, modifier = Modifier.fillMaxWidth()) {
                    Text("View All ${pigs.size} Pigs →", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun HeroSubMetricCard(
    label: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Column {
                Text(label, fontSize = 11.sp, color = Color(0xFFC9D1D9), fontWeight = FontWeight.Medium, maxLines = 1)
                Text(subtitle, fontSize = 9.sp, color = Color(0xFF8B949E), maxLines = 1)
            }
        }
    }
}

@Composable
fun DashQuickAction(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Text(label, fontSize = 12.sp, color = Color(0xFFE6EDF3), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CompactPigRow(pig: PigEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun DashKpiCard(title: String, value: String, subtitle: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF6E7681))
        }
    }
}

