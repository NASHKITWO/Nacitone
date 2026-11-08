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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import com.nacitone.music.utils.dataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.datastore.preferences.core.edit
import androidx.compose.runtime.getValue
import com.nacitone.music.constants.SupportBannerDismissedAtKey

/** Tautan dukungan untuk pengembang (Sociabuzz). Satu-satunya tempat URL ini didefinisikan. */
const val SUPPORT_URL = "https://sociabuzz.com/nacitwo/tribe"

private const val SUPPORT_BANNER_COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000

/** Banner dukungan di Beranda. Setelah ditutup, tidak tampil lagi selama 30 hari. */
@Composable
fun SupportBanner(modifier: Modifier = Modifier) {
    val (dismissedAt, setDismissedAt) = rememberLoadedLong(SupportBannerDismissedAtKey)
    val uriHandler = LocalUriHandler.current

    // Sebelum DataStore selesai dibaca (null), jangan tampilkan: mencegah kedipan banner.
    if (dismissedAt == null || System.currentTimeMillis() - dismissedAt < SUPPORT_BANNER_COOLDOWN_MS) return

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.support_banner_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.support_banner_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Jeda minimal antar pop-up dukungan. */
private const val SUPPORT_POPUP_INTERVAL_MS = 24L * 60 * 60 * 1000

/** Pop-up hanya dijadwalkan sekali per proses aplikasi, supaya tidak mengganggu saat berpindah layar. */
@Volatile
private var supportPopupShownThisProcess = false

/**
 * Status tampil disimpan di luar composable: kalau Beranda dibuat ulang (mis. layar dirender ulang
 * setelah antrean dipulihkan), pop-up tidak hilang begitu saja.
 */
private var supportPopupVisible by androidx.compose.runtime.mutableStateOf(false)

/**
 * Pop-up dukungan untuk pengembang. Muncul di Beranda paling cepat setiap 24 jam (dan saat pertama
 * kali membuka aplikasi), sekali per pembukaan aplikasi. Waktu dicatat saat pengguna menutupnya.
 */
@Composable
fun SupportPopup() {
    val (shownAt, setShownAt) = rememberLoadedLong(com.nacitone.music.constants.SupportPopupShownAtKey)
    val uriHandler = LocalUriHandler.current

    androidx.compose.runtime.LaunchedEffect(shownAt) {
        if (shownAt != null &&
            !supportPopupShownThisProcess &&
            System.currentTimeMillis() - shownAt >= SUPPORT_POPUP_INTERVAL_MS
        ) {
            kotlinx.coroutines.delay(2500)
            supportPopupShownThisProcess = true
            supportPopupVisible = true
        }
    }

    if (!supportPopupVisible) return
    val close = {
        supportPopupVisible = false
        setShownAt(System.currentTimeMillis())
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { close() },
        title = {
            Text(
                text = stringResource(R.string.support_banner_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = { Text(stringResource(R.string.support_banner_message)) },
        confirmButton = {
            Button(
                onClick = {
                    close()
                    uriHandler.openUri(SUPPORT_URL)
                },
            ) {
                Text(stringResource(R.string.support_founder))
            }
        },
        dismissButton = {
            TextButton(onClick = { close() }) {
                Text(stringResource(R.string.support_banner_later))
            }
        },
    )
}

/**
 * Seperti rememberPreference, tetapi bernilai null sampai DataStore selesai dibaca, sehingga
 * tidak ada keputusan yang diambil dari nilai bawaan sementara (mencegah kedipan/pop-up salah).
 */
@Composable
private fun rememberLoadedLong(key: androidx.datastore.preferences.core.Preferences.Key<Long>): Pair<Long?, (Long) -> Unit> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val flow = androidx.compose.runtime.remember {
        context.dataStore.data.map<androidx.datastore.preferences.core.Preferences, Long?> { it[key] ?: 0L }
    }
    val value by flow.collectAsStateWithLifecycle(initialValue = null)
    return value to { newValue: Long ->
        scope.launch { context.dataStore.edit { it[key] = newValue } }
        Unit
    }
}
