package com.phms.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phms.app.R
import kotlinx.coroutines.delay

@Composable
fun PiggishSplashScreen(
    farmName: String,
    onLoadingComplete: () -> Unit
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }

    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 2200, easing = LinearOutSlowInEasing),
        label = "pigProgress"
    )

    // Infinite trotting / bobbing animation for the pig mascot
    val infiniteTransition = rememberInfiniteTransition(label = "trot")
    val trotOffsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trotOffsetY"
    )

    // Start loading on launch
    LaunchedEffect(Unit) {
        targetProgress = 1f
    }

    // Automatically transition to dashboard upon completion
    LaunchedEffect(progress) {
        if (progress >= 0.999f) {
            delay(250) // Brief pause to see 100%
            onLoadingComplete()
        }
    }

    // Fun pig farm status messages based on progress
    val statusText = when {
        progress < 0.22f -> "Waking up the piglets... 🌅"
        progress < 0.45f -> "Inspecting waterers & pens... 🚿"
        progress < 0.70f -> "Calculating daily herd rations... ⚖️"
        progress < 0.90f -> "Checking health records & alerts... 🩺"
        else -> "Herd ready! Opening dashboard... 🐷✨"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B0E),
                        Color(0xFF0E1A11),
                        Color(0xFF070B0E)
                    )
                )
            )
            .clickable { onLoadingComplete() } // Quick tap to skip
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App / Farm Branding Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2E7D32).copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    .border(2.dp, Color(0xFF2E7D32), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "PHMS Mascot",
                    modifier = Modifier
                        .size(76.dp)
                        .scale(1f + (progress * 0.05f))
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = farmName.ifBlank { "Homes Best" },
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Pig Herd Management System",
                color = Color(0xFF81C784),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.height(52.dp))

            // ── PIGGISH PROGRESS BAR ──────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Box container with the trotting pig moving across the top of the bar
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    val totalWidth = maxWidth
                    val pigOffset = (totalWidth - 36.dp) * progress

                    // Trotting pig on top of the progress head
                    Box(
                        modifier = Modifier
                            .offset(x = pigOffset, y = trotOffsetY.dp)
                            .size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (progress >= 1f) "🐷" else "🐖",
                            fontSize = 26.sp
                        )
                    }

                    // Progress Bar Track
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF162316))
                            .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(7.dp))
                    ) {
                        // Gradient fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0.01f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF1B5E20),
                                            Color(0xFF4CAF50),
                                            Color(0xFF81C784)
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Status message + percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        color = Color(0xFFC9D1D9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color(0xFF81C784),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(64.dp))

            Text(
                text = "Tap anywhere to skip",
                color = Color(0xFF484F58),
                fontSize = 11.sp
            )
        }
    }
}
