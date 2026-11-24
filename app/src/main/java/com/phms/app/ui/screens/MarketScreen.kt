package com.phms.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.phms.app.data.local.entity.BuyerEntity
import com.phms.app.ui.components.LocationFilterSelector
import com.phms.app.ui.components.LocationSelector
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

// ─────────────────────────────────────────────────────────────────────────────
// MARKET SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MarketScreen(viewModel: MainViewModel, onStartSale: () -> Unit) {
    val pigs by viewModel.activePigs.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val context = LocalContext.current
    val marketPigs = pigs.filter { it.current_stage_id == 5L && it.status == "Active" }
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    var selectedCountyFilter by remember { mutableStateOf("All Counties") }
    var selectedSubCountyFilter by remember { mutableStateOf("All Sub-Counties") }
    var selectedWardFilter by remember { mutableStateOf("All Wards") }
    var buyerSearchQuery by remember { mutableStateOf("") }
    var showAddBuyerDialog by remember { mutableStateOf(false) }
    var showClearBuyersConfirm by remember { mutableStateOf(false) }
    var buyerToDelete by remember { mutableStateOf<BuyerEntity?>(null) }
    var pigPricePerKg by remember { mutableStateOf("") }
    var showPriceEditor by remember { mutableStateOf(false) }
    val priceHistory = remember { mutableStateListOf<Pair<String, Double>>() }

    val filteredBuyers = buyers.filter { buyer ->
        val matchesCounty = selectedCountyFilter == "All Counties" || buyer.county.equals(selectedCountyFilter, ignoreCase = true) || buyer.location?.contains(selectedCountyFilter, ignoreCase = true) == true
        val matchesSubCounty = selectedSubCountyFilter == "All Sub-Counties" || buyer.sub_county.equals(selectedSubCountyFilter, ignoreCase = true) || buyer.location?.contains(selectedSubCountyFilter, ignoreCase = true) == true
        val matchesWard = selectedWardFilter == "All Wards" || buyer.ward.equals(selectedWardFilter, ignoreCase = true) || buyer.location?.contains(selectedWardFilter, ignoreCase = true) == true
        val matchesQuery = buyerSearchQuery.isBlank() || buyer.name.contains(buyerSearchQuery, ignoreCase = true) || buyer.phone.contains(buyerSearchQuery) || buyer.location?.contains(buyerSearchQuery, ignoreCase = true) == true

        matchesCounty && matchesSubCounty && matchesWard && matchesQuery
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Market & Sales", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        // ── MARKET PRICE TRACKER ──────────────────────────────────────────────
        item {
            val currentPriceKg = pigPricePerKg.toDoubleOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFFFB74D))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A1E00)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
                            }
                            Text("Live Market Price (KSh/kg)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        TextButton(onClick = { showPriceEditor = !showPriceEditor }) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Update", color = Color(0xFFFFB74D), fontSize = 12.sp)
                        }
                    }

                    if (currentPriceKg != null && currentPriceKg > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("KSh ${String.format("%.0f", currentPriceKg)}", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                    Text("per kg live weight", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    val avg90kgValue = currentPriceKg * 90
                                    Text("KSh ${avg90kgValue.toInt()}", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("per pig @ 90 kg", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                        // Market-ready pig portfolio value
                        if (marketPigs.isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, border = BorderStroke(1.dp, Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${marketPigs.size} market-ready pigs worth approx:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    Text("KSh ${(currentPriceKg * 90 * marketPigs.size).toInt()}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(14.dp))
                                Text("Tap 'Update' to record today's market pig price (KSh per kg live weight)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }

                    if (showPriceEditor) {
                        OutlinedTextField(
                            value = pigPricePerKg,
                            onValueChange = { pigPricePerKg = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Price (KSh / kg live weight)") },
                            placeholder = { Text("e.g. 350") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB74D),
                                focusedLabelColor = Color(0xFFFFB74D),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = {
                                TextButton(onClick = {
                                    val p = pigPricePerKg.toDoubleOrNull()
                                    if (p != null && p > 0) {
                                        val today = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())
                                        priceHistory.add(0, today to p)
                                        if (priceHistory.size > 10) priceHistory.removeLastOrNull()
                                        showPriceEditor = false
                                    }
                                }) { Text("Save", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold) }
                            }
                        )
                    }

                    if (priceHistory.isNotEmpty()) {
                        Text("Recent Prices:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        priceHistory.take(5).forEach { (date, price) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Text("KSh ${String.format("%.0f", price)}/kg", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartSale,
                    modifier = Modifier.weight(1.2f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Record Sale", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { showAddBuyerDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Buyer", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Text("Market-Ready Stock (${marketPigs.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
        items(marketPigs) { pig ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
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
                        Text("#${pig.tag_number}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("${pig.breed} • Est. 95kg", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(18.dp))
                            Text("Buyers Directory", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Shared across all Kenya pig farmers", fontSize = 11.sp, color = Color(0xFF00B4D8))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (buyers.isNotEmpty()) {
                            IconButton(onClick = { showClearBuyersConfirm = true }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All Buyers", tint = Color(0xFFFF5252))
                            }
                        }
                        IconButton(onClick = {
                            viewModel.syncBuyersFromCloud()
                            Toast.makeText(context, "Syncing Kenya-wide buyers...", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Directory", tint = Color(0xFF00B4D8))
                        }
                    }
                }
                OutlinedTextField(
                    value = buyerSearchQuery,
                    onValueChange = { buyerSearchQuery = it },
                    placeholder = { Text("Search buyer name, location, phone...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
                )
                LocationFilterSelector(
                    selectedCounty = selectedCountyFilter,
                    selectedSubCounty = selectedSubCountyFilter,
                    selectedWard = selectedWardFilter,
                    onFilterChanged = { county, subCounty, ward ->
                        selectedCountyFilter = county
                        selectedSubCountyFilter = subCounty
                        selectedWardFilter = ward
                    }
                )
            }
        }

        if (filteredBuyers.isEmpty()) {
            item {
                if (buyers.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF8B949E), modifier = Modifier.size(36.dp))
                            Text("No buyers in directory yet", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Tap '+ Add Buyer' above to register buyers, butcheries, or pork wholesalers.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text("No buyers found matching location filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredBuyers) { buyer ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(buyer.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (buyer.is_community) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B3A4B)) {
                                        Text(
                                            "DIRECTORY",
                                            color = Color(0xFF00B4D8),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            val locText = listOfNotNull(buyer.ward, buyer.sub_county, buyer.county ?: buyer.location).filter { it.isNotBlank() }.joinToString(", ")
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(13.dp))
                                Text(locText.ifBlank { "Location N/A" }, color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("${buyer.type} • ${buyer.phone}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!buyer.phone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${buyer.phone}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) { e.printStackTrace() }
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Call", color = Color(0xFF4CAF50), fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { buyerToDelete = buyer },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Buyer", tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        if (sales.isNotEmpty()) {
            item { Text("Recent Sales (${sales.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface) }
            items(sales.take(10)) { sale ->
                val buyer = buyers.find { it.id == sale.buyer_id }
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(buyer?.name ?: "Unknown Buyer", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            Text("${sale.total_weight}kg @ KSh ${sale.price_per_kg}/kg", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(dateFormat.format(Date(sale.date)), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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

    if (showAddBuyerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var buyerType by remember { mutableStateOf("Wholesaler") }
        var county by remember { mutableStateOf("Mombasa") }
        var subCounty by remember { mutableStateOf("Nyali") }
        var ward by remember { mutableStateOf("Frere Town") }
        var notes by remember { mutableStateOf("") }
        var shareToAllFarmers by remember { mutableStateOf(false) } // default OFF — require explicit consent
        var showConsentDialog by remember { mutableStateOf(false) }

        // ── Sharing consent dialog ────────────────────────────────────────────
        if (showConsentDialog) {
            AlertDialog(
                onDismissRequest = { showConsentDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = Color(0xFF00B4D8),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "Share to National Directory",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2137)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "By enabling this, the buyer's name, phone number, and location will be published to a shared directory visible to ALL pig farmers using this app across Kenya.",
                                color = Color(0xFF90CAF9),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text(
                            "This helps farmers across Kenya discover trusted pig buyers. Only share buyers you have personally dealt with.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(
                                "You can turn this off to keep the buyer private to your phone only.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            shareToAllFarmers = true
                            showConsentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                    ) {
                        Text("Yes, Share to Directory", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            shareToAllFarmers = false
                            showConsentDialog = false
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Keep Private", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { showAddBuyerDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Register Pig Buyer", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormField("Buyer / Business Name *", name, { name = it }, placeholder = "e.g. Kitale Pork Butchery")
                    FormField("Phone Number *", phone, { phone = it }, placeholder = "e.g. 0712345678", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    FormField("Email (Optional)", email, { email = it }, keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)

                    DropdownSelector(
                        label = "Buyer Category",
                        options = listOf(
                            1L to "Wholesaler / Aggregator",
                            2L to "Local Butchery",
                            3L to "Pork Joint / Eatery",
                            4L to "Individual Consumer",
                            5L to "Breeding Stock Buyer",
                            6L to "Exporter / Processor"
                        ),
                        selectedId = when (buyerType) {
                            "Wholesaler / Aggregator", "Wholesaler" -> 1L
                            "Local Butchery" -> 2L
                            "Pork Joint / Eatery" -> 3L
                            "Individual Consumer" -> 4L
                            "Breeding Stock Buyer" -> 5L
                            else -> 6L
                        },
                        onSelect = { id ->
                            buyerType = when (id) {
                                1L -> "Wholesaler"
                                2L -> "Local Butchery"
                                3L -> "Pork Joint"
                                4L -> "Individual"
                                5L -> "Breeding Buyer"
                                else -> "Processor"
                            }
                        }
                    )

                    Text("Buyer Location (Kenya Administrative)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LocationSelector(
                        selectedCounty = county,
                        selectedSubCounty = subCounty,
                        selectedWard = ward,
                        onLocationChanged = { c, sc, w ->
                            county = c
                            subCounty = sc
                            ward = w
                        }
                    )

                    FormField("Additional Notes", notes, { notes = it }, placeholder = "e.g. Preferred weight 90-100kg")

                    // SHARE TO ALL FARMERS TOGGLE
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (shareToAllFarmers) Color(0xFF0D2137) else Color(0xFF21262D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = if (shareToAllFarmers) Color(0xFF00B4D8) else Color(0xFF8B949E), modifier = Modifier.size(16.dp))
                                    Text(
                                        "Share with ALL farmers in Kenya",
                                        color = if (shareToAllFarmers) Color(0xFF00B4D8) else Color(0xFF8B949E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    if (shareToAllFarmers)
                                        "This buyer will appear in the DIRECTORY for every farmer using this app across Kenya."
                                    else
                                        "This buyer will only be visible on your phone.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = shareToAllFarmers,
                                onCheckedChange = { newValue ->
                                    if (newValue) {
                                        // Show consent dialog before enabling sharing
                                        showConsentDialog = true
                                    } else {
                                        // Turning off — no consent needed
                                        shareToAllFarmers = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00B4D8)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank()) {
                            Toast.makeText(context, "Please enter buyer name and phone number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addBuyer(
                            name = name,
                            phone = phone,
                            email = email.ifBlank { null },
                            location = "$ward, $subCounty, $county",
                            county = county,
                            subCounty = subCounty,
                            ward = ward,
                            type = buyerType,
                            notes = notes,
                            shareToAllFarmers = shareToAllFarmers
                        )
                        showAddBuyerDialog = false
                        val msg = if (shareToAllFarmers)
                            "Buyer saved and shared with all farmers in Kenya!"
                        else
                            "Buyer registered on your phone only."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (shareToAllFarmers) {
                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Text(if (shareToAllFarmers) "Save & Share" else "Save Buyer")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddBuyerDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (buyerToDelete != null) {
        val target = buyerToDelete!!
        AlertDialog(
            onDismissRequest = { buyerToDelete = null },
            title = { Text("Delete Buyer", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete '${target.name}' from your directory?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBuyer(target)
                        buyerToDelete = null
                        Toast.makeText(context, "Buyer removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { buyerToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showClearBuyersConfirm) {
        AlertDialog(
            onDismissRequest = { showClearBuyersConfirm = false },
            title = { Text("Clear Buyers Directory", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to remove all buyers from your directory? This will remove all contacts.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllBuyers(wipeCloud = true)
                        showClearBuyersConfirm = false
                        Toast.makeText(context, "Buyers directory cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearBuyersConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
