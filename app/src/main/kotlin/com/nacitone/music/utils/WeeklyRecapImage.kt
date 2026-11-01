/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import com.nacitone.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Draws the shareable weekly recap card (Instagram Story size, 1080x1920). */
object WeeklyRecapImage {
    data class Row(val title: String, val subtitle: String?)

    suspend fun create(
        context: Context,
        dateRange: String,
        songs: List<Row>,
        artists: List<String>,
        accentColor: Int,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val w = 1080
            val h = 1920
            val bitmap = createBitmap(w, h)
            val canvas = Canvas(bitmap)

            val background = 0xFF14121A.toInt()
            val primaryText = 0xFFE6E1EC.toInt()
            val secondaryText = 0xFFA9A4B4.toInt()

            canvas.drawRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint().apply { color = background })

            fun paint(size: Float, color: Int, bold: Boolean) =
                TextPaint().apply {
                    this.color = color
                    textSize = size
                    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    isAntiAlias = true
                }

            fun ellipsized(text: String, p: TextPaint, width: Float): String =
                TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString()

            val left = 90f
            val textWidth = w - left * 2

            // Header (kept inside the Story safe zone)
            val labelPaint = paint(40f, accentColor, true).apply { letterSpacing = 0.12f }
            canvas.drawText(context.getString(R.string.weekly_recap).uppercase(), left, 280f, labelPaint)
            canvas.drawText(dateRange, left, 345f, paint(38f, secondaryText, false))

            // Top songs
            var y = 470f
            canvas.drawText(context.getString(R.string.weekly_recap_top_songs), left, y, paint(46f, primaryText, true))
            y += 40f
            val rankPaint = paint(64f, accentColor, true)
            val titlePaint = paint(48f, primaryText, true)
            val subPaint = paint(36f, secondaryText, false)
            songs.take(5).forEachIndexed { index, row ->
                y += 120f
                canvas.drawText("${index + 1}", left, y, rankPaint)
                val tx = left + 90f
                val tw = textWidth - 90f
                canvas.drawText(ellipsized(row.title, titlePaint, tw), tx, y - 12f, titlePaint)
                row.subtitle?.let { canvas.drawText(ellipsized(it, subPaint, tw), tx, y + 32f, subPaint) }
                y += 24f
            }

            // Top artists
            y = 1230f
            canvas.drawText(context.getString(R.string.weekly_recap_top_artists), left, y, paint(46f, primaryText, true))
            val artistPaint = paint(56f, primaryText, true)
            artists.take(3).forEachIndexed { index, name ->
                y += 100f
                canvas.drawText("${index + 1}", left, y, rankPaint)
                canvas.drawText(ellipsized(name, artistPaint, textWidth - 90f), left + 90f, y, artistPaint)
            }

            // Footer watermark
            val footer = context.getString(R.string.lyrics_card_from, context.getString(R.string.app_name))
            canvas.drawText(footer, left, h - 260f, paint(40f, secondaryText, true))

            bitmap
        }
}
