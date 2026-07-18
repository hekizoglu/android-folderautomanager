package com.armutlu.apporganizer.domain.home

import android.content.Context
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.AppSnapshot
import com.armutlu.apporganizer.utils.FolderSnapshot
import com.armutlu.apporganizer.utils.InsightCard
import com.armutlu.apporganizer.utils.InsightSnapshot
import com.armutlu.apporganizer.utils.SharedPrefsSuggestionHistoryStore
import com.armutlu.apporganizer.utils.SuggestionCandidate
import com.armutlu.apporganizer.utils.SuggestionChannel
import com.armutlu.apporganizer.utils.SuggestionCoordinator
import com.armutlu.apporganizer.utils.TickerComposer

/**
 * Döngü U01 — LauncherViewModel'in `tickerItems` combine bloğunda taşıdığı ticker üretim/
 * filtre işi buraya taşındı. ViewModel artık yalnızca kaynak flow'ları combine edip bu use-case'i
 * çağırır; snapshot inşası, TickerComposer/TickerRanker çağrısı, tür/hassasiyet/dismiss filtreleri
 * ve ekstra haberler (arama istatistiği, Wrapped teaser) burada saf/yarı-saf fonksiyonlar olarak
 * yaşar. Davranış AYNEN korunur — bu saf bir taşıma refactor'üdür.
 *
 * NOT: [RealSmartTickerSource] (T01) aynı compose+rank+suppress deseniyle
 * [HomeIntelligenceCoordinator] için AYRI bir ticker state üretir; ancak o state hâlâ hiçbir UI
 * tarafından tüketilmiyor (`HomeScreen` yalnız bu use-case'in ürettiği `LauncherViewModel.tickerItems`'ı
 * okur). İki kaynağın birleştirilmesi ayrı bir döngüde ele alınmalı — bu döngüde davranışı
 * değiştirmemek için dokunulmadı.
 */
object HomeTickerComposer {

    /** [dismissTickerItem] tarafından kapatılan dedupeKey seti gibi çağıran taraf state'leri. */
    data class Input(
        val folders: List<AppFolder>,
        val insightCards: List<InsightCard>,
        val badgeCounts: Map<String, Int>,
        val dismissedKeys: Set<String>,
        val hiddenTickerTypes: Set<String>,
        val nowMillis: Long = System.currentTimeMillis(),
    )

    /**
     * [Input]'u tam ticker listesine dönüştürür — snapshot inşası, TickerComposer.compose,
     * TickerRanker.rank + suppression kaydı, ekstra üreticiler (arama istatistiği/Wrapped),
     * tür/hassasiyet/dismiss filtreleri ve "hepsi kapatıldıysa sıfırla" davranışı dahil.
     */
    fun compose(context: Context, classifier: AppClassifier, input: Input): List<SmartTickerItem> {
        val folderSnapshots = input.folders.map { f ->
            FolderSnapshot(
                categoryId = f.category.categoryId,
                categoryName = f.category.categoryName,
                emoji = f.category.iconEmoji,
                appCount = f.apps.size,
            )
        }
        val appSnapshots = input.folders.flatMap { it.apps }.map { a ->
            AppSnapshot(
                packageName = a.packageName,
                appName = a.appName,
                usageCount = a.usageCount,
                lastUsedTimestamp = a.lastUsedTimestamp,
            )
        }
        val insightSnapshots = input.insightCards.map { card ->
            InsightSnapshot(
                id = card.id,
                message = card.message,
                categoryId = card.categoryId,
                packageName = card.packageName,
            )
        }
        val showSystemApps = AppPrefs.isShowSystemApps(context)
        // Dusuk guvenli otomatik kategorileme uyarisi (K3, Dongu 227, Fable danismanligi) —
        // getConfidence() mevcuttu ama hicbir UX'e baglanmamisti. Esik: 60 altinda "belirsiz" sayilir.
        val lowConfidenceCount = input.folders.sumOf { f ->
            f.apps.count { app ->
                (showSystemApps || !app.isSystemApp) && classifier.isLowConfidence(app, f.category.categoryId)
            }
        }
        val totalNotif = input.badgeCounts.values.sum()

        // Dijital yasam skoru ticker'dan KALDIRILDI (Dongu D00, P0 2.1) — TickerComposer artik
        // skor hesaplamiyor. Skor ayri DigitalLifeCard'da homePulseSummary StateFlow'undan
        // (HomeIntelligenceCoordinator -> DigitalPulseRepository) gosteriliyor (Dongu D02).
        val historyStore = SharedPrefsSuggestionHistoryStore(context)
        val nowForCompose = input.nowMillis
        val rawComposed = TickerComposer.compose(
            folders = folderSnapshots,
            apps = appSnapshots,
            badgeTotal = totalNotif,
            insights = insightSnapshots,
            lowConfidenceCount = lowConfidenceCount,
            nowMillis = nowForCompose,
        )
        // Döngü T02: TickerRanker en fazla 3 öğe + tür kotası uygular (roadmap 2.7). Bastırma
        // (dismiss/çapraz-kanal cooldown) hâlâ mevcut SuggestionCoordinator üzerinden yapılır —
        // paralel bir history deposu eklenmedi (roadmap notu, RealSmartTickerSource ile aynı desen).
        val composed = TickerRanker.rank(
            candidates = rawComposed,
            now = nowForCompose,
            isSuppressed = isSuppressed@{ spec ->
                val key = spec.suggestionKey ?: return@isSuppressed false
                !SuggestionCoordinator.canShow(
                    candidate = SuggestionCandidate(
                        dedupeKey = key,
                        highValue = key == "notification_summary",
                        timeSensitive = key == "notification_summary" || key == "low_confidence_review",
                    ),
                    channel = SuggestionChannel.TICKER,
                    store = historyStore,
                    nowMillis = nowForCompose,
                )
            },
        ).map { spec ->
            val key = spec.suggestionKey
            if (key != null) {
                SuggestionCoordinator.recordShown(
                    candidate = SuggestionCandidate(
                        dedupeKey = key,
                        highValue = key == "notification_summary",
                        timeSensitive = key == "notification_summary" || key == "low_confidence_review",
                    ),
                    channel = SuggestionChannel.TICKER,
                    store = historyStore,
                    nowMillis = System.currentTimeMillis(),
                )
            }
            spec
        } + buildSearchStatsTicker(context) + buildWrappedTicker(context)

        // İçerik bazlı bastırma (roadmap T04 "Bu tür bilgileri gösterme") — dismissed'ten ÖNCE
        // uygulanır, tür kalıcı olarak gizlenmişse tekil dismiss listesine hiç girmez.
        val notHiddenType = composed.filterNot { it.type.name in input.hiddenTickerTypes }
        // T05 (Akıllı Nabız ayarları) — toplu içerik-türü/hassas-bilgi switch'leri doğrudan
        // AppPrefs'e yazılır; T04'ün tekil "bu türü gösterme" kapatmasıyla aynı sonuca (öğe hiç
        // üretilmemiş gibi davranır) varır ama ayrı bir tercih kümesidir.
        val notHiddenByGroup = notHiddenType.filter {
            AppPrefs.isSmartTickerTypeVisible(context, it.type.name)
        }
        // T05 — "Hassas bilgileri göster" kapalıyken sensitive=true işaretli öğeler (ör. aktif
        // bildirim sayısı) şeritte hiç görünmez; varsayılan kapalı (roadmap mock satır 1868).
        val notSensitive = if (AppPrefs.isTickerSensitiveVisible(context)) {
            notHiddenByGroup
        } else {
            notHiddenByGroup.filterNot { it.sensitive }
        }
        val visible = notSensitive.filterNot { it.dedupeKey in input.dismissedKeys }
        // Hepsi dismiss edildiyse bu oturumda haberler tükendi demektir — sıfırla ki
        // ticker boş kalmasın (yeni klasör/içgörü verisi geldiğinde zaten otomatik güncellenir).
        // notSensitive kullanılır (notHiddenType değil) — aksi halde tür/hassas-bilgi ayarıyla
        // kapatılmış öğeler dismiss-reset sırasında yanlışlıkla geri gelirdi.
        return if (visible.isEmpty()) notSensitive else visible
    }

    /** Haftalik Rapor (Wrapped) teaser haberi — hafta sonu ve pazartesi gorunur, dokununca rapor acilir. */
    private fun buildWrappedTicker(context: Context): List<SmartTickerItem> = runCatching {
        if (!AppPrefs.isWrappedEnabled(context)) return@runCatching emptyList()
        val day = java.time.LocalDate.now().dayOfWeek
        val weekendOrMonday = day == java.time.DayOfWeek.SATURDAY ||
            day == java.time.DayOfWeek.SUNDAY || day == java.time.DayOfWeek.MONDAY
        if (!weekendOrMonday) return@runCatching emptyList()
        listOf(
            SmartTickerItem(
                id = "wrapped_teaser",
                type = SmartTickerType.WEEKLY_REPORT,
                title = "Haftalık raporun hazır",
                subtitle = "Skorunu ve rozetlerini gör",
                icon = "🎁",
                priority = 60,
                createdAt = System.currentTimeMillis(),
                action = TickerAction.OpenWeeklyReport,
                suggestionKey = "wrapped_teaser",
            )
        )
    }.getOrDefault(emptyList())

    /** Arama istatistigi haberi — SearchStatsPrefs anonim agregatlarindan uretilir; 5+ arama olunca gorunur. */
    private fun buildSearchStatsTicker(context: Context): List<SmartTickerItem> = runCatching {
        if (!AppPrefs.isSearchStatsEnabled(context)) return@runCatching emptyList()
        val s = com.armutlu.apporganizer.utils.SearchStatsPrefs.getSummary(context)
        if (s.totalSearches < 5) return@runCatching emptyList()
        val title: String
        val subtitle: String
        if (s.totalClicks > 0) {
            val firstPct = s.firstResultClicks * 100 / s.totalClicks
            title = "${s.totalSearches} arama yaptın"
            subtitle = "%$firstPct ilk sonuçta buldu — detay için dokun"
        } else {
            title = "${s.totalSearches} arama yaptın"
            subtitle = "İstatistikler için dokun"
        }
        listOf(
            SmartTickerItem(
                id = "search_stats_summary",
                type = SmartTickerType.CONTEXTUAL_SUGGESTION,
                title = title,
                subtitle = subtitle,
                icon = "🔎",
                priority = 20,
                createdAt = System.currentTimeMillis(),
                action = TickerAction.OpenSearchStats,
                suggestionKey = "search_stats_summary",
            )
        )
    }.getOrDefault(emptyList())
}
