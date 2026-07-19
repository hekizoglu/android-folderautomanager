package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId

/**
 * Döngü T03 — Görev ve Dijital Yaşam kaynaklı ticker üreticileri
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1727-1789).
 *
 * Amaç: şeridin görev kartı ([HomeMissionSummary]) ve Dijital Yaşam kartı
 * ([DigitalPulseSnapshot]) ile aynı bilgiyi TEKRAR etmemesi — yalnız risk/son
 * süre/yeni başarı (görev) veya anlamlı değişim/çözülebilir sorun (skor) şeride
 * girer. Roadmap 1.4 tekrar önleme tablosu (satır 122-133): "Günlük görev
 * ilerlemesi" ve "Dijital Yaşam skoru" birincil bileşeni kart olduğu için ham
 * `1/3 tamamlandı` veya ham `Skor 72` ASLA burada üretilmez.
 *
 * Kaynak okuma kararı: [RealSmartTickerSource] [SmartTickerEngine] arayüzünü
 * uygular ve [HomeIntelligenceCoordinator] onun ALTINDA, SmartTickerEngine'i
 * enjekte ederek çalışır (bkz. HomeIntelligenceCoordinator constructor'ı). Bu
 * yüzden bu üreticiler koordinatör state'ini DEĞİL, [MissionRuntimeRepository]
 * ve [DigitalPulseRepository] state'lerini DOĞRUDAN okur — koordinatörü
 * enjekte etmek döngüsel bağımlılık yaratırdı (RealSmartTickerSource ->
 * HomeIntelligenceCoordinator -> SmartTickerEngine -> RealSmartTickerSource).
 *
 * Saf Kotlin — Android bağımlılığı yok, unit test edilebilir. [RealSmartTickerSource]
 * bu fonksiyonların çıktısını [com.armutlu.apporganizer.utils.TickerComposer.compose]
 * çıktısıyla birleştirip [TickerRanker.rank]'e verir.
 */
object MissionPulseTickerFactory {

    /** Haftalık Dijital Yaşam skor değişiminin şeride girmesi için gereken minimum mutlak fark (roadmap satır 1757). */
    const val PULSE_SCORE_DELTA_THRESHOLD = 5

    /** Dongu G4 — seri kilometre taşları (roadmap G4: "3/7/30 gün → MISSION_ACHIEVEMENT"). */
    val STREAK_MILESTONES = listOf(3, 7, 30)

    private const val MS_PER_DAY = 24L * 3600 * 1000
    /** Görev/başarı öğeleri en fazla bir gün geçerli — ertesi gün yeni dönemin verisiyle yeniden üretilir. */
    private const val MISSION_ITEM_EXPIRY_MS = MS_PER_DAY

    /** Pulse öğeleri de aynı gün içinde geçerli — bir sonraki gün skor yeniden değerlendirilir. */
    private const val PULSE_ITEM_EXPIRY_MS = MS_PER_DAY

    /** Seri kilometre taşı öğesi de aynı gün içinde geçerli. */
    private const val STREAK_ITEM_EXPIRY_MS = MS_PER_DAY

    /**
     * Görev kaynaklı ticker adayları. Roadmap satır 1733-1739 üretim şartları:
     * - AT_RISK oldu (yüksek öncelik, aciliyet).
     * - Tek eylemle tamamlanabilir (progressFraction >= 0.99, henüz COMPLETED değil).
     * - Yeni COMPLETED başarı (dönem içinde bir kez — suggestionKey ile dedupe [TickerRanker]'a bırakılır).
     *
     * "Dönem bitimine 2 saatten az kaldı" ve "günlük bütün görevler tamamlandı" roadmap'te
     * ayrıca listelenir ama [HomeMissionSummary] ham deadline milis'i taşımaz (yalnız
     * [HomeMissionSummary.primaryRemainingText] gibi zaten çözülmüş metinler) — bu yüzden
     * "son süre" sinyali AT_RISK durumuyla birleştirilerek karşılanır (AT_RISK zaten M07'de
     * "hedefe çok yaklaşılmış, başarısızlık riski yüksek" anlamına geliyor, roadmap satır 25).
     * "Günlük bütün görevler tamamlandı" [completedCount] == [totalCount] > 0 ile üretilir.
     *
     * Normal `1/3 tamamlandı` (IN_PROGRESS/SAFE/NOT_STARTED, risk yok, tek eylem uzak) ASLA
     * üretilmez — roadmap satır 1751.
     */
    fun missionCandidates(
        summary: HomeMissionSummary?,
        nowMillis: Long,
    ): List<SmartTickerItem> {
        if (summary == null) return emptyList()
        val items = mutableListOf<SmartTickerItem>()

        // Tüm görevler tamamlandı — yeni başarı/kutlama öğesi (roadmap satır 1739, 1748 örneği).
        if (summary.totalCount > 0 && summary.completedCount == summary.totalCount) {
            items.add(
                SmartTickerItem(
                    id = "mission_all_completed_${nowMillis / MISSION_ITEM_EXPIRY_MS}",
                    type = SmartTickerType.MISSION_ACHIEVEMENT,
                    title = "⭐ Bugünkü ${summary.totalCount} görevi tamamladın",
                    subtitle = "Sonuçları gör",
                    icon = "⭐",
                    priority = 70,
                    createdAt = nowMillis,
                    expiresAt = nowMillis + MISSION_ITEM_EXPIRY_MS,
                    action = TickerAction.OpenMissions,
                    suggestionKey = "mission_all_completed",
                )
            )
            return items
        }

        val primaryId = summary.primaryMissionId
        if (primaryId != null) {
            when (summary.primaryStatus) {
                MissionStatus.AT_RISK -> {
                    items.add(
                        SmartTickerItem(
                            id = "mission_at_risk_$primaryId",
                            type = SmartTickerType.MISSION_PROGRESS,
                            title = missionAtRiskTitle(summary),
                            subtitle = "Detay",
                            icon = "⏳",
                            priority = 85,
                            createdAt = nowMillis,
                            expiresAt = nowMillis + MISSION_ITEM_EXPIRY_MS,
                            action = TickerAction.OpenMissions,
                            suggestionKey = "mission_at_risk_$primaryId",
                        )
                    )
                }
                MissionStatus.COMPLETED -> {
                    items.add(
                        SmartTickerItem(
                            id = "mission_completed_$primaryId",
                            type = SmartTickerType.MISSION_ACHIEVEMENT,
                            title = "⭐ Görev tamamlandı: ${summary.primaryTitle}",
                            icon = "⭐",
                            priority = 65,
                            createdAt = nowMillis,
                            expiresAt = nowMillis + MISSION_ITEM_EXPIRY_MS,
                            action = TickerAction.OpenMissions,
                            suggestionKey = "mission_completed_$primaryId",
                        )
                    )
                }
                else -> {
                    // Tek eylemle tamamlanabilir (roadmap satır 1737) — henüz AT_RISK/COMPLETED
                    // değil ama hedefe son adım kaldı (bkz. HomeMissionSummarySelector oncelik 3,
                    // aynı >= 0.99 eşiği burada da kullanılır — ham fraction artık
                    // HomeMissionSummary.primaryProgressFraction alanından okunur).
                    val fraction = summary.primaryProgressFraction
                    if (fraction != null && fraction >= 0.99f) {
                        items.add(
                            SmartTickerItem(
                                id = "mission_single_action_$primaryId",
                                type = SmartTickerType.MISSION_PROGRESS,
                                title = "🎯 ${summary.primaryTitle}: son adım kaldı",
                                subtitle = summary.primaryRemainingText ?: "Detay",
                                icon = "🎯",
                                priority = 55,
                                createdAt = nowMillis,
                                expiresAt = nowMillis + MISSION_ITEM_EXPIRY_MS,
                                action = TickerAction.OpenMissions,
                                suggestionKey = "mission_single_action_$primaryId",
                            )
                        )
                    }
                }
            }
        }

        return items
    }

    /**
     * Dongu G4 — seri kilometre taşı öğesi: [currentStreak] tam olarak [STREAK_MILESTONES]
     * içindeki bir değere ULAŞTIĞI gün (>= değil == — aksi halde her gün aynı milestone tekrar
     * üretilir, `suggestionKey` dedupe TickerRanker'da yalnız "aynı ID" için çalışır, gün
     * değişince suggestionKey aynı kaldığı sürece yeniden gösterilmez ama expiresAt 1 gün olduğu
     * için ertesi gün yine == kontrolüyle üretilmeyecektir çünkü seri o gün artık farklı bir
     * sayıdadır). Dönemde bir kez: `suggestionKey = "streak_milestone_<n>"` — TickerRanker aynı
     * suggestionKey'i tekrar göstermez (roadmap G4 "dönemde bir kez").
     */
    fun streakCandidates(
        currentStreak: Int,
        nowMillis: Long,
    ): List<SmartTickerItem> {
        val milestone = STREAK_MILESTONES.firstOrNull { it == currentStreak } ?: return emptyList()
        return listOf(
            SmartTickerItem(
                id = "streak_milestone_${milestone}_${nowMillis / STREAK_ITEM_EXPIRY_MS}",
                type = SmartTickerType.MISSION_ACHIEVEMENT,
                title = "🔥 $milestone günlük seri!",
                subtitle = "Görevleri gör",
                icon = "🔥",
                priority = 60,
                createdAt = nowMillis,
                expiresAt = nowMillis + STREAK_ITEM_EXPIRY_MS,
                action = TickerAction.OpenMissions,
                suggestionKey = "streak_milestone_$milestone",
            )
        )
    }

    /** Sabah özeti öğesi de aynı gün içinde geçerli (bir sonraki gün yeni dünle yeniden üretilir). */
    private const val MORNING_SUMMARY_ITEM_EXPIRY_MS = MS_PER_DAY

    /**
     * Dongu G5 — sabah özeti: "Dün 2/3 tamamladın · Serin N günde" (roadmap G5, satır 48).
     * Push bildirimi YOK — yalnız şeride (ticker) düşen, günde BİR KEZ üretilen bir öğe.
     *
     * [yesterdayCompletedCount]/[yesterdayTotalCount] dünün GÜNLÜK görev instance'larının
     * settlement SONUCU (bkz. MissionsRepository.getYesterdaySettlementCounts) — henüz hiç
     * settled instance yoksa (gün henüz kapanmadı veya hiç görev atanmadı) totalCount 0 gelir
     * ve öğe üretilmez (roadmap "veri yoksa üretme" ilkesiyle tutarlı, M08 ceza yok).
     *
     * [currentStreak] MissionStreakPrefs'ten güncel seri — 0 ise metne serİ eklenmez (roadmap
     * "seri varsa göster", 0 gün gibi olumsuz bir sayı asla üretilmez, G4 ile aynı ilke).
     *
     * suggestionKey = "morning_summary_<epochDay>" — [todayEpochDay] bazlı, bu yüzden aynı gün
     * içinde tekrar tekrar üretilse bile TickerRanker/SuggestionCoordinator dedupe eder (günde
     * bir). Tip MISSION_ACHIEVEMENT (roadmap notu: "WEEKLY_REPORT veya MISSION_ACHIEVEMENT" —
     * içerik görev sonucu olduğu için MISSION_ACHIEVEMENT seçildi, KEY_SMART_TICKER_MISSIONS
     * toggle'ıyla aynı görünürlük grubuna girer).
     */
    fun morningSummaryCandidate(
        yesterdayCompletedCount: Int,
        yesterdayTotalCount: Int,
        currentStreak: Int,
        todayEpochDay: Long,
        nowMillis: Long,
    ): List<SmartTickerItem> {
        if (yesterdayTotalCount <= 0) return emptyList()
        val title = if (currentStreak >= 1) {
            "☀️ Dün $yesterdayCompletedCount/$yesterdayTotalCount tamamladın · Serin $currentStreak günde"
        } else {
            "☀️ Dün $yesterdayCompletedCount/$yesterdayTotalCount tamamladın"
        }
        return listOf(
            SmartTickerItem(
                id = "morning_summary_$todayEpochDay",
                type = SmartTickerType.MISSION_ACHIEVEMENT,
                title = title,
                subtitle = "Görevleri gör",
                icon = "☀️",
                priority = 40,
                createdAt = nowMillis,
                expiresAt = nowMillis + MORNING_SUMMARY_ITEM_EXPIRY_MS,
                action = TickerAction.OpenMissions,
                suggestionKey = "morning_summary_$todayEpochDay",
            )
        )
    }

    /**
     * Roadmap örneği: "⏳ Ekran süresi hedefinde yalnız 12 dk kaldı" — [HomeMissionSummary]
     * ham dakika taşımadığı için mevcut çözülmüş metinlerden ([primaryCurrentText]/
     * [primaryRemainingText]) en açıklayıcı olanı kullanılır.
     */
    private fun missionAtRiskTitle(summary: HomeMissionSummary): String {
        val detail = summary.primaryRemainingText ?: summary.primaryCurrentText
        return if (detail != null) {
            "⏳ Limite yaklaştın: ${summary.primaryTitle} — $detail"
        } else {
            "⏳ Limite yaklaştın: ${summary.primaryTitle}"
        }
    }

    /**
     * Dijital Yaşam skor kaynaklı ticker adayları. Roadmap satır 1755-1760 üretim şartları:
     * - |scoreDelta| >= [PULSE_SCORE_DELTA_THRESHOLD] (anlamlı değişim).
     * - Top reason çözülebilir bir sorun (negatif + [PulseAction] != None).
     *
     * "Veri güveni LOW'dan MEDIUM/HIGH'a geçti" ve "haftalık rapor hazır" [DigitalPulseSnapshot]
     * içinde önceki confidence'ı veya rapor hazırlık bayrağını taşımadığı için (roadmap bu iki
     * şartı ayrı bir veri kaynağına bağlamıyor, mevcut modelde sinyal yok) bu iki üretici
     * KAPSAM DIŞI bırakıldı — eklenirse DigitalPulseSnapshot'a yeni alan gerekir, T03 kapsamı
     * "üretici + test" olduğu için model genişletmesi burada yapılmadı (rapor notu).
     *
     * Ham `Skor 72` mesajı ASLA üretilmez — roadmap satır 1772.
     */
    fun pulseCandidates(
        snapshot: DigitalPulseSnapshot?,
        nowMillis: Long,
    ): List<SmartTickerItem> {
        if (snapshot == null) return emptyList()
        val items = mutableListOf<SmartTickerItem>()

        val delta = snapshot.scoreDelta
        if (delta != null && kotlin.math.abs(delta) >= PULSE_SCORE_DELTA_THRESHOLD) {
            val sign = if (delta > 0) "+" else ""
            val direction = if (delta > 0) "yükseldi" else "düştü"
            items.add(
                SmartTickerItem(
                    id = "pulse_score_delta_${nowMillis / PULSE_ITEM_EXPIRY_MS}",
                    type = SmartTickerType.PULSE_CHANGE,
                    title = "📈 Dijital Yaşam skorun $sign$delta puan $direction",
                    subtitle = "Nedenini gör",
                    icon = "📈",
                    priority = 50,
                    createdAt = nowMillis,
                    expiresAt = nowMillis + PULSE_ITEM_EXPIRY_MS,
                    action = TickerAction.OpenWeeklyReport,
                    suggestionKey = "pulse_score_delta",
                )
            )
        }

        // Çözülebilir sorun — en yüksek |delta| ile negatif etkiyen ve eyleme bağlanan neden.
        val topNegativeReason = snapshot.score.reasons
            .filter { it.delta < 0 }
            .minByOrNull { it.delta } // en negatif = en büyük olumsuz katkı
        if (topNegativeReason != null) {
            val presented = PulseReasonPresenter.present(topNegativeReason)
            if (presented.action != PulseAction.None) {
                items.add(
                    SmartTickerItem(
                        id = "pulse_resolvable_issue_${topNegativeReason.id.name}",
                        type = SmartTickerType.PULSE_CHANGE,
                        title = pulseIssueTitle(topNegativeReason.id),
                        subtitle = "Raporu aç",
                        icon = "🔔",
                        priority = 45,
                        createdAt = nowMillis,
                        expiresAt = nowMillis + PULSE_ITEM_EXPIRY_MS,
                        action = pulseActionToTickerAction(presented.action),
                        suggestionKey = "pulse_resolvable_${topNegativeReason.id.name}",
                    )
                )
            }
        }

        return items
    }

    /**
     * [PulseReasonId] -> kısa, eyleme çağıran şerit başlığı. Ham skor/alt-skor sayısı YOK —
     * [PulseReasonPresenter]'ın kart etiketleri daha uzun/açıklayıcı, şerit kısa ve dokunma
     * odaklı olmalı (roadmap örneği: "🔔 Bildirim yoğunluğu skorunu etkiliyor · Raporu aç").
     */
    private fun pulseIssueTitle(id: PulseReasonId): String = when (id) {
        PulseReasonId.ATTENTION_NOISY -> "🔔 Bildirim yoğunluğu skorunu etkiliyor"
        PulseReasonId.ATTENTION_NIGHT -> "🌙 Gece bildirimleri skorunu etkiliyor"
        PulseReasonId.ORGANIZATION_UNCATEGORIZED -> "🤔 Kategorisiz uygulamalar skorunu etkiliyor"
        PulseReasonId.CLEANUP_UNUSED -> "🧹 Kullanılmayan uygulamalar skorunu etkiliyor"
        PulseReasonId.BALANCE_SHIFT -> "⚖️ Kullanım dağılımın değişti"
        PulseReasonId.CONSISTENCY_VOLATILE -> "📉 Kullanım düzenin değişken"
        PulseReasonId.TASK_MISSIONS -> "🎯 Görev sonuçların skorunu etkiliyor"
        else -> "📉 Dijital Yaşam skorunu etkileyen bir durum var"
    }

    /** [PulseAction] -> [TickerAction] köprüsü — iki eylem modeli de aynı hedeflere işaret eder. */
    private fun pulseActionToTickerAction(action: PulseAction): TickerAction = when (action) {
        PulseAction.OpenClassificationReview -> TickerAction.OpenClassificationReview
        PulseAction.OpenNotificationReport -> TickerAction.OpenNotificationReport
        PulseAction.OpenAppList -> TickerAction.OpenAppList
        PulseAction.OpenWeeklyReport -> TickerAction.OpenWeeklyReport
        PulseAction.OpenMissions -> TickerAction.OpenMissions
        PulseAction.None -> TickerAction.None
    }
}
