/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.nacitone.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.ColorUtils
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
            val full = RectF(0f, 0f, w.toFloat(), h.toFloat())

            val white = 0xFFFFFFFF.toInt()
            val softWhite = 0xCCFFFFFF.toInt()
            val ice = 0xFFDDE6F5.toInt()

            canvas.drawRect(full, Paint().apply { color = 0xFF000000.toInt() })

            // Latar: sampul lagu #1 dikaburkan (diperkecil lalu dibentangkan), digelapkan ke bawah.
            val topCover = songs.firstOrNull()?.cover?.takeIf { it.width > 0 && it.height > 0 }
            if (topCover != null) {
                val tiny = Bitmap.createScaledBitmap(topCover, 24, 24, true)
                canvas.drawBitmap(
                    tiny,
                    null,
                    RectF(-200f, -200f, w + 200f, h * 0.75f),
                    Paint().apply { isFilterBitmap = true; alpha = 150 },
                )
                if (tiny !== topCover) tiny.recycle()
            } else {
                val glow = ColorUtils.setAlphaComponent(accentColor, 0x3A)
                canvas.drawRect(
                    full,
                    Paint().apply { shader = RadialGradient(w * 0.15f, h * 0.08f, w * 1.1f, glow, 0x00000000, Shader.TileMode.CLAMP) },
                )
            }
            canvas.drawRect(
                full,
                Paint().apply {
                    shader =
                        LinearGradient(
                            0f, 0f, 0f, h.toFloat(),
                            intArrayOf(0x66000000, 0xCC000000.toInt(), 0xFF000000.toInt()),
                            floatArrayOf(0f, 0.45f, 0.85f),
                            Shader.TileMode.CLAMP,
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

            // Isi kotak penuh (potong tengah sesuai rasio kotak), bukan meregangkan sampul.
            fun drawCover(bmp: Bitmap?, box: RectF, radius: Float) {
                if (bmp == null || bmp.width == 0 || bmp.height == 0) {
                    canvas.drawRoundRect(box, radius, radius, Paint().apply { isAntiAlias = true; color = 0x33FFFFFF })
                    return
                }
                val boxRatio = box.width() / box.height()
                val bmpRatio = bmp.width.toFloat() / bmp.height
                val src =
                    if (bmpRatio > boxRatio) {
                        val sw = (bmp.height * boxRatio).toInt().coerceAtLeast(1)
                        Rect((bmp.width - sw) / 2, 0, (bmp.width + sw) / 2, bmp.height)
                    } else {
                        val sh = (bmp.width / boxRatio).toInt().coerceAtLeast(1)
                        Rect(0, (bmp.height - sh) / 2, bmp.width, (bmp.height + sh) / 2)
                    }
                val save = canvas.save()
                canvas.clipPath(Path().apply { addRoundRect(box, radius, radius, Path.Direction.CW) })
                canvas.drawBitmap(bmp, src, box, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
                canvas.restoreToCount(save)
            }

            fun pill(text: String, x: Float, baseline: Float, size: Float, textColor: Int) {
                val p = paint(size, textColor, true).apply { letterSpacing = 0.12f }
                val padX = 28f
                val tw = p.measureText(text)
                val rect = RectF(x, baseline - size - 12f, x + tw + padX * 2, baseline + 18f)
                canvas.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, Paint().apply { isAntiAlias = true; color = 0x2AFFFFFF })
                canvas.drawRoundRect(
                    rect, rect.height() / 2, rect.height() / 2,
                    Paint().apply { isAntiAlias = true; color = 0x55FFFFFF; style = Paint.Style.STROKE; strokeWidth = 2f },
                )
                canvas.drawText(text, x + padX, baseline, p)
            }

            val left = 80f
            val right = w - 80f
            val textWidth = right - left

            // Header (di dalam area aman Story)
            canvas.drawText(
                context.getString(R.string.app_name).uppercase(),
                left, 200f,
                paint(32f, ice, true).apply { letterSpacing = 0.34f },
            )
            val titleText = context.getString(R.string.weekly_recap)
            val titlePaint = paint(104f, white, true, serif = true)
            while (titlePaint.measureText(titleText) > textWidth && titlePaint.textSize > 40f) {
                titlePaint.textSize -= 2f
            }
            canvas.drawText(titleText, left, 315f, titlePaint)
            pill(dateRange, left, 400f, 32f, white)

            // Lagu #1: kartu pahlawan, sampul memenuhi kartu, angka raksasa di sudut.
            val hero = RectF(left, 470f, right, 1010f)
            songs.firstOrNull()?.let { top ->
                drawCover(top.cover, hero, 56f)
                val save = canvas.save()
                canvas.clipPath(Path().apply { addRoundRect(hero, 56f, 56f, Path.Direction.CW) })
                canvas.drawRect(
                    hero,
                    Paint().apply {
                        shader =
                            LinearGradient(
                                0f, hero.top + 120f, 0f, hero.bottom,
                                0x00000000, 0xE6000000.toInt(), Shader.TileMode.CLAMP,
                            )
                    },
                )
                canvas.restoreToCount(save)
                canvas.drawRoundRect(
                    hero, 56f, 56f,
                    Paint().apply { isAntiAlias = true; color = 0x44FFFFFF; style = Paint.Style.STROKE; strokeWidth = 2f },
                )
                pill(context.getString(R.string.weekly_recap_top_songs).uppercase(), hero.left + 32f, hero.top + 74f, 28f, white)
                val big = paint(340f, 0xEBFFFFFF.toInt(), true, serif = true).apply { textAlign = Paint.Align.RIGHT }
                canvas.drawText("1", hero.right - 36f, hero.bottom - 28f, big)
                val tw = textWidth * 0.66f
                val tp = paint(58f, white, true, serif = true)
                canvas.drawText(ellipsized(top.title, tp, tw), hero.left + 40f, hero.bottom - 96f, tp)
                top.subtitle?.let {
                    val sp = paint(36f, softWhite, false)
                    canvas.drawText(ellipsized(it, sp, tw), hero.left + 40f, hero.bottom - 44f, sp)
                }
            }

            // Lagu #2-5: baris kaca
            var y = 1046f
            songs.drop(1).take(4).forEachIndexed { index, row ->
                val rect = RectF(left, y, right, y + 104f)
                canvas.drawRoundRect(rect, 52f, 52f, Paint().apply { isAntiAlias = true; color = 0x1AFFFFFF })
                canvas.drawRoundRect(
                    rect, 52f, 52f,
                    Paint().apply { isAntiAlias = true; color = 0x33FFFFFF; style = Paint.Style.STROKE; strokeWidth = 2f },
                )
                val num = paint(46f, ice, true, serif = true).apply { textAlign = Paint.Align.CENTER }
                canvas.drawText("${index + 2}", left + 48f, y + 68f, num)
                drawCover(row.cover, RectF(left + 92f, y + 10f, left + 176f, y + 94f), 42f)
                val tp = paint(38f, white, true)
                val sp = paint(28f, softWhite, false)
                val tw = textWidth - 220f
                if (row.subtitle != null) {
                    canvas.drawText(ellipsized(row.title, tp, tw), left + 204f, y + 48f, tp)
                    canvas.drawText(ellipsized(row.subtitle, sp, tw), left + 204f, y + 88f, sp)
                } else {
                    canvas.drawText(ellipsized(row.title, tp, tw), left + 204f, y + 66f, tp)
                }
                y += 120f
            }

            // Artis teratas: tiga lingkaran berinisial, yang pertama lebih besar dan bergaris terang.
            val artistTop = maxOf(y + 40f, 1590f)
            canvas.drawText(
                context.getString(R.string.weekly_recap_top_artists).uppercase(),
                left, artistTop,
                paint(28f, ice, true).apply { letterSpacing = 0.22f },
            )
            val shown = artists.take(3)
            if (shown.isNotEmpty()) {
                val colW = textWidth / shown.size
                shown.forEachIndexed { index, name ->
                    val cx = left + colW * index + colW / 2
                    val r = if (index == 0) 62f else 52f
                    val cy = artistTop + 40f + r + (62f - r)
                    canvas.drawCircle(cx, cy, r, Paint().apply { isAntiAlias = true; color = 0x22FFFFFF })
                    canvas.drawCircle(
                        cx, cy, r,
                        Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeWidth = if (index == 0) 4f else 2f
                            color = if (index == 0) white else 0x55FFFFFF
                        },
                    )
                    canvas.drawText(
                        name.trim().take(1).uppercase(),
                        cx, cy + 20f,
                        paint(if (index == 0) 58f else 48f, white, true, serif = true).apply { textAlign = Paint.Align.CENTER },
                    )
                    val np = paint(30f, white, index == 0).apply { textAlign = Paint.Align.CENTER }
                    canvas.drawText(ellipsized(name, np, colW - 24f), cx, artistTop + 40f + 62f * 2 + 44f, np)
                }
            }

            // Footer
            val footer = context.getString(R.string.lyrics_card_from, context.getString(R.string.app_name))
            canvas.drawText(footer, left, h - 70f, paint(30f, softWhite, true))

            bitmap
        }
}
