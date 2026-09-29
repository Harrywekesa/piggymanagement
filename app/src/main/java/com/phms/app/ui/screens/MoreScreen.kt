package com.phms.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class MoreModuleItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color
)

@Composable
fun MoreScreen(navController: NavController) {
    val modules = listOf(
        MoreModuleItem(
            route = "health",
            title = "Health & Vet",
            subtitle = "Vaccines, treatments & drug withdrawal",
            icon = Icons.Default.LocalHospital,
            iconTint = Color(0xFFE57373),
            iconBg = Color(0xFF3E1A1A)
        ),
        MoreModuleItem(
            route = "breeding",
            title = "Breeding & Gestation",
            subtitle = "Sow mating, gestation & farrowing",
            icon = Icons.Default.Favorite,
            iconTint = Color(0xFFF06292),
            iconBg = Color(0xFF3E1A2B)
        ),
        MoreModuleItem(
            route = "market",
            title = "Market & Sales",
            subtitle = "Pig sales & Kenya buyers directory",
            icon = Icons.Default.ShoppingCart,
            iconTint = Color(0xFFFFB74D),
            iconBg = Color(0xFF3E2D1A)
        ),
        MoreModuleItem(
            route = "reports",
            title = "Reports & Analytics",
            subtitle = "Financial P&L, feed usage & herd census",
            icon = Icons.Default.BarChart,
            iconTint = Color(0xFF64B5F6),
            iconBg = Color(0xFF1A2B3E)
        ),
        MoreModuleItem(
            route = "settings",
            title = "Farm Settings",
            subtitle = "Farm profile, backup, restore & demo",
            icon = Icons.Default.Settings,
            iconTint = Color(0xFF81C784),
            iconBg = Color(0xFF1A3E22)
        ),
        MoreModuleItem(
            route = "help_center",
            title = "Help & Guides",
            subtitle = "Guided tour, manual & farmer tips",
            icon = Icons.AutoMirrored.Filled.Help,
            iconTint = Color(0xFFBA68C8),
            iconBg = Color(0xFF331A3E)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Farm Management Modules",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Select a module to manage your herd operations",
            color = Color(0xFF8B949E),
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(modules) { module ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(module.route) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(module.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = module.icon,
                                contentDescription = null,
                                tint = module.iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = module.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = module.subtitle,
                                color = Color(0xFF8B949E),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
