package com.nacitone.music.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldColors
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

private const val GLASS_FILL_ALPHA = 0.08f
private const val GLASS_STROKE_ALPHA = 0.18f

/** Warna kaca bening: putih transparan, tanpa semburat ungu. */
val GlassTint: Color = Color.White

/** Isi transparan bening: semua kartu memakai satu nada yang sama. */
@Composable
fun glassCardColors(tint: Color = GlassTint): CardColors =
    CardDefaults.cardColors(
        containerColor = tint.copy(alpha = GLASS_FILL_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

/** Same glass look for list rows that can be selected: a stronger tint and border when [selected]. */
@Composable
fun glassSelectableColors(selected: Boolean): CardColors =
    CardDefaults.cardColors(
        containerColor = GlassTint.copy(alpha = if (selected) 0.20f else GLASS_FILL_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
fun glassSelectableBorder(selected: Boolean): BorderStroke =
    BorderStroke(1.dp, GlassTint.copy(alpha = if (selected) 0.45f else GLASS_STROKE_ALPHA))

/** Opaque variant for cards inside dialogs, where a see-through fill would show the screen behind. */
@Composable
fun glassPanelColors(): CardColors =
    CardDefaults.cardColors(
        containerColor =
            GlassTint
                .copy(alpha = GLASS_FILL_ALPHA)
                .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
fun glassCardBorder(tint: Color = GlassTint): BorderStroke =
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


/** Bentuk pil-bulat untuk field teks kaca. */
val GlassFieldShape = RoundedCornerShape(20.dp)

/** Warna field teks terisi: kaca bening, tanpa garis bawah; menebal saat fokus. */
@Composable
fun glassTextFieldColors(): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = GlassTint.copy(alpha = 0.14f),
        unfocusedContainerColor = GlassTint.copy(alpha = 0.08f),
        disabledContainerColor = GlassTint.copy(alpha = 0.04f),
        errorContainerColor = GlassTint.copy(alpha = 0.08f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    )

/** Warna field teks bergaris: tepi kaca, tepi utama saat fokus. */
@Composable
fun glassOutlinedTextFieldColors(): TextFieldColors =
    TextFieldDefaults.colors(
        focusedContainerColor = GlassTint.copy(alpha = 0.08f),
        unfocusedContainerColor = GlassTint.copy(alpha = 0.04f),
        focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        unfocusedIndicatorColor = GlassTint.copy(alpha = 0.22f),
    )
