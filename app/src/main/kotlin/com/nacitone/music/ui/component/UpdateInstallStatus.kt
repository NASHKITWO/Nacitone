package com.nacitone.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nacitone.music.R
import com.nacitone.music.utils.ApkInstaller
import com.nacitone.music.utils.UpdateInstallState

/** Teks status pembaruan dalam aplikasi, atau null bila tidak ada yang berjalan. */
@Composable
fun updateInstallStatusText(): String? {
    val state by ApkInstaller.state.collectAsState()
    return when (val s = state) {
        UpdateInstallState.Idle -> null
        is UpdateInstallState.Downloading ->
            if (s.percent >= 0) {
                stringResource(R.string.update_download_progress, s.percent)
            } else {
                stringResource(R.string.update_downloading)
            }
        UpdateInstallState.Verifying -> stringResource(R.string.update_verifying)
        UpdateInstallState.Installing -> stringResource(R.string.update_installing)
        is UpdateInstallState.Failed ->
            when (s.reason) {
                UpdateInstallState.Reason.NEED_PERMISSION -> stringResource(R.string.update_need_permission)
                UpdateInstallState.Reason.DOWNLOAD -> stringResource(R.string.update_failed_download)
                UpdateInstallState.Reason.VERIFY -> stringResource(R.string.update_failed_verify, s.detail.orEmpty())
                UpdateInstallState.Reason.INSTALL -> stringResource(R.string.update_failed_install, s.detail.orEmpty())
            }
    }
}

@Composable
fun UpdateInstallProgress(modifier: Modifier = Modifier) {
    val state by ApkInstaller.state.collectAsState()
    when (val s = state) {
        is UpdateInstallState.Downloading ->
            if (s.percent >= 0) {
                LinearProgressIndicator(progress = { s.percent / 100f }, modifier = modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = modifier.fillMaxWidth())
            }
        UpdateInstallState.Verifying, UpdateInstallState.Installing ->
            LinearProgressIndicator(modifier = modifier.fillMaxWidth())
        else -> Unit
    }
}

/** Blok status + progres untuk dialog pembaruan. */
@Composable
fun UpdateInstallBlock(modifier: Modifier = Modifier) {
    val text = updateInstallStatusText() ?: return
    val state by ApkInstaller.state.collectAsState()
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (state is UpdateInstallState.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        UpdateInstallProgress(Modifier.padding(top = 6.dp))
    }
}
