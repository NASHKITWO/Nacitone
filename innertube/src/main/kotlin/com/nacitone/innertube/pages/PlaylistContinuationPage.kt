package com.nacitone.innertube.pages

import com.nacitone.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
