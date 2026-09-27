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
import com.phms.app.ui.components.LocationFilterSelector
import com.phms.app.ui.components.LocationSelector
import com.phms.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

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
            Text("Market & Sales", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        item { Text("Market-Ready Stock (${marketPigs.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        items(marketPigs) { pig ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF21262D)),
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
                        Text("#${pig.tag_number}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${pig.breed} • Est. 95kg", color = Color(0xFF8B949E), fontSize = 12.sp)
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
                        Text("Buyers Directory 🌍", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Shared across all Kenya pig farmers", fontSize = 11.sp, color = Color(0xFF00B4D8))
                    }
                    IconButton(onClick = {
                        viewModel.syncBuyersFromCloud()
                        Toast.makeText(context, "Syncing Kenya-wide buyers...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Directory", tint = Color(0xFF00B4D8))
                    }
                }
                OutlinedTextField(
                    value = buyerSearchQuery,
                    onValueChange = { buyerSearchQuery = it },
                    placeholder = { Text("Search buyer name, location, phone...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF8B949E)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF4CAF50), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
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
                Text("No buyers found matching location filter.", color = Color(0xFF6E7681), fontSize = 13.sp)
            }
        } else {
            items(filteredBuyers) { buyer ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(buyer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            Text(if (locText.isNotBlank()) "📍 $locText" else "📍 Location N/A", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("${buyer.type} • ${buyer.phone}", color = Color(0xFF8B949E), fontSize = 11.sp)
                        }
                        if (!buyer.phone.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${buyer.phone}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) { e.printStackTrace() }
                                },
                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Call Buyer", color = Color(0xFF4CAF50), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        if (sales.isNotEmpty()) {
            item { Text("Recent Sales (${sales.size})", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
            items(sales.take(10)) { sale ->
                val buyer = buyers.find { it.id == sale.buyer_id }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(buyer?.name ?: "Unknown Buyer", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${sale.total_weight}kg @ KSh ${sale.price_per_kg}/kg", color = Color(0xFF8B949E), fontSize = 12.sp)
                            Text(dateFormat.format(Date(sale.date)), color = Color(0xFF6E7681), fontSize = 11.sp)
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
        var shareToAllFarmers by remember { mutableStateOf(true) } // default ON — encourage sharing

        AlertDialog(
            onDismissRequest = { showAddBuyerDialog = false },
            containerColor = Color(0xFF161B22),
            title = { Text("Register Pig Buyer", color = Color.White, fontWeight = FontWeight.Bold) },
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

                    Text("Buyer Location (Kenya Administrative)", color = Color(0xFF8B949E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Text("🌍", fontSize = 16.sp)
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
                                    color = Color(0xFF6E7681),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = shareToAllFarmers,
                                onCheckedChange = { shareToAllFarmers = it },
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
                            "Buyer saved and shared with all farmers in Kenya! 🌍"
                        else
                            "Buyer registered on your phone only."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (shareToAllFarmers) "Save & Share 🌍" else "Save Buyer")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddBuyerDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            }
        )
    }
}
