/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nacitone.music.BuildConfig
import com.nacitone.music.R
import com.nacitone.music.ui.component.Material3SettingsGroup
import com.nacitone.music.ui.component.Material3SettingsItem
import com.nacitone.music.utils.Updater

/**
 * Quick menu shown from the home top bar: integrations, settings and update notice.
 * Account login (Google / token) has been removed from the app.
 */
@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String
) {
    val uriHandler = LocalUriHandler.current
    val hasUpdate = BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.close), contentDescription = null)
            }
        }

        Spacer(Modifier.height(12.dp))

        Material3SettingsGroup(
            items = buildList {
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.weekly_recap)) },
                        icon = painterResource(R.drawable.trending_up),
                        onClick = {
                            onClose()
                            navController.navigate("weekly_recap")
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.integrations)) },
                        icon = painterResource(R.drawable.integration),
                        onClick = {
                            onClose()
                            navController.navigate("settings/integrations")
                        }
                    )
                )
                add(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.settings)) },
                        icon = painterResource(R.drawable.settings),
                        showBadge = hasUpdate,
                        onClick = {
                            onClose()
                            navController.navigate("settings")
                        }
                    )
                )

                if (hasUpdate) {
                    val releaseInfo = Updater.getCachedLatestRelease()
                    val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
                    if (downloadUrl != null) {
                        add(
                            Material3SettingsItem(
                                title = { Text(stringResource(R.string.new_version_available)) },
                                description = { Text(latestVersionName) },
                                icon = painterResource(R.drawable.update),
                                showBadge = true,
                                onClick = { try { uriHandler.openUri(downloadUrl) } catch (_: Exception) {} }
                            )
                        )
                    }
                }
            },
            useLowContrast = true
        )
    }
}
