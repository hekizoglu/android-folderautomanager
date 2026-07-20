package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.common.valueOrNull
import com.armutlu.apporganizer.domain.home.DigitalPulseRepository
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedAiCoach
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import com.armutlu.apporganizer.utils.TaskScoreManager
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WrappedSnapshotPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class WrappedUiState(
    val loading: Boolean = true,
    val hasUsagePermission: Boolean = false,
    val report: WrappedEngine.WrappedReport? = null,
    val charts: WrappedChartData = WrappedChartData(),
    val previousScore: Int? = null,
    val aiCoachLoading: Boolean = false,
    val aiCoachComment: String? = null,
    val aiCoachNeedsKey: Boolean = false,
    val unlockCount: Int? = null,
    val previousUnlockCount: Int? = null,
)

data class WrappedChartData(
    val dailyUsageMinutes: List<Int> = emptyList(),
    val dailyNotificationCounts: List<Int> = emptyList(),
    val dailyNightNotificationCounts: List<Int> = emptyList(),
    val categoryShares: List<CategoryShare> = emptyList(),
)

data class CategoryShare(
    val categoryId: String,
    val percent: Int,
)

/**
 * Haftalık Rapor (Wrapped) ekranı için veri hazırlar — AppRepository (Room apps) +
 * NotificationEventDao (bildirim özeti, içerik hariç) + WrappedSnapshotPrefs (geçen hafta
 * kategori agregatı) girdilerini WrappedEngine.compute()'a besler. Tüm işlem cihazda,
 * sunucuya hiçbir şey gönderilmez.
 */
@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val notificationEventDao: NotificationEventDao,
    private val wrappedAiCoach: WrappedAiCoach,
    private val digitalPulseRepository: DigitalPulseRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrappedUiState())
    val uiState: StateFlow<WrappedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(loading = true)
        viewModelScope.launch {
            val unlockCount = withContext(Dispatchers.IO) {
                UsageStatsHelper.getUnlockCount(context, days = 7)
            }
            val previousUnlockCount = WrappedSnapshotPrefs.getPreviousUnlockCount(context)
            val result = withContext(Dispatchers.IO) {
                runCatching { buildReport(unlockCount, previousUnlockCount) }
                    .onFailure { e -> Timber.e(e, "WrappedReport üretilemedi") }
                    .getOrNull()
            }
            // Döngü D01: previousScore artık gerçek ISO takvim haftasına dayanır ve paylaşılan
            // DigitalPulseSnapshot'tan (RealDigitalPulseSource → PulseHistoryPrefs) gelir — state
            // üzerinden ScoreCard'a akar. İlk hafta null döner — sahte +0 karşılaştırması gösterilmez.
            val previousScore = result?.previousScore
            result?.report?.let { WrappedSnapshotPrefs.setLatestPulseScore(context, it.pulse.total) }
            _uiState.value = WrappedUiState(
                loading = false,
                hasUsagePermission = UsageStatsHelper.hasPermission(context),
                report = result?.report,
                charts = result?.charts ?: WrappedChartData(),
                previousScore = previousScore,
                aiCoachLoading = shouldLoadAiCoach(result?.report),
                aiCoachNeedsKey = result?.report != null &&
                    AppPrefs.isWrappedAiCoachEnabled(context) &&
                    AppPrefs.getDeepSeekApiKey(context).isBlank(),
                unlockCount = unlockCount,
                previousUnlockCount = previousUnlockCount,
            )
            loadAiCoachIfNeeded(result?.report)
        }
    }

    private fun shouldLoadAiCoach(report: WrappedEngine.WrappedReport?): Boolean =
        report != null &&
            AppPrefs.isWrappedAiCoachEnabled(context) &&
            AppPrefs.getDeepSeekApiKey(context).isNotBlank()

    private fun loadAiCoachIfNeeded(report: WrappedEngine.WrappedReport?) {
        if (!shouldLoadAiCoach(report)) return
        viewModelScope.launch {
            val apiKey = AppPrefs.getDeepSeekApiKey(context)
            val comment = withContext(Dispatchers.IO) {
                wrappedAiCoach.summarize(report!!, apiKey)
            }
            _uiState.value = _uiState.value.copy(
                aiCoachLoading = false,
                aiCoachComment = comment,
            )
        }
    }

    private suspend fun buildReport(
        unlockCount: Int?,
        previousUnlockCount: Int?,
    ): BuildResult {
        val apps = appRepository.getAllApps()
        val dailySessionResult = UsageStatsHelper.getDailySessionUsage(context, days = 7)
        val dailySessions = (dailySessionResult as? UsageStatsHelper.DailySessionResult.Available)
            ?.days
        val weeklyLaunches = dailySessions?.groupBy { it.packageName }
            ?.mapValues { (_, days) -> days.sumOf { it.launchCount }.toLong() }
        val snapshots = apps.map { app ->
            WrappedEngine.AppSnapshot(
                packageName = app.packageName,
                appName = app.appName,
                categoryId = app.categoryId,
                // AppSnapshot.usageCount = adet (kez açıldı) — tutarlı olsun: gerçek haftalık
                // başlatma sayısı yoksa launcher launchCount'a düş (app.usageCount ms tutar, kullanma).
                usageCount = weeklyLaunches?.get(app.packageName) ?: app.launchCount,
                lastUsedTimestamp = app.lastUsedTimestamp,
                installTime = app.installTime,
                firstInstalledTime = app.firstInstalledTime,
                appSizeBytes = app.appSizeBytes,
                isHidden = app.isHidden,
                isSystemApp = app.isSystemApp,
            )
        }

        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val notificationEvents = runCatching {
            notificationEventDao.eventsSince(since)
        }.onFailure { e -> Timber.e(e, "Bildirim olaylari alinamadi") }.getOrDefault(emptyList())

        val notifSummary = runCatching {
            if (notificationEvents.isEmpty()) return@runCatching null
            val appNames = apps.associate { it.packageName to it.appName }
            val usageMs = dailySessions?.groupBy { it.packageName }
                ?.mapValues { (_, days) -> days.sumOf { it.foregroundDurationMs } }
                ?: emptyMap()
            val report = NotificationAnalyzer.analyze(notificationEvents, appNames, usageMs)
            WrappedEngine.NotificationSummary(
                totalNotifications = report.totalNotifications,
                disturbingCount = report.disturbing.size,
                distractingCount = report.distracting.size,
                nightCount = report.appStats.sumOf { it.nightCount },
            )
        }.onFailure { e -> Timber.e(e, "Bildirim ozeti alinamadi") }.getOrNull()

        val previousSnapshot = WrappedSnapshotPrefs.getPrevious(context)

        val folderCount = apps
            .filter { !it.isHidden && it.categoryId != "uncategorized" }
            .groupBy { it.categoryId }
            .count { (_, list) -> list.isNotEmpty() }

        val launcherInstalledDays = runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            ((System.currentTimeMillis() - pkgInfo.firstInstallTime) / (24L * 60 * 60 * 1000)).toInt()
        }.getOrDefault(0)

        val input = WrappedEngine.WrappedInput(
            apps = snapshots,
            notificationSummary = notifSummary,
            previousSnapshot = previousSnapshot,
            folderCount = folderCount,
            launcherInstalledDays = launcherInstalledDays,
            unlockCount = unlockCount,
            previousUnlockCount = previousUnlockCount,
            taskScoreContribution = TaskScoreManager.getPulseContribution(context),
            hasUsageAccess = UsageStatsHelper.hasPermission(context),
        )

        val engineReport = WrappedEngine.compute(input)
        // Döngü D00 — tek skor kaynağı: DigitalPulseEngine.compute() halen WrappedEngine.compute()
        // içinde çağrılıyor (motorun hesap mantığı DEĞİŞMEDİ), ama Wrapped ekranında GÖSTERİLEN
        // skor artık DigitalPulseRepository'nin paylaşılan snapshot'ından gelir — ana ekran kartı,
        // Pulse Clock ve bu rapor aynı sayıyı gösterir (P0 2.1 çözümü).
        digitalPulseRepository.refresh()
        val sharedSnapshot = digitalPulseRepository.state.value.valueOrNull()
        val sharedPulse = sharedSnapshot?.score
        val report = if (sharedPulse != null) {
            engineReport.copy(
                score = engineReport.score.copy(
                    score = sharedPulse.total,
                    reasons = sharedPulse.reasons.map {
                        WrappedEngine.ScoreReason(it.id.logLabel, it.delta)
                    },
                ),
                pulse = sharedPulse,
            )
        } else {
            engineReport
        }
        return BuildResult(
            report = report,
            previousScore = sharedSnapshot?.previousScore,
            charts = WrappedChartData(
                dailyUsageMinutes = buildDailyUsageTrend(dailySessions),
                dailyNotificationCounts = buildDailyNotificationTrend(notificationEvents, nightOnly = false),
                dailyNightNotificationCounts = buildDailyNotificationTrend(notificationEvents, nightOnly = true),
                categoryShares = report.personality.categoryPercentages
                    .filterValues { it > 0 }
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { CategoryShare(categoryId = it.key, percent = it.value) },
            ),
        )
    }

    fun enableUsagePermission() {
        UsageStatsHelper.openPermissionSettings(context)
    }

    companion object {
        fun isFeatureEnabled(context: Context): Boolean = AppPrefs.isWrappedEnabled(context)
    }
}

private data class BuildResult(
    val report: WrappedEngine.WrappedReport,
    val charts: WrappedChartData,
    val previousScore: Int? = null,
)

private fun buildDailyUsageTrend(dailySessions: List<com.armutlu.apporganizer.domain.usecase.usage.DailyPackageUsage>?): List<Int> {
    val today = java.time.LocalDate.now().toEpochDay()
    val byDay = dailySessions.orEmpty()
        .groupBy { it.epochDay }
        .mapValues { (_, rows) -> rows.maxOfOrNull { it.globalForegroundMs } ?: 0L }
    return (6 downTo 0).map { offset ->
        ((byDay[today - offset] ?: 0L) / 60_000L).toInt()
    }
}

private fun buildDailyNotificationTrend(
    events: List<com.armutlu.apporganizer.domain.models.NotificationEvent>,
    nightOnly: Boolean,
): List<Int> {
    val now = System.currentTimeMillis()
    val dayMs = TimeUnit.DAYS.toMillis(1)
    val counts = IntArray(7)
    val calendar = java.util.Calendar.getInstance()
    events.forEach { event ->
        val index = 6 - ((now - event.postedAt) / dayMs).toInt().coerceIn(0, 6)
        if (index !in 0..6) return@forEach
        calendar.timeInMillis = event.postedAt
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour >= 23 || hour < 7
        if (!nightOnly || isNight) counts[index]++
    }
    return counts.toList()
}
