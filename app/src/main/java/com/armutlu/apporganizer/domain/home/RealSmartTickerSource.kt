package com.armutlu.apporganizer.domain.home

import android.content.Context
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.common.valueOrNull
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.service.AppNotificationListenerService
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.AppSnapshot
import com.armutlu.apporganizer.utils.FolderSnapshot
import com.armutlu.apporganizer.utils.InsightEngine
import com.armutlu.apporganizer.utils.InsightSnapshot
import com.armutlu.apporganizer.utils.SharedPrefsSuggestionHistoryStore
import com.armutlu.apporganizer.utils.SuggestionCandidate
import com.armutlu.apporganizer.utils.SuggestionChannel
import com.armutlu.apporganizer.utils.SuggestionCoordinator
import com.armutlu.apporganizer.utils.TickerComposer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Döngü T01 — [SmartTickerEngine] için GERÇEK kaynak. [TickerComposer]'ın ürettiği
 * [SmartTickerItem] listesini [HomeIntelligenceCoordinator]'a taşır.
 *
 * NOT: Bu kaynak TickerComposer'ın "tekil, koşulsuz" üreticilerini (bildirim özeti, unutulan
 * uygulama, içgörü, düşük güven uyarısı, özellik ipucu, haftalık özet) VE Döngü T03'te eklenen
 * [MissionPulseTickerFactory] üreticilerini (görev risk/başarı, Dijital Yaşam skor değişimi/
 * çözülebilir sorun) besler. `LauncherViewModel.tickerItems` ekranın gerçek kaynağıdır — kendi
 * dock/klasör state'lerini kullanarak reaktif biçimde aynı [TickerComposer.compose] fonksiyonunu
 * çağırır ve ek olarak arama istatistiği/Wrapped teaser haberlerini + oturum-bazlı dismiss
 * filtresini uygular. Bu sınıf [HomeIntelligenceCoordinator] üzerinden okunan koordinatör-
 * seviyesi state için ayrı bir (daha basit, DB'den taze) anlık görüntü üretir.
 *
 * Döngü T03 — [missionRuntimeRepository]/[digitalPulseRepository] BURADA DOĞRUDAN enjekte
 * edilir, [HomeIntelligenceCoordinator] ÜZERİNDEN DEĞİL: koordinatör bu sınıfı ([SmartTickerEngine]
 * arayüzü üzerinden) kendisi enjekte ediyor (bkz. HomeIntelligenceCoordinator constructor'ı) —
 * koordinatörü burada enjekte etmek döngüsel bağımlılık yaratırdı. Her iki repository de zaten
 * bağımsız Hilt singleton'ları (RealMissionRuntimeSource/RealDigitalPulseSource), bu yüzden
 * doğrudan enjeksiyon güvenli.
 *
 * Döngü T02: çıktı [TickerRanker.rank] üzerinden geçirilir — en fazla 3 öğe, tür kotası ve
 * mevcut [SuggestionCoordinator] tekrar/bastırma geçmişi uygulanır.
 */
@Singleton
class RealSmartTickerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val classifier: AppClassifier,
    private val missionRuntimeRepository: MissionRuntimeRepository,
    private val digitalPulseRepository: DigitalPulseRepository,
) : SmartTickerEngine {

    private val _state = MutableStateFlow(TickerSourceState())
    override val state: StateFlow<TickerSourceState> = _state.asStateFlow()

    override suspend fun refresh() {
        runCatching {
            val apps = appRepository.getAllApps().filter { !it.isHidden }
            val categories = Category.getDefaultCategories()
            val categoryNameById = categories.associate { it.categoryId to it.categoryName }
            val categoryEmojiById = categories.associate { it.categoryId to it.iconEmoji }

            val folderSnapshots = apps
                .groupBy { it.categoryId }
                .map { (categoryId, appsInCategory) ->
                    FolderSnapshot(
                        categoryId = categoryId,
                        categoryName = categoryNameById[categoryId] ?: categoryId,
                        emoji = categoryEmojiById[categoryId] ?: "📁",
                        appCount = appsInCategory.size,
                    )
                }

            val appSnapshots = apps.map { app ->
                AppSnapshot(
                    packageName = app.packageName,
                    appName = app.appName,
                    usageCount = app.usageCount,
                    lastUsedTimestamp = app.lastUsedTimestamp,
                )
            }

            val badgeCounts = AppNotificationListenerService.badgeCounts.value
            val badgeTotal = badgeCounts.values.sum()

            val insightSnapshots = InsightEngine.generate(
                context = context,
                apps = apps,
                categories = categories,
                badgeCounts = badgeCounts,
            ).map { card ->
                InsightSnapshot(
                    id = card.id,
                    message = card.message,
                    categoryId = card.categoryId,
                    packageName = card.packageName,
                )
            }

            val showSystemApps = AppPrefs.isShowSystemApps(context)
            val lowConfidenceCount = apps.count { app ->
                (showSystemApps || !app.isSystemApp) && classifier.isLowConfidence(app, app.categoryId)
            }

            val nowMillis = System.currentTimeMillis()
            val composed = TickerComposer.compose(
                folders = folderSnapshots,
                apps = appSnapshots,
                badgeTotal = badgeTotal,
                insights = insightSnapshots,
                lowConfidenceCount = lowConfidenceCount,
                nowMillis = nowMillis,
            )

            // Döngü T03 — görev/Dijital Yaşam üreticileri. Kendi refresh()'leri kendi
            // koordinatör-kaynak try/catch'lerinde çalışır (HomeIntelligenceCoordinator sırayla
            // her kaynağı refreshSource ile çağırır); burada sadece güncel state okunur, ikinci
            // bir refresh TETİKLENMEZ — aksi halde aynı veri iki kez (ve farklı thread/zamanlama
            // ile) hesaplanabilir.
            val missionSummary = missionRuntimeRepository.state.value.summary
            val pulseSnapshot = digitalPulseRepository.state.value.valueOrNull()
            val missionItems = MissionPulseTickerFactory.missionCandidates(missionSummary, nowMillis)
            val pulseItems = MissionPulseTickerFactory.pulseCandidates(pulseSnapshot, nowMillis)

            val allCandidates = composed + missionItems + pulseItems

            // Döngü T02: en fazla 3 yüksek değerli öğe + tekrar/suistimal önleme (roadmap 2.7).
            // Suppression mevcut SuggestionCoordinator/SharedPrefsSuggestionHistoryStore üzerinden
            // yeniden kullanılır — paralel bir history deposu YAZILMADI (roadmap notu).
            val historyStore = SharedPrefsSuggestionHistoryStore(context)
            TickerRanker.rank(
                candidates = allCandidates,
                now = nowMillis,
                isSuppressed = isSuppressed@{ item ->
                    val key = item.suggestionKey ?: return@isSuppressed false
                    !SuggestionCoordinator.canShow(
                        candidate = SuggestionCandidate(
                            dedupeKey = key,
                            highValue = key == "notification_summary" ||
                                key.startsWith("mission_at_risk_") ||
                                key == "mission_all_completed",
                            timeSensitive = key == "notification_summary" ||
                                key == "low_confidence_review" ||
                                key.startsWith("mission_at_risk_"),
                        ),
                        channel = SuggestionChannel.TICKER,
                        store = historyStore,
                        nowMillis = nowMillis,
                    )
                },
            )
        }.fold(
            onSuccess = { items -> _state.value = TickerSourceState(items = items) },
            onFailure = { error ->
                Timber.w(error, "RealSmartTickerSource: refresh hatasi")
                // Hata durumunda onceki state korunur (coordinator Stale/Failed sinifladirmasini
                // digerleri gibi kendi refreshSource try/catch'inde yapar).
            },
        )
    }
}
