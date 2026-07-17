package com.armutlu.apporganizer.domain.usecase.pulse

import android.content.Context
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import com.armutlu.apporganizer.utils.TaskScoreManager
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WrappedSnapshotPrefs
import java.util.concurrent.TimeUnit

/**
 * Döngü D00 — [PulseInput] hazırlama mantığı; eskiden `PulseClockViewModel.compute()` içinde
 * gömülüydü, tek skor motoruna geçişte [com.armutlu.apporganizer.domain.home.DigitalPulseRepository]
 * tarafından çağrılabilmesi için ayrıştırıldı. Android bağımlılıkları (Context, DAO, Repository)
 * burada kalır — [DigitalPulseEngine] saf Kotlin kalmaya devam eder.
 */
object PulseInputFactory {

    /** Haftalık pencere için gün sayısı — DigitalPulseEngine/WrappedEngine ile tutarlı 7 gün. */
    private const val WINDOW_DAYS = 7L

    suspend fun build(
        context: Context,
        appRepository: AppRepository,
        notificationEventDao: NotificationEventDao,
        nowMillis: Long = System.currentTimeMillis(),
    ): PulseInput {
        val apps = appRepository.getAllApps()
        val hasUsageAccess = UsageStatsHelper.hasPermission(context)
        val dailySessions = (UsageStatsHelper.getDailySessionUsage(context, days = WINDOW_DAYS.toInt())
            as? UsageStatsHelper.DailySessionResult.Available)?.days
        val weeklyLaunches = dailySessions?.groupBy { it.packageName }
            ?.mapValues { (_, days) -> days.sumOf { it.launchCount }.toLong() }

        val snapshots = apps.map { app ->
            WrappedEngine.AppSnapshot(
                packageName = app.packageName,
                appName = app.appName,
                categoryId = app.categoryId,
                usageCount = weeklyLaunches?.get(app.packageName) ?: app.launchCount,
                lastUsedTimestamp = app.lastUsedTimestamp,
                installTime = app.installTime,
                firstInstalledTime = app.firstInstalledTime,
                appSizeBytes = app.appSizeBytes,
                isHidden = app.isHidden,
                isSystemApp = app.isSystemApp,
            )
        }

        val notifSignals = runCatching {
            val since = nowMillis - TimeUnit.DAYS.toMillis(WINDOW_DAYS)
            val events = notificationEventDao.eventsSince(since)
            if (events.isEmpty()) return@runCatching null
            val appNames = apps.associate { it.packageName to it.appName }
            val usageMs = dailySessions?.groupBy { it.packageName }
                ?.mapValues { (_, days) -> days.sumOf { it.foregroundDurationMs } }
                ?: emptyMap()
            val report = NotificationAnalyzer.analyze(events, appNames, usageMs)
            PulseNotificationSignals(
                totalNotifications = report.totalNotifications,
                disturbingCount = report.disturbing.size,
                distractingCount = report.distracting.size,
                nightCount = report.appStats.sumOf { it.nightCount },
            )
        }.getOrNull()

        return PulseInput(
            apps = snapshots.filter { !it.isHidden },
            notification = notifSignals,
            previousCategoryUsage = WrappedSnapshotPrefs.getPrevious(context)?.categoryUsage,
            unlockCount = UsageStatsHelper.getUnlockCount(context, days = WINDOW_DAYS.toInt()),
            previousUnlockCount = WrappedSnapshotPrefs.getPreviousUnlockCount(context),
            taskScoreContribution = TaskScoreManager.getPulseContribution(context),
            hasUsageAccess = hasUsageAccess,
            nowMillis = nowMillis,
        )
    }
}
