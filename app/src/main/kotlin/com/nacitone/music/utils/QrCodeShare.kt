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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.nacitone.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Builds QR codes and the shareable playlist QR card (Story size, 1080x1920). */
object QrCodeShare {
    /** Black-on-white QR code for [text], [size] pixels square. */
    fun qrBitmap(
        text: String,
        size: Int,
    ): Bitmap {
        val hints =
            mapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels =
            IntArray(size * size) { i ->
                if (matrix.get(i % size, i / size)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    suspend fun createCard(
        context: Context,
        title: String,
        link: String,
        accentColor: Int,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val w = 1080
            val h = 1920
            val bitmap = createBitmap(w, h)
            val canvas = Canvas(bitmap)
            canvas.drawRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint().apply { color = 0xFF14121A.toInt() })

            val primaryText = 0xFFE6E1EC.toInt()
            val secondaryText = 0xFFA9A4B4.toInt()
            val left = 90f
            val contentWidth = (w - left * 2).toInt()

            // Label + playlist name (kept inside the Story safe zone)
            val labelPaint =
                TextPaint().apply {
                    color = accentColor
                    textSize = 40f
                    typeface = Typeface.DEFAULT_BOLD
                    letterSpacing = 0.12f
                    isAntiAlias = true
                }
            canvas.drawText(context.getString(R.string.qr_card_label).uppercase(), left, 300f, labelPaint)

            val titlePaint =
                TextPaint().apply {
                    color = primaryText
                    textSize = 84f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
            val titleLayout =
                StaticLayout.Builder
                    .obtain(title, 0, title.length, titlePaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setMaxLines(3)
                    .setEllipsize(android.text.TextUtils.TruncateAt.END)
                    .build()
            canvas.withTranslation(left, 340f) { titleLayout.draw(this) }

            // QR on a white rounded tile
            val tile = 800f
            val tileLeft = (w - tile) / 2f
            val tileTop = 760f
            canvas.drawRoundRect(
                RectF(tileLeft, tileTop, tileLeft + tile, tileTop + tile),
                48f,
                48f,
                Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    isAntiAlias = true
                },
            )
            val qr = qrBitmap(link, 680)
            canvas.drawBitmap(qr, tileLeft + 60f, tileTop + 60f, null)

            // Hint + watermark
            val hintPaint =
                TextPaint().apply {
                    color = secondaryText
                    textSize = 44f
                    isAntiAlias = true
                }
            val hint = context.getString(R.string.qr_scan_hint)
            val hintWidth = hintPaint.measureText(hint)
            canvas.drawText(hint, (w - hintWidth) / 2f, tileTop + tile + 100f, hintPaint)

            val footer = context.getString(R.string.lyrics_card_from, context.getString(R.string.app_name))
            val footerPaint =
                TextPaint().apply {
                    color = secondaryText
                    textSize = 40f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
            canvas.drawText(footer, left, h - 260f, footerPaint)

            bitmap
        }
}
