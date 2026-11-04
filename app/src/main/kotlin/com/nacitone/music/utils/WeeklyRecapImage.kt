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

            val white = 0xFFFFFFFF.toInt()
            val softWhite = 0xCCFFFFFF.toInt()
            val gold = 0xFFE9C46A.toInt()

            // Latar gradasi: aksen tema pengguna yang digelapkan -> hampir hitam keunguan.
            val deep = androidx.core.graphics.ColorUtils.blendARGB(accentColor, 0xFF0B0A12.toInt(), 0.72f)
            val mid = androidx.core.graphics.ColorUtils.blendARGB(accentColor, 0xFF0B0A12.toInt(), 0.45f)
            canvas.drawRect(
                RectF(0f, 0f, w.toFloat(), h.toFloat()),
                Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, w.toFloat(), h.toFloat(),
                        intArrayOf(mid, deep, 0xFF0B0A12.toInt()),
                        floatArrayOf(0f, 0.55f, 1f),
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                },
            )
            // Lingkaran dekoratif
            val deco = Paint().apply { isAntiAlias = true; color = 0x14FFFFFF }
            canvas.drawCircle(w - 80f, 260f, 420f, deco)
            canvas.drawCircle(60f, 1500f, 520f, deco)
            canvas.drawCircle(w * 0.62f, 1180f, 260f, Paint().apply { isAntiAlias = true; color = 0x0DFFFFFF })

            fun paint(size: Float, color: Int, bold: Boolean, serif: Boolean = false) =
                TextPaint().apply {
                    this.color = color
                    textSize = size
                    typeface =
                        when {
                            serif && bold -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
                            serif -> Typeface.SERIF
                            bold -> Typeface.DEFAULT_BOLD
                            else -> Typeface.DEFAULT
                        }
                    isAntiAlias = true
                }

            fun ellipsized(text: String, p: TextPaint, width: Float): String =
                TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString()

            val left = 90f
            val right = w - 90f
            val textWidth = right - left

            // Header (di dalam area aman Story)
            canvas.drawText(
                context.getString(R.string.app_name).uppercase(),
                left, 250f,
                paint(34f, gold, true).apply { letterSpacing = 0.3f },
            )
            canvas.drawText(
                context.getString(R.string.weekly_recap),
                left, 350f,
                paint(88f, white, true, serif = true),
            )
            canvas.drawText(dateRange, left, 415f, paint(38f, softWhite, false))

            val card = Paint().apply { isAntiAlias = true; color = 0x22FFFFFF }
            val cardStroke = Paint().apply {
                isAntiAlias = true
                color = 0x33FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            // Lagu #1: kartu besar
            songs.firstOrNull()?.let { top ->
                val rect = RectF(left, 480f, right, 800f)
                canvas.drawRoundRect(rect, 48f, 48f, card)
                canvas.drawRoundRect(rect, 48f, 48f, cardStroke)
                canvas.drawText(
                    context.getString(R.string.weekly_recap_top_songs).uppercase(),
                    left + 44f, 550f,
                    paint(30f, gold, true).apply { letterSpacing = 0.2f },
                )
                canvas.drawText("1", left + 44f, 730f, paint(170f, 0x66FFFFFF, true, serif = true))
                val tp = paint(56f, white, true, serif = true)
                canvas.drawText(ellipsized(top.title, tp, textWidth - 230f), left + 200f, 690f, tp)
                top.subtitle?.let {
                    val sp = paint(36f, softWhite, false)
                    canvas.drawText(ellipsized(it, sp, textWidth - 230f), left + 200f, 745f, sp)
                }
            }

            // Lagu #2-5: baris kartu
            var y = 840f
            songs.drop(1).take(4).forEachIndexed { index, row ->
                val rect = RectF(left, y, right, y + 116f)
                canvas.drawRoundRect(rect, 36f, 36f, card)
                canvas.drawText("${index + 2}", left + 40f, y + 76f, paint(52f, gold, true, serif = true))
                val tp = paint(40f, white, true)
                val sp = paint(30f, softWhite, false)
                val tw = textWidth - 130f
                if (row.subtitle != null) {
                    canvas.drawText(ellipsized(row.title, tp, tw), left + 110f, y + 52f, tp)
                    canvas.drawText(ellipsized(row.subtitle, sp, tw), left + 110f, y + 92f, sp)
                } else {
                    canvas.drawText(ellipsized(row.title, tp, tw), left + 110f, y + 72f, tp)
                }
                y += 136f
            }

            // Artis teratas: chip bergaris emas
            y = maxOf(y + 30f, 1420f)
            canvas.drawText(
                context.getString(R.string.weekly_recap_top_artists).uppercase(),
                left, y,
                paint(30f, gold, true).apply { letterSpacing = 0.2f },
            )
            val artistPaint = paint(52f, white, true, serif = true)
            artists.take(3).forEachIndexed { index, name ->
                y += 92f
                canvas.drawText("${index + 1}", left, y, paint(52f, gold, true, serif = true))
                canvas.drawText(ellipsized(name, artistPaint, textWidth - 90f), left + 90f, y, artistPaint)
            }

            // Footer
            val footer = context.getString(R.string.lyrics_card_from, context.getString(R.string.app_name))
            canvas.drawText(footer, left, h - 230f, paint(36f, softWhite, true))

            bitmap
        }
}
