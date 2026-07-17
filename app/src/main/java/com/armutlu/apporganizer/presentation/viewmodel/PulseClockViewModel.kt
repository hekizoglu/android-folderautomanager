package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.common.valueOrNull
import com.armutlu.apporganizer.domain.home.DigitalPulseRepository
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInputFactory
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInsightEngine
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInsightSpec
import com.armutlu.apporganizer.domain.usecase.pulse.PulseInsightType
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WeatherRepository
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
    private val digitalPulseRepository: DigitalPulseRepository,
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
        val hourlyUsageMinutes: List<Int>? = null, // 24 kova — index 23 = şu anki saat
        val personalityLabel: String? = null, // "🎯 Uretici" — D257 dijital kişilik etiketi
        val weather: WeatherRepository.Snapshot? = null,
        val weatherError: String? = null,
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
        // Döngü D00 — tek skor kaynağı: DigitalPulseEngine.compute() SADECE DigitalPulseRepository
        // içinde çağrılır. Burada repository'yi tazeleyip paylaşılan snapshot'ı okuyoruz; ana ekran
        // kartı ve Wrapped raporu da AYNI snapshot'ı görür (iki motor P0 2.1 çözüldü).
        digitalPulseRepository.refresh()
        val pulse = digitalPulseRepository.state.value.valueOrNull()?.score
            ?: return PulseClockUiState(loading = false)

        // Içgörü üretimi ve dijital kişilik etiketi için PulseInput yeniden kurulur — bu SADECE
        // sunum katmanı zenginleştirmesi, skor DEĞİLDİR (skor yukarıda repository'den geldi).
        val input = PulseInputFactory.build(
            context = context,
            appRepository = appRepository,
            notificationEventDao = notificationEventDao,
        )
        val dailySessions = (UsageStatsHelper.getDailySessionUsage(context, days = 7)
            as? UsageStatsHelper.DailySessionResult.Available)?.days
        val weeklyScreenTimeMinutes = dailySessions
            ?.groupBy { it.epochDay }
            ?.values
            ?.sumOf { days -> days.maxOfOrNull { it.globalForegroundMs } ?: 0L }
            ?.let { (it / TimeUnit.MINUTES.toMillis(1)).toInt() }

        // D257: dijital kişilik — gece kullanım oranı saatlik kovalardan türetilir (23:00-06:00).
        val nightUsageRatio = dailySessions?.takeIf { it.isNotEmpty() }?.let { days ->
            var night = 0L
            var total = 0L
            days.forEach { day ->
                day.hourlyForegroundMs.forEachIndexed { hour, ms ->
                    total += ms
                    if (hour >= 23 || hour <= 5) night += ms
                }
            }
            if (total > 0L) night.toDouble() / total else null
        }
        val personality = WrappedEngine.computePersonality(input.apps, nightUsageRatio)
        val personalityLabel = "${personality.type.emoji} ${personality.type.label}"

        // Haftalık karşılaştırma baseline'i — ilk hafta null (sahte +0 yasak).
        val previousScore = WrappedSnapshotPrefs.updateWeeklyPulseScore(context, pulse.total)
        WrappedSnapshotPrefs.setLatestPulseScore(context, pulse.total)

        // Tek içgörü — son gösterilen atlanarak dönüşümlü seçim.
        val insight = PulseInsightEngine.pickInsight(
            PulseInsightEngine.generate(input, pulse),
            AppPrefs.getPulseLastInsightId(context),
        )
        insight?.let { AppPrefs.setPulseLastInsightId(context, it.id) }
        val weatherResult = WeatherRepository.getWeather(context)

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
            hourlyUsageMinutes = UsageStatsHelper.getHourlyUsageLast24h(context),
            personalityLabel = personalityLabel,
            weather = (weatherResult as? WeatherRepository.Result.Success)?.snapshot,
            weatherError = (weatherResult as? WeatherRepository.Result.Error)?.message,
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
