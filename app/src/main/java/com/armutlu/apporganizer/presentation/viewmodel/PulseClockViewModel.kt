package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseEngine
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInput
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInsightEngine
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInsightSpec
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInsightType
import com.armutlu.apporganizer.domain.usecase.pulse.PulseNotificationSignals
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

/**
 * Pulse Clock (ana ekran saat kartı) durumu — Dijital Nabız skorunu ve tek satırlık
 * içgörüyü TEK motor (DigitalPulseEngine) üzerinden üretir.
 *
 * Performans: skor/içgörü hesabı yalnızca resume'da veya cache süresi (15 dk) dolunca
 * yapılır; saat güncellemesi (dakika tik'i) HİÇBİR skor hesabı tetiklemez. Tüm hesap
 * cihazda, IO dispatcher'da çalışır — sunucuya hiçbir veri gitmez.
 */
@HiltViewModel
class PulseClockViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val notificationEventDao: NotificationEventDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class PulseClockUiState(
        val score: Int? = null,
        val previousScore: Int? = null,
        val scoreDelta: Int? = null, // null = ilk hafta (veri birikiyor) — sahte +0 gösterilmez
        val insightText: String? = null,
        val insightPositive: Boolean? = null,
        val insightRouteKey: String? = null,
        val subScores: DigitalPulseScore? = null,
        val confidence: DataConfidence = DataConfidence.LOW,
        val weeklyScreenTimeMinutes: Int? = null,
        val loading: Boolean = true,
    )

    private val _uiState = MutableStateFlow(PulseClockUiState())
    val uiState: StateFlow<PulseClockUiState> = _uiState.asStateFlow()

    @Volatile private var lastComputedAt = 0L

    init {
        refreshIfStale()
    }

    /** Resume'da çağrılır — cache taze ise hiçbir iş yapmaz (ana ekran açılışını yormaz). */
    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastComputedAt < CACHE_TTL_MS && _uiState.value.score != null) return
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                runCatching { compute() }
                    .onFailure { e -> Timber.e(e, "PulseClock skoru hesaplanamadi") }
                    .getOrNull()
            }
            if (state != null) {
                lastComputedAt = System.currentTimeMillis()
                _uiState.value = state
            } else {
                _uiState.value = _uiState.value.copy(loading = false)
            }
        }
    }

    private suspend fun compute(): PulseClockUiState {
        val apps = appRepository.getAllApps()
        val hasUsageAccess = UsageStatsHelper.hasPermission(context)
        val dailySessions = (UsageStatsHelper.getDailySessionUsage(context, days = 7)
            as? UsageStatsHelper.DailySessionResult.Available)?.days
        val weeklyLaunches = dailySessions?.groupBy { it.packageName }
            ?.mapValues { (_, days) -> days.sumOf { it.launchCount }.toLong() }
        val weeklyScreenTimeMinutes = dailySessions
            ?.groupBy { it.epochDay }
            ?.values
            ?.sumOf { days -> days.maxOfOrNull { it.globalForegroundMs } ?: 0L }
            ?.let { (it / TimeUnit.MINUTES.toMillis(1)).toInt() }

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
            val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
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

        val input = PulseInput(
            apps = snapshots.filter { !it.isHidden },
            notification = notifSignals,
            previousCategoryUsage = WrappedSnapshotPrefs.getPrevious(context)?.categoryUsage,
            unlockCount = UsageStatsHelper.getUnlockCount(context, days = 7),
            previousUnlockCount = WrappedSnapshotPrefs.getPreviousUnlockCount(context),
            hasUsageAccess = hasUsageAccess,
        )
        val pulse = DigitalPulseEngine.compute(input)

        // Haftalık karşılaştırma baseline'i — ilk hafta null (sahte +0 yasak).
        val previousScore = WrappedSnapshotPrefs.updateWeeklyPulseScore(context, pulse.total)
        WrappedSnapshotPrefs.setLatestPulseScore(context, pulse.total)

        // Tek içgörü — son gösterilen atlanarak dönüşümlü seçim.
        val insight = PulseInsightEngine.pickInsight(
            PulseInsightEngine.generate(input, pulse),
            AppPrefs.getPulseLastInsightId(context),
        )
        insight?.let { AppPrefs.setPulseLastInsightId(context, it.id) }

        return PulseClockUiState(
            score = pulse.total,
            previousScore = previousScore,
            scoreDelta = previousScore?.let { pulse.total - it },
            insightText = insight?.let { resolveInsightText(it) },
            insightPositive = insight?.positive,
            insightRouteKey = insight?.routeKey,
            subScores = pulse,
            confidence = pulse.confidence,
            weeklyScreenTimeMinutes = weeklyScreenTimeMinutes,
            loading = false,
        )
    }

    /** Yapılandırılmış içgörüyü string resource üzerinden kullanıcı metnine çevirir. */
    private fun resolveInsightText(spec: PulseInsightSpec): String = when (spec.type) {
        PulseInsightType.NOTIF_ISSUE ->
            context.getString(R.string.pulse_insight_notif_issue, spec.intArg ?: 0)
        PulseInsightType.NOTIF_CALM ->
            context.getString(R.string.pulse_insight_notif_calm)
        PulseInsightType.UNUSED_APPS ->
            context.getString(R.string.pulse_insight_unused, spec.intArg ?: 0)
        PulseInsightType.CATEGORY_SHIFT ->
            context.getString(
                R.string.pulse_insight_category_shift,
                categoryLabel(spec.textArg),
                spec.intArg ?: 0,
            )
        PulseInsightType.UNLOCK_TREND ->
            if (spec.positive == true) {
                context.getString(R.string.pulse_insight_unlock_down, spec.intArg ?: 0)
            } else {
                context.getString(R.string.pulse_insight_unlock_up, spec.intArg ?: 0)
            }
        PulseInsightType.ORGANIZED_WELL ->
            context.getString(R.string.pulse_insight_organized)
        PulseInsightType.GENERAL ->
            context.getString(R.string.pulse_insight_general)
    }

    private fun categoryLabel(categoryId: String?): String {
        if (categoryId == null) return ""
        return Category.getDefaultCategories()
            .firstOrNull { it.categoryId == categoryId }
            ?.categoryName ?: categoryId
    }

    companion object {
        private const val CACHE_TTL_MS = 15L * 60 * 1000 // 15 dk — saat tik'i hesap tetiklemez
    }
}
