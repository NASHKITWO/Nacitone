/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.extensions

import com.nacitone.music.models.MediaMetadata
import com.nacitone.music.models.PersistQueue
import com.nacitone.music.models.QueueData
import com.nacitone.music.models.QueueType
import com.nacitone.music.playback.queues.ListQueue
import com.nacitone.music.playback.queues.LocalAlbumRadio
import com.nacitone.music.playback.queues.Queue
import com.nacitone.music.playback.queues.YouTubeAlbumRadio
import com.nacitone.music.playback.queues.YouTubeQueue

fun Queue.toPersistQueue(
    title: String?,
    items: List<MediaMetadata>,
    mediaItemIndex: Int,
    position: Long
): PersistQueue {
    return when (this) {
        is ListQueue -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
        is YouTubeQueue -> {
            // Since endpoint is private, we'll store a simplified version
            val endpoint = "youtube_queue"
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE,
                queueData = QueueData.YouTubeData(endpoint = endpoint)
            )
        }
        is YouTubeAlbumRadio -> {
            // Since playlistId is private, we'll store a simplified version
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.YOUTUBE_ALBUM_RADIO,
                queueData = QueueData.YouTubeAlbumRadioData(
                    playlistId = "youtube_album_radio"
                )
            )
        }
        is LocalAlbumRadio -> {
            // Since albumWithSongs and startIndex are private, we'll store a simplified version
            PersistQueue(
                title = title,
                items = items,
                mediaItemIndex = mediaItemIndex,
                position = position,
                queueType = QueueType.LOCAL_ALBUM_RADIO,
                queueData = QueueData.LocalAlbumRadioData(
                    albumId = "local_album_radio",
                    startIndex = 0
                )
            )
        }
        else -> PersistQueue(
            title = title,
            items = items,
            mediaItemIndex = mediaItemIndex,
            position = position,
            queueType = QueueType.LIST
        )
    }
}

fun PersistQueue.toQueue(): Queue {
    // Berkas antrean bisa usang/rusak; indeks di luar rentang membuat ExoPlayer melempar exception.
    val safeIndex = mediaItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val safePosition = if (safeIndex == mediaItemIndex) position.coerceAtLeast(0L) else 0L
    return when (queueType) {
        is QueueType.LIST -> ListQueue(
            title = title,
            items = items.map { it.toMediaItem() },
            startIndex = safeIndex,
            position = safePosition
        )
        is QueueType.YOUTUBE -> {
            // For now, fallback to ListQueue since we can't reconstruct YouTubeQueue properly
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = safeIndex,
                position = safePosition
            )
        }
        is QueueType.YOUTUBE_ALBUM_RADIO -> {
            // For now, fallback to ListQueue since we can't reconstruct YouTubeAlbumRadio properly
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = safeIndex,
                position = safePosition
            )
        }
        is QueueType.LOCAL_ALBUM_RADIO -> {
            // For now, fallback to ListQueue since we can't reconstruct LocalAlbumRadio properly
            ListQueue(
                title = title,
                items = items.map { it.toMediaItem() },
                startIndex = safeIndex,
                position = safePosition
            )
        }
    }
}
