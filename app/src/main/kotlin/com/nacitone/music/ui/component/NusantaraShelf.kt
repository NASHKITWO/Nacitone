/**
 * Nacitone (C) 2026
 * Licensed under GPL-3.0
 */

package com.nacitone.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nacitone.music.R

private data class NusantaraTile(
    val title: String,
    val region: String,
    val query: String,
    val from: Color,
    val to: Color,
)

// Nama genre/daerah adalah nama diri, jadi sama untuk semua bahasa.
private val nusantaraTiles =
    listOf(
        NusantaraTile("Dangdut", "Melayu · Nasional", "dangdut", Color(0xFFB4232C), Color(0xFF6E1018)),
        NusantaraTile("Koplo", "Jawa Timur", "dangdut koplo", Color(0xFFE07B00), Color(0xFF8C3F00)),
        NusantaraTile("Campursari", "Jawa Tengah", "campursari", Color(0xFF8A5A2B), Color(0xFF4A2E12)),
        NusantaraTile("Gamelan", "Jawa · Bali", "gamelan", Color(0xFFC9A227), Color(0xFF6B5410)),
        NusantaraTile("Pop Sunda", "Jawa Barat", "pop sunda", Color(0xFF1F8A5B), Color(0xFF0E4A31)),
        NusantaraTile("Pop Jawa", "Jawa", "lagu jawa", Color(0xFF2E7D9A), Color(0xFF123F52)),
        NusantaraTile("Minang", "Sumatera Barat", "lagu minang", Color(0xFFC2185B), Color(0xFF6A0F32)),
        NusantaraTile("Batak", "Sumatera Utara", "lagu batak", Color(0xFF5E35B1), Color(0xFF2E1A5C)),
        NusantaraTile("Bali", "Pulau Dewata", "lagu bali", Color(0xFF00897B), Color(0xFF004B43)),
        NusantaraTile("Banyuwangi", "Using", "musik banyuwangi", Color(0xFF7B1FA2), Color(0xFF3C0F50)),
        NusantaraTile("Melayu", "Sumatera · Kalimantan", "lagu melayu", Color(0xFF3949AB), Color(0xFF1A2260)),
        NusantaraTile("Ambon & Papua", "Timur Indonesia", "lagu ambon papua", Color(0xFF00796B), Color(0xFF023B35)),
        NusantaraTile("Qasidah", "Religi", "qasidah", Color(0xFF558B2F), Color(0xFF29441A)),
    )

/** Rak "Nusantara" di Beranda: pintasan besar ke musik daerah, dengan kartu bergradasi. */
@Composable
fun NusantaraShelf(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.nusantara_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.nusantara_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(nusantaraTiles, key = { it.title }) { tile ->
                Box(
                    modifier =
                        Modifier
                            .width(168.dp)
                            .height(104.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(tile.from, tile.to)))
                            .clickable { onSearch(tile.query) },
                ) {
                    // Hiasan lingkaran tipis ala motif batik/gelombang.
                    Box(
                        modifier =
                            Modifier
                                .size(110.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 0.dp)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(percent = 50)),
                    )
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            text = tile.title,
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = tile.region,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
