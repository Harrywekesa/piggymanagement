package com.phms.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AgricultureGreen,
    onPrimary = Color.White,
    primaryContainer = AgricultureGreenLight,
    secondary = AgricultureGold,
    error = AlertRed,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun PHMSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
