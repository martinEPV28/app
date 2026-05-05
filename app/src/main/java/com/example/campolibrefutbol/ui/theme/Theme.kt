package com.example.campolibrefutbol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GrassGreen,
    secondary = StadiumGold,
    tertiary = CrowdGray,
    background = NightGrass,
    surface = DarkSurface,
    primaryContainer = DarkContainer,
    onPrimary = Color(0xFF06210F),
    onSecondary = NightGrass,
    onBackground = PitchLine,
    onSurface = PitchLine,
    onSurfaceVariant = Color(0xFF94A89A)
)

private val LightColorScheme = lightColorScheme(
    primary = FieldGreen,
    secondary = StadiumGold,
    tertiary = GrassGreen,
    background = PitchLine,
    surface = CardGreen,
    primaryContainer = LeafContainer,
    onPrimary = PitchLine,
    onSecondary = ForestInk,
    onBackground = ForestInk,
    onSurface = ForestInk,
    onSurfaceVariant = Color(0xFF4A6153)
)

@Composable
fun CampoLibreFutbolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}