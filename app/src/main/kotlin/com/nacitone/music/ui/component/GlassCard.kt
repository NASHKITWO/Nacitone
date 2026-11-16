package com.nacitone.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val GLASS_FILL_ALPHA = 0.10f
private const val GLASS_STROKE_ALPHA = 0.22f

/** Translucent fill tinted with the theme's primary color: every card shares one tone. */
@Composable
fun glassCardColors(tint: Color = MaterialTheme.colorScheme.primary): CardColors =
    CardDefaults.cardColors(
        containerColor = tint.copy(alpha = GLASS_FILL_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/** Same glass look for list rows that can be selected: a stronger tint and border when [selected]. */
@Composable
fun glassSelectableColors(selected: Boolean): CardColors =
    CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.24f else GLASS_FILL_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
fun glassSelectableBorder(selected: Boolean): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.5f else GLASS_STROKE_ALPHA))

/** Opaque variant for cards inside dialogs, where a see-through fill would show the screen behind. */
@Composable
fun glassPanelColors(): CardColors =
    CardDefaults.cardColors(
        containerColor =
            MaterialTheme.colorScheme.primary
                .copy(alpha = GLASS_FILL_ALPHA)
                .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
fun glassCardBorder(tint: Color = MaterialTheme.colorScheme.primary): BorderStroke =
    BorderStroke(1.dp, tint.copy(alpha = GLASS_STROKE_ALPHA))

/** Fades and slides the element up a few dp once, when it first enters composition. */
fun Modifier.softEnter(delayMillis: Int = 0): Modifier =
    composed {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            if (delayMillis > 0) delay(delayMillis.toLong())
            progress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
        graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 18.dp.toPx()
        }
    }
