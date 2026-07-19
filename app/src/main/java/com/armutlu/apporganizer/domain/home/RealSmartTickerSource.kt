package com.armutlu.apporganizer.domain.home

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.common.valueOrNull
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.insight.DeviceTidinessInsights
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
import com.armutlu.apporganizer.utils.UsageStatsHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
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
    private val notificationEventDao: NotificationEventDao,
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
            val streakItems = MissionPulseTickerFactory.streakCandidates(
                currentStreak = missionSummary?.currentStreak ?: 0,
                nowMillis = nowMillis,
            )

            // Döngü G8 — Cihaz Düzeni İçgörüleri. Toggle kapalıyken hiçbir tidiness öğesi
            // üretilmez (plan G8 kısıtı). DeviceTidinessInsights saf Kotlin — bu sınıf
            // Android/DB kaynaklarını (StatFs, apps, NotificationEventDao) okuyup besler.
            val tidinessItems = if (AppPrefs.isDeviceTidinessInsightsEnabled(context)) {
                buildTidinessCandidates(apps, nowMillis)
            } else {
                emptyList()
            }

            val allCandidates = composed + missionItems + pulseItems + streakItems + tidinessItems

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
                                key == "mission_all_completed" ||
                                key.startsWith("streak_milestone_"),
                            timeSensitive = key == "notification_summary" ||
                                key == "low_confidence_review" ||
                                key.startsWith("mission_at_risk_") ||
                                key == "tidiness_permission",
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

    /**
     * Döngü G8 — [DeviceTidinessInsights] için Android/DB kaynaklarını (StatFs, apps,
     * haftalık bildirim sayıları) okuyup saf Kotlin üreticiye besler. Yerelleştirilmiş metinler
     * burada `context.getString(...)` ile üretilir — üretici objenin kendisi hiçbir hardcoded
     * kullanıcı-dili string TAŞIMAZ (TR/EN ikisi de burada çözülür, plan G8 "EN'i unutma" notu).
     */
    private suspend fun buildTidinessCandidates(
        apps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
        nowMillis: Long,
    ): List<SmartTickerItem> {
        val hasUsageAccess = UsageStatsHelper.hasPermission(context)

        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.let { it.blockSizeLong * it.blockCountLong } ?: 0L
        val freeBytes = statFs?.let { it.blockSizeLong * it.availableBlocksLong } ?: 0L

        val appSnapshots = apps.map { app ->
            DeviceTidinessInsights.AppUsageSnapshot(
                packageName = app.packageName,
                lastUsedTimestamp = app.lastUsedTimestamp,
                appSizeBytes = app.appSizeBytes,
            )
        }

        val weeklyCounts = runCatching {
            notificationEventDao.countsSince(nowMillis - TimeUnit.DAYS.toMillis(7))
        }.getOrDefault(emptyList()).map { it.count }

        val texts = DeviceTidinessInsights.TidinessTexts(
            storageTitle = { percent, unusedCount, gbFreed ->
                context.getString(
                    com.armutlu.apporganizer.R.string.tidiness_storage_title,
                    percent,
                    DeviceTidinessInsights.UNUSED_THRESHOLD_DAYS,
                    unusedCount,
                    gbFreed,
                )
            },
            unusedTitle = { count ->
                context.getString(
                    com.armutlu.apporganizer.R.string.tidiness_unused_title,
                    DeviceTidinessInsights.UNUSED_THRESHOLD_DAYS,
                    count,
                )
            },
            notificationTitle = { total, topSharePercent ->
                context.getString(
                    com.armutlu.apporganizer.R.string.tidiness_notifications_title,
                    total,
                    topSharePercent,
                )
            },
            diagnosticsTitle = {
                context.getString(com.armutlu.apporganizer.R.string.tidiness_diagnostics_title)
            },
            actionInspect = context.getString(com.armutlu.apporganizer.R.string.tidiness_action_inspect),
            actionReport = context.getString(com.armutlu.apporganizer.R.string.tidiness_action_report),
            actionFix = context.getString(com.armutlu.apporganizer.R.string.tidiness_action_fix),
        )

        return DeviceTidinessInsights.all(
            hasUsageAccessPermission = hasUsageAccess,
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            apps = appSnapshots,
            weeklyNotificationCountsByPackage = weeklyCounts,
            nowMillis = nowMillis,
            texts = texts,
        )
    }
}
