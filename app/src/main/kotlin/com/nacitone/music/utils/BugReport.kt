package com.nacitone.music.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.nacitone.music.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Laporan masalah: menggabungkan info aplikasi/perangkat dan crash terakhir (bila ada),
 * lalu membagikannya lewat email atau aplikasi apa pun yang dipilih pengguna.
 * Tidak ada yang dikirim otomatis; pengguna melihat dan memilih tujuan sendiri.
 */
object BugReport {
    const val DEVELOPER_EMAIL = "ican3192@gmail.com"
    private const val LAST_CRASH_FILE = "last_crash.txt"
    private const val MAX_CRASH_CHARS = 30_000

    fun saveLastCrash(context: Context, crashLog: String) {
        File(context.filesDir, LAST_CRASH_FILE).writeText(crashLog.take(MAX_CRASH_CHARS))
    }

    fun hasLastCrash(context: Context): Boolean = File(context.filesDir, LAST_CRASH_FILE).exists()

    fun clearLastCrash(context: Context) {
        File(context.filesDir, LAST_CRASH_FILE).delete()
    }

    fun buildReport(context: Context, note: String = ""): String =
        buildString {
            appendLine("Nacitone - Laporan Masalah")
            appendLine("=".repeat(40))
            appendLine("Versi aplikasi : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Perangkat      : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android        : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Waktu          : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            if (note.isNotBlank()) {
                appendLine()
                appendLine("Catatan pengguna:")
                appendLine(note)
            }
            val crash = File(context.filesDir, LAST_CRASH_FILE)
            if (crash.exists()) {
                appendLine()
                appendLine("Crash terakhir:")
                appendLine("-".repeat(40))
                appendLine(runCatching { crash.readText() }.getOrDefault("(tidak terbaca)"))
            } else {
                appendLine()
                appendLine("Tidak ada crash tercatat.")
            }
        }

    /** Membuka pemilih aplikasi (email, WhatsApp, dll.) dengan laporan sebagai lampiran teks. */
    fun share(context: Context, chooserTitle: String, subject: String, intro: String) {
        val file = File(context.cacheDir, "laporan_nacitone.txt")
        file.writeText(buildReport(context))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEVELOPER_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, intro)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
