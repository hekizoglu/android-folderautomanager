package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.R

/**
 * Görev S1 — Dashboard'da ayrı ayrı çizilen zeka kartlarının (HomeMissionCard, DigitalLifeCard,
 * bugün yüklenenler chip'i, AssistantInsightRow) yerine geçebilecek TEK bağlamsal "BUGÜN" kartı
 * için öncelik seçici. Saf Kotlin (Android/Compose bağımlılığı yok) — unit test edilebilir.
 *
 * Girdiler MEVCUT akan verilerden türetilir, yeni bir veri kaynağı EKLENMEZ:
 * - [HomeMissionSummary] — Ana ekran "Görevler" kartının özeti (HomeMissionSummarySelector.build).
 * - [HomePulseSummary] — "Dijital Yaşam" kartının özeti (toHomePulseSummary). Kritik izin/veri
 *   eksikliği [HomePulseSummary.freshness] == [DataFreshness.UNAVAILABLE] ile, klasör/sınıflandırma
 *   incelemesi ihtiyacı [HomePulseSummary.topReasonId] == [PulseReasonId.ORGANIZATION_UNCATEGORIZED]
 *   ile sinyallenir — ikisi de zaten hesaplanan alanlar, yeni bir akış gerekmez.
 * - [weeklyReportReady] — haftalık/Wrapped raporun hazır olduğu sinyali. Yeni bir kaynak yerine
 *   HomeScreen'in zaten sahip olduğu `tickerItems` (SmartTickerItem) listesinden
 *   `tickerItems.any { it.type == SmartTickerType.WEEKLY_REPORT }` ile türetilir — çağıran taraf
 *   bunu hesaplar, bu obje SmartTickerItem'a bağımlı olmaz (yalnızca Boolean alır).
 *
 * Öncelik sırası (S1 görev tanımı + Görev 3 eklentisi):
 * 1. CRITICAL_PERMISSION — kritik izin/veri eksik (pulse veri-yok/izin-yok sinyali)
 * 2. RISKY_MISSION — riskli görev (mission.urgent, yani birincil görev AT_RISK)
 * 3. FOLDER_REVIEW — klasör/sınıflandırma incelemesi gerekiyor
 * 4. REPORT_READY — haftalık rapor hazır
 * 5. DAILY_MISSIONS — mission != null (görev listesi aktif) ve yukarıdakilerin hiçbiri
 *    eşleşmediyse "Bugünün görevleri: X/Y tamamlandı" gösterilir (Görev 3, D26x). Normal bir
 *    günde (acil/riskli görev yok, rapor hazır değil) görev/yıldız ilerlemesi ana ekranda hiç
 *    görünmüyordu — bu adım BALANCE_SUMMARY'den ÖNCE devreye girer.
 * 6. BALANCE_SUMMARY — hiçbiri yoksa, pulse'ın normal denge özeti (veri varsa)
 *
 * Hiçbir girdi (mission/pulse) yoksa veya hiçbir öncelik eşleşmezse `null` döner — kart hiç
 * çizilmez (SmartDashboardPage bu durumda eski davranışa döner ya da hiçbir şey göstermez).
 */
object TodayCardSelector {

    fun select(
        mission: HomeMissionSummary?,
        pulse: HomePulseSummary?,
        weeklyReportReady: Boolean = false,
    ): TodayCardSpec? {
        // 1. Kritik izin/veri eksikliği — Dijital Yaşam kartının hiç hesaplanamadığı durum.
        if (pulse != null && pulse.freshness == DataFreshness.UNAVAILABLE) {
            return TodayCardSpec(
                kind = TodayCardKind.CRITICAL_PERMISSION,
                titleRes = R.string.today_card_title,
                subtitleRes = R.string.today_card_critical_permission_subtitle,
            )
        }

        // 2. Riskli görev — birincil görev AT_RISK (HomeMissionSummary.urgent zaten bu kuralı taşır).
        if (mission != null && mission.urgent && mission.primaryStatus != null) {
            return TodayCardSpec(
                kind = TodayCardKind.RISKY_MISSION,
                titleRes = R.string.today_card_title,
                subtitleRes = R.string.today_card_risky_mission_subtitle,
                missionTitle = mission.primaryTitle,
                missionCurrentText = mission.primaryCurrentText,
                missionRemainingText = mission.primaryRemainingText,
            )
        }

        // 3. Klasör/sınıflandırma incelemesi — Dijital Yaşam'ın en büyük etki sebebi kategorisiz
        // uygulama yoğunluğuysa (ORGANIZATION_UNCATEGORIZED), kullanıcıyı klasör düzenine yönlendir.
        if (pulse != null && pulse.topReasonId == PulseReasonId.ORGANIZATION_UNCATEGORIZED) {
            return TodayCardSpec(
                kind = TodayCardKind.FOLDER_REVIEW,
                titleRes = R.string.today_card_title,
                subtitleRes = R.string.today_card_folder_review_subtitle,
            )
        }

        // 4. Haftalık rapor hazır.
        if (weeklyReportReady) {
            return TodayCardSpec(
                kind = TodayCardKind.REPORT_READY,
                titleRes = R.string.today_card_title,
                subtitleRes = R.string.today_card_report_ready_subtitle,
            )
        }

        // 5. Günlük görevler — mission aktif (liste dolu) ve yukarıdaki hiçbir öncelik
        // eşleşmedi (acil görev yok, klasör incelemesi yok, rapor hazır değil). Normal günde
        // görev/yıldız ilerlemesini görünür kılar; BALANCE_SUMMARY'den önce değerlendirilir.
        if (mission != null && mission.totalCount > 0) {
            return TodayCardSpec(
                kind = TodayCardKind.DAILY_MISSIONS,
                titleRes = R.string.today_card_title,
                subtitleRes = R.string.today_card_daily_missions_subtitle,
                missionCompletedCount = mission.completedCount,
                missionTotalCount = mission.totalCount,
                missionTotalStars = mission.totalStars,
            )
        }

        // 6. Denge özeti — pulse verisi var ve yukarıdaki hiçbir öncelik eşleşmedi.
        if (pulse != null && pulse.freshness != DataFreshness.UNAVAILABLE && !pulse.shouldHideScore) {
            return TodayCardSpec(
                kind = TodayCardKind.BALANCE_SUMMARY,
                titleRes = R.string.today_card_title,
                subtitleRes = R.string.today_card_balance_summary_subtitle,
                pulseScore = pulse.score,
                pulseStatusBand = pulse.statusBand,
            )
        }

        return null
    }
}

/** [TodayCardSelector.select] tarafından üretilen kart içeriği — string metin yerine kaynak ID taşır. */
data class TodayCardSpec(
    val kind: TodayCardKind,
    val titleRes: Int,
    val subtitleRes: Int,
    // RISKY_MISSION için — HomeMissionCard'daki mevcut alanlarla aynı kaynaktan (mission.primaryXxx).
    val missionTitle: String? = null,
    val missionCurrentText: String? = null,
    val missionRemainingText: String? = null,
    // BALANCE_SUMMARY için — DigitalLifeCard'daki mevcut alanlarla aynı kaynaktan.
    val pulseScore: Int? = null,
    val pulseStatusBand: PulseStatusBand? = null,
    // DAILY_MISSIONS için — HomeMissionSummary'nin mevcut alanlarıyla aynı kaynaktan (Görev 3).
    val missionCompletedCount: Int? = null,
    val missionTotalCount: Int? = null,
    val missionTotalStars: Int? = null,
)

/** Tek "BUGÜN" kartının gösterebileceği bağlamsal içerik türleri — öncelik sırasıyla aynı sırada. */
enum class TodayCardKind {
    CRITICAL_PERMISSION,
    RISKY_MISSION,
    FOLDER_REVIEW,
    REPORT_READY,
    DAILY_MISSIONS,
    BALANCE_SUMMARY,
}
