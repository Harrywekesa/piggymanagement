package com.phms.app.ui.screens

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
    val buyers by viewModel.buyers.collectAsState()
    val marketReadyPigs = allPigs.filter { it.status == "Active" }

    var currentStep by remember { mutableStateOf(SaleStep.SELECT_PIGS) }
    val selectedPigIds = remember { mutableStateListOf<Long>() }
    var pricePerKg by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf("individual") } // "individual" or "quantity"
    var selectedBuyerId by remember { mutableStateOf<Long?>(null) }
    var paymentStatus by remember { mutableStateOf("Paid") }

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
    val pigWeightsMap by produceState<Map<Long, Double>>(initialValue = emptyMap(), key1 = selectedPigIds.toList()) {
        val map = mutableMapOf<Long, Double>()
        selectedPigs.forEach { pig ->
            val loggedWeight = viewModel.repository.pigDao.getWeightsForPigSync(pig.id).firstOrNull()?.weight_kg ?: 85.0
            map[pig.id] = loggedWeight
        }
        value = map
    }
    val totalWeight = selectedPigs.sumOf { pig -> pigWeightsMap[pig.id] ?: 85.0 }
    val price = pricePerKg.toDoubleOrNull() ?: 0.0
    val totalAmount = totalWeight * price

    Scaffold(
        containerColor = Color(0xFF0D1117),
        topBar = {
            TopAppBar(
                title = { Text("Record Sale", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161B22))
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
                            Text("Select Pigs for Sale", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        if (selectionMode == "quantity") {
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
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
                                                focusedLabelColor = Color(0xFF4CAF50),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                unfocusedBorderColor = Color(0xFF30363D)
                                            )
                                        )
                                        if (selectedPigIds.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text("Auto-selected: ${selectedPigIds.joinToString(", ") { "pig #$it" }}", color = Color(0xFF8B949E), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(marketReadyPigs) { pig ->
                                SalePigCard(pig = pig, isSelected = pig.id in selectedPigIds, onToggle = {
                                    if (pig.id in selectedPigIds) selectedPigIds.remove(pig.id) else selectedPigIds.add(pig.id)
                                })
                            }
                            if (marketReadyPigs.isEmpty()) {
                                item { EmptyTab("No active pigs available for sale") }
                            }
                        }
                    }

                    SaleStep.SET_PRICE -> {
                        item {
                            Text("Set Sale Price", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
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
                                            focusedLabelColor = Color(0xFF4CAF50),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            unfocusedBorderColor = Color(0xFF30363D)
                                        )
                                    )
                                    HorizontalDivider(color = Color(0xFF21262D))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Pigs Selected", color = Color(0xFF8B949E))
                                        Text("${selectedPigIds.size}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Est. Total Weight", color = Color(0xFF8B949E))
                                        Text("~${String.format("%.0f", totalWeight)} kg", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    if (price > 0) {
                                        HorizontalDivider(color = Color(0xFF21262D))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Estimated Total", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("KSh ${String.format("%,.0f", totalAmount)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SaleStep.BUYER_DETAILS -> {
                        item {
                            Text("Buyer Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        if (!showNewBuyerForm) {
                            item {
                                Button(
                                    onClick = { showNewBuyerForm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF4CAF50))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add New Buyer", color = Color(0xFF4CAF50))
                                }
                            }
                            if (buyers.isNotEmpty()) {
                                item { Text("Or select existing buyer:", color = Color(0xFF8B949E), fontSize = 13.sp) }
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                                ) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("New Buyer", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                        FormField("Buyer Name *", newBuyerName, { newBuyerName = it })
                                        FormField("Phone Number", newBuyerPhone, { newBuyerPhone = it },
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                                        Text("Buyer Type", color = Color(0xFF8B949E), fontSize = 13.sp)
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
                                            ) { Text("Cancel", color = Color(0xFF8B949E)) }
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
                            Text("Payment Status", color = Color(0xFF8B949E), fontSize = 13.sp)
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
                            Text("Confirm Sale", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ConfirmRow("Pigs to Sell", "${selectedPigIds.size} pigs")
                                    ConfirmRow("Est. Weight", "~${String.format("%.0f", totalWeight)} kg")
                                    ConfirmRow("Price/kg", "KSh ${pricePerKg}")
                                    HorizontalDivider(color = Color(0xFF21262D))
                                    ConfirmRow("Total Amount", "KSh ${String.format("%,.0f", totalAmount)}", highlight = true)
                                    ConfirmRow("Payment", paymentStatus)
                                    val buyer = buyers.find { it.id == effectiveBuyerId }
                                    val displayBuyerName = buyer?.name ?: newBuyerName.ifBlank { "Walk-in Buyer" }
                                    val displayBuyerPhone = buyer?.phone ?: newBuyerPhone.ifBlank { "N/A" }
                                    ConfirmRow("Buyer", displayBuyerName)
                                    if (displayBuyerPhone.isNotBlank() && displayBuyerPhone != "N/A") {
                                        ConfirmRow("Phone", displayBuyerPhone)
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
                            SaleStep.SELECT_PIGS -> selectedPigIds.isNotEmpty()
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
            .background(Color(0xFF161B22))
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
fun SalePigCard(pig: PigEntity, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B3A1B) else Color(0xFF161B22)
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50))
        ) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                if (pig.photo_path != null) {
                    AsyncImage(model = pig.photo_path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(pig.tag_number.take(3), color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("#${pig.tag_number}", color = Color.White, fontWeight = FontWeight.SemiBold)
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
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                Text(buyer.name.first().uppercase(), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(buyer.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("${buyer.type} • ${buyer.phone}", color = Color(0xFF8B949E), fontSize = 12.sp)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun ConfirmRow(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF8B949E), fontSize = 14.sp)
        Text(value, color = if (highlight) Color(0xFF4CAF50) else Color.White,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}
