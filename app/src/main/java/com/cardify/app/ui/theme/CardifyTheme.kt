package com.cardify.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Light color scheme reflecting the Cardify brand palette (teal primary, lime green secondary). */
private val CardifyColorScheme = lightColorScheme(
    primary = Color(0xFF0D7377),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF14FFEC),
    secondary = Color(0xFFA0FF9D),
    background = Color.White,
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A)
)

/**
 * Root Material 3 theme for the Cardify application.
 *
 * Wraps [content] in a [MaterialTheme] configured with the Cardify brand colors.
 * All screens should be hosted inside this composable.
 *
 * @param content The composable content tree to display within the theme.
 */
@Composable
fun CardifyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CardifyColorScheme,
        content = content
    )
}
