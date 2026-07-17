package com.armutlu.apporganizer.domain.home

/**
 * Döngü T02 — roadmap §Döngü T02 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satır 1654-1723): şeridin en fazla üç yüksek değerli öğe taşımasını garanti eden saf sıralama
 * ve tekrar-önleme katmanı.
 *
 * [TickerComposer] her üreticiyi kendi kısıtlarıyla (expiresAt, suggestionKey, autoAdvanceAllowed)
 * üretir — bu nesne son güvenlik ağıdır: adayları roadmap'in önerdiği skor formülüyle sıralar,
 * süresi geçmiş/kapatılmış/aşırı tekrarlanan öğeleri eler, TÜR BAŞINA en fazla 1 öğe bırakır
 * (CRITICAL_HEALTH hariç) ve sonucu en fazla 3 öğeyle sınırlar.
 *
 * Mevcut tekrar geçmişi altyapısı ([SuggestionCoordinator] + [SuggestionHistoryStore]) burada
 * YENİDEN KULLANILIR — roadmap'in "paralel ikinci history sistemi oluşturulmadan önce mevcut API
 * incelenmeli" notu gereği ayrı bir SharedPreferences deposu (TickerSuppressionStore) YAZILMADI.
 * [SuggestionCoordinator.canShow] zaten şunları karşılıyor:
 * - Kullanıcı türü kapattı (dismiss) → [SuggestionHistoryStore.recordRejected] ile 3 gün gizlenir
 *   ([SuggestionCoordinatorPolicy.rejectionCooldownMs], varsayılan 3 gün — roadmap'in önerdiği
 *   "dismiss 7 gün" değeri yerine repo'da zaten var olan ve LauncherViewModel.dismissTickerItem
 *   tarafından kullanılan 3 günlük politika korundu; iki farklı süre aynı anahtar için çelişki
 *   yaratmasın diye TEK kaynak — [SuggestionCoordinatorPolicy] — esas alındı).
 * - Aynı öğe başka bir kanalda (task card, sistem bildirimi) yakın zamanda gösterildiyse ticker'da
 *   bastırılır (cross-channel cooldown, varsayılan 6 saat).
 *
 * Bu ranker EK olarak roadmap'in "Bugün daha önce gösterildi / Son üç günde üç kez gösterildi"
 * cezalarını uygulamak için TICKER kanalının kendi gösterim geçmişini kullanır — reddetme
 * (dismiss) skor cezası ÜRETMEZ (M08 ilkesi), sadece [SuggestionCoordinator] üzerinden gösterim
 * kısıtı uygular; ranker tarafında dismiss edilmiş bir öğe zaten `isSuppressed` ile filtrelenir.
 */
object TickerRanker {

    /** Şeritte aynı anda gösterilecek en fazla öğe sayısı (roadmap 2.7 kararı). */
    const val MAX_VISIBLE = 3

    /** Aynı türden en fazla kaç öğe gösterilebilir — CRITICAL_HEALTH hariç tüm türler için 1. */
    private const val MAX_PER_TYPE = 1

    private const val MS_PER_DAY = 24L * 3600 * 1000
    private const val REPEAT_WINDOW_DAYS = 3L
    private const val REPEAT_WINDOW_MS = REPEAT_WINDOW_DAYS * MS_PER_DAY
    private const val REPEAT_WINDOW_THRESHOLD = 3

    private const val PENALTY_SHOWN_TODAY = 35
    private const val PENALTY_REPEATED_IN_WINDOW = 70

    /**
     * [candidates] listesini roadmap skor formülüyle sıralar ve en fazla [MAX_VISIBLE] öğe
     * döndürür. Süresi geçmiş veya [isSuppressed] tarafından bastırılmış öğeler elenir.
     *
     * @param now deterministik test için dışarıdan verilir (System.currentTimeMillis() yerine).
     * @param isSuppressed "kullanıcı türü kapattı" veya çapraz-kanal bastırma kontrolü —
     *   [SuggestionCoordinator.canShow] ile köprülenmesi beklenir; saf fonksiyon burada Android
     *   bağımlılığı taşımamak için lambda olarak enjekte edilir.
     * @param history TICKER kanalının kendi gösterim geçmişi — "bugün gösterildi" / "son 3 günde
     *   3+ kez gösterildi" cezaları için. Boş [TickerHistory] hiçbir ceza uygulamaz.
     */
    fun rank(
        candidates: List<SmartTickerItem>,
        history: TickerHistory = TickerHistory.EMPTY,
        now: Long,
        isSuppressed: (SmartTickerItem) -> Boolean = { false },
    ): List<SmartTickerItem> {
        if (candidates.isEmpty()) return emptyList()

        val eligible = candidates
            .filterNot { it.isExpired(now) }
            .filterNot { isSuppressed(it) }
            .distinctByDedupeKey()

        val scored = eligible
            .map { item -> item to score(item, history, now) }
            .sortedWith(
                compareByDescending<Pair<SmartTickerItem, Int>> { it.second }
                    .thenByDescending { it.first.priority },
            )

        val result = mutableListOf<SmartTickerItem>()
        val typeCounts = mutableMapOf<SmartTickerType, Int>()

        for ((item, _) in scored) {
            if (result.size >= MAX_VISIBLE) break
            val cap = if (item.type == SmartTickerType.CRITICAL_HEALTH) Int.MAX_VALUE else MAX_PER_TYPE
            val currentCount = typeCounts.getOrDefault(item.type, 0)
            if (currentCount >= cap) continue
            result.add(item)
            typeCounts[item.type] = currentCount + 1
        }

        return result
    }

    /** Aynı [SmartTickerItem.dedupeKey]'den yalnızca ilk (en yüksek priority sonrası ilk) öğe kalır. */
    private fun List<SmartTickerItem>.distinctByDedupeKey(): List<SmartTickerItem> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<SmartTickerItem>()
        for (item in this.sortedByDescending { it.priority }) {
            if (seen.add(item.dedupeKey)) out.add(item)
        }
        return out
    }

    private fun score(item: SmartTickerItem, history: TickerHistory, now: Long): Int {
        var total = item.priority

        val key = item.dedupeKey
        val lastShownAt = history.lastShownAt[key]
        if (lastShownAt != null && isSameDay(lastShownAt, now)) {
            total -= PENALTY_SHOWN_TODAY
        }

        val recentShowCount = history.showTimestamps[key]
            ?.count { now - it in 0..REPEAT_WINDOW_MS }
            ?: 0
        if (recentShowCount >= REPEAT_WINDOW_THRESHOLD) {
            total -= PENALTY_REPEATED_IN_WINDOW
        }

        return total
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val dayA = Math.floorDiv(a, MS_PER_DAY)
        val dayB = Math.floorDiv(b, MS_PER_DAY)
        return dayA == dayB
    }
}

/**
 * TICKER kanalının kendi gösterim geçmişi — roadmap'in "bugün gösterildi" / "son 3 günde 3+ kez
 * gösterildi" cezaları için gereken minimum veri. Kalıcı depolama (SharedPreferences) burada
 * MODELLENMEZ — [RealSmartTickerSource]/LauncherViewModel bu değerleri mevcut
 * [SuggestionHistoryStore] üzerinden türetip enjekte eder; ranker saf kalır.
 */
data class TickerHistory(
    /** dedupeKey -> en son gösterildiği zaman (ms). */
    val lastShownAt: Map<String, Long> = emptyMap(),
    /** dedupeKey -> tüm gösterim zaman damgaları (tekrar penceresi hesaplamak için). */
    val showTimestamps: Map<String, List<Long>> = emptyMap(),
) {
    companion object {
        val EMPTY = TickerHistory()
    }
}
