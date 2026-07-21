package com.armutlu.apporganizer.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.data.local.TickerHistoryDao
import com.armutlu.apporganizer.domain.home.TickerHistoryMapper
import com.armutlu.apporganizer.utils.WorkerTelemetryPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import timber.log.Timber

/**
 * Ticker arşivi ("Tüm haberler") günlük temizlik işi — [TickerHistoryDao.deleteOlderThan] ile
 * 7 günden eski kayıtları siler (mail kutusu benzeri otomatik silme). [SmartInsightWorker]/
 * [MissionSettlementWorker] ile aynı desen: Hilt EntryPoint + WorkerTelemetryPrefs.
 */
class TickerHistoryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CleanupEntryPoint {
        fun tickerHistoryDao(): TickerHistoryDao
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val startedAt = WorkerTelemetryPrefs.markStarted(ctx, WORK_NAME)
        return runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(ctx, CleanupEntryPoint::class.java)
            val dao = entryPoint.tickerHistoryDao()
            val cutoff = TickerHistoryMapper.cutoffMillis(System.currentTimeMillis())
            dao.deleteOlderThan(cutoff)
            Timber.d("TickerHistoryCleanupWorker: cutoff=$cutoff")
            WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
            Result.success()
        }.getOrElse { error ->
            Timber.e(error, "TickerHistoryCleanupWorker hatasi")
            WorkerTelemetryPrefs.markFailed(ctx, WORK_NAME, startedAt, WorkerTelemetryPrefs.FAILURE_UNKNOWN)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "ticker_history_cleanup_daily"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<TickerHistoryCleanupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
