package com.cardify.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ערכת צבעים של Cardify
private val CardifyColorScheme = lightColorScheme(
    primary = Color(0xFF0D7377),
    onPrimary = Color.White,
    primaryContainer = Color(0x29034C64),
    secondary = Color(0xFFA0FF9D),
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun CardifyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CardifyColorScheme,
        content = content
    )
}