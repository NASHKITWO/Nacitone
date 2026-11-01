/**
 * Metrolist Project (C) 2026
 * OuterTune Project Copyright (C) 2025
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.utils

import android.content.Context
import com.nacitone.music.db.MusicDatabase
import com.nacitone.music.db.entities.PlaylistEntity
import com.nacitone.music.db.entities.SongEntity
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
 * Nacitone tidak punya login akun, jadi sinkronisasi dua arah dengan akun YouTube Music
 * sudah dihapus. Kelas ini tinggal menyimpan operasi lokal (membuat playlist, membersihkan
 * data podcast). Fungsi sinkronisasi di bawah dipertahankan sebagai no-op supaya pemanggil
 * lama tetap berkompilasi; hapus pemanggilnya satu per satu setelah build berhasil.
 */
@Singleton
class SyncUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Membuat playlist lokal. [syncWithYouTube] diabaikan karena tidak ada akun. */
    fun createPlaylist(
        playlist: PlaylistEntity,
        @Suppress("UNUSED_PARAMETER") syncWithYouTube: Boolean,
        onCreated: ((String, Boolean) -> Unit)? = null,
    ) {
        scope.launch {
            database.insert(playlist.copy(browseId = null, isAutoSync = false))
            withContext(Dispatchers.Main) { onCreated?.invoke(playlist.id, false) }
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

    // --- No-op: tidak ada akun untuk disinkronkan ---

    fun tryAutoSync() {}
    suspend fun performFullSyncSuspend() {}
    fun likeSong(@Suppress("UNUSED_PARAMETER") s: SongEntity) {}
    fun subscribeChannel(channelId: String, subscribe: Boolean) {}
    fun savePodcast(podcastId: String, save: Boolean) {}
    fun saveEpisode(episodeId: String, save: Boolean, setVideoId: String? = null) {}
    fun syncLikedSongs() {}
    fun syncLibrarySongs() {}
    fun syncUploadedSongs() {}
    fun syncLikedAlbums() {}
    fun syncArtistsSubscriptions() {}
    fun syncSavedPlaylists() {}
    suspend fun syncLikedSongsSuspend() {}
    suspend fun syncUploadedSongsSuspend() {}
    suspend fun syncPodcastSubscriptionsSuspend() {}
    suspend fun syncEpisodesForLaterSuspend() {}
    suspend fun syncPlaylistSuspend(browseId: String, playlistId: String) {}
    fun scheduleAddToPlaylist(browseId: String, playlistId: String, songIds: List<String>) {}
    fun scheduleRemoveFromPlaylist(
        browseId: String,
        songId: String,
        playlistId: String,
        getSetVideoId: suspend () -> String?,
    ) {}
}
