package com.armutlu.apporganizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.usecase.goals.EnsureCurrentWeekAdaptiveGoalsUseCase
import com.armutlu.apporganizer.domain.usecase.goals.SettlePreviousWeekAdaptiveGoalsUseCase
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WorkerTelemetryPrefs
import com.armutlu.apporganizer.utils.WrappedSnapshotPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WeeklyDigestWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface DigestEntryPoint {
        fun appRepository(): AppRepository
        fun ensureCurrentWeekAdaptiveGoalsUseCase(): EnsureCurrentWeekAdaptiveGoalsUseCase
        fun settlePreviousWeekAdaptiveGoalsUseCase(): SettlePreviousWeekAdaptiveGoalsUseCase
    }

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val startedAt = WorkerTelemetryPrefs.markStarted(ctx, WORK_NAME)
        return runCatching {
            if (!AppPrefs.isWeeklyDigestEnabled(ctx)) {
                WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
                return@runCatching Result.success()
            }

            val entryPoint = EntryPointAccessors.fromApplication(
                ctx,
                DigestEntryPoint::class.java,
            )
            val repo = entryPoint.appRepository()

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
            Timber.d("WeeklyDigest: $totalUnused unused apps detected")

            // Keep Wrapped snapshot failures isolated from the digest worker result.
            runCatching {
                val categoryUsage = apps.groupBy { it.categoryId }
                    .mapValues { (_, list) -> list.sumOf { it.usageCount } }
                WrappedSnapshotPrefs.saveCurrent(
                    ctx,
                    categoryUsage,
                    apps.size,
                    UsageStatsHelper.getUnlockCount(ctx, days = 7, nowMillis = now),
                )
            }.onFailure { e -> Timber.e(e, "Wrapped snapshot save failed") }

            // P4 — adaptif kategori hedefi güvenlik ağı (roadmap §6). Asıl idempotent tetikleme
            // app açılışı/Dashboard/Görevler açılışında da olur (P5/P6); worker sadece bunları
            // hiç görmeyen (uygulamayı hiç açmayan) kullanıcılar için yedek tetikleyicidir.
            runCatching {
                if (AppPrefs.isGoalsEnabled(ctx)) {
                    val completedCount = entryPoint.settlePreviousWeekAdaptiveGoalsUseCase().execute()
                    entryPoint.ensureCurrentWeekAdaptiveGoalsUseCase().execute()
                    if (completedCount > 0) sendGoalNotification(completedCount)
                }
            }.onFailure { e -> Timber.e(e, "Adaptive goals settlement/generation failed") }

            WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
            Result.success()
        }.getOrElse { e ->
            WorkerTelemetryPrefs.markFailed(
                ctx,
                WORK_NAME,
                startedAt,
                WorkerTelemetryPrefs.FAILURE_UNKNOWN,
            )
            Timber.e(e, "WeeklyDigest error")
            Result.retry()
        }
    }

    private fun sendGoalNotification(count: Int) {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return
        val mgr = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(mgr)
        val body = if (count == 1) {
            "Bu hafta 1 kategori hedefini tamamladin. Dashboard'da rozetini gorebilirsin."
        } else {
            "Bu hafta $count kategori hedefini tamamladin. Dashboard'da rozetlerini gorebilirsin."
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIF_ID + 1,
            Intent(applicationContext, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.DASHBOARD)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Haftalik hedef tamamlandi")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIF_ID + 1, notification)
    }

    private fun sendDigestNotification(count: Int, sampleApps: List<String>) {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return
        val mgr = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(mgr)
        val sample = if (sampleApps.isNotEmpty()) " (${sampleApps.joinToString(", ")})" else ""
        val body = "$count uygulama 7+ gundur acilmadi$sample. Gizleyebilir veya kaldirabilirsin."
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIF_ID,
            Intent(applicationContext, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.USAGE_REPORT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Haftalik Uygulama Raporu")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        mgr.notify(NOTIF_ID, notification)
    }

    private fun ensureChannel(mgr: NotificationManager) {
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(CHANNEL_ID, "Haftalik Ozet", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Kullanilmayan uygulama bildirimleri"
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
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
