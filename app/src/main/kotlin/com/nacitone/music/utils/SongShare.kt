/**
 * Nacitone (C) 2026
 * Licensed under GPL-3.0
 */

package com.nacitone.music.utils

import android.content.Context
import android.content.Intent
import com.nacitone.music.db.MusicDatabase
import com.nacitone.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Berbagi lagu: judul, artis, cuplikan lirik (bila sudah tersimpan), lalu tautan putar.
 * Judul dan lirik ada di bagian atas supaya yang terlihat penerima adalah lagunya,
 * bukan hanya pratinjau tautan YouTube Music.
 */
object SongShare {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val MAX_LYRIC_LINES = 4
    private val timestampRegex = Regex("""\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?]|<\d{1,2}:\d{2}(?:[.:]\d{1,3})?>""")

    fun share(
        context: Context,
        database: MusicDatabase?,
        songId: String,
        title: String,
        artist: String,
        footer: String,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val snippet =
                runCatching { database?.lyrics(songId)?.first()?.lyrics?.let(::snippetFrom) }.getOrNull()
            val text =
                buildString {
                    append("🎵 ").append(title)
                    if (artist.isNotBlank()) append(" – ").append(artist)
                    if (!snippet.isNullOrBlank()) {
                        append("\n\n")
                        append(snippet.lines().joinToString("\n") { "“$it”" })
                    }
                    append("\n\n▶ https://music.youtube.com/watch?v=").append(songId)
                    append("\n\n").append(footer)
                }
            withContext(Dispatchers.Main) {
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                context.startActivity(
                    Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    /** Ambil beberapa baris pertama lirik, tanpa penanda waktu LRC dan baris kosong. */
    internal fun snippetFrom(raw: String): String? {
        if (raw.isBlank() || raw == LYRICS_NOT_FOUND) return null
        val lines =
            raw
                .lineSequence()
                .map { timestampRegex.replace(it, "").trim() }
                .filter { it.isNotEmpty() && !it.startsWith("[") }
                .take(MAX_LYRIC_LINES)
                .toList()
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }
}
