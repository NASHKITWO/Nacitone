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
        primary = Color(0xFFE8EEF7),
        onPrimary = Color(0xFF0A0A0B),
        primaryContainer = Color(0xFF2A2D33),
        onPrimaryContainer = Color(0xFFF2F5FA),
        secondary = Color(0xFFC9CDD3),
        onSecondary = Color(0xFF101114),
        secondaryContainer = Color(0x33FFFFFF),
        onSecondaryContainer = Color(0xFFF0F2F5),
        tertiary = Color(0xFFA9C7D8),
        onTertiary = Color(0xFF0E1A21),
        tertiaryContainer = Color(0xFF23323B),
        onTertiaryContainer = Color(0xFFDDEEF7),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF2F2F2),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF2F2F2),
        surfaceVariant = Color(0xFF1E1F22),
        onSurfaceVariant = Color(0xFFB4B7BC),
        surfaceTint = Color(0xFFE8EEF7),
        outline = Color(0xFF8B8E93),
        outlineVariant = Color(0x2EFFFFFF),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF0A0A0B),
        surfaceContainer = Color(0xFF111113),
        surfaceContainerHigh = Color(0xFF18181B),
        surfaceContainerHighest = Color(0xFF202024),
    )

/** Satu-satunya tema Nacitone: hitam pekat dengan kaca bening. */
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


