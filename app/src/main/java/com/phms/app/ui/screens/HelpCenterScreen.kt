package com.phms.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.phms.app.ui.viewmodel.MainViewModel

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String,
    val actionRoute: String? = null,
    val actionLabel: String? = null
)

data class TourStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val icon: String,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    viewModel: MainViewModel,
    navController: NavController,
    autoStartTour: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedFaqId by remember { mutableStateOf<String?>(null) }
    var showGuidedTour by remember { mutableStateOf(autoStartTour) }

    val faqList = remember {
        listOf(
            FaqItem(
                id = "pigs_1",
                category = "Pigs & Herd",
                question = "How do I add a new pig with a camera photo?",
                answer = "Go to the Pigs screen and tap the '+' button. Tap the pig photo avatar to take a picture directly with your camera or select from gallery. Fill in tag number, breed, sex, and parentage details.",
                actionRoute = "add_edit_pig/-1",
                actionLabel = "Add Pig Now"
            ),
            FaqItem(
                id = "pigs_2",
                category = "Pigs & Herd",
                question = "How does automatic growth stage promotion work?",
                answer = "Promotions are weight-based: Piglet (<15kg), Weaner (15-30kg), Grower (30-60kg), and Finisher (60kg+). Whenever you log a new weight, the system automatically evaluates and promotes the pig if it reaches a new weight threshold.",
                actionRoute = "pigs",
                actionLabel = "View Herd"
            ),
            FaqItem(
                id = "pigs_3",
                category = "Pigs & Herd",
                question = "How do I link Mother Sow and Father Boar?",
                answer = "When adding or editing a pig, scroll to the 'Parentage & Pedigree Lineage' section and select the Mother Sow and Father Boar from the dropdown lists.",
                actionRoute = "pigs",
                actionLabel = "Go to Pigs"
            ),
            FaqItem(
                id = "feed_1",
                category = "Feed & Nutrition",
                question = "How do I use Feed Formulator presets?",
                answer = "Open the Feed tab and switch to 'Formulator 🌾'. Tap preset buttons like 'Creep Starter', 'Grower Mash', or 'Finisher Meal' to automatically balance energy, protein, and mineral ratios for your target batch weight.",
                actionRoute = "feed",
                actionLabel = "Open Feed Formulator"
            ),
            FaqItem(
                id = "feed_2",
                category = "Feed & Nutrition",
                question = "How does daily feeding automatically reduce stock?",
                answer = "Tap '🥣 Log Feeding' at the top of the Feed screen. Select your feed item/bag, feeding time, amount per pig, and number of pigs. Upon confirming, the total feed weight is automatically deducted from your stock inventory.",
                actionRoute = "feed",
                actionLabel = "Log Daily Feeding"
            ),
            FaqItem(
                id = "feed_3",
                category = "Feed & Nutrition",
                question = "Can I add commercial shop-bought feed bags?",
                answer = "Yes! In the Feed tab under Inventory, tap 'Add Feed Stock / Bag'. Select 'Commercial Premix', enter the brand name, bag size in kg (e.g. 70kg), and bag price (e.g. KSh 3,200).",
                actionRoute = "feed",
                actionLabel = "Manage Inventory"
            ),
            FaqItem(
                id = "health_1",
                category = "Health & Vet",
                question = "How do I log a health event for a specific single pig?",
                answer = "Open the pig's detail page and tap 'Log Health 💉'. Alternatively, in the Health screen, tap 'Log Event', select 'Single Pig' as the target scope, and pick the pig from the dropdown list.",
                actionRoute = "health",
                actionLabel = "Health Module"
            ),
            FaqItem(
                id = "health_2",
                category = "Health & Vet",
                question = "How does drug withdrawal period blocking work?",
                answer = "When you log a medical treatment with withdrawal days, the pig is automatically placed under withdrawal monitor. Active withdrawal pigs are blocked from market sales until the withdrawal period expires.",
                actionRoute = "health",
                actionLabel = "View Health Monitor"
            ),
            FaqItem(
                id = "sales_1",
                category = "Sales & Market",
                question = "Why are complete buyer details saved during a sale?",
                answer = "Recording buyer name, phone, type, and location builds your farm's buyer network directory and ensures sales records and P&L financial statements accurately attribute income.",
                actionRoute = "market",
                actionLabel = "Record Sale"
            ),
            FaqItem(
                id = "reports_1",
                category = "Financials",
                question = "How do I export Profit & Loss reports as PDF?",
                answer = "Navigate to More -> Financial Reports. Select your date range (e.g., This Month, 3 Months), view the breakdown, and tap 'Export as PDF' to generate a downloadable PDF report.",
                actionRoute = "reports",
                actionLabel = "Financial Reports"
            )
        )
    }

    val categories = listOf("All", "Pigs & Herd", "Feed & Nutrition", "Health & Vet", "Sales & Market", "Financials")

    val filteredFaqs = faqList.filter { faq ->
        (selectedCategory == "All" || faq.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                        faq.question.contains(searchQuery, ignoreCase = true) ||
                        faq.answer.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            TopAppBar(
                title = { Text("Help Center & FAQ", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showGuidedTour = true }) {
                        Icon(Icons.Default.Explore, "Guided Tour", tint = Color(0xFF4CAF50))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161B22))
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Tour Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGuidedTour = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B281B)),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Explore, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Take Interactive Guided Tour", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Step-by-step interactive walkthrough of all key farm features", color = Color(0xFF81C784), fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF4CAF50))
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search help topics, questions, features...", color = Color(0xFF6E7681)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF6E7681)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Color(0xFF6E7681))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color(0xFF30363D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF161B22),
                            unfocusedContainerColor = Color(0xFF161B22)
                        )
                    )
                }

                // Category Chips
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
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

                item {
                    Text(
                        "Frequently Asked Questions (${filteredFaqs.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (filteredFaqs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No help topics match your search query.", color = Color(0xFF6E7681), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(filteredFaqs) { faq ->
                        val isExpanded = expandedFaqId == faq.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedFaqId = if (isExpanded) null else faq.id },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                            border = if (isExpanded) BorderStroke(1.dp, Color(0xFF4CAF50)) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            if (isExpanded) Icons.Default.Help else Icons.Default.HelpOutline,
                                            null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(faq.question, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null,
                                        tint = Color(0xFF6E7681)
                                    )
                                }
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        HorizontalDivider(color = Color(0xFF21262D))
                                        Spacer(Modifier.height(10.dp))
                                        Text(faq.answer, color = Color(0xFF8B949E), fontSize = 13.sp, lineHeight = 20.sp)
                                        if (faq.actionRoute != null && faq.actionLabel != null) {
                                            Spacer(Modifier.height(12.dp))
                                            Button(
                                                onClick = { navController.navigate(faq.actionRoute) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(faq.actionLabel, color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

        // Guided Tour Dialog Overlay
        if (showGuidedTour) {
            GuidedTourOverlay(onDismiss = { showGuidedTour = false }, navController = navController)
        }
    }
}

@Composable
fun GuidedTourOverlay(onDismiss: () -> Unit, navController: NavController) {
    val tourSteps = remember {
        listOf(
            TourStep(1, "Herd & Individual Pig Tracking", "Add pigs with camera photo capture, tag ear numbers, mother/father lineage, and log weights for automatic stage promotions.", "🐷", "pigs"),
            TourStep(2, "Balanced Feed & Stock Deduction", "Use presets to auto-fill meal ratios, add commercial shop bags, and log daily feeding to automatically deduct stock inventory.", "🌾", "feed"),
            TourStep(3, "Single Pig & Herd Health", "Log vaccinations, dewormings, and medical treatments for a single pig or entire herd, with automatic drug withdrawal period monitoring.", "💉", "health"),
            TourStep(4, "Pedigree & Breeding Hub", "Track Sows, Boars, farrowings, and direct offspring litters to manage breeding lineage.", "🧬", "breeding"),
            TourStep(5, "Market Sales & Buyer Directory", "Record sales capturing full buyer names, phone numbers, and location details to generate profit & loss statements.", "💰", "market"),
            TourStep(6, "PDF Financial Reports", "Export date-filtered P&L financial reports directly to PDF for offline auditing.", "📊", "reports")
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = tourSteps[currentStepIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(step.icon, fontSize = 24.sp)
                Column {
                    Text("Step ${step.stepNumber} of ${tourSteps.size}", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(step.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(step.description, color = Color(0xFF8B949E), fontSize = 14.sp, lineHeight = 20.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tourSteps.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (i == currentStepIndex) Color(0xFF4CAF50) else Color(0xFF30363D))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentStepIndex < tourSteps.size - 1) {
                    Button(
                        onClick = { currentStepIndex++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Next Step →", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            onDismiss()
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Finish Tour 🎉", fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    navController.navigate(step.route)
                }
            ) {
                Text("Go to ${step.title.split(" ").first()}", color = Color(0xFF81C784), fontSize = 12.sp)
            }
        }
    )
}
