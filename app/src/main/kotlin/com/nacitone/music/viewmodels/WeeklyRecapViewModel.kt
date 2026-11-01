/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nacitone.music.db.MusicDatabase
import com.nacitone.music.db.entities.Artist
import com.nacitone.music.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class WeeklyRecap(
    val songs: List<Song>,
    val artists: List<Artist>,
    val from: LocalDateTime,
    val to: LocalDateTime,
)

@HiltViewModel
class WeeklyRecapViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {
    val recap = MutableStateFlow<WeeklyRecap?>(null)
    val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val to = LocalDateTime.now()
                val from = to.minusDays(7)
                val songs = database.mostPlayedSongs(from, limit = 5, toTimeStamp = to).first()
                val artists = database.mostPlayedArtists(from, limit = 3, toTimeStamp = to).first()
                recap.value = WeeklyRecap(songs, artists, from, to)
            } finally {
                isLoading.value = false
            }
        }
    }
}
