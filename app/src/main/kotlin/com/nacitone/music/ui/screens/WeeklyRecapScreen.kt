/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nacitone.music.LocalPlayerAwareWindowInsets
import com.nacitone.music.R
import com.nacitone.music.ui.component.IconButton
import com.nacitone.music.ui.utils.backToMain
import com.nacitone.music.utils.ComposeToImage
import com.nacitone.music.utils.WeeklyRecapImage
import com.nacitone.music.viewmodels.WeeklyRecapViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyRecapScreen(
    navController: NavController,
    viewModel: WeeklyRecapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val recap by viewModel.recap.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var isSharing by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    val dateRange =
        recap?.let {
            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
            "${it.from.format(fmt)} - ${it.to.format(fmt)}"
        } ?: ""
    val failedTemplate = stringResource(R.string.failed_to_create_image)
    val shareTitle = stringResource(R.string.share)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(windowInsets.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(64.dp))

        Text(
            text = stringResource(R.string.weekly_recap_subtitle),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (dateRange.isNotEmpty()) {
            Text(
                text = dateRange,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))

        val data = recap
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            data == null || data.songs.isEmpty() -> {
                Text(
                    text = stringResource(R.string.weekly_recap_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.weekly_recap_top_songs),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                data.songs.forEachIndexed { index, song ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp),
                        )
                        AsyncImage(
                            model = song.song.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (data.artists.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.weekly_recap_top_artists),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    data.artists.forEachIndexed { index, artist ->
                        Text(
                            text = "${index + 1}   ${artist.artist.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    enabled = !isSharing,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        isSharing = true
                        scope.launch {
                            try {
                                val image =
                                    WeeklyRecapImage.create(
                                        context = context,
                                        dateRange = dateRange,
                                        songs =
                                            data.songs.map {
                                                WeeklyRecapImage.Row(
                                                    it.song.title,
                                                    it.artists.joinToString { a -> a.name },
                                                )
                                            },
                                        artists = data.artists.map { it.artist.name },
                                        accentColor = accent.toArgb(),
                                    )
                                val uri =
                                    ComposeToImage.saveBitmapAsFile(
                                        context,
                                        image,
                                        "recap_${System.currentTimeMillis()}",
                                    )
                                val intent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                context.startActivity(Intent.createChooser(intent, shareTitle))
                            } catch (e: Exception) {
                                Toast
                                    .makeText(context, String.format(failedTemplate, e.message ?: ""), Toast.LENGTH_SHORT)
                                    .show()
                            } finally {
                                isSharing = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.weekly_recap)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
    )
}
