package com.armutlu.apporganizer.telemetry

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class TelemetryDailySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!TelemetryManager.isCollectionEnabled()) return Result.success()
        val summaries = LocalTelemetryStore.takeDailySummaries(applicationContext) ?: return Result.success()
        summaries.toList().forEach(TelemetryManager::logDirect)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "telemetry_daily_summary"
        fun sync(context: Context, enabled: Boolean) {
            LocalTelemetryStore.initialize(context)
            val workManager = WorkManager.getInstance(context)
            if (!enabled) { workManager.cancelUniqueWork(UNIQUE_NAME); return }
            val request = PeriodicWorkRequestBuilder<TelemetryDailySummaryWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()).build()
            workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
