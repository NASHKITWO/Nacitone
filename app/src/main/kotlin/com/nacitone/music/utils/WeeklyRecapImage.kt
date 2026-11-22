/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.createBitmap
import com.nacitone.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Draws the shareable weekly recap card (Instagram Story size, 1080x1920). */
object WeeklyRecapImage {
    data class Row(val title: String, val subtitle: String?, val cover: Bitmap? = null)

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
            val gold = 0xFFDDE6F5.toInt() // putih es, selaras tema bening

            // Latar hitam pekat; aksen tema hanya jadi cahaya tipis di sudut agar kaca punya sesuatu untuk dibiaskan.
            canvas.drawRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint().apply { color = 0xFF000000.toInt() })
            val glow = androidx.core.graphics.ColorUtils.setAlphaComponent(accentColor, 0x2E)
            canvas.drawRect(
                RectF(0f, 0f, w.toFloat(), h.toFloat()),
                Paint().apply {
                    shader = android.graphics.RadialGradient(
                        w * 0.1f, h * 0.05f, w * 1.1f,
                        glow, 0x00000000,
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                },
            )
            canvas.drawRect(
                RectF(0f, 0f, w.toFloat(), h.toFloat()),
                Paint().apply {
                    shader = android.graphics.RadialGradient(
                        w * 0.95f, h * 0.97f, w * 0.9f,
                        0x14FFFFFF, 0x00000000,
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                },
            )

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

            fun drawCover(bmp: Bitmap?, box: RectF, radius: Float) {
                if (bmp == null || bmp.width == 0 || bmp.height == 0) {
                    canvas.drawRoundRect(box, radius, radius, Paint().apply { isAntiAlias = true; color = 0x33FFFFFF })
                    return
                }
                val side = minOf(bmp.width, bmp.height)
                val src = Rect((bmp.width - side) / 2, (bmp.height - side) / 2, (bmp.width + side) / 2, (bmp.height + side) / 2)
                val save = canvas.save()
                canvas.clipPath(Path().apply { addRoundRect(box, radius, radius, Path.Direction.CW) })
                canvas.drawBitmap(bmp, src, box, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
                canvas.restoreToCount(save)
            }

            val left = 90f
            val right = w - 90f
            val textWidth = right - left

            // Header (di dalam area aman Story)
            canvas.drawText(
                context.getString(R.string.app_name).uppercase(),
                left, 250f,
                paint(34f, gold, true).apply { letterSpacing = 0.3f },
            )
            val titleText = context.getString(R.string.weekly_recap)
            val titlePaint = paint(88f, white, true, serif = true)
            // Kecilkan huruf sampai judul muat di lebar kartu (hindari terpotong di tepi).
            while (titlePaint.measureText(titleText) > textWidth && titlePaint.textSize > 40f) {
                titlePaint.textSize -= 2f
            }
            canvas.drawText(titleText, left, 350f, titlePaint)
            canvas.drawText(dateRange, left, 415f, paint(38f, softWhite, false))

            val card = Paint().apply { isAntiAlias = true; color = 0x14FFFFFF }
            val cardStroke = Paint().apply {
                isAntiAlias = true
                color = 0x2EFFFFFF
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
                drawCover(top.cover, RectF(left + 36f, 580f, left + 236f, 780f), 32f)
                val tp = paint(52f, white, true, serif = true)
                canvas.drawText(ellipsized(top.title, tp, textWidth - 300f), left + 270f, 690f, tp)
                top.subtitle?.let {
                    val sp = paint(34f, softWhite, false)
                    canvas.drawText(ellipsized(it, sp, textWidth - 300f), left + 270f, 745f, sp)
                }
            }

            // Lagu #2-5: baris kartu
            var y = 840f
            songs.drop(1).take(4).forEachIndexed { index, row ->
                val rect = RectF(left, y, right, y + 116f)
                canvas.drawRoundRect(rect, 36f, 36f, card)
                canvas.drawRoundRect(rect, 36f, 36f, cardStroke)
                canvas.drawText("${index + 2}", left + 32f, y + 74f, paint(44f, gold, true, serif = true))
                drawCover(row.cover, RectF(left + 90f, y + 16f, left + 190f, y + 100f), 22f)
                val tp = paint(40f, white, true)
                val sp = paint(30f, softWhite, false)
                val tw = textWidth - 240f
                if (row.subtitle != null) {
                    canvas.drawText(ellipsized(row.title, tp, tw), left + 215f, y + 52f, tp)
                    canvas.drawText(ellipsized(row.subtitle, sp, tw), left + 215f, y + 92f, sp)
                } else {
                    canvas.drawText(ellipsized(row.title, tp, tw), left + 215f, y + 72f, tp)
                }
                y += 136f
            }

            // Artis teratas: chip bergaris emas
            y = maxOf(y + 10f, 1400f)
            canvas.drawText(
                context.getString(R.string.weekly_recap_top_artists).uppercase(),
                left, y,
                paint(30f, gold, true).apply { letterSpacing = 0.2f },
            )
            val artistPaint = paint(52f, white, true, serif = true)
            artists.take(3).forEachIndexed { index, name ->
                y += 84f
                canvas.drawText("${index + 1}", left, y, paint(52f, gold, true, serif = true))
                canvas.drawText(ellipsized(name, artistPaint, textWidth - 90f), left + 90f, y, artistPaint)
            }

            // Footer
            val footer = context.getString(R.string.lyrics_card_from, context.getString(R.string.app_name))
            canvas.drawText(footer, left, h - 110f, paint(34f, softWhite, true))

            bitmap
        }
}
