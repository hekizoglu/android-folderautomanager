package com.armutlu.apporganizer.utils

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.armutlu.apporganizer.data.local.FilesIndexer
import com.armutlu.apporganizer.data.local.FilesIndexWorker

object FilesIndexWorkCoordinator {
    fun ensurePeriodicWorkScheduled(context: Context) {
        val appContext = context.applicationContext
        val filesIndexEnabled = AppPrefs.isSearchSourceFilesEnabled(appContext)
        val filesIndexReady = filesIndexEnabled && FilesIndexer.hasMediaStoreReadAccess(appContext)
        val existingWork = runCatching {
            WorkManager.getInstance(appContext)
                .getWorkInfosForUniqueWork(FilesIndexWorker.FILES_INDEX_PERIODIC_WORK_NAME)
                .get()
        }.getOrDefault(emptyList())
        val hasActivePeriodicWork = existingWork.any { info ->
            info.state == WorkInfo.State.ENQUEUED ||
                info.state == WorkInfo.State.RUNNING ||
                info.state == WorkInfo.State.BLOCKED
        }
        val replanFailureCode = when {
            existingWork.any { it.state == WorkInfo.State.CANCELLED } ->
                WorkerTelemetryPrefs.FAILURE_REPLAN_CANCELLED_WORK
            existingWork.any { it.state == WorkInfo.State.FAILED } ->
                WorkerTelemetryPrefs.FAILURE_REPLAN_FAILED_WORK
            else -> null
        }
        if (filesIndexReady) {
            if (!hasActivePeriodicWork) {
                replanFailureCode?.let { code ->
                    val startedAt = WorkerTelemetryPrefs.markStarted(
                        appContext,
                        FilesIndexWorker.FILES_INDEX_PERIODIC_WORK_NAME,
                    )
                    WorkerTelemetryPrefs.markFailed(
                        appContext,
                        FilesIndexWorker.FILES_INDEX_PERIODIC_WORK_NAME,
                        startedAt,
                        code,
                    )
                }
                FilesIndexWorker.schedule(appContext)
            }
        } else {
            FilesIndexWorker.cancel(appContext)
        }
    }
}
