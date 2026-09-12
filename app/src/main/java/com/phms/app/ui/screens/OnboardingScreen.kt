package com.phms.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.phms.app.ui.viewmodel.MainViewModel

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String,
    val color: Color
)

@Composable
fun OnboardingScreen(viewModel: MainViewModel, navController: NavController) {
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Smart Pig & Herd Tracking",
                description = "Effortlessly manage pig records, weight growth, photo records, and auto-promotions across growth stages.",
                icon = "🐷",
                color = Color(0xFF4CAF50)
            ),
            OnboardingPage(
                title = "Balanced Feed & Stock Reduction",
                description = "Formulate custom nutrient-balanced mixes, track shop premix bags, and auto-deduct daily feed consumption.",
                icon = "🌾",
                color = Color(0xFF81C784)
            ),
            OnboardingPage(
                title = "Sales Ledger & Buyer Network",
                description = "Record sales capturing complete buyer information, view P&L statements, and optimize farm revenue.",
                icon = "💰",
                color = Color(0xFFFFB300)
            )
        )
    }

    var currentPage by remember { mutableIntStateOf(0) }
    val page = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Icon Box
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(page.color.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    .border(2.dp, page.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(page.icon, fontSize = 64.sp)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                page.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Text(
                page.description,
                color = Color(0xFF8B949E),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(36.dp))

            // Page Indicator Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (index == currentPage) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentPage) page.color else Color(0xFF30363D))
                    )
                }
            }
        }

        // Bottom Nav Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage < pages.size - 1) {
                TextButton(
                    onClick = {
                        viewModel.setOnboarded(true)
                        navController.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } }
                    }
                ) {
                    Text("Skip", color = Color(0xFF6E7681), fontSize = 15.sp)
                }

                Button(
                    onClick = { currentPage++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Next", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(
                    onClick = {
                        viewModel.setOnboarded(true)
                        navController.navigate("dashboard") { popUpTo("onboarding") { inclusive = true } }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
