package com.phms.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.phms.app.data.local.entity.BuyerEntity
import com.phms.app.data.local.entity.PigEntity
import com.phms.app.ui.viewmodel.MainViewModel

enum class SaleStep { SELECT_PIGS, SET_PRICE, BUYER_DETAILS, CONFIRM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleFlowScreen(viewModel: MainViewModel, navController: NavController) {
    val allPigs by viewModel.activePigs.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val healthEvents by viewModel.repository.allHealthEvents.collectAsState(initial = emptyList())
    val feedingLogs by viewModel.repository.allFeedingLogs.collectAsState(initial = emptyList())
    val marketReadyPigs = allPigs.filter { it.status == "Active" }

    var currentStep by remember { mutableStateOf(SaleStep.SELECT_PIGS) }
    val selectedPigIds = remember { mutableStateListOf<Long>() }
    var pricePerKg by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf("individual") } // "individual" or "quantity"
    var selectedBuyerId by remember { mutableStateOf<Long?>(null) }
    var paymentStatus by remember { mutableStateOf("Paid") }
    var confirmNonSlaughterTransfer by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    fun getWithdrawalForPig(pig: PigEntity): com.phms.app.data.local.entity.HealthEventEntity? {
        val pigStageName = stages.find { it.id == pig.current_stage_id }?.name
        return healthEvents.firstOrNull { event ->
            (event.pig_id == pig.id ||
             event.target_scope == "Herd" ||
             (event.target_scope == "Category" && event.target_category.equals(pigStageName, ignoreCase = true))) &&
            event.withdrawal_days > 0 &&
            now < (event.date + event.withdrawal_days * 86400000L)
        }
    }

    // New buyer form
    var showNewBuyerForm by remember { mutableStateOf(false) }
    var newBuyerName by remember { mutableStateOf("") }
    var newBuyerPhone by remember { mutableStateOf("") }
    var newBuyerEmail by remember { mutableStateOf("") }
    var newBuyerLocation by remember { mutableStateOf("") }
    var newBuyerType by remember { mutableStateOf("Butchery") }
    var newBuyerNotes by remember { mutableStateOf("") }
    var savedNewBuyerId by remember { mutableStateOf<Long?>(null) }

    val effectiveBuyerId = selectedBuyerId ?: savedNewBuyerId

    val selectedPigs = marketReadyPigs.filter { it.id in selectedPigIds }

    // manualWeightsMap: user-editable weight overrides keyed by pig ID (text for field binding)
    val manualWeightStrings = remember { mutableStateMapOf<Long, String>() }

    // Seed from DB when selection changes
    val pigDbWeights by produceState<Map<Long, Double>>(initialValue = emptyMap(), key1 = selectedPigIds.toList()) {
        val map = mutableMapOf<Long, Double>()
        selectedPigs.forEach { pig ->
            val logged = viewModel.repository.pigDao.getWeightsForPigSync(pig.id).firstOrNull()?.weight_kg ?: 85.0
            map[pig.id] = logged
            // Pre-fill manual field with DB weight only if not already set by user
            if (!manualWeightStrings.containsKey(pig.id)) {
                manualWeightStrings[pig.id] = String.format("%.1f", logged)
            }
        }
        value = map
    }

    // Effective weight per pig: use manual entry if valid, else DB value
    fun effectiveWeight(pigId: Long): Double =
        manualWeightStrings[pigId]?.toDoubleOrNull() ?: pigDbWeights[pigId] ?: 85.0

    val totalWeight = selectedPigs.sumOf { effectiveWeight(it.id) }
    val price = pricePerKg.toDoubleOrNull() ?: 0.0
    val totalAmount = totalWeight * price

    val selectedWithWithdrawal = selectedPigs.mapNotNull { pig -> getWithdrawalForPig(pig)?.let { pig to it } }

    val directFeedCost = selectedPigs.sumOf { pig ->
        feedingLogs.filter { it.pig_id == pig.id }.sumOf { it.quantity_kg * 60.0 }
    }
    val totalFeedCost = if (directFeedCost > 500.0) directFeedCost else (totalWeight * 2.8 * 55.0)
    val totalMedCost = selectedPigs.sumOf { pig -> healthEvents.filter { it.pig_id == pig.id }.sumOf { it.cost } }
    val totalBaseCost = selectedPigs.sumOf { pig -> if ((pig.purchase_price ?: 0.0) > 0.0) pig.purchase_price!! else 2500.0 }
    val totalLifetimeInvestment = totalFeedCost + totalMedCost + totalBaseCost
    val breakEvenPricePerKg = if (totalWeight > 0.0) totalLifetimeInvestment / totalWeight else 0.0
    val projectedProfit = totalAmount - totalLifetimeInvestment
    val profitMarginPct = if (totalAmount > 0.0) (projectedProfit / totalAmount) * 100.0 else 0.0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Record Sale", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Step indicator
            SaleStepIndicator(currentStep)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentStep) {
                    SaleStep.SELECT_PIGS -> {
                        item {
                            Text("Select Pigs for Sale", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${selectedPigIds.size} pig(s) selected", color = Color(0xFF4CAF50), fontSize = 13.sp)
                        }
                        item {
                            // Toggle: individual or by quantity
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("individual" to "Select Individually", "quantity" to "By Quantity").forEach { (mode, label) ->
                                    FilterChip(
                                        selected = selectionMode == mode,
                                        onClick = {
                                            selectionMode = mode
                                            selectedPigIds.clear()
                                        },
                                        label = { Text(label, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF1B5E20),
                                            selectedLabelColor = Color(0xFF4CAF50)
                                        )
                                    )
                                }
                            }
                        }

                        if (selectedWithWithdrawal.isNotEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF3E1A1A),
                                    border = BorderStroke(1.dp, Color(0xFFE53935)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                            Text("Slaughter Safety Lock: Withdrawal Active", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(
                                            "${selectedWithWithdrawal.size} selected pig(s) are currently under medication withdrawal. Meat cannot be sold for human slaughter until withdrawal periods expire.",
                                            color = Color(0xFFFFCDD2),
                                            fontSize = 11.5.sp
                                        )
                                        selectedWithWithdrawal.forEach { (p, ev) ->
                                            val daysLeft = Math.ceil(((ev.date + ev.withdrawal_days * 86400000L) - System.currentTimeMillis()) / 86400000.0).toInt().coerceAtLeast(1)
                                            Text("• Pig #${p.tag_number}: Treated with ${ev.product} ($daysLeft days remaining)", color = Color.White, fontSize = 11.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = confirmNonSlaughterTransfer,
                                                onCheckedChange = { confirmNonSlaughterTransfer = it },
                                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), uncheckedColor = Color(0xFFFF8A80))
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Confirm live transfer / breeding only (NOT for immediate slaughter)", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        if (selectionMode == "quantity") {
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        OutlinedTextField(
                                            value = quantity,
                                            onValueChange = { q ->
                                                quantity = q
                                                val n = q.toIntOrNull() ?: 0
                                                selectedPigIds.clear()
                                                selectedPigIds.addAll(marketReadyPigs.take(n).map { it.id })
                                            },
                                            label = { Text("Number of pigs to sell") },
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF4CAF50),
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        if (selectedPigIds.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text("Auto-selected: ${selectedPigIds.joinToString(", ") { "pig #$it" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(marketReadyPigs) { pig ->
                                SalePigCard(
                                    pig = pig,
                                    isSelected = pig.id in selectedPigIds,
                                    withdrawalEvent = getWithdrawalForPig(pig),
                                    onToggle = {
                                        if (pig.id in selectedPigIds) selectedPigIds.remove(pig.id) else selectedPigIds.add(pig.id)
                                    }
                                )
                            }
                            if (marketReadyPigs.isEmpty()) {
                                item { EmptyTab("No active pigs available for sale") }
                            }
                        }
                    }

                    SaleStep.SET_PRICE -> {
                        item {
                            Text("Set Sale Price & Weights", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text("Enter the actual weighed weight for each pig at point of sale.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = pricePerKg,
                                        onValueChange = { pricePerKg = it },
                                        label = { Text("Price per kg (KSh)") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4CAF50),
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text("Enter actual scale weight per pig:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    // Editable per-pig weight fields
                                    selectedPigs.forEach { pig ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text("#${pig.tag_number}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(pig.breed, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                            }
                                            OutlinedTextField(
                                                value = manualWeightStrings[pig.id] ?: "",
                                                onValueChange = { v ->
                                                    manualWeightStrings[pig.id] = v
                                                },
                                                label = { Text("kg") },
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                                ),
                                                modifier = Modifier.width(100.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF4CAF50),
                                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Weight", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${String.format("%.1f", totalWeight)} kg", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    if (price > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Total Amount", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("KSh ${String.format("%,.0f", totalAmount)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (totalWeight > 0.0) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D24)),
                                    border = BorderStroke(1.dp, Color(0xFF1B3A4B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                                            Text("Break-Even & Margin Analysis", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        }
                                        HorizontalDivider(color = Color(0xFF1E3A4B))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Est. Lifetime Cost (${selectedPigIds.size} pigs)", color = Color(0xFF8B949E), fontSize = 12.sp)
                                            Text("KSh ${totalLifetimeInvestment.toInt()}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        }
                                        Text(
                                            "Feed: KSh ${totalFeedCost.toInt()} • Meds: KSh ${totalMedCost.toInt()} • Piglet: KSh ${totalBaseCost.toInt()}",
                                            color = Color(0xFF6E7681),
                                            fontSize = 10.5.sp
                                        )
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Break-Even Selling Price", color = Color(0xFF8B949E), fontSize = 12.sp)
                                            Text("KSh ${String.format(java.util.Locale.US, "%.1f", breakEvenPricePerKg)} / kg", color = Color(0xFF90E0EF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        if (price > 0) {
                                            HorizontalDivider(color = Color(0xFF1E3A4B))
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("Projected Net Profit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (projectedProfit >= 0) Color(0xFF1B3A1B) else Color(0xFF3E1A1A)
                                                ) {
                                                    Text(
                                                        "${if (projectedProfit >= 0) "+KSh " else "-KSh "}${Math.abs(projectedProfit.toInt())} (${String.format(java.util.Locale.US, "%.1f", profitMarginPct)}%)",
                                                        color = if (projectedProfit >= 0) Color(0xFF81C784) else Color(0xFFFF5252),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
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

                    SaleStep.BUYER_DETAILS -> {
                        item {
                            Text("Buyer Details", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        if (!showNewBuyerForm) {
                            item {
                                Button(
                                    onClick = { showNewBuyerForm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF4CAF50))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add New Buyer", color = Color(0xFF4CAF50))
                                }
                            }
                            if (buyers.isNotEmpty()) {
                                item { Text("Or select existing buyer:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                                items(buyers) { buyer ->
                                    BuyerSelectCard(buyer = buyer, isSelected = selectedBuyerId == buyer.id, onClick = {
                                        selectedBuyerId = buyer.id
                                        savedNewBuyerId = null
                                    })
                                }
                            }
                        } else {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("New Buyer", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                        FormField("Buyer Name *", newBuyerName, { newBuyerName = it })
                                        FormField("Phone Number", newBuyerPhone, { newBuyerPhone = it },
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                                        Text("Buyer Type", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("Butchery", "Wholesaler", "Retailer", "Slaughterhouse", "Individual").forEach { type ->
                                                FilterChip(
                                                    selected = newBuyerType == type,
                                                    onClick = { newBuyerType = type },
                                                    label = { Text(type, fontSize = 12.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = Color(0xFF1B5E20),
                                                        selectedLabelColor = Color(0xFF4CAF50)
                                                    )
                                                )
                                            }
                                        }
                                        FormField("Notes (optional)", newBuyerNotes, { newBuyerNotes = it })
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { showNewBuyerForm = false },
                                                modifier = Modifier.weight(1f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6E7681))
                                            ) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            Button(
                                                onClick = {
                                                    if (newBuyerName.isNotBlank()) {
                                                        viewModel.addBuyer(newBuyerName, newBuyerPhone, newBuyerType, newBuyerNotes)
                                                        selectedBuyerId = null
                                                        showNewBuyerForm = false
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                            ) { Text("Save Buyer") }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Payment Status", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Paid", "Partial", "Pending").forEach { status ->
                                    FilterChip(
                                        selected = paymentStatus == status,
                                        onClick = { paymentStatus = status },
                                        label = { Text(status) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (status) {
                                                "Paid" -> Color(0xFF1B5E20)
                                                "Partial" -> Color(0xFF7B4F00)
                                                else -> Color(0xFF7B1F1F)
                                            },
                                            selectedLabelColor = when (status) {
                                                "Paid" -> Color(0xFF4CAF50)
                                                "Partial" -> Color(0xFFFFB300)
                                                else -> Color(0xFFFF5252)
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    SaleStep.CONFIRM -> {
                        item {
                            Text("Confirm Sale", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ConfirmRow("Pigs to Sell", "${selectedPigIds.size} pigs")
                                    ConfirmRow("Est. Weight", "~${String.format("%.0f", totalWeight)} kg")
                                    ConfirmRow("Price/kg", "KSh ${pricePerKg}")
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    ConfirmRow("Total Amount", "KSh ${String.format("%,.0f", totalAmount)}", highlight = true)
                                    ConfirmRow("Payment", paymentStatus)
                                    val buyer = buyers.find { it.id == effectiveBuyerId }
                                    val displayBuyerName = buyer?.name ?: newBuyerName.ifBlank { "Walk-in Buyer" }
                                    val displayBuyerPhone = buyer?.phone ?: newBuyerPhone.ifBlank { "N/A" }
                                    ConfirmRow("Buyer", displayBuyerName)
                                    if (displayBuyerPhone.isNotBlank() && displayBuyerPhone != "N/A") {
                                        ConfirmRow("Phone", displayBuyerPhone)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    ConfirmRow(
                                        "Est. Net Profit",
                                        "${if (projectedProfit >= 0) "+KSh " else "-KSh "}${Math.abs(projectedProfit.toInt())} (${String.format(java.util.Locale.US, "%.1f", profitMarginPct)}%)",
                                        highlight = projectedProfit >= 0
                                    )
                                    if (selectedWithWithdrawal.isNotEmpty()) {
                                        ConfirmRow("Slaughter Status", "⚠️ Live transfer only (Withdrawal Active)")
                                    }
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    val buyer = buyers.find { it.id == effectiveBuyerId }
                                    val bName = buyer?.name ?: newBuyerName.ifBlank { "Walk-in Buyer" }
                                    val bPhone = buyer?.phone ?: newBuyerPhone.ifBlank { "N/A" }
                                    val bEmail = buyer?.email ?: newBuyerEmail.ifBlank { null }
                                    val bLoc = buyer?.location ?: newBuyerLocation.ifBlank { null }
                                    val bType = buyer?.type ?: newBuyerType

                                    viewModel.recordSaleWithBuyer(
                                        buyerName = bName,
                                        buyerPhone = bPhone,
                                        buyerEmail = bEmail,
                                        buyerLocation = bLoc,
                                        buyerType = bType,
                                        selectedPigIds = selectedPigIds.toList(),
                                        totalWeight = totalWeight,
                                        pricePerKg = price,
                                        paymentStatus = paymentStatus
                                    )
                                    navController.popBackStack()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Complete Sale & Save Buyer Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStep != SaleStep.SELECT_PIGS) {
                    OutlinedButton(
                        onClick = {
                            currentStep = when (currentStep) {
                                SaleStep.SET_PRICE -> SaleStep.SELECT_PIGS
                                SaleStep.BUYER_DETAILS -> SaleStep.SET_PRICE
                                SaleStep.CONFIRM -> SaleStep.BUYER_DETAILS
                                else -> SaleStep.SELECT_PIGS
                            }
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back", color = Color(0xFF4CAF50))
                    }
                }
                if (currentStep != SaleStep.CONFIRM) {
                    Button(
                        onClick = {
                            currentStep = when (currentStep) {
                                SaleStep.SELECT_PIGS -> SaleStep.SET_PRICE
                                SaleStep.SET_PRICE -> SaleStep.BUYER_DETAILS
                                SaleStep.BUYER_DETAILS -> SaleStep.CONFIRM
                                else -> SaleStep.CONFIRM
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        enabled = when (currentStep) {
                            SaleStep.SELECT_PIGS -> selectedPigIds.isNotEmpty() && (selectedWithWithdrawal.isEmpty() || confirmNonSlaughterTransfer)
                            SaleStep.SET_PRICE -> pricePerKg.toDoubleOrNull() != null && pricePerKg.toDouble() > 0
                            else -> true
                        }
                    ) {
                        Text("Next")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SaleStepIndicator(currentStep: SaleStep) {
    val steps = listOf("Pigs", "Price", "Buyer", "Confirm")
    val currentIndex = currentStep.ordinal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                i < currentIndex -> Color(0xFF4CAF50)
                                i == currentIndex -> Color(0xFF2E7D32)
                                else -> Color(0xFF21262D)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (i < currentIndex) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text("${i + 1}", color = if (i == currentIndex) Color.White else Color(0xFF6E7681), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(step, fontSize = 11.sp, color = if (i <= currentIndex) Color.White else Color(0xFF6E7681))
                if (i < steps.size - 1) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF30363D), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun SalePigCard(
    pig: PigEntity,
    isSelected: Boolean,
    withdrawalEvent: com.phms.app.data.local.entity.HealthEventEntity? = null,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B3A1B) else Color(0xFF161B22)
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4CAF50)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (pig.photo_path != null) {
                    AsyncImage(model = pig.photo_path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(pig.tag_number.take(3), color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#${pig.tag_number}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    if (withdrawalEvent != null) {
                        val daysLeft = Math.ceil(((withdrawalEvent.date + withdrawalEvent.withdrawal_days * 86400000L) - System.currentTimeMillis()) / 86400000.0).toInt().coerceAtLeast(1)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3E1A1A)) {
                            Text(
                                "⛔ $daysLeft d left (${withdrawalEvent.product})",
                                color = Color(0xFFFF5252),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text("${pig.breed} • ${if (pig.sex == "M") "Boar" else "Sow"}", color = Color(0xFF8B949E), fontSize = 12.sp)
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50),
                    uncheckedColor = Color(0xFF6E7681)
                )
            )
        }
    }
}

@Composable
fun BuyerSelectCard(buyer: BuyerEntity, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B3A1B) else Color(0xFF161B22)
        ),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF4CAF50)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(buyer.name.first().uppercase(), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(buyer.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("${buyer.type} • ${buyer.phone}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun ConfirmRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = if (highlight) Color(0xFF4CAF50) else Color.White,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}
