package com.nacitone.music.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bilah atas bergaya kaca: latar transparan, judul tebal, dan tombol kembali
 * dalam lingkaran kaca. Menjadi sedikit pekat hanya saat konten digulir di bawahnya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        ),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)) {
                title()
            }
        },
        modifier = modifier,
        navigationIcon = {
            Box(
                modifier =
                    Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GlassTint.copy(alpha = 0.08f))
                        .border(1.dp, GlassTint.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                navigationIcon()
            }
        },
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}
