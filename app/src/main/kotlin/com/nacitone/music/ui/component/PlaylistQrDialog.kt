/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.ui.component

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nacitone.music.R
import com.nacitone.music.utils.ComposeToImage
import com.nacitone.music.utils.QrCodeShare
import kotlinx.coroutines.launch

/** Shows a QR code for a playlist link and lets the user share it as a Story-sized image card. */
@Composable
fun PlaylistQrDialog(
    link: String,
    title: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    val qr = remember(link) { QrCodeShare.qrBitmap(link, 720) }
    var isSharing by remember { mutableStateOf(false) }
    val failedTemplate = stringResource(R.string.failed_to_create_image)
    val shareTitle = stringResource(R.string.share)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.qr_share_title)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = stringResource(R.string.qr_share_title),
                    modifier = Modifier.size(240.dp).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.qr_scan_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSharing,
                onClick = {
                    isSharing = true
                    scope.launch {
                        try {
                            val image = QrCodeShare.createCard(context, title, link, accent)
                            val uri =
                                ComposeToImage.saveBitmapAsFile(
                                    context,
                                    image,
                                    "playlist_qr_${System.currentTimeMillis()}",
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
                Text(stringResource(R.string.qr_share_image))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
