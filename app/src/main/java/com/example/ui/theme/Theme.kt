package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MotoCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F55),
    onPrimaryContainer = Color(0xFF8CF8FF),

    secondary = MotoViolet,
    onSecondary = Color(0xFF28004F),
    secondaryContainer = Color(0xFF431175),
    onSecondaryContainer = Color(0xFFE9D5FF),

    tertiary = MotoGreen,
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFF6FFFB6),

    background = MotoBackground,
    onBackground = MotoTextPrimary,
    surface = MotoSurface,
    onSurface = MotoTextPrimary,
    surfaceVariant = MotoSurfaceVariant,
    onSurfaceVariant = MotoTextSecondary,

    outline = MotoBorder,
    outlineVariant = MotoSurfaceElevated,
    error = MotoRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Futuristic MOTO AI defaults to its signature dark cyberpunk theme
    dynamicColor: Boolean = false, // Keep the custom aesthetic intact
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
