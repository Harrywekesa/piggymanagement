package com.phms.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.phms.app.data.local.entity.AlertEntity
import com.phms.app.ui.viewmodel.MainViewModel

@Composable
fun AlertsScreen(viewModel: MainViewModel, navController: NavController) {
    val alerts by viewModel.activeAlerts.collectAsState()
    var filterPriority by remember { mutableStateOf("All") }

    val filteredAlerts = alerts.filter {
        filterPriority == "All" || it.priority == filterPriority
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Alerts Inbox", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Critical", "High", "Medium", "Low").forEach { p ->
                    FilterChip(
                        selected = filterPriority == p,
                        onClick = { filterPriority = p },
                        label = { Text(p, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (p) {
                                "Critical" -> Color(0xFF7B1F1F)
                                "High" -> Color(0xFF7B4F00)
                                else -> Color(0xFF1B5E20)
                            },
                            selectedLabelColor = when (p) {
                                "Critical" -> Color(0xFFFF5252)
                                "High" -> Color(0xFFFFB300)
                                else -> Color(0xFF4CAF50)
                            },
                            containerColor = Color(0xFF161B22),
                            labelColor = Color(0xFF8B949E)
                        )
                    )
                }
            }
        }
        if (filteredAlerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("All clear! No alerts here.", color = Color(0xFF8B949E), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onDone = { viewModel.markAlertDone(alert.id) },
                        onNavigateToPig = { pigId -> navController.navigate("pig_detail/$pigId") }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertItemCard(
    alert: AlertEntity,
    onDone: () -> Unit,
    onNavigateToPig: ((Long) -> Unit)? = null
) {
    val (bg, border, textColor) = when (alert.priority) {
        "Critical" -> Triple(Color(0xFF1A0A0A), Color(0xFFFF5252), Color(0xFFFF5252))
        "High" -> Triple(Color(0xFF1A1400), Color(0xFFFFB300), Color(0xFFFFB300))
        else -> Triple(Color(0xFF161B22), Color(0xFF30363D), Color(0xFF4CAF50))
    }

    val hasPig = alert.related_pig_id != null && onNavigateToPig != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasPig) Modifier.clickable { onNavigateToPig!!(alert.related_pig_id!!) }
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(textColor)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(alert.type.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(alert.message, fontSize = 13.sp, color = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDone, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Check, "Done", tint = Color(0xFF4CAF50))
                }
            }
            // "View Pig" chip shown when this alert is linked to a specific pig
            if (hasPig) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(textColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Pets, null, tint = textColor, modifier = Modifier.size(12.dp))
                    Text("View Animal", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ArrowForward, null, tint = textColor, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
