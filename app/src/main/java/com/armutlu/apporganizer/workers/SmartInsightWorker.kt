package com.armutlu.apporganizer.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SharedPrefsSuggestionHistoryStore
import com.armutlu.apporganizer.utils.SuggestionCandidate
import com.armutlu.apporganizer.utils.SuggestionChannel
import com.armutlu.apporganizer.utils.SuggestionCoordinator
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WorkerTelemetryPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SmartInsightWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @dagger.hilt.InstallIn(SingletonComponent::class)
    interface InsightEntryPoint {
        fun appRepository(): AppRepository
        fun categoryDao(): CategoryDao
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val startedAt = WorkerTelemetryPrefs.markStarted(ctx, WORK_NAME)
        return runCatching {
            if (!AppPrefs.isSmartNotifEnabled(ctx)) {
                WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
                return@runCatching Result.success()
            }
            if (!canPostNotifications()) {
                Timber.i("SmartInsight skipped: app notifications are disabled")
                WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
                return@runCatching Result.success()
            }

            val ep = EntryPointAccessors.fromApplication(ctx, InsightEntryPoint::class.java)
            val repo = ep.appRepository()
            val catDao = ep.categoryDao()
            val apps = repo.getAllApps().filter { !it.isHidden && !it.isSystemApp }
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000
            val todayUsage = todayUsage(ctx)
            val todayLaunchCountByPackage = todayUsage.associate { it.packageName to it.launchCount.toLong() }
            val todayForegroundMsByPackage = todayUsage.associate { it.packageName to it.foregroundDurationMs }

            val candidates = buildList<Triple<String, String, String>> {
                if (AppPrefs.isSmartNotifDailyUsage(ctx)) {
                    val topLaunchApp = apps.maxByOrNull { todayLaunchCountByPackage[it.packageName] ?: 0L }
                    val topLaunchCount = topLaunchApp?.let { todayLaunchCountByPackage[it.packageName] ?: 0L } ?: 0L
                    if (topLaunchApp != null && topLaunchCount > 0L) {
                        add(
                            Triple(
                                "top_used_${topLaunchApp.packageName}",
                                "Gunluk Kullanim",
                                "${topLaunchApp.appName} bugun en cok actigin uygulama. Ekran suren nasil gidiyor?",
                            ),
                        )
                    } else {
                        val topForegroundApp = apps.maxByOrNull { todayForegroundMsByPackage[it.packageName] ?: 0L }
                        val topForegroundMs = topForegroundApp?.let { todayForegroundMsByPackage[it.packageName] ?: 0L } ?: 0L
                        if (topForegroundApp != null && topForegroundMs > 0L) {
                            add(
                                Triple(
                                    "top_used_${topForegroundApp.packageName}",
                                    "Gunluk Kullanim",
                                    "${topForegroundApp.appName} bugun en cok vakit gecirdigin uygulama gibi gorunuyor.",
                                ),
                            )
                        }
                    }
                    val sessionCount = todayLaunchCountByPackage.values.sum().coerceAtMost(99L)
                    if (sessionCount > 10L) {
                        add(Triple("high_session_count", "Gunluk Kullanim", "Bugun toplam $sessionCount uygulama acilisi yaptin. Verimliligini takip et!"))
                    }
                }

                if (AppPrefs.isSmartNotifUnusedApps(ctx)) {
                    val unused3weeks = apps.filter { app ->
                        app.lastUsedTimestamp > 0L && (now - app.lastUsedTimestamp) >= 21 * dayMs
                    }
                    if (unused3weeks.isNotEmpty()) {
                        val app = unused3weeks.random()
                        add(Triple("long_unused_${app.packageName}", "Temizlik Onerisi", "${app.appName} uygulamasini 3 haftadir acmadin. Silmeyi dusunur musun?"))
                    }
                    val neverOpened = apps.filter { it.lastUsedTimestamp == 0L && (now - it.installTime) >= 14 * dayMs }
                    if (neverOpened.size >= 3) {
                        add(Triple("never_opened_batch", "Temizlik Onerisi", "${neverOpened.size} uygulama kuruldugundan beri hic acilmamis. Bir goz at!"))
                    }
                }

                if (AppPrefs.isSmartNotifCatStats(ctx)) {
                    val cats = catDao.getAllCategoriesFlow().first()
                    val fullestCat = cats.maxByOrNull { cat ->
                        apps.count { it.categoryId == cat.categoryId }
                    }
                    if (fullestCat != null) {
                        val count = apps.count { it.categoryId == fullestCat.categoryId }
                        if (count >= 5) {
                            add(Triple("cat_summary_${fullestCat.categoryId}", "Klasor Istatistikleri", "'${fullestCat.categoryName}' klasorun en kalabalik. $count uygulama var."))
                        }
                    }
                    val uncategorized = apps.count { it.categoryId == "uncategorized" || it.categoryId.isBlank() }
                    if (uncategorized >= 5) {
                        add(Triple("low_confidence_review", "Klasor Istatistikleri", "$uncategorized uygulamanin henuz bir klasoru yok. Organize etmeye ne dersin?"))
                    }
                }

                if (AppPrefs.isSmartNotifCatStats(ctx)) {
                    val newApps = apps.filter { (now - it.installTime) <= 3 * dayMs && it.installTime > 0L }
                    if (newApps.isNotEmpty()) {
                        val app = requireNotNull(newApps.maxByOrNull { maxOf(it.firstInstalledTime, it.installTime) })
                        add(Triple("new_install_${app.packageName}", "Yeni Uygulama", "${app.appName} yeni kuruldu. Uygulamayi bir klasore eklemek ister misin?"))
                    }
                }

                val anySubtypeEnabled = AppPrefs.isSmartNotifDailyUsage(ctx) ||
                    AppPrefs.isSmartNotifUnusedApps(ctx) ||
                    AppPrefs.isSmartNotifCatStats(ctx)
                if (anySubtypeEnabled) {
                    val weeklyMessages = listOf(
                        "Uygulamalarini duzenlemek hafizayi ozgurlestirir. Bu hafta bir klasor olustursan?",
                        "Organize bir telefon, gunde ortalama 20 dakika kazandirabilir.",
                        "Bu haftaki hedefin: kullanmadigin 3 uygulamayi gizle veya sil.",
                        "En cok kullandigin 5 uygulamayi ust klasore almayi dene.",
                        "Arka planda calisan uygulamalari kontrol et, pilini kurtarabilirsin.",
                    )
                    if (Random.nextInt(3) == 0) {
                        add(Triple("weekly_tip", "Haftalik Ipucu", weeklyMessages.random()))
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                val historyStore = SharedPrefsSuggestionHistoryStore(ctx)
                candidates.shuffled().firstOrNull { (key, _, _) ->
                    SuggestionCoordinator.canShow(
                        candidate = SuggestionCandidate(
                            dedupeKey = key,
                            highValue = key == "low_confidence_review" || key.startsWith("new_install_"),
                            timeSensitive = key == "low_confidence_review" || key.startsWith("new_install_"),
                        ),
                        channel = SuggestionChannel.SYSTEM_NOTIFICATION,
                        store = historyStore,
                        nowMillis = now,
                    )
                }?.let { (key, title, body) ->
                    if (sendNotification(title, body)) {
                        SuggestionCoordinator.recordShown(
                            candidate = SuggestionCandidate(
                                dedupeKey = key,
                                highValue = key == "low_confidence_review" || key.startsWith("new_install_"),
                                timeSensitive = key == "low_confidence_review" || key.startsWith("new_install_"),
                            ),
                            channel = SuggestionChannel.SYSTEM_NOTIFICATION,
                            store = historyStore,
                            nowMillis = now,
                        )
                    }
                }
            }

            Timber.d("SmartInsight: ${candidates.size} oneri olusturuldu")
            WorkerTelemetryPrefs.markSucceeded(ctx, WORK_NAME, startedAt)
            Result.success()
        }.getOrElse { error ->
            WorkerTelemetryPrefs.markFailed(
                ctx,
                WORK_NAME,
                startedAt,
                WorkerTelemetryPrefs.FAILURE_UNKNOWN,
            )
            Timber.e(error, "SmartInsight hatasi")
            Result.retry()
        }
    }

    private fun canPostNotifications(): Boolean =
        NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()

    private fun sendNotification(title: String, body: String): Boolean {
        if (!canPostNotifications()) return false
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return false
        ensureChannel(manager)

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.DASHBOARD)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        return runCatching {
            manager.notify(NOTIF_ID + System.currentTimeMillis().toInt() % 100, notification)
            true
        }.getOrElse { error ->
            Timber.w(error, "SmartInsight notification could not be posted")
            false
        }
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Akilli Bildirimler",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Gunluk kullanim icgoru ve temizlik onerileri"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val WORK_NAME = "smart_insight_daily"
        private const val CHANNEL_ID = "smart_insight"
        private const val NOTIF_ID = 2001

        fun schedule(context: Context) {
            if (!AppPrefs.isSmartNotifEnabled(context)) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }
            val targetHour = AppPrefs.getSmartNotifHour(context)
            val initialDelayMs = calculateInitialDelayMs(targetHour = targetHour)
            val request = PeriodicWorkRequestBuilder<SmartInsightWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        internal fun calculateInitialDelayMs(
            targetHour: Int,
            targetMinute: Int = 0,
            nowMillis: Long = System.currentTimeMillis(),
        ): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return calendar.timeInMillis - nowMillis
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    private fun todayUsage(context: Context): List<DailyPackageUsage> {
        return when (val result = UsageStatsHelper.getDailySessionUsage(context, days = 1)) {
            is UsageStatsHelper.DailySessionResult.Available -> result.days.filterNot { it.isPartial }
            is UsageStatsHelper.DailySessionResult.Unavailable -> emptyList()
        }
    }
}
