/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score

private val NacitoneColorScheme =
    darkColorScheme(
        primary = Color(0xFFCFBCFF),
        onPrimary = Color(0xFF2B1B5E),
        primaryContainer = Color(0xFF4A3A8A),
        onPrimaryContainer = Color(0xFFE9DDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0x38CFBCFF),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFF2A6B8),
        onTertiary = Color(0xFF4A1E2C),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD9E2),
        background = Color(0xFF0F0D17),
        onBackground = Color(0xFFEAE6F5),
        surface = Color(0xFF0F0D17),
        onSurface = Color(0xFFEAE6F5),
        surfaceVariant = Color(0xFF2A2638),
        onSurfaceVariant = Color(0xFFB9B2D0),
        surfaceTint = Color(0xFFCFBCFF),
        outline = Color(0xFF8A84A0),
        outlineVariant = Color(0x38CFBCFF),
        surfaceContainerLowest = Color(0xFF0B0911),
        surfaceContainerLow = Color(0xFF15121F),
        surfaceContainer = Color(0xFF1A1726),
        surfaceContainerHigh = Color(0xFF221E31),
        surfaceContainerHighest = Color(0xFF2A263B),
    )

/** Satu-satunya tema Nacitone: gelap dengan kartu transparan. */
@Composable
fun NacitoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NacitoneColorScheme,
        typography = NacitoneTypography,
        shapes = NacitoneShapes,
        content = content,
    )
}

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)


