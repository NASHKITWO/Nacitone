package com.nacitone.music.playback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import timber.log.Timber

/**
 * Jendela lirik mengambang di atas aplikasi lain: kaca bening di atas hitam, bisa digeser.
 * Dibuat dari View biasa (bukan Compose) supaya tidak butuh LifecycleOwner di dalam Service.
 */
class LyricsOverlay(
    private val context: Context,
    private val onClose: () -> Unit,
    private val onOpenApp: () -> Unit = {},
) {
    private val positionPrefs = context.getSharedPreferences("lyrics_overlay", Context.MODE_PRIVATE)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private var titleView: TextView? = null
    private var prevView: TextView? = null
    private var currentView: TextView? = null
    private var nextView: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    val isShowing: Boolean get() = root != null

    private fun overlayWidth(): Int = (context.resources.displayMetrics.widthPixels * 0.92f).toInt()

    /** Jaga jendela tetap di dalam layar (posisi lama bisa di luar layar setelah rotasi). */
    private fun clamp(lp: WindowManager.LayoutParams, view: View) {
        val dm = context.resources.displayMetrics
        val maxX = ((dm.widthPixels - lp.width) / 2).coerceAtLeast(0)
        val viewHeight = if (view.height > 0) view.height else dp(120)
        val maxY = (dm.heightPixels - viewHeight - dp(24)).coerceAtLeast(0)
        lp.x = lp.x.coerceIn(-maxX, maxX)
        lp.y = lp.y.coerceIn(0, maxY)
    }

    /** Dipanggil saat konfigurasi berubah (rotasi, ukuran layar lipat). */
    fun onConfigurationChanged() {
        val view = root ?: return
        val lp = params ?: return
        lp.width = overlayWidth()
        clamp(lp, view)
        runCatching { windowManager.updateViewLayout(view, lp) }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (root != null) return
        val bg =
            GradientDrawable().apply {
                setColor(0xE6000000.toInt())
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), 0x2EFFFFFF)
            }
        fun line(sizeSp: Float, color: Int, bold: Boolean, maxLines: Int) =
            TextView(context).apply {
                setTextColor(color)
                textSize = sizeSp
                gravity = Gravity.CENTER
                this.maxLines = maxLines
                ellipsize = TextUtils.TruncateAt.END
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        val header =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
        val title = line(12f, 0x99FFFFFF.toInt(), false, 1).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }
        val close =
            TextView(context).apply {
                text = "✕"
                textSize = 16f
                setTextColor(0xCCFFFFFF.toInt())
                setPadding(dp(12), dp(4), dp(4), dp(4))
                setOnClickListener { onClose() }
            }
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(close)

        val prev = line(13f, 0x80FFFFFF.toInt(), false, 1)
        val current = line(19f, 0xFFFFFFFF.toInt(), true, 3).apply { setPadding(0, dp(6), 0, dp(6)) }
        val next = line(13f, 0x80FFFFFF.toInt(), false, 1)

        val container =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = bg
                setPadding(dp(18), dp(12), dp(14), dp(14))
                addView(header)
                addView(prev)
                addView(current)
                addView(next)
            }

        val width = overlayWidth()
        val lp =
            WindowManager.LayoutParams(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = positionPrefs.getInt("x", 0)
                y = positionPrefs.getInt("y", dp(96))
            }

        // Geser bebas dengan satu jari; ketukan singkat membuka aplikasi.
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var moved = false
        container.setOnTouchListener { _: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    downX = event.rawX
                    downY = event.rawY
                    downTime = event.eventTime
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!moved && (kotlin.math.abs(dx) > dp(8) || kotlin.math.abs(dy) > dp(8))) moved = true
                    if (moved) {
                        lp.x = (startX + dx).toInt()
                        lp.y = (startY + dy).toInt()
                        clamp(lp, container)
                        runCatching { windowManager.updateViewLayout(container, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        positionPrefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    } else if (event.eventTime - downTime < 400L) {
                        onOpenApp()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(container, lp)
            root = container
            params = lp
            titleView = title
            prevView = prev
            currentView = current
            nextView = next
        } catch (e: Exception) {
            // Izin hilang atau jendela ditolak sistem: jangan sampai menjatuhkan layanan musik.
            Timber.w(e, "LyricsOverlay: gagal menampilkan jendela")
        }
    }

    fun update(title: String?, previous: String, current: String, next: String) {
        if (root == null) return
        titleView?.text = title.orEmpty()
        prevView?.text = previous
        currentView?.text = current
        nextView?.text = next
    }

    fun hide() {
        val view = root ?: return
        runCatching { windowManager.removeViewImmediate(view) }
        root = null
        params = null
        titleView = null
        prevView = null
        currentView = null
        nextView = null
    }
}
