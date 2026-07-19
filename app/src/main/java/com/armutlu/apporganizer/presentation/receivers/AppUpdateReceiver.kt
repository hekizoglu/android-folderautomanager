package com.armutlu.apporganizer.presentation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.armutlu.apporganizer.utils.FilesIndexWorkCoordinator
import timber.log.Timber

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        Thread({
            runCatching {
                FilesIndexWorkCoordinator.ensurePeriodicWorkScheduled(context)
            }.onFailure { Timber.w(it, "FilesIndexWorker update reconciliation failed") }
            pendingResult.finish()
        }, "app-update-reconcile").start()
    }
}
