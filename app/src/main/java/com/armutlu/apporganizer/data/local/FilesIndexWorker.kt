package com.armutlu.apporganizer.data.local

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.WorkerTelemetryPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic file indexing worker.
 *
 * Uses EntryPoint instead of WorkerFactory so the worker reaches the app-wide
 * singleton FilesIndexer instance and UI observers see indexing state updates.
 */
class FilesIndexWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FilesIndexWorkerEntryPoint {
        fun filesIndexer(): FilesIndexer
    }

    override suspend fun doWork(): Result {
        val workName = inputData.getString(KEY_WORK_NAME) ?: WORK_NAME
        val startedAt = WorkerTelemetryPrefs.markStarted(applicationContext, workName)
        return try {
            if (!AppPrefs.isSearchSourceFilesEnabled(applicationContext)) {
                WorkerTelemetryPrefs.markSucceeded(applicationContext, workName, startedAt)
                return Result.success()
            }
            val indexer = EntryPointAccessors.fromApplication(
                applicationContext,
                FilesIndexWorkerEntryPoint::class.java,
            ).filesIndexer()
            indexer.indexAll()
            WorkerTelemetryPrefs.markSucceeded(applicationContext, workName, startedAt)
            Timber.d("FilesIndexWorker: completed")
            Result.success()
        } catch (e: Exception) {
            WorkerTelemetryPrefs.markFailed(
                applicationContext,
                workName,
                startedAt,
                WorkerTelemetryPrefs.FAILURE_IO_ERROR,
            )
            Timber.e(e, "FilesIndexWorker error")
            Result.retry()
        }
    }

    companion object {
        private const val KEY_WORK_NAME = "work_name"
        private const val WORK_NAME = "files_index_periodic"
        private const val ONE_TIME_WORK_NAME = "files_index_once"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<FilesIndexWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .setInputData(workDataOf(KEY_WORK_NAME to ONE_TIME_WORK_NAME))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
            Timber.d("FilesIndexWorker: one-time work scheduled")
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FilesIndexWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .setInputData(workDataOf(KEY_WORK_NAME to WORK_NAME))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            Timber.d("FilesIndexWorker: periodic work scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
