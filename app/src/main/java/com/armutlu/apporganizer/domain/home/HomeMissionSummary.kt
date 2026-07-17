package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.domain.usecase.missions.MissionSummaryUseCase

/**
 * Dongu M07 — Ana ekran "Görevler" kartinin gösterecegi özet
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 1118-1196).
 *
 * [primaryMissionId]/[primaryTitle]/[primaryCurrentText]/[primaryRemainingText]/[primaryStatus]
 * hep birlikte null ya da hep birlikte dolu olur — "birincil gorev" secilemediyse (liste bos)
 * tumu null kalir.
 */
data class HomeMissionSummary(
    val completedCount: Int,
    val totalCount: Int,
    val primaryMissionId: String?,
    val primaryTitle: String?,
    val primaryCurrentText: String?,
    val primaryRemainingText: String?,
    val primaryStatus: MissionStatus?,
    // AT_RISK durumundaki birincil gorevi vurgulamak icin — secim onceligi 1 (AT_RISK) ile
    // dogrudan orer: birincil gorev AT_RISK ise urgent=true, roadmap baska bir "urgent" tanimi
    // vermiyor, bu yorum secim sirasiyla tutarli oldugu icin tercih edildi.
    val urgent: Boolean,
    // Dongu T03 — birincil gorevin ham ilerleme orani (0f-1f). Sadece sablon secimi/karsilastirma
    // icin degil, sertte "tek eylemle tamamlanabilir" ureticisinin (>= 0.99) ham veriye
    // dayanmasi icin eklendi — onceden yalniz metin (remainingText) tahminine dayaniyordu.
    val primaryProgressFraction: Float? = null,
)

/**
 * Saf Kotlin (Android/Context bagimliligi yok) secici — [MissionSummaryUseCase.MissionOutcome]
 * listesinden (gunluk + haftalik birlestirilmis) [HomeMissionSummary] uretir. Unit test edilebilir.
 *
 * Gunluk + haftalik birlestirilir: roadmap ornekleri ("Görevler 1/3", "Ekran süresi: ...",
 * "1 uygulama kategorisi kaldı") gunluk gorev basliklarina karsilik gelir ama roadmap metni
 * "sadece gunluk" diye sinirlamiyor — kart N/M sayacinin TUM aktif gorevleri (gunluk+haftalik)
 * yansitmasi kullanicinin gunluk gorev ekranindaki toplam ilerlemesiyle tutarli olur.
 */
object HomeMissionSummarySelector {

    fun build(missions: List<MissionSummaryUseCase.MissionOutcome>): HomeMissionSummary {
        val totalCount = missions.size
        val completedCount = missions.count { it.status == MissionStatus.COMPLETED }

        val primary = selectPrimary(missions)

        return HomeMissionSummary(
            completedCount = completedCount,
            totalCount = totalCount,
            primaryMissionId = primary?.id,
            primaryTitle = primary?.title,
            primaryCurrentText = primary?.currentText,
            primaryRemainingText = primary?.remainingText,
            primaryStatus = primary?.status,
            urgent = primary?.status == MissionStatus.AT_RISK,
            primaryProgressFraction = primary?.progressFraction,
        )
    }

    /**
     * Birincil gorev secim onceligi (roadmap M07, birebir):
     * 1. AT_RISK durumundaki gorev
     * 2. Kalanlar (tamamlanmamis) arasinda en yakin deadline'a sahip olan
     * 3. Tek bir eylemle tamamlanabilecek gorev (remainingText dolu + progressFraction'a gore
     *    hedefe en yakin — burada "tek eylem kaldi" ACTION_COUNT tipi gorevlerde remainingValue
     *    ile ifade edilir; MissionOutcome'da dogrudan "kalan eylem sayisi" alani yok, bu yuzden
     *    progressFraction >= yuksek bir esik (>= 0.99, yani sadece 1 adim kaldi) ile yorumlanir)
     * 4. En yuksek progressFraction'a sahip gorev
     * 5. Ilk bekleyen (NOT_STARTED/IN_PROGRESS/SAFE... - tamamlanmamis) gorev
     */
    private fun selectPrimary(
        missions: List<MissionSummaryUseCase.MissionOutcome>,
    ): MissionSummaryUseCase.MissionOutcome? {
        if (missions.isEmpty()) return null

        // 1. AT_RISK
        missions.firstOrNull { it.status == MissionStatus.AT_RISK }?.let { return it }

        val remaining = missions.filterNot { it.status == MissionStatus.COMPLETED }
        if (remaining.isEmpty()) {
            // Hepsi tamamlanmis — birincil gorev anlamsiz, cagiran taraf completedCount==totalCount
            // durumunu ayrica ele alir (kutlama metni gosterir). Yine de robust olmak icin ilk
            // gorevi don (bos degilse).
            return missions.firstOrNull()
        }

        // 2. Soonest deadline (deadlineText dolu olanlar arasinda — metin zaten "kalan sure"
        // formatinda ama siralama icin dogrudan dakika bilgisine erisimimiz yok; bu yuzden
        // deadline'i olan gorevleri (donem suruyor, SAFE/AT_RISK/IN_PROGRESS/NOT_STARTED)
        // once ele aliriz — AT_RISK zaten 1. adimda tuketildi, kalan tipik olarak IN_PROGRESS/
        // NOT_STARTED/SAFE olur. MissionOutcome'da ham deadline milis'i yok; roadmap sadece
        // "soonest deadline" diyor, elimizdeki tek sinyal deadlineText'in varligi + gorev tipi
        // (DAILY gorevler haftalik gorevlerden her zaman daha erken biter) - bu yuzden deadline'i
        // olan gorevler arasinda ONCE gunluk (daha yakin bitis), sonra liste sirasi kullanilir.
        val withDeadline = remaining.filter { it.deadlineText != null }
        if (withDeadline.isNotEmpty()) {
            return withDeadline.first()
        }

        // 3. Tek eylemle tamamlanabilir: progressFraction hedefe cok yakin (>= 0.99) veya
        // remainingText "1" iceren tek adimlik eylem gorevleri (ACTION_COUNT, hedef=1 gibi).
        val singleActionAway = remaining.firstOrNull { outcome ->
            val fraction = outcome.progressFraction
            fraction != null && fraction >= 0.99f
        }
        if (singleActionAway != null) return singleActionAway

        // 4. En yuksek progressFraction
        val highestProgress = remaining
            .filter { it.progressFraction != null }
            .maxByOrNull { it.progressFraction!! }
        if (highestProgress != null) return highestProgress

        // 5. Ilk bekleyen gorev
        return remaining.first()
    }
}
