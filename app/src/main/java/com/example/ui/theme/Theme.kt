package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AuraDarkColorScheme = darkColorScheme(
    primary = AuraIceCyan,
    onPrimary = Color(0xFF031628),
    primaryContainer = AuraSurfaceElevated,
    onPrimaryContainer = AuraCyanGlow,
    secondary = AuraViolet,
    onSecondary = Color(0xFF0C1033),
    secondaryContainer = Color(0xFF1E234A),
    onSecondaryContainer = AuraLavender,
    tertiary = AuraLavender,
    onTertiary = Color(0xFF240E3E),
    tertiaryContainer = Color(0xFF2C194D),
    onTertiaryContainer = Color(0xFFE9D5FF),
    background = AuraCosmicBlack,
    onBackground = AuraTextPrimary,
    surface = AuraDeepNavy,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceNavy,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraBorderNavy,
    outlineVariant = Color(0xFF1E2A47),
    error = AuraCrisisCoral,
    onError = Color(0xFF3B0707)
)

@Composable
fun AuraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
