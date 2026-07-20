package com.armutlu.apporganizer.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.data.remote.FetchResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Paket -> kategori veritabanini (GitHub'daki app_database.json) haftalik olarak tazeler.
 * FCM push kaldirildiktan sonra (D-S6) bu worker guncel kategori verisinin tek kaynagi:
 * uygulama acilisinda zaten AppListViewModel.fetchAndCache() cagriliyor, bu worker ise
 * uygulama uzun sure acilmasa bile arka planda haftalik tazeleme saglar.
 */
class CategoryDbUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CategoryDbUpdateEntryPoint {
        fun appDatabaseService(): AppDatabaseService
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val service = EntryPointAccessors.fromApplication(
                applicationContext,
                CategoryDbUpdateEntryPoint::class.java
            ).appDatabaseService()
            val result = service.fetchAndCache()
            Timber.d("CategoryDbUpdateWorker: $result")
            when (result) {
                is FetchResult.Success -> Result.success()
                is FetchResult.FromCache -> Result.success()
                is FetchResult.Error -> Result.retry()
                FetchResult.NoCache -> Result.retry()
            }
        }.getOrElse { e ->
            Timber.e(e, "CategoryDbUpdateWorker hatasi")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "category_db_update_weekly"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CategoryDbUpdateWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
