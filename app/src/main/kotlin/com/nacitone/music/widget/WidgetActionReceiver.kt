package com.nacitone.music.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nacitone.music.MainActivity
import com.nacitone.music.playback.MusicService
import timber.log.Timber

/**
 * Menerima ketukan tombol widget. Receiver ini TIDAK diekspor: hanya PendingIntent milik aplikasi
 * sendiri (yang dibuat di NacitoneWidgetManager/PlaylistWidgetManager) yang bisa sampai ke sini,
 * jadi aplikasi lain tidak dapat memicu play/pause/suka/berikutnya.
 */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceAction = when (intent.action) {
            MusicWidgetReceiver.ACTION_PLAY_PAUSE,
            TurntableWidgetReceiver.ACTION_TURNTABLE_PLAY_PAUSE -> MusicWidgetReceiver.ACTION_PLAY_PAUSE
            MusicWidgetReceiver.ACTION_LIKE -> MusicWidgetReceiver.ACTION_LIKE
            MusicWidgetReceiver.ACTION_NEXT,
            TurntableWidgetReceiver.ACTION_TURNTABLE_NEXT -> MusicWidgetReceiver.ACTION_NEXT
            MusicWidgetReceiver.ACTION_PREVIOUS,
            TurntableWidgetReceiver.ACTION_TURNTABLE_PREVIOUS -> MusicWidgetReceiver.ACTION_PREVIOUS
            PlaylistWidgetReceiver.ACTION_PLAY_TARGET -> PlaylistWidgetReceiver.ACTION_PLAY_TARGET
            else -> return
        }
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = serviceAction
            putExtras(intent)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Timber.tag("WidgetActionReceiver").w(e, "Gagal memulai layanan dari widget")
            if (serviceAction == PlaylistWidgetReceiver.ACTION_PLAY_TARGET) {
                runCatching {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_OPEN_WIDGET_TARGET
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(
                                MainActivity.EXTRA_WIDGET_TARGET_TYPE,
                                intent.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_TYPE),
                            )
                            putExtra(
                                MainActivity.EXTRA_WIDGET_TARGET_ID,
                                intent.getStringExtra(PlaylistWidgetReceiver.EXTRA_TARGET_ID),
                            )
                        },
                    )
                }
            }
        }
    }
}
