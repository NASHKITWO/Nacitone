/**
 * Nacitone (C) 2026
 * Licensed under GPL-3.0
 */

package com.nacitone.music.utils

import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/**
 * Menghitung data jaringan yang dipakai pemutaran selama aplikasi berjalan (sejak proses dimulai
 * atau sejak diatur ulang). Byte dari cache tidak dihitung karena tidak memakai jaringan.
 */
@OptIn(UnstableApi::class)
class DataUsageMeter private constructor(
    context: Context,
) : TransferListener {
    private val connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val _meteredBytes = MutableStateFlow(0L)
    private val _unmeteredBytes = MutableStateFlow(0L)

    /** Data di jaringan berkuota (biasanya seluler). */
    val meteredBytes: StateFlow<Long> = _meteredBytes.asStateFlow()

    /** Data di jaringan tanpa kuota (Wi-Fi). */
    val unmeteredBytes: StateFlow<Long> = _unmeteredBytes.asStateFlow()

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onBytesTransferred(
        source: DataSource,
        dataSpec: DataSpec,
        isNetwork: Boolean,
        bytesTransferred: Int,
    ) {
        if (!isNetwork || bytesTransferred <= 0) return
        val metered = runCatching { connectivity?.isActiveNetworkMetered == true }.getOrDefault(true)
        (if (metered) _meteredBytes else _unmeteredBytes).update { it + bytesTransferred }
    }

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    fun reset() {
        _meteredBytes.value = 0L
        _unmeteredBytes.value = 0L
    }

    companion object {
        @Volatile private var instance: DataUsageMeter? = null

        fun get(context: Context): DataUsageMeter =
            instance ?: synchronized(this) {
                instance ?: DataUsageMeter(context).also { instance = it }
            }

        fun format(bytes: Long): String =
            when {
                bytes >= 1L shl 30 -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1L shl 30).toDouble())
                bytes >= 1L shl 20 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1L shl 20).toDouble())
                else -> String.format(Locale.getDefault(), "%d KB", bytes / 1024)
            }
    }
}
