package com.nacitone.music.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import timber.log.Timber

/** Menerima hasil sesi PackageInstaller dari [ApkInstaller]. */
class UpdateInstallReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ApkInstaller.ACTION_INSTALL_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    } else {
                        intent.getParcelableExtra(Intent.EXTRA_INTENT)
                    }
                if (confirm == null) {
                    ApkInstaller.onInstallResult(PackageInstaller.STATUS_FAILURE, "Tidak ada layar konfirmasi")
                } else {
                    runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure {
                            Timber.w(it, "UpdateInstallReceiver: gagal membuka konfirmasi pemasangan")
                            ApkInstaller.onInstallResult(PackageInstaller.STATUS_FAILURE, it.message)
                        }
                }
            }
            else -> ApkInstaller.onInstallResult(status, message)
        }
    }
}
