/**
 * Metrolist Project (C) 2026
 * OuterTune Project Copyright (C) 2025
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.utils

import android.content.Context
import com.nacitone.music.db.MusicDatabase
import com.nacitone.music.db.entities.PlaylistEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nacitone tidak punya login akun, jadi sinkronisasi dengan akun YouTube Music sudah dihapus.
 * Kelas ini hanya berisi operasi lokal: membuat playlist dan membersihkan data podcast.
 */
@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Membuat playlist lokal, lalu memanggil [onCreated] dengan ID-nya di thread utama. */
    fun createPlaylist(
        playlist: PlaylistEntity,
        onCreated: ((String) -> Unit)? = null,
    ) {
        scope.launch {
            database.insert(playlist.copy(browseId = null, isAutoSync = false))
            withContext(Dispatchers.Main) { onCreated?.invoke(playlist.id) }
        }
    }

    /** Menghapus podcast yang berlangganan dan episode tersimpan dari pustaka lokal. */
    fun clearPodcastData() {
        scope.launch {
            try {
                val subscribed = database.subscribedPodcasts().first()
                val savedEpisodes = database.podcastEpisodesByCreateDateAsc().first()
                    .filter { it.song.inLibrary != null }
                database.withTransaction {
                    subscribed.forEach { database.update(it.copy(bookmarkedAt = null)) }
                    savedEpisodes.forEach { database.update(it.song.copy(inLibrary = null)) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Gagal membersihkan data podcast")
            }
        }
    }
}
