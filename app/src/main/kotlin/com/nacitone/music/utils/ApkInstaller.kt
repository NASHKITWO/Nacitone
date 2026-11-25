package com.nacitone.music.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

sealed interface UpdateInstallState {
    data object Idle : UpdateInstallState

    /** [percent] bernilai -1 bila ukuran berkas tidak diketahui. */
    data class Downloading(val percent: Int) : UpdateInstallState

    data object Verifying : UpdateInstallState

    data object Installing : UpdateInstallState

    data class Failed(val reason: Reason, val detail: String? = null) : UpdateInstallState

    enum class Reason { NEED_PERMISSION, DOWNLOAD, VERIFY, INSTALL }
}

/**
 * Pembaruan dalam aplikasi: unduh APK rilis dari GitHub, periksa nama paket, versi, dan tanda tangannya
 * terhadap aplikasi yang terpasang, lalu serahkan ke PackageInstaller. Android tetap meminta
 * konfirmasi pengguna; pemasangan senyap tidak mungkin untuk aplikasi di luar toko aplikasi.
 */
object ApkInstaller {
    const val ACTION_INSTALL_RESULT = "com.nacitone.music.action.UPDATE_INSTALL_RESULT"

    private const val TAG = "ApkInstaller"
    private const val MIN_APK_BYTES = 1_000_000L
    private const val MAX_REDIRECTS = 5

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    val state: StateFlow<UpdateInstallState> = _state.asStateFlow()

    val isBusy: Boolean
        get() =
            when (_state.value) {
                is UpdateInstallState.Downloading,
                UpdateInstallState.Verifying,
                UpdateInstallState.Installing,
                -> true
                else -> false
            }

    fun reset() {
        if (!isBusy) _state.value = UpdateInstallState.Idle
    }

    /** Dipanggil oleh [UpdateInstallReceiver] dengan hasil sesi pemasangan. */
    internal fun onInstallResult(status: Int, message: String?) {
        _state.value =
            when (status) {
                PackageInstaller.STATUS_SUCCESS -> UpdateInstallState.Idle
                PackageInstaller.STATUS_FAILURE_ABORTED -> UpdateInstallState.Idle // pengguna membatalkan
                else -> UpdateInstallState.Failed(UpdateInstallState.Reason.INSTALL, message)
            }
    }

    fun start(context: Context, url: String) {
        if (isBusy) return
        val appContext = context.applicationContext

        if (!isAllowedUrl(url)) {
            _state.value = UpdateInstallState.Failed(UpdateInstallState.Reason.DOWNLOAD, "URL tidak dikenali")
            return
        }
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateInstallState.Failed(UpdateInstallState.Reason.NEED_PERMISSION)
            runCatching {
                appContext.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${appContext.packageName}".toUri(),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }

        _state.value = UpdateInstallState.Downloading(-1)
        job =
            scope.launch {
                try {
                    val apk = download(appContext, url)
                    _state.value = UpdateInstallState.Verifying
                    verify(appContext, apk)?.let { problem ->
                        apk.delete()
                        _state.value = UpdateInstallState.Failed(UpdateInstallState.Reason.VERIFY, problem)
                        return@launch
                    }
                    _state.value = UpdateInstallState.Installing
                    install(appContext, apk)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Pembaruan gagal")
                    val reason =
                        if (_state.value == UpdateInstallState.Installing) {
                            UpdateInstallState.Reason.INSTALL
                        } else {
                            UpdateInstallState.Reason.DOWNLOAD
                        }
                    _state.value = UpdateInstallState.Failed(reason, e.message)
                }
            }
    }

    /** Hanya unduh dari rilis GitHub lewat HTTPS. */
    private fun isAllowedUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme == "https" && (host == "github.com" || host.endsWith(".githubusercontent.com"))
    }

    private fun download(context: Context, url: String): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "update.apk")

        var current = url
        var connection: HttpURLConnection? = null
        try {
            var redirects = 0
            while (true) {
                if (!isAllowedUrl(current)) error("Pengalihan ke alamat yang tidak diizinkan")
                connection =
                    (URL(current).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 15_000
                        readTimeout = 30_000
                        setRequestProperty("Accept", "application/octet-stream")
                    }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: error("Pengalihan tanpa tujuan")
                    connection.disconnect()
                    current = URL(URL(current), location).toString()
                    if (++redirects > MAX_REDIRECTS) error("Terlalu banyak pengalihan")
                    continue
                }
                if (code != HttpURLConnection.HTTP_OK) error("Server menjawab HTTP $code")
                break
            }

            val total = connection!!.contentLengthLong
            var read = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var lastPercent = -2
                    while (true) {
                        if (!scope.isActive) error("Dibatalkan")
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        val percent = if (total > 0) ((read * 100) / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _state.value = UpdateInstallState.Downloading(percent)
                        }
                    }
                }
            }
            if (total > 0 && read != total) error("Unduhan tidak lengkap ($read dari $total byte)")
            if (read < MIN_APK_BYTES) error("Berkas terlalu kecil untuk APK")
            return target
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    /** Mengembalikan pesan masalah, atau null bila APK aman dipasang sebagai pembaruan aplikasi ini. */
    private fun verify(context: Context, apk: File): String? {
        val pm = context.packageManager
        val archive = archiveInfo(pm, apk.absolutePath) ?: return "APK tidak bisa dibaca"
        if (archive.packageName != context.packageName) {
            return "Nama paket berbeda (${archive.packageName})"
        }
        val installed = installedInfo(pm, context.packageName) ?: return "Info aplikasi terpasang tidak tersedia"
        if (versionCodeOf(archive) <= versionCodeOf(installed)) {
            return "Versi berkas tidak lebih baru dari yang terpasang"
        }
        val archiveSigners = signerDigests(archive)
        val installedSigners = signerDigests(installed)
        if (archiveSigners.isEmpty() || installedSigners.isEmpty() || archiveSigners.intersect(installedSigners).isEmpty()) {
            return "Tanda tangan tidak cocok dengan aplikasi terpasang"
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? {
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
        return pm.getPackageArchiveInfo(path, flags)
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager, pkg: String): PackageInfo? {
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
        return runCatching { pm.getPackageInfo(pkg, flags) }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures: Array<Signature>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signing = info.signingInfo
                when {
                    signing == null -> null
                    signing.hasMultipleSigners() -> signing.apkContentsSigners
                    else -> signing.signingCertificateHistory
                }
            } else {
                info.signatures
            }
        val digest = MessageDigest.getInstance("SHA-256")
        return signatures
            ?.map { sig -> digest.digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }
            ?.toSet()
            .orEmpty()
    }

    private fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("nacitone-update", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }
            val intent =
                Intent(ACTION_INSTALL_RESULT)
                    .setClass(context, UpdateInstallReceiver::class.java)
                    .setPackage(context.packageName)
            val flags =
                PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }
}
