package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedAiCoach
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.NotificationAnalyzer
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
    val previousScore: Int? = null,
    val aiCoachLoading: Boolean = false,
    val aiCoachComment: String? = null,
    val unlockCount: Int? = null,
    val previousUnlockCount: Int? = null,
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
            val result = withContext(Dispatchers.IO) {
                runCatching { buildReport() }
                    .onFailure { e -> Timber.e(e, "WrappedReport üretilemedi") }
                    .getOrNull()
            }
            val unlockCount = withContext(Dispatchers.IO) {
                UsageStatsHelper.getUnlockCount(context, days = 7)
            }
            _uiState.value = WrappedUiState(
                loading = false,
                hasUsagePermission = UsageStatsHelper.hasPermission(context),
                report = result,
                previousScore = WrappedSnapshotPrefs.getLastScore(context),
                aiCoachLoading = shouldLoadAiCoach(result),
                unlockCount = unlockCount,
                previousUnlockCount = WrappedSnapshotPrefs.getPreviousUnlockCount(context),
            )
            // Bir sonraki karşılaştırma için mevcut skoru kaydet.
            result?.let { WrappedSnapshotPrefs.setLastScore(context, it.score.score) }
            loadAiCoachIfNeeded(result)
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

    private suspend fun buildReport(): WrappedEngine.WrappedReport {
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

        val notifSummary = runCatching {
            val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            val events = notificationEventDao.eventsSince(since)
            if (events.isEmpty()) return@runCatching null
            val appNames = apps.associate { it.packageName to it.appName }
            val usageMs = dailySessions?.groupBy { it.packageName }
                ?.mapValues { (_, days) -> days.sumOf { it.foregroundDurationMs } }
                ?: emptyMap()
            val report = NotificationAnalyzer.analyze(events, appNames, usageMs)
            WrappedEngine.NotificationSummary(
                totalNotifications = report.totalNotifications,
                disturbingCount = report.disturbing.size,
                distractingCount = report.distracting.size,
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
        )

        return WrappedEngine.compute(input)
    }

    fun enableUsagePermission() {
        UsageStatsHelper.openPermissionSettings(context)
    }

    companion object {
        fun isFeatureEnabled(context: Context): Boolean = AppPrefs.isWrappedEnabled(context)
    }
}
