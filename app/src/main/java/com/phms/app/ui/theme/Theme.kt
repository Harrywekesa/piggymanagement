package com.phms.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── DARK THEME (default) ─────────────────────────────────────────────────────
val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFF4CAF50),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary          = Color(0xFF81C784),
    onSecondary        = Color(0xFF1B1B1B),
    background         = Color(0xFF0D1117),
    onBackground       = Color(0xFFE6EDF3),
    surface            = Color(0xFF161B22),
    onSurface          = Color(0xFFE6EDF3),
    surfaceVariant     = Color(0xFF21262D),
    onSurfaceVariant   = Color(0xFF8B949E),
    outline            = Color(0xFF30363D),
    outlineVariant     = Color(0xFF30363D),
    error              = Color(0xFFFF5252),
    onError            = Color.White
)

// ── LIGHT THEME ──────────────────────────────────────────────────────────────
val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF2E7D32),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFB7DFB9),
    onPrimaryContainer = Color(0xFF003909),
    secondary          = Color(0xFF388E3C),
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFD4EDDA),
    onSecondaryContainer = Color(0xFF0A2E0D),
    tertiary           = Color(0xFFE65100),
    onTertiary         = Color.White,
    tertiaryContainer  = Color(0xFFFFE0C2),
    onTertiaryContainer = Color(0xFF3E1500),
    background         = Color(0xFFF4FAF4),
    onBackground       = Color(0xFF1A1C18),
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF1A1C18),
    surfaceVariant     = Color(0xFFDEECDE),
    onSurfaceVariant   = Color(0xFF4A5568),
    outline            = Color(0xFFB0C4B0),
    outlineVariant     = Color(0xFFCDDECD),
    error              = Color(0xFFB00020),
    onError            = Color.White,
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF410002)
)

@Composable
fun PHMSTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography(),
        content     = content
    )
}