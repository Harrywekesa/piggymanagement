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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.phms.app.data.local.entity.PigEntity
import com.phms.app.domain.engine.SwineFeedingKey
import com.phms.app.ui.viewmodel.MainViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

// ─────────────────────────────────────────────────────────────────────────────
// EDUCATE ME SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EducateMeScreen(viewModel: MainViewModel, navController: NavController) {
    val pigs by viewModel.activePigs.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = Color(0xFF0D1117),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B3A1B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "Educate Me",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Feeding guide, ingredients & combinations",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161B22),
            contentColor = Color(0xFF4CAF50),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF4CAF50)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Feeding Key", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                selectedContentColor = Color(0xFF4CAF50),
                unselectedContentColor = Color(0xFF8B949E)
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Ingredients", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                selectedContentColor = Color(0xFF4CAF50),
                unselectedContentColor = Color(0xFF8B949E)
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Combinations", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                selectedContentColor = Color(0xFF4CAF50),
                unselectedContentColor = Color(0xFF8B949E)
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().padding(top = 2.dp)) {
            when (selectedTab) {
                0 -> SwineFeedingKeyTab(pigs = pigs)
                1 -> IngredientExplainerTab()
                2 -> FeedCombinationsTab()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: INGREDIENT EXPLAINER
// ─────────────────────────────────────────────────────────────────────────────

private data class IngredientInfo(
    val name: String,
    val emoji: String,
    val role: String,
    val typicalPct: String,
    val whyItMatters: String,
    val tip: String,
    val accentColor: Color
)

@Composable
fun IngredientExplainerTab() {
    val ingredients = listOf(
        IngredientInfo(
            name = "Whole Ground Maize",
            emoji = "🌽",
            role = "Primary Energy Source",
            typicalPct = "60–71% of diet",
            whyItMatters = "High starch content drives growth energy. The backbone of every pig ration. Yellow maize also supplies beta-carotene. Grind fine for young pigs, coarse for finishers.",
            tip = "Never exceed 75% — too much maize dilutes protein and can cause fatty deposits in finishers.",
            accentColor = Color(0xFFFFB74D)
        ),
        IngredientInfo(
            name = "Whole Ground Wheat",
            emoji = "🌾",
            role = "Secondary Energy + Fibre",
            typicalPct = "10% of diet",
            whyItMatters = "Provides fibre for gut health alongside energy. Slightly lower energy than maize but improves stool consistency. Good for sows and growers.",
            tip = "Keep at 10% — higher levels can cause pasty stools due to water-binding fibre.",
            accentColor = Color(0xFFD4A373)
        ),
        IngredientInfo(
            name = "Soybean Meal",
            emoji = "🫘",
            role = "Primary Plant Protein",
            typicalPct = "8–18% of diet",
            whyItMatters = "Contains 44–48% crude protein with an excellent amino acid profile (lysine, methionine). Heat-processed to destroy anti-nutritional factors (trypsin inhibitors) found in raw soy. Highest value plant protein for pigs.",
            tip = "Always use meal (processed), never raw whole beans — raw soy reduces feed digestibility by up to 30%.",
            accentColor = Color(0xFF8BC34A)
        ),
        IngredientInfo(
            name = "Sunflower Cake",
            emoji = "🌻",
            role = "Secondary Protein + Fat",
            typicalPct = "5–7% of diet",
            whyItMatters = "By-product of oil extraction. Contains ~28–32% crude protein and residual oil for energy. Reduces diet cost compared to soybean meal. Well-suited for grower and finisher rations.",
            tip = "Use cake (solvent-extracted), not raw seeds or reducing oils — raw sunflower has excess fat that can cause soft carcass fat in finishers.",
            accentColor = Color(0xFFFFCC02)
        ),
        IngredientInfo(
            name = "Fishmeal / Omena",
            emoji = "🐟",
            role = "Best Animal Protein",
            typicalPct = "1–3% of diet",
            whyItMatters = "60–65% crude protein. Richest natural source of lysine, threonine, and B12. Improves piglet immunity and sow milk quality. Lake Victoria omena is the most available locally.",
            tip = "Reduce to 1% in finishers — high inclusion causes fishy taint in pork. Keep fresh; rancid fishmeal is toxic.",
            accentColor = Color(0xFF4DD0E1)
        ),
        IngredientInfo(
            name = "Vegetable Oil",
            emoji = "🛢️",
            role = "Energy Density Booster",
            typicalPct = "0.2–1.5% of diet",
            whyItMatters = "Fat has 2.25× the energy of carbohydrates. Improves palatability, reduces dustiness of feed, and helps fat-soluble vitamin absorption. Critical in starter rations for fast energy.",
            tip = "Keep below 5% total fat in diet — excess causes greasy stools and poor feed conversion.",
            accentColor = Color(0xFFFFD54F)
        ),
        IngredientInfo(
            name = "Feed Lime (Limestone)",
            emoji = "🪨",
            role = "Calcium Source",
            typicalPct = "0.3–0.8% of diet",
            whyItMatters = "Provides calcium for bone mineralisation and sow milk production. Essential for piglet skeletal development. Food-grade limestone (38% Ca) is the cheapest calcium source.",
            tip = "Do not exceed 1% — excess calcium blocks phosphorus and zinc absorption, causing lameness.",
            accentColor = Color(0xFFB0BEC5)
        ),
        IngredientInfo(
            name = "DCP / Bone Meal",
            emoji = "🦴",
            role = "Phosphorus + Calcium Balance",
            typicalPct = "0.2–0.7% of diet",
            whyItMatters = "Dicalcium Phosphate (DCP) provides both available phosphorus and calcium in correct ratio. Phosphorus is critical for energy metabolism (ATP) and bone health. Pigs cannot absorb plant phytate-phosphorus without supplements.",
            tip = "Use alongside lime — DCP alone skews Ca:P ratio. Target 1.2:1 (Ca:P) for growers.",
            accentColor = Color(0xFF90A4AE)
        ),
        IngredientInfo(
            name = "2% Pig Feed Premix",
            emoji = "💊",
            role = "Vitamins & Trace Minerals",
            typicalPct = "2% of diet (fixed)",
            whyItMatters = "Delivers all fat-soluble vitamins (A, D3, E, K), B-complex (B1, B2, B6, B12, folic acid, pantothenic acid), and essential trace minerals (Zn, Mn, Fe, Cu, I, Se). Prevents deficiency diseases, improves growth rate.",
            tip = "Never reduce below 2% — trace mineral deficiencies show up 4–6 weeks later as poor growth, skin issues, or reproductive failure.",
            accentColor = Color(0xFFCE93D8)
        ),
        IngredientInfo(
            name = "Stock / Animal Salt",
            emoji = "🧂",
            role = "Electrolyte Balance",
            typicalPct = "0.1–0.5% of diet",
            whyItMatters = "Provides sodium and chloride for nerve function, acid-base balance, and water intake regulation. Improves feed palatability. Iodised salt also covers iodine needs if no premix iodine.",
            tip = "Never exceed 0.5% — salt toxicity causes brain oedema (salt poisoning) especially when water access is restricted.",
            accentColor = Color(0xFF80DEEA)
        )
    )

    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF122316),
                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                    Text(
                        "Tap any ingredient to expand its full explanation. Based on PHMS recipes using real-world Kenyan feed inputs.",
                        color = Color(0xFFC9D1D9),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(ingredients.size) { idx ->
            val ing = ingredients[idx]
            val isExpanded = expandedIndex == idx
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedIndex = if (isExpanded) null else idx },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) Color(0xFF1A2410) else Color(0xFF161B22)
                ),
                border = BorderStroke(1.dp, if (isExpanded) ing.accentColor.copy(alpha = 0.5f) else Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header row — always visible
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(ing.emoji, fontSize = 24.sp)
                            Column {
                                Text(ing.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = ing.accentColor.copy(alpha = 0.15f)) {
                                        Text(ing.role, color = ing.accentColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text("·", color = Color(0xFF8B949E), fontSize = 10.sp)
                                    Text(ing.typicalPct, color = Color(0xFF8B949E), fontSize = 10.sp)
                                }
                            }
                        }
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Expanded content
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFF30363D))
                            Text(ing.whyItMatters, color = Color(0xFFC9D1D9), fontSize = 12.sp, lineHeight = 17.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E2D14),
                                border = BorderStroke(1.dp, ing.accentColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💡", fontSize = 14.sp)
                                    Text(ing.tip, color = Color(0xFF81C784), fontSize = 11.sp, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: FEED COMBINATIONS
// ─────────────────────────────────────────────────────────────────────────────

private data class FeedStage(
    val stage: String,
    val emoji: String,
    val ageWeight: String,
    val targetCP: String,
    val energyMj: String,
    val keyIngredients: List<Pair<String, String>>,  // name to note
    val substitutions: List<String>,
    val accentColor: Color,
    val bgColor: Color
)

@Composable
fun FeedCombinationsTab() {
    val stages = listOf(
        FeedStage(
            stage = "Starter / Weaner",
            emoji = "🐷",
            ageWeight = "4–10 weeks · 7–25 kg",
            targetCP = ">18% Crude Protein",
            energyMj = "13–14 MJ/kg ME",
            keyIngredients = listOf(
                "Maize 600 kg" to "60% — primary energy base",
                "Soybean Meal 180 kg" to "18% — high CP for rapid growth",
                "Wheat 100 kg" to "10% — gut fibre + energy",
                "Sunflower Cake 50 kg" to "5% — complementary protein",
                "Fishmeal 30 kg" to "3% — best lysine source for young pigs",
                "Oil 15 kg" to "1.5% — energy density for small stomachs",
                "Premix 2% + Lime + DCP + Salt" to "micronutrient foundation"
            ),
            substitutions = listOf(
                "Fishmeal → blood meal at equal rate if omena is expensive",
                "Soybean Meal → field peas (max 15%) if soy unavailable",
                "Wheat → sorghum (max 10%) — dehulled preferred"
            ),
            accentColor = Color(0xFF81C784),
            bgColor = Color(0xFF122316)
        ),
        FeedStage(
            stage = "Grower",
            emoji = "🐖",
            ageWeight = "10–18 weeks · 25–60 kg",
            targetCP = "16–18% Crude Protein",
            energyMj = "12.5–13.5 MJ/kg ME",
            keyIngredients = listOf(
                "Maize 670 kg" to "67% — increase energy for muscle",
                "Wheat 100 kg" to "10% — keep constant",
                "Soybean Meal 120 kg" to "12% — slightly reduced from starter",
                "Sunflower Cake 70 kg" to "7% — higher than starter for cost saving",
                "Fishmeal 10 kg" to "1% — reduce to cut cost",
                "Oil 2 kg" to "0.2% — minimal in grower",
                "Premix 2% + Lime + DCP + Salt" to "unchanged from starter"
            ),
            substitutions = listOf(
                "Sunflower Cake ↑ can partially replace Soybean Meal at this stage",
                "Canola/rapeseed meal → up to 10% if soy unavailable (watch glucosinolates)",
                "Cassava flour → up to 15% maize replacement (lower energy, watch mycotoxins)"
            ),
            accentColor = Color(0xFF64B5F6),
            bgColor = Color(0xFF0D1E2E)
        ),
        FeedStage(
            stage = "Finisher",
            emoji = "🏆",
            ageWeight = "18–24 weeks · 60–100 kg",
            targetCP = "14–16% Crude Protein",
            energyMj = "12–13 MJ/kg ME",
            keyIngredients = listOf(
                "Maize 710 kg" to "71% — maximise energy for weight gain",
                "Wheat 100 kg" to "10% — constant throughout",
                "Soybean Meal 80 kg" to "8% — further reduced",
                "Sunflower Cake 70 kg" to "7% — holds protein level cheaply",
                "Fishmeal 10 kg" to "1% — keep low to avoid taint",
                "Oil 2 kg" to "0.2% — minimal",
                "Premix 2% + Lime + DCP + Salt" to "unchanged"
            ),
            substitutions = listOf(
                "Fishmeal must stay ≤1% — prevents fishy pork flavour",
                "Molasses → up to 5% maize replacement, improves palatability",
                "Palm kernel cake → up to 10% if available (reduce sunflower cake equally)"
            ),
            accentColor = Color(0xFFFFB74D),
            bgColor = Color(0xFF1E180A)
        ),
        FeedStage(
            stage = "Lactating Sow",
            emoji = "🐷💕",
            ageWeight = "Farrowing to weaning · 150–250 kg",
            targetCP = ">17% Crude Protein",
            energyMj = "13–14 MJ/kg ME",
            keyIngredients = listOf(
                "Maize 620 kg" to "62% — high energy for milk production",
                "Soybean Meal 180 kg" to "18% — protein for milk synthesis",
                "Wheat 100 kg" to "10% — gut health during stress",
                "Fishmeal 30 kg" to "3% — extra lysine for piglet immunoglobulins",
                "Feed Lime ↑ 12 kg" to "1.2% — extra calcium for milk (normal: 0.8%)",
                "Oil 20 kg" to "2% — energy density without bulk"
            ),
            substitutions = listOf(
                "Add alfalfa (lucerne) meal up to 5% for extra roughage during lactation",
                "Increase oil to 3% if sow loses too much condition during lactation",
                "Lysine supplement (500g/tonne) if fishmeal unavailable"
            ),
            accentColor = Color(0xFFF06292),
            bgColor = Color(0xFF2A0D1A)
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Intro banner
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1A1A2E),
                border = BorderStroke(1.dp, Color(0xFF3949AB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 16.sp)
                        Text("How Feed Combinations Work", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        "As pigs grow, their protein needs decrease while energy needs increase. Shift from protein-heavy starters to energy-heavy finishers to maximise feed conversion efficiency (FCR).",
                        color = Color(0xFFC9D1D9),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Stage cards
        items(stages) { stage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Stage header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stage.emoji, fontSize = 26.sp)
                            Column {
                                Text(stage.stage, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(stage.ageWeight, color = Color(0xFF8B949E), fontSize = 11.sp)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(shape = RoundedCornerShape(6.dp), color = stage.bgColor) {
                                Text(stage.targetCP, color = stage.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(stage.energyMj, color = Color(0xFF8B949E), fontSize = 10.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF30363D))

                    // Ingredients breakdown
                    Text("Per 1,000 kg batch:", color = Color(0xFF8B949E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    stage.keyIngredients.forEach { (name, note) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "• $name",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.45f)
                            )
                            Text(
                                note,
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.55f)
                            )
                        }
                    }

                    // Substitutions
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = stage.bgColor,
                        border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = stage.accentColor, modifier = Modifier.size(14.dp))
                                Text("Possible Substitutions", color = stage.accentColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            stage.substitutions.forEach { sub ->
                                Text("→ $sub", color = Color(0xFFC9D1D9), fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // FCR tip card
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1A1200),
                border = BorderStroke(1.dp, Color(0xFFFF8A65).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 16.sp)
                        Text("Golden Rules for Feed Efficiency", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    listOf(
                        "Target FCR of 2.5–3.0 kg feed per kg live weight gain",
                        "Always provide clean water — pigs need 2–3 L water per 1 kg feed",
                        "Weigh your batch size accurately — small errors in micro-ingredients (DCP, salt, premix) have large effects",
                        "Store mixed feed ≤2 weeks — rancidity destroys fat-soluble vitamins",
                        "Transition between ration stages gradually over 5–7 days to avoid digestive upset"
                    ).forEach { rule ->
                        Text("• $rule", color = Color(0xFFC9D1D9), fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}
