package com.nacitone.music.playback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import timber.log.Timber
import kotlin.math.abs

/**
 * Jendela lirik mengambang di atas aplikasi lain.
 *
 * Dua bentuk: kartu kaca (judul, baris sebelumnya, baris aktif, baris berikutnya) dan gelembung kecil "♪"
 * saat diminimalkan. Bisa digeser bebas; posisi dan bentuk terakhir diingat. Memakai View biasa
 * (bukan Compose) supaya tidak butuh LifecycleOwner di dalam Service.
 */
class LyricsOverlay(
    private val context: Context,
    /** Sembunyikan sampai Nacitone dibuka lagi (fitur tetap menyala di pengaturan). */
    private val onDismiss: () -> Unit,
    private val onOpenApp: () -> Unit = {},
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("lyrics_overlay", Context.MODE_PRIVATE)

    private var root: FrameLayout? = null
    private var card: LinearLayout? = null
    private var bubble: TextView? = null
    private var titleView: TextView? = null
    private var prevView: TextView? = null
    private var currentView: TextView? = null
    private var nextView: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var collapsed = prefs.getBoolean("collapsed", false)

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    val isShowing: Boolean get() = root != null

    private fun blurSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled

    private fun cardWidth(): Int = (context.resources.displayMetrics.widthPixels * 0.92f).toInt()

    /** Kaca: dasar gelap agak tembus, kilau putih tipis dari atas, dan garis tepi terang. */
    private fun glass(radiusPx: Float): LayerDrawable {
        // Dengan blur latar sistem, dasarnya boleh lebih tembus; tanpa blur dibuat lebih pekat agar tetap terbaca.
        val baseColor = if (blurSupported()) 0xB3101114.toInt() else 0xE6141518.toInt()
        val base =
            GradientDrawable().apply {
                setColor(baseColor)
                cornerRadius = radiusPx
                setStroke(dp(1), 0x40FFFFFF)
            }
        val sheen =
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x24FFFFFF, 0x04FFFFFF)).apply {
                cornerRadius = radiusPx
            }
        return LayerDrawable(arrayOf(base, sheen))
    }

    private fun textLine(sizeSp: Float, color: Int, bold: Boolean, maxLines: Int) =
        TextView(context).apply {
            setTextColor(color)
            textSize = sizeSp
            gravity = Gravity.CENTER
            this.maxLines = maxLines
            ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun headerButton(label: String, onClick: () -> Unit) =
        TextView(context).apply {
            text = label
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xE6FFFFFF.toInt())
            // Area sentuh 44dp supaya mudah ditekan.
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener { onClick() }
        }

    private fun clamp(lp: WindowManager.LayoutParams, view: View) {
        val dm = context.resources.displayMetrics
        val viewWidth = if (lp.width > 0) lp.width else view.width.coerceAtLeast(dp(48))
        val viewHeight = if (view.height > 0) view.height else dp(120)
        val maxX = ((dm.widthPixels - viewWidth) / 2).coerceAtLeast(0)
        val maxY = (dm.heightPixels - viewHeight - dp(24)).coerceAtLeast(0)
        lp.x = lp.x.coerceIn(-maxX, maxX)
        lp.y = lp.y.coerceIn(0, maxY)
    }

    private fun applyMode(animate: Boolean = false) {
        val container = root ?: return
        val lp = params ?: return
        card?.visibility = if (collapsed) View.GONE else View.VISIBLE
        bubble?.visibility = if (collapsed) View.VISIBLE else View.GONE
        lp.width = if (collapsed) WindowManager.LayoutParams.WRAP_CONTENT else cardWidth()
        container.background = glass(if (collapsed) dp(28).toFloat() else dp(24).toFloat())
        if (animate) {
            container.alpha = 0f
            container.animate().alpha(1f).setDuration(160).start()
        }
        container.post {
            // WRAP_CONTENT belum punya ukuran sebelum diukur: jaga posisi setelah layout.
            clamp(lp, container)
            runCatching { windowManager.updateViewLayout(container, lp) }
        }
        runCatching { windowManager.updateViewLayout(container, lp) }
    }

    private fun setCollapsed(value: Boolean) {
        collapsed = value
        prefs.edit().putBoolean("collapsed", value).apply()
        applyMode(animate = true)
    }

    /** Dipanggil saat konfigurasi berubah (rotasi, ukuran layar lipat). */
    fun onConfigurationChanged() {
        val view = root ?: return
        val lp = params ?: return
        if (!collapsed) lp.width = cardWidth()
        clamp(lp, view)
        runCatching { windowManager.updateViewLayout(view, lp) }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (root != null) return

        val title =
            textLine(12f, 0xB3FFFFFF.toInt(), false, 1).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSelected = true
            }
        val header =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                        text = "♪"
                        textSize = 14f
                        setTextColor(0xB3FFFFFF.toInt())
                        setPadding(0, 0, dp(8), 0)
                    },
                )
                addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(headerButton("–") { setCollapsed(true) })
                addView(headerButton("✕") { onDismiss() })
            }

        val prev = textLine(13f, 0x80FFFFFF.toInt(), false, 1)
        val current = textLine(20f, 0xFFFFFFFF.toInt(), true, 3).apply { setPadding(0, dp(8), 0, dp(8)) }
        val next = textLine(13f, 0x80FFFFFF.toInt(), false, 1)

        val cardView =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(6), dp(6), dp(16))
                addView(header)
                // Baris lirik diberi margin kanan agar tampak seimbang dengan padding kiri.
                val lineParams = { LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(12) } }
                addView(prev, lineParams())
                addView(current, lineParams())
                addView(next, lineParams())
            }

        val bubbleView =
            TextView(context).apply {
                text = "♪"
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                minWidth = dp(52)
                minHeight = dp(52)
            }

        val container =
            FrameLayout(context).apply {
                addView(cardView)
                addView(bubbleView)
                elevation = dp(8).toFloat()
            }

        val flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val lp =
            WindowManager.LayoutParams(
                if (collapsed) WindowManager.LayoutParams.WRAP_CONTENT else cardWidth(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                flags,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = prefs.getInt("x", 0)
                y = prefs.getInt("y", dp(96))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurSupported()) {
                    // Blur latar sungguhan (Android 12+) = efek kaca yang sebenarnya.
                    this.flags = this.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                    blurBehindRadius = dp(24)
                }
            }

        // Geser bebas dengan satu jari; ketukan singkat membuka aplikasi (atau mengembangkan gelembung).
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
                    if (!moved && (abs(dx) > dp(8) || abs(dy) > dp(8))) moved = true
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
                        prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    } else if (event.eventTime - downTime < 400L) {
                        if (collapsed) setCollapsed(false) else onOpenApp()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            container.background = glass(if (collapsed) dp(28).toFloat() else dp(24).toFloat())
            cardView.visibility = if (collapsed) View.GONE else View.VISIBLE
            bubbleView.visibility = if (collapsed) View.VISIBLE else View.GONE
            windowManager.addView(container, lp)
            root = container
            params = lp
            card = cardView
            bubble = bubbleView
            titleView = title
            prevView = prev
            currentView = current
            nextView = next
        } catch (e: Exception) {
            // Izin hilang atau jendela ditolak sistem: jangan sampai menjatuhkan layanan musik.
            Timber.w(e, "LyricsOverlay: gagal menampilkan jendela")
        }
    }

    private fun TextView.setTextAnimated(value: String) {
        if (text.toString() == value) return
        text = value
        alpha = 0f
        animate().alpha(1f).setDuration(180).start()
    }

    fun update(title: String?, previous: String, current: String, next: String) {
        if (root == null) return
        titleView?.text = title.orEmpty()
        prevView?.text = previous
        currentView?.setTextAnimated(current)
        nextView?.text = next
    }

    fun hide() {
        val view = root ?: return
        runCatching { windowManager.removeViewImmediate(view) }
        root = null
        card = null
        bubble = null
        params = null
        titleView = null
        prevView = null
        currentView = null
        nextView = null
    }
}
