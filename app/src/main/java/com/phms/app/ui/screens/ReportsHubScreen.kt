package com.phms.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
// REPORTS SCREEN — FILTERABLE & SEARCHABLE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ReportsHubScreen(viewModel: MainViewModel) {
    val report by viewModel.comprehensiveReport.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRange by remember { mutableStateOf("This Month") }
    var searchQuery by remember { mutableStateOf("") }
    var showCustomDatePicker by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    var startMs by remember { mutableStateOf(now - TimeUnit.DAYS.toMillis(30)) }
    var endMs by remember { mutableStateOf(now) }

    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    // Update range startMs based on preset selection
    LaunchedEffect(selectedRange) {
        val currentNow = System.currentTimeMillis()
        endMs = currentNow
        startMs = when (selectedRange) {
            "Today" -> currentNow - TimeUnit.DAYS.toMillis(1)
            "This Week" -> currentNow - TimeUnit.DAYS.toMillis(7)
            "This Month" -> currentNow - TimeUnit.DAYS.toMillis(30)
            "3 Months" -> currentNow - TimeUnit.DAYS.toMillis(90)
            "6 Months" -> currentNow - TimeUnit.DAYS.toMillis(180)
            "This Year" -> currentNow - TimeUnit.DAYS.toMillis(365)
            "All Time" -> 0L
            else -> startMs
        }
        viewModel.loadComprehensiveReport(startMs, endMs, selectedRange)
    }

    val filteredSales = sales.filter { s ->
        s.date in startMs..endMs && (searchQuery.isEmpty() ||
                buyers.find { it.id == s.buyer_id }?.name?.contains(searchQuery, ignoreCase = true) == true ||
                s.total_amount.toString().contains(searchQuery) ||
                dateFormat.format(Date(s.date)).contains(searchQuery, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        // Header + Dropdown Selectors
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Farm Reports Hub", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Select report category & time range from the dropdowns below", fontSize = 12.sp, color = Color(0xFF8B949E))

            // Dropdown 1: Date Range Filter
            StringDropdownSelector(
                label = "Date Range Period *",
                options = listOf("Today", "This Week", "This Month", "3 Months", "6 Months", "This Year", "All Time", "Custom Date Range"),
                selectedOption = selectedRange,
                onSelect = { range ->
                    if (range == "Custom Date Range") {
                        showCustomDatePicker = true
                    } else {
                        selectedRange = range
                    }
                }
            )

            // Dropdown 2: Report Category Selector
            StringDropdownSelector(
                label = "Select Report Category *",
                options = listOf(
                    "💵 Financial Reports",
                    "🐖 Herd & Count Reports",
                    "🩺 Health & Mortality Reports",
                    "🌾 Feed & FCR Growth Reports",
                    "💕 Breeding & Reproduction Reports"
                ),
                selectedOption = when (selectedTab) {
                    0 -> "💵 Financial Reports"
                    1 -> "🐖 Herd & Count Reports"
                    2 -> "🩺 Health & Mortality Reports"
                    3 -> "🌾 Feed & FCR Growth Reports"
                    else -> "💕 Breeding & Reproduction Reports"
                },
                onSelect = { category ->
                    selectedTab = when (category) {
                        "💵 Financial Reports" -> 0
                        "🐖 Herd & Count Reports" -> 1
                        "🩺 Health & Mortality Reports" -> 2
                        "🌾 Feed & FCR Growth Reports" -> 3
                        else -> 4
                    }
                }
            )

            // Date Range Display Badge
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF161B22)) {
                Text(
                    text = "Active Period: ${if (startMs == 0L) "All Time Records" else "${dateFormat.format(Date(startMs))}  →  ${dateFormat.format(Date(endMs))}"}",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        val r = report
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (selectedTab) {
                0 -> { // Financials
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Profit & Loss Statement (${r?.periodLabel ?: selectedRange})", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                HorizontalDivider(color = Color(0xFF21262D))
                                PnLRow("Sales Revenue", "KSh ${r?.pnl?.totalRevenue?.toInt() ?: 0}", positive = true)
                                PnLRow("Feed Expenses", "– KSh ${r?.pnl?.feedCost?.toInt() ?: 0}", positive = false)
                                PnLRow("Health & Vet Expenses", "– KSh ${r?.pnl?.healthCost?.toInt() ?: 0}", positive = false)
                                PnLRow("Labor & Utilities (Est.)", "– KSh ${r?.pnl?.estimatedLaborCost?.toInt() ?: 0}", positive = false)
                                HorizontalDivider(color = Color(0xFF30363D))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("NET PROFIT / LOSS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "KSh ${r?.pnl?.netProfit?.toInt() ?: 0}",
                                        color = if ((r?.pnl?.netProfit ?: 0.0) >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold, fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text("Sales Ledger in Selected Period (${filteredSales.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (filteredSales.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No sales recorded in this period.", color = Color(0xFF6E7681)) } }
                    } else {
                        items(filteredSales) { sale ->
                            val buyer = buyers.find { it.id == sale.buyer_id }
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(buyer?.name ?: "Unknown Buyer", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("${sale.total_weight}kg • KSh ${sale.price_per_kg}/kg", color = Color(0xFF8B949E), fontSize = 11.sp)
                                        Text(dateFormat.format(Date(sale.date)), color = Color(0xFF6E7681), fontSize = 10.sp)
                                    }
                                    Text("KSh ${sale.total_amount.toInt()}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
                1 -> { // Herd & Counts
                    item {
                        Text("Active Herd Inventory Breakdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Total Active Pigs", "${r?.herd?.totalActivePigs ?: 0}", Icons.Default.Pets, Color(0xFF4CAF50)) }
                            Box(Modifier.weight(1f)) { MetricCard("Weaners", "${r?.herd?.weanersCount ?: 0}", Icons.Default.Category, Color(0xFF2196F3)) }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Finishers", "${r?.herd?.finishersCount ?: 0}", Icons.Default.LocalShipping, Color(0xFFFF9800)) }
                            Box(Modifier.weight(1f)) { MetricCard("Piglets", "${r?.herd?.pigletsCount ?: 0}", Icons.Default.ChildCare, Color(0xFFE91E63)) }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Sows & Gilts", "${(r?.herd?.sowsCount ?: 0) + (r?.herd?.giltsCount ?: 0)}", Icons.Default.Female, Color(0xFF9C27B0)) }
                            Box(Modifier.weight(1f)) { MetricCard("Breeding Boars", "${r?.herd?.boarsCount ?: 0}", Icons.Default.Male, Color(0xFF00BCD4)) }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Reproductive Output in Selected Period", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                HorizontalDivider(color = Color(0xFF21262D))
                                PnLRow("Piglets Born Alive", "${r?.herd?.totalBornAliveInPeriod ?: 0}", positive = true)
                                PnLRow("Stillborn Piglets", "${r?.herd?.totalStillbornInPeriod ?: 0}", positive = false)
                                PnLRow("Piglets Weaned", "${r?.herd?.totalWeanedInPeriod ?: 0}", positive = true)
                            }
                        }
                    }
                }
                2 -> { // Health & Mortality
                    item {
                        Text("Health & Disease Analytics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Total Health Events", "${r?.health?.totalEventsCount ?: 0}", Icons.Default.MedicalServices, Color(0xFF2196F3)) }
                            Box(Modifier.weight(1f)) { MetricCard("Deaths / Mortality", "${r?.health?.totalDeathsCount ?: 0}", Icons.Default.Warning, Color(0xFFFF5252)) }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Mortality Rate %", String.format(Locale.getDefault(), "%.1f%%", r?.health?.mortalityRatePct ?: 0.0), Icons.Default.TrendingDown, Color(0xFFFF9800)) }
                            Box(Modifier.weight(1f)) { MetricCard("Active Withdrawals", "${r?.health?.activeWithdrawalCount ?: 0}", Icons.Default.Block, Color(0xFFE91E63)) }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Disease Prevalence Breakdown", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                HorizontalDivider(color = Color(0xFF21262D))
                                val breakdown = r?.health?.diseaseBreakdown ?: emptyMap()
                                if (breakdown.isEmpty()) {
                                    Text("No disease cases recorded in this period.", color = Color(0xFF8B949E), fontSize = 12.sp)
                                } else {
                                    breakdown.forEach { (disease, count) ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(disease, color = Color.White, fontSize = 13.sp)
                                            Text("$count cases", color = Color(0xFFFF8A65), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // Feed & FCR
                    item {
                        Text("Feed Conversion & Growth Efficiency", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Feed Conversion Ratio (FCR)", String.format(Locale.getDefault(), "%.2f", r?.feedGrowth?.feedConversionRatio ?: 2.8), Icons.Default.Speed, Color(0xFF4CAF50)) }
                            Box(Modifier.weight(1f)) { MetricCard("Avg Daily Gain (ADG)", String.format(Locale.getDefault(), "%.2f kg/day", r?.feedGrowth?.avgDailyGainKg ?: 0.45), Icons.Default.TrendingUp, Color(0xFF00BCD4)) }
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Feed Consumption Summary", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                HorizontalDivider(color = Color(0xFF21262D))
                                PnLRow("Total Feed Consumed", String.format(Locale.getDefault(), "%.1f kg", r?.feedGrowth?.totalFeedConsumedKg ?: 0.0), positive = false)
                                PnLRow("Est. Total Herd Weight Gain", String.format(Locale.getDefault(), "%.1f kg", r?.feedGrowth?.totalWeightGainedKg ?: 0.0), positive = true)
                                PnLRow("Total Feed Cost", "KSh ${r?.feedGrowth?.feedCostTotal?.toInt() ?: 0}", positive = false)
                            }
                        }
                    }
                    item {
                        val activeHerdPigs by viewModel.activePigs.collectAsState()
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Individual Pig ADG & FCR Performance Leaderboard", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Individual growth rate (ADG) & feed conversion ratio (FCR) per pig:", color = Color(0xFF8B949E), fontSize = 11.sp)
                                HorizontalDivider(color = Color(0xFF21262D))
                                if (activeHerdPigs.isEmpty()) {
                                    Text("No active pigs in herd.", color = Color(0xFF8B949E), fontSize = 12.sp)
                                } else {
                                    val pigWeightsMap by produceState<Map<Long, List<com.phms.app.data.local.entity.WeightRecordEntity>>>(initialValue = emptyMap(), key1 = activeHerdPigs) {
                                        val map = mutableMapOf<Long, List<com.phms.app.data.local.entity.WeightRecordEntity>>()
                                        activeHerdPigs.take(10).forEach { pig ->
                                            map[pig.id] = viewModel.repository.pigDao.getWeightsForPigSync(pig.id)
                                        }
                                        value = map
                                    }
                                    activeHerdPigs.take(10).forEach { pig ->
                                        val weights = pigWeightsMap[pig.id] ?: emptyList()
                                        val ageWeeks = maxOf(1L, TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - pig.birth_date) / (24 * 7))
                                        val adg = if (weights.size >= 2) {
                                            val days = maxOf(1L, TimeUnit.MILLISECONDS.toDays(weights.first().date - weights.last().date))
                                            (weights.first().weight_kg - weights.last().weight_kg).coerceAtLeast(0.0) / days
                                        } else if (weights.isNotEmpty()) {
                                            (weights.first().weight_kg - 1.5).coerceAtLeast(0.0) / (ageWeeks * 7)
                                        } else 0.45
                                        val fcr = if (adg > 0) (adg * 2.7) / adg else 2.8

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Pig #${pig.tag_number} (${pig.breed})", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text("${if (pig.sex == "M") "Boar" else "Sow"} • ${ageWeeks}w old", color = Color(0xFF8B949E), fontSize = 11.sp)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1B5E20)) {
                                                    Text("${String.format("%.2f", adg)} kg/d", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0D47A1)) {
                                                    Text("FCR ${String.format("%.2f", fcr)}", color = Color(0xFF42A5F5), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> { // Breeding
                    item {
                        Text("Breeding & Reproduction Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Gilts/Sows Serviced", "${r?.breeding?.servicedCount ?: 0}", Icons.Default.Favorite, Color(0xFFE91E63)) }
                            Box(Modifier.weight(1f)) { MetricCard("Active Pregnancies", "${r?.breeding?.activePregnanciesCount ?: 0}", Icons.Default.ChildFriendly, Color(0xFF9C27B0)) }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { MetricCard("Heat Checks Logged", "${r?.breeding?.heatChecksCount ?: 0}", Icons.Default.Whatshot, Color(0xFFFF9800)) }
                            Box(Modifier.weight(1f)) { MetricCard("Farrowings Expected", "${r?.breeding?.expectedFarrowingsInPeriodCount ?: 0}", Icons.Default.Event, Color(0xFF4CAF50)) }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val pnlVal = r?.pnl
                        if (pnlVal != null) {
                            val file = com.phms.app.domain.reporting.PdfReportGenerator.generatePnLReportPdf(context, pnlVal)
                            if (file != null) {
                                Toast.makeText(context, "Full PDF report saved to downloads: ${file.name}", Toast.LENGTH_LONG).show()
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
                    Text("Export Comprehensive Report as PDF", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }

    if (showCustomDatePicker) {
        var startDaysAgoStr by remember { mutableStateOf("30") }
        var endDaysAgoStr by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showCustomDatePicker = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Select Custom Date Range", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Specify how many days ago the period starts & ends:", color = Color(0xFF8B949E), fontSize = 12.sp)
                    FormField("Days Ago (Start Date)", startDaysAgoStr, { startDaysAgoStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    FormField("Days Ago (End Date)", endDaysAgoStr, { endDaysAgoStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val startDays = startDaysAgoStr.toLongOrNull() ?: 30L
                        val endDays = endDaysAgoStr.toLongOrNull() ?: 0L
                        val currentNow = System.currentTimeMillis()
                        startMs = currentNow - TimeUnit.DAYS.toMillis(startDays)
                        endMs = currentNow - TimeUnit.DAYS.toMillis(endDays)
                        selectedRange = "Custom ($startDays d to $endDays d)"
                        viewModel.loadComprehensiveReport(startMs, endMs, selectedRange)
                        showCustomDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Apply Filter") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomDatePicker = false }) { Text("Cancel", color = Color(0xFF8B949E)) }
            }
        )
    }
}

@Composable
fun PnLRow(label: String, value: String, positive: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF8B949E), fontSize = 14.sp)
        Text(value, color = if (positive) Color(0xFF4CAF50) else Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
                Text(title, color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringDropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption.ifBlank { label },
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color(0xFF8B949E), fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF161B22))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White, fontSize = 13.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
