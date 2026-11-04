/**
 * Nacitone (C) 2026
 * Licensed under GPL-3.0
 */

package com.nacitone.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nacitone.music.R
import com.nacitone.music.constants.SupportBannerDismissedAtKey
import com.nacitone.music.utils.rememberPreference

/** Tautan dukungan untuk pengembang (Sociabuzz). Satu-satunya tempat URL ini didefinisikan. */
const val SUPPORT_URL = "https://sociabuzz.com/nacitwo/tribe"

private const val SUPPORT_BANNER_COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000

/** Banner dukungan di Beranda. Setelah ditutup, tidak tampil lagi selama 30 hari. */
@Composable
fun SupportBanner(modifier: Modifier = Modifier) {
    val (dismissedAt, setDismissedAt) = rememberPreference(SupportBannerDismissedAtKey, 0L)
    val uriHandler = LocalUriHandler.current

    if (System.currentTimeMillis() - dismissedAt < SUPPORT_BANNER_COOLDOWN_MS) return

    val gold = Color(0xFFD4AF37)
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(gold.copy(alpha = 0.85f), gold.copy(alpha = 0.15f))),
        ),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        )
                    )
                )
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.support_banner_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.support_banner_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { setDismissedAt(System.currentTimeMillis()) }) {
                    Text(stringResource(R.string.support_banner_later))
                }
                Button(
                    onClick = {
                        setDismissedAt(System.currentTimeMillis())
                        uriHandler.openUri(SUPPORT_URL)
                    },
                ) {
                    Text(stringResource(R.string.support_founder))
                }
            }
        }
    }
}
