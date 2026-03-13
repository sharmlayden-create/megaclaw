package com.megaclaw.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MegaclawColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
)

@Composable
fun MegaclawTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MegaclawColorScheme,
        content = content
    )
}
