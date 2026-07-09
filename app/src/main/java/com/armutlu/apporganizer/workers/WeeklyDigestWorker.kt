package com.armutlu.apporganizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.WrappedSnapshotPrefs
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WeeklyDigestWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface DigestEntryPoint {
        fun appRepository(): AppRepository
    }

    override suspend fun doWork(): Result {
        return runCatching {
            if (!AppPrefs.isWeeklyDigestEnabled(applicationContext)) return@runCatching Result.success()

            val repo = EntryPointAccessors.fromApplication(
                applicationContext,
                DigestEntryPoint::class.java
            ).appRepository()

            val apps = repo.getAllApps().filter { !it.isHidden && !it.isSystemApp }
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            val now = System.currentTimeMillis()

            val unusedApps = apps.filter { app ->
                app.lastUsedTimestamp > 0L && (now - app.lastUsedTimestamp) >= sevenDaysMs
            }
            val neverUsed = apps.filter { app ->
                app.lastUsedTimestamp == 0L && app.installTime > 0L && (now - app.installTime) >= sevenDaysMs
            }

            val totalUnused = unusedApps.size + neverUsed.size
            if (totalUnused > 0) {
                sendDigestNotification(totalUnused, unusedApps.take(3).map { it.appName })
            }
            Timber.d("WeeklyDigest: $totalUnused kullanılmayan uygulama tespit edildi")

            // Haftalık Rapor (Wrapped) için kategori bazlı kullanım snapshot'ı kaydet.
            // Mevcut davranışı etkilemesin diye ayrı runCatching ile izole edildi.
            runCatching {
                val categoryUsage = apps.groupBy { it.categoryId }
                    .mapValues { (_, list) -> list.sumOf { it.usageCount } }
                WrappedSnapshotPrefs.saveCurrent(applicationContext, categoryUsage, apps.size)
            }.onFailure { e -> Timber.e(e, "Wrapped snapshot kaydı başarısız") }

            Result.success()
        }.getOrElse { e ->
            Timber.e(e, "WeeklyDigest hatası")
            Result.retry()
        }
    }

    private fun sendDigestNotification(count: Int, sampleApps: List<String>) {
        val mgr = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(mgr)
        val sample = if (sampleApps.isNotEmpty()) " (${sampleApps.joinToString(", ")}…)" else ""
        val body = "$count uygulama 7+ gündür açılmadı$sample. Gizleyebilir veya kaldırabilirsin."
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Haftalık Uygulama Raporu")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIF_ID, notification)
    }

    private fun ensureChannel(mgr: NotificationManager) {
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(CHANNEL_ID, "Haftalık Özet", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Kullanılmayan uygulama bildirimleri"
        }
        mgr.createNotificationChannel(ch)
    }

    companion object {
        private const val WORK_NAME = "weekly_digest"
        private const val CHANNEL_ID = "weekly_digest"
        private const val NOTIF_ID = 1002

        fun schedule(context: Context) {
            if (!AppPrefs.isWeeklyDigestEnabled(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
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
