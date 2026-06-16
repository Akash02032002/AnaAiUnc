package com.aigirlcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF4F87),
    onPrimary = Color.White,
    secondary = Color(0xFF3D7DFF),
    tertiary = Color(0xFF141722),
    background = Color(0xFFF9F9FC),
    surface = Color.White,
    onSurface = Color(0xFF15151C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6F9F),
    onPrimary = Color(0xFF1A0710),
    secondary = Color(0xFF80A8FF),
    tertiary = Color(0xFFFFD8E5),
    background = Color(0xFF0F1118),
    surface = Color(0xFF171A24),
    onSurface = Color(0xFFF8F8FA),
)

@Composable
fun AIGirlTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}

