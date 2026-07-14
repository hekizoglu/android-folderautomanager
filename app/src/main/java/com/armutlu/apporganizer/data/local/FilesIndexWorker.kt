package com.armutlu.apporganizer.data.local

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.utils.AppPrefs
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * C2: Periyodik dosya indeksleme WorkManager worker'ı.
 * 24 saatte bir veya şarjda çalışır.
 *
 * Not: Hilt @AssistedInject/kapt uyumsuzluğunu önlemek için
 * WorkerFactory yerine entryPoint pattern kullanılır.
 */
class FilesIndexWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!AppPrefs.isSearchSourceFilesEnabled(applicationContext)) {
                return Result.success()
            }
            val db = com.armutlu.apporganizer.data.local.AppDatabase.getInstance(applicationContext)
            val indexer = FilesIndexer(applicationContext, db.searchDao())
            indexer.indexAll()
            Timber.d("FilesIndexWorker: tamamlandı")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "FilesIndexWorker hatası")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "files_index_periodic"
        private const val ONE_TIME_WORK_NAME = "files_index_once"

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<FilesIndexWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Timber.d("FilesIndexWorker: tek seferlik gorev planlandi")
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FilesIndexWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresCharging(true)
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Timber.d("FilesIndexWorker: periyodik görev planlandı (24h, şarjda)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
