package com.armutlu.apporganizer.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BackupManager
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface BackupWorkerEntryPoint {
        fun appRepository(): AppRepository
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val repo = EntryPointAccessors.fromApplication(
                applicationContext,
                BackupWorkerEntryPoint::class.java
            ).appRepository()
            val json = BackupManager.exportToJson(repo)
            val file = File(applicationContext.filesDir, "auto_backup.json")
            file.writeText(json)
            Timber.d("Otomatik yedekleme tamamlandi: ${file.absolutePath}")
            Result.success()
        }.getOrElse { e ->
            Timber.e(e, "Otomatik yedekleme hatasi")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "auto_backup_weekly"

        fun schedule(context: Context) {
            if (!AppPrefs.isAutoBackupEnabled(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            // Her 7 gunde bir calisdiran periyodik gorev
            val request = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
