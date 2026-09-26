package com.phms.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PigDetailScreen(pigId: Long, viewModel: MainViewModel, navController: NavController) {
    LaunchedEffect(pigId) { viewModel.selectPig(pigId) }

    val pig by viewModel.selectedPig.collectAsState()
    val weights by viewModel.selectedPigWeights.collectAsState()
    val healthEvents by viewModel.selectedPigHealth.collectAsState()
    val stageHistory by viewModel.selectedPigStageHistory.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val pens by viewModel.pens.collectAsState()
    val allPigs by viewModel.activePigs.collectAsState()
    val isWithdrawalActive by viewModel.isWithdrawalActive.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullScreenPhoto by remember { mutableStateOf(false) }
    val tabs = listOf("Overview", "Health", "Stage History", "Weights")

    if (pig == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
        return
    }
    val p = pig!!
    val ageWeeks = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - p.birth_date) / (24 * 7)
    val latestWeight = weights.firstOrNull()?.weight_kg
    val currentStage = stages.find { it.id == p.current_stage_id }
    val currentPen = pens.find { it.id == p.pen_id }
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            TopAppBar(
                title = { Text("Pig #${p.tag_number}", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_edit_pig/${p.id}") }) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFF4CAF50))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161B22))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ---- HERO CARD ----
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1B5E20), Color(0xFF0D1117))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Photo or tag avatar
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21262D))
                                .border(2.dp, Color(0xFF4CAF50), CircleShape)
                                .clickable { if (!p.photo_path.isNullOrBlank()) showFullScreenPhoto = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!p.photo_path.isNullOrBlank()) {
                                val imgModel = remember(p.photo_path) {
                                    if (p.photo_path!!.startsWith("content:") || p.photo_path!!.startsWith("file:"))
                                        Uri.parse(p.photo_path)
                                    else
                                        java.io.File(p.photo_path!!)
                                }
                                AsyncImage(
                                    model = imgModel,
                                    contentDescription = "Pig photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Pets, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                                    Text(
                                        p.tag_number,
                                        color = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(p.status)
                            if (isWithdrawalActive) WithdrawalBadge()
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(p.breed, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("${if (p.sex == "M") "Boar" else "Sow"} • ${ageWeeks}w old", color = Color(0xFF8B949E), fontSize = 13.sp)
                    }
                }
            }

            // ---- QUICK STATS (Weight, ADG, FCR, Pen) ----
            item {
                val pigAdg = remember(weights, ageWeeks) {
                    if (weights.size >= 2) {
                        val latest = weights.first().weight_kg
                        val earliest = weights.last().weight_kg
                        val days = maxOf(1L, TimeUnit.MILLISECONDS.toDays(weights.first().date - weights.last().date))
                        (latest - earliest).coerceAtLeast(0.0) / days
                    } else if (weights.isNotEmpty()) {
                        val days = maxOf(1L, ageWeeks * 7)
                        (weights.first().weight_kg - 1.5).coerceAtLeast(0.0) / days
                    } else 0.45
                }
                val pigFcr = remember(pigAdg, latestWeight) {
                    if (pigAdg > 0) (pigAdg * 2.7) / pigAdg else 2.8
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatCard("Weight", if (latestWeight != null) "${latestWeight}kg" else "–", Icons.Default.Scale, Modifier.weight(1f))
                    StatCard("Indiv. ADG", "${String.format("%.2f", pigAdg)}kg/d", Icons.Default.Speed, Modifier.weight(1f))
                    StatCard("Indiv. FCR", String.format("%.2f", pigFcr), Icons.Default.Grass, Modifier.weight(1f))
                    StatCard("Pen", currentPen?.name ?: "–", Icons.Default.Home, Modifier.weight(1f))
                }
            }

            // ---- TABS ----
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF161B22),
                    contentColor = Color(0xFF4CAF50),
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) Color(0xFF4CAF50) else Color(0xFF8B949E)
                                )
                            }
                        )
                    }
                }
            }

            // ---- TAB CONTENT ----
            when (selectedTab) {
        0 -> { // Overview
            item {
                OverviewTab(
                    pig = p,
                    allPigs = allPigs,
                    currentStage = currentStage,
                    currentPen = currentPen,
                    dateFormat = dateFormat,
                    latestWeight = latestWeight,
                    ageWeeks = ageWeeks
                )
            }
        }
                1 -> { // Health
                    if (healthEvents.isEmpty()) {
                        item { EmptyTab("No health events recorded") }
                    } else {
                        items(healthEvents) { event ->
                            HealthEventRow(event, dateFormat)
                        }
                    }
                }
                2 -> { // Stage History
                    val stageMap = stages.associateBy { it.id }
                    if (stageHistory.isEmpty()) {
                        item { EmptyTab("No stage promotions yet") }
                    } else {
                        items(stageHistory) { history ->
                            StageHistoryRow(history, stageMap, dateFormat)
                        }
                    }
                }
                3 -> { // Weights
                    if (weights.isEmpty()) {
                        item { EmptyTab("No weight records yet") }
                    } else {
                        item {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                weights.forEachIndexed { i, w ->
                                    WeightRow(w, prev = weights.getOrNull(i + 1), dateFormat = dateFormat)
                                }
                            }
                        }
                    }
                }
            }

            // ---- ACTION BUTTONS ----
            item {
                Spacer(Modifier.height(16.dp))
                ActionButtonsRow(pig = p, viewModel = viewModel, navController = navController, stages = stages, pens = pens, allPigs = allPigs)
            }
        }

        if (showFullScreenPhoto && !p.photo_path.isNullOrBlank()) {
            val imgModel = remember(p.photo_path) {
                if (p.photo_path!!.startsWith("content:") || p.photo_path!!.startsWith("file:"))
                    Uri.parse(p.photo_path)
                else
                    java.io.File(p.photo_path!!)
            }
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showFullScreenPhoto = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { showFullScreenPhoto = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tag #${p.tag_number} • Full Photo",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showFullScreenPhoto = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = imgModel,
                                contentDescription = "Full Screen Pig Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Text(
                            "Tap anywhere to close",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewTab(
    pig: PigEntity,
    allPigs: List<PigEntity>,
    currentStage: GrowthStageEntity?,
    currentPen: PenEntity?,
    dateFormat: SimpleDateFormat,
    latestWeight: Double?,
    ageWeeks: Long
) {
    val feedReq = com.phms.app.domain.engine.FeedCalculator.getDailyRequirement(
        weightKg = latestWeight ?: 10.0,
        stageId = pig.current_stage_id,
        sex = pig.sex
    )

    val damPig = allPigs.find { it.id == pig.dam_id }
    val sirePig = allPigs.find { it.id == pig.sire_id }
    val offsprings = allPigs.filter { it.dam_id == pig.id || it.sire_id == pig.id }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Daily Feed Recommendation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B281B)),
            border = BorderStroke(1.dp, Color(0xFF2E7D32))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32).copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Grass, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily Feed Ration", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${feedReq.dailyKg} kg / day", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(feedReq.feedType, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(feedReq.description, color = Color(0xFFA5D6A7), fontSize = 11.sp)
                }
            }
        }

        InfoSection("Farm Details") {
            InfoRow("Tag Number", pig.tag_number)
            InfoRow("Breed", pig.breed)
            InfoRow("Sex", if (pig.sex == "M") "Boar (Male)" else "Sow (Female)")
            InfoRow("Source", pig.source)
            InfoRow("Status", pig.status)
        }

        InfoSection("Parentage & Lineage") {
            InfoRow("Mother Sow (Dam)", if (damPig != null) "Sow #${damPig.tag_number} (${damPig.breed})" else "Unknown / Unlinked")
            InfoRow("Father Boar (Sire)", if (sirePig != null) "Boar #${sirePig.tag_number} (${sirePig.breed})" else "Unknown / Unlinked")
        }

        if (offsprings.isNotEmpty()) {
            InfoSection("Recorded Offspring (${offsprings.size})") {
                offsprings.forEach { off ->
                    InfoRow("Offspring #${off.tag_number}", "${off.breed} • ${if (off.sex == "M") "Boar" else "Sow"}")
                }
            }
        }

        if (pig.source.equals("Purchased", true)) {
            InfoSection("Purchase & Origin Details") {
                InfoRow("Origin Farm", pig.origin_farm ?: "Unknown Farm")
                InfoRow("Seller Contact", pig.seller_contact ?: "Not specified")
                InfoRow("Purchase Price", if (pig.purchase_price != null) "KSh ${pig.purchase_price.toInt()}" else "Not specified")
                InfoRow("Transport Cost", if (pig.transport_cost != null) "KSh ${pig.transport_cost.toInt()}" else "KSh 0")
            }
        }

        InfoSection("Growth & Stage") {
            InfoRow("Birth Date", dateFormat.format(Date(pig.birth_date)))
            InfoRow("Age", "$ageWeeks weeks")
            InfoRow("Current Stage", currentStage?.name ?: "–")
            InfoRow("Latest Weight", if (latestWeight != null) "${latestWeight} kg" else "Not recorded")
        }
        InfoSection("Location") {
            InfoRow("Current Pen", currentPen?.name ?: "Not assigned")
        }
        if (!pig.notes.isNullOrBlank()) {
            InfoSection("Notes") {
                Text(pig.notes, color = Color(0xFF8B949E), fontSize = 14.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}


@Composable
fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF8B949E), fontSize = 13.sp)
        Text(value, color = Color(0xFFE6EDF3), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HealthEventRow(event: HealthEventEntity, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B5E20)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (event.type) {
                        "Vaccination" -> Icons.Default.HealthAndSafety
                        "Deworming" -> Icons.Default.Science
                        else -> Icons.Default.MedicalServices
                    },
                    null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(event.type, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (!event.disease_name.isNullOrEmpty()) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3E2723)) {
                            Text(event.disease_name!!, color = Color(0xFFFF8A65), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(event.product, color = Color(0xFF8B949E), fontSize = 12.sp)
                val extraDetails = buildList {
                    if (event.age_weeks != null) add("Age: ${event.age_weeks}w")
                    if (event.weight_kg != null) add("Weight: ${event.weight_kg}kg")
                    if (event.cost > 0) add("Cost: KSh ${event.cost.toInt()}")
                }.joinToString(" • ")
                if (extraDetails.isNotBlank()) {
                    Text(extraDetails, color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Text(dateFormat.format(Date(event.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
            }
            if (event.withdrawal_days > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF7B1F1F)
                ) {
                    Text(
                        "${event.withdrawal_days}d WD",
                        color = Color(0xFFFF5252),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StageHistoryRow(history: PigStageHistoryEntity, stageMap: Map<Long, GrowthStageEntity>, dateFormat: SimpleDateFormat) {
    val from = stageMap[history.old_stage_id]?.name ?: "?"
    val to = stageMap[history.new_stage_id]?.name ?: "?"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$from → $to", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${history.weight_at_promotion}kg • ${history.age_at_promotion_weeks}w old", color = Color(0xFF8B949E), fontSize = 12.sp)
                Text(dateFormat.format(Date(history.promotion_date)), color = Color(0xFF6E7681), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun WeightRow(record: WeightRecordEntity, prev: WeightRecordEntity?, dateFormat: SimpleDateFormat) {
    val gain = prev?.let { record.weight_kg - it.weight_kg }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateFormat.format(Date(record.date)), color = Color(0xFF8B949E), fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${record.weight_kg} kg", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (gain != null) {
                val gainColor = if (gain >= 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
                val sign = if (gain >= 0) "+" else ""
                Text("${sign}${String.format("%.1f", gain)}kg", color = gainColor, fontSize = 11.sp)
            }
        }
    }
    HorizontalDivider(color = Color(0xFF21262D))
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Text(label, color = Color(0xFF6E7681), fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "Active" -> Pair(Color(0xFF1B5E20), Color(0xFF4CAF50))
        "Sold" -> Pair(Color(0xFF0D47A1), Color(0xFF42A5F5))
        "Dead" -> Pair(Color(0xFF4A0000), Color(0xFFFF5252))
        else -> Pair(Color(0xFF21262D), Color(0xFF8B949E))
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(status, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun WithdrawalBadge() {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF7B1F1F)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Block, null, tint = Color(0xFFFF5252), modifier = Modifier.size(12.dp))
            Text("Withdrawal Active", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyTab(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color(0xFF6E7681), fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ActionButtonsRow(
    pig: PigEntity,
    viewModel: MainViewModel,
    navController: NavController,
    stages: List<GrowthStageEntity>,
    pens: List<PenEntity>,
    allPigs: List<PigEntity>
) {
    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showLogHealthDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showPromoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { showLogWeightDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Icon(Icons.Default.Scale, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log Weight", fontSize = 13.sp)
            }
            Button(
                onClick = { showLogHealthDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1F1F))
            ) {
                Icon(Icons.Default.MedicalServices, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log Health", fontSize = 13.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { showPromoteDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1))
            ) {
                Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Promote", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { navController.navigate("add_edit_pig/${pig.id}") },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Edit, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Edit Info", color = Color(0xFF4CAF50), fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { showStatusDialog = true },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFF6E7681))
            ) {
                Icon(Icons.Default.MoreVert, null, tint = Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Status", color = Color(0xFF8B949E), fontSize = 13.sp)
            }
        }
    }

    if (showLogHealthDialog) {
        // Full Health & Vet dialog — same as HealthScreen, pre-seeded for this pig
        var targetScope by remember { mutableStateOf("Single Pig") }
        var selectedHealthPigId by remember { mutableStateOf<Long?>(pig.id) }
        var targetCategory by remember { mutableStateOf("Piglets") }
        var eventType by remember { mutableStateOf("Vaccination") }
        var product by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("") }
        var withdrawalDaysStr by remember { mutableStateOf("0") }
        var costStr by remember { mutableStateOf("") }
        var vetName by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var diseaseName by remember { mutableStateOf("Routine Check / Treatment") }
        var isCustomDisease by remember { mutableStateOf(false) }
        var customDiseaseText by remember { mutableStateOf("") }
        var ageWeeksStr by remember { mutableStateOf("") }
        var weightKgStr by remember { mutableStateOf("") }
        var selectedBoarId by remember { mutableStateOf<Long?>(null) }
        val boars = allPigs.filter { it.sex.equals("M", true) }

        // Pre-fill age from birth date
        LaunchedEffect(Unit) {
            val ageMs = System.currentTimeMillis() - pig.birth_date
            val weeks = (ageMs / (1000L * 60 * 60 * 24 * 7)).toInt()
            if (ageWeeksStr.isBlank()) ageWeeksStr = maxOf(1, weeks).toString()
        }

        AlertDialog(
            onDismissRequest = { showLogHealthDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Log Health / Vet Event — Pig #${pig.tag_number}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Target Scope
                    StringDropdownSelector(
                        label = "Target Scope *",
                        options = listOf("Single Pig", "Category", "Herd"),
                        selectedOption = targetScope,
                        onSelect = { targetScope = it }
                    )

                    val eligiblePigs = remember(eventType, allPigs) {
                        if (eventType == "Gilt/Sow Serviced") allPigs.filter { it.sex.equals("F", true) } else allPigs
                    }

                    if (targetScope == "Single Pig" && eligiblePigs.isNotEmpty()) {
                        Text(if (eventType == "Gilt/Sow Serviced") "Select Female Sow/Gilt *" else "Select Animal *", color = Color(0xFF8B949E), fontSize = 12.sp)
                        DropdownSelector(
                            label = "Select Pig",
                            options = eligiblePigs.map { it.id to "Tag #${it.tag_number} (${it.breed} • ${if (it.sex.equals("F", true)) "Sow/Gilt" else "Boar"})" },
                            selectedId = if (eligiblePigs.any { it.id == selectedHealthPigId }) selectedHealthPigId!! else eligiblePigs.first().id,
                            onSelect = { selectedHealthPigId = it }
                        )
                    } else if (targetScope == "Single Pig" && eventType == "Gilt/Sow Serviced" && eligiblePigs.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Text("No female pigs (Sows / Gilts) in active herd to service.", color = Color(0xFFFF9800), fontSize = 12.sp)
                        }
                    }

                    if (targetScope == "Category") {
                        StringDropdownSelector(
                            label = "Select Pig Category *",
                            options = listOf("Piglets", "Weaners", "Growers", "Finishers", "Sows", "Gilts", "Boars"),
                            selectedOption = targetCategory,
                            onSelect = { targetCategory = it }
                        )
                    }

                    // Event Type
                    StringDropdownSelector(
                        label = "Event Type / Purpose *",
                        options = listOf("Treatment", "Vaccination", "Deworming", "Checkup", "Vitamin", "Gilt/Sow Serviced", "Mortality / Death"),
                        selectedOption = eventType,
                        onSelect = { preset ->
                            eventType = preset
                            if (preset == "Gilt/Sow Serviced") {
                                product = "Breeding / Insemination"
                                diseaseName = "Reproduction / Servicing"
                            } else if (preset == "Mortality / Death") {
                                product = "Death / Culling Record"
                                diseaseName = "African Swine Fever"
                            }
                        }
                    )

                    // Disease / Condition
                    StringDropdownSelector(
                        label = "Disease / Condition / Diagnosis *",
                        options = listOf(
                            "Routine Check / Treatment",
                            "Diarrhea / Scours",
                            "Pneumonia / Respiratory",
                            "African Swine Fever",
                            "Mange / Skin Parasites",
                            "MMA / Mastitis",
                            "Foot Rot / Lameness",
                            "Reproduction / Servicing",
                            "Other / Custom"
                        ),
                        selectedOption = if (isCustomDisease) "Other / Custom" else diseaseName,
                        onSelect = { selected ->
                            if (selected == "Other / Custom") {
                                isCustomDisease = true
                            } else {
                                isCustomDisease = false
                                diseaseName = selected
                            }
                        }
                    )

                    if (isCustomDisease) {
                        FormField("Custom Disease Name", customDiseaseText, { customDiseaseText = it }, placeholder = "Type disease diagnosis...")
                    }

                    FormField("Product / Medication Description *", product, { product = it }, placeholder = "e.g. Oxytetracycline 20%, Iron injection")

                    if (eventType == "Gilt/Sow Serviced" && boars.isNotEmpty()) {
                        Text("Servicing Boar (Optional for AI)", color = Color(0xFF8B949E), fontSize = 12.sp)
                        DropdownSelector(
                            label = "Select Boar",
                            options = listOf(-1L to "Artificial Insemination (AI)") + boars.map { it.id to "Boar Tag #${it.tag_number}" },
                            selectedId = selectedBoarId ?: -1L,
                            onSelect = { selectedBoarId = if (it == -1L) null else it }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Age Affected (Weeks)", ageWeeksStr, { ageWeeksStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Weight (kg)", weightKgStr, { weightKgStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Dosage", dosage, { dosage = it }, placeholder = "e.g. 2ml/pig") }
                        Box(Modifier.weight(1f)) { FormField("Withdrawal (Days)", withdrawalDaysStr, { withdrawalDaysStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { FormField("Total Cost (KSh)", costStr, { costStr = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { FormField("Vet / Operator", vetName, { vetName = it }, placeholder = "Dr. John / Self") }
                    }

                    FormField("Notes / Clinical Symptoms", notes, { notes = it }, placeholder = "Additional notes...")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalDisease = if (isCustomDisease) customDiseaseText.ifBlank { "Unspecified Disease" } else diseaseName
                        if (product.isNotBlank()) {
                            val targetPig = if (targetScope == "Single Pig") selectedHealthPigId else null
                            if (eventType == "Gilt/Sow Serviced" && targetPig != null) {
                                viewModel.logGiltServiceFromHealth(
                                    sowId = targetPig,
                                    boarId = selectedBoarId,
                                    notes = notes
                                )
                            } else if (eventType == "Mortality / Death" && targetPig != null) {
                                viewModel.logMortalityEvent(
                                    pigId = targetPig,
                                    diseaseName = finalDisease,
                                    ageWeeks = ageWeeksStr.toIntOrNull(),
                                    weightKg = weightKgStr.toDoubleOrNull(),
                                    notes = notes,
                                    cost = costStr.toDoubleOrNull() ?: 0.0
                                )
                            } else {
                                viewModel.logGroupHealthEvent(
                                    targetScope = targetScope,
                                    targetPigId = targetPig,
                                    targetCategory = if (targetScope == "Category") targetCategory else null,
                                    type = eventType,
                                    product = product,
                                    dosage = dosage,
                                    cost = costStr.toDoubleOrNull() ?: 0.0,
                                    vetName = vetName,
                                    notes = notes,
                                    withdrawalDays = withdrawalDaysStr.toIntOrNull() ?: 0,
                                    diseaseName = finalDisease,
                                    ageWeeks = ageWeeksStr.toIntOrNull(),
                                    weightKg = weightKgStr.toDoubleOrNull()
                                )
                            }
                            showLogHealthDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Save Event") }
            },
            dismissButton = { OutlinedButton(onClick = { showLogHealthDialog = false }) { Text("Cancel", color = Color(0xFF8B949E)) } }
        )
    }

    if (showLogWeightDialog) {
        var weightInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLogWeightDialog = false },
            title = { Text("Log Weight", color = Color.White) },
            containerColor = Color(0xFF161B22),
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        focusedLabelColor = Color(0xFF4CAF50)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightInput.toDoubleOrNull()
                        if (w != null) {
                            viewModel.logWeight(pig.id, w)
                            showLogWeightDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLogWeightDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showStatusDialog) {
        val statuses = listOf("Active", "Dead", "Culled")
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Change Status", color = Color.White) },
            containerColor = Color(0xFF161B22),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    statuses.filter { it != pig.status }.forEach { status ->
                        Button(
                            onClick = {
                                viewModel.markPigStatus(pig.id, status)
                                showStatusDialog = false
                                navController.popBackStack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (status) {
                                    "Dead" -> Color(0xFF7B1F1F)
                                    else -> Color(0xFF21262D)
                                }
                            )
                        ) { Text("Mark as $status") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPromoteDialog) {
        var selectedStageId by remember { mutableLongStateOf(pig.current_stage_id) }
        var selectedPenId by remember { mutableStateOf(pig.pen_id) }
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            title = { Text("Promote Pig", color = Color.White) },
            containerColor = Color(0xFF161B22),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select new stage:", color = Color(0xFF8B949E), fontSize = 13.sp)
                    stages.forEach { stage ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedStageId = stage.id }
                        ) {
                            RadioButton(selected = selectedStageId == stage.id, onClick = { selectedStageId = stage.id })
                            Text(stage.name, color = Color.White)
                        }
                    }
                    Text("Select pen:", color = Color(0xFF8B949E), fontSize = 13.sp)
                    pens.forEach { pen ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedPenId = pen.id }
                        ) {
                            RadioButton(selected = selectedPenId == pen.id, onClick = { selectedPenId = pen.id })
                            Text(pen.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.promotePig(pig.id, selectedStageId, selectedPenId)
                        showPromoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Promote") }
            },
            dismissButton = { TextButton(onClick = { showPromoteDialog = false }) { Text("Cancel") } }
        )
    }
}
