package com.example.nokiacam.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5E5E5),
    onPrimary = Color(0xFF111111),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFECECEC),
)

@Composable
fun NokiaCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
