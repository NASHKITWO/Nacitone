/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.lyrics

import android.content.Context
import com.nacitone.music.betterlyrics.BetterLyrics
import com.nacitone.music.constants.EnableBetterLyricsKey
import com.nacitone.music.utils.dataStore
import com.nacitone.music.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)
}
