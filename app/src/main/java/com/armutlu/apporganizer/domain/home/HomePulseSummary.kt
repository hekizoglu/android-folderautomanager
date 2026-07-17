package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason

/**
 * Döngü D02 — "Dijital Yaşam" kartının UI'a taşıdığı özet (roadmap satır 1360-1450).
 * Saf Kotlin, Android bağımlılığı yok — [DigitalPulseSnapshot]'tan test edilebilir bir
 * [toHomePulseSummary] fonksiyonuyla türetilir. Gerçek diller (TR/EN) UI katmanında
 * [statusLabel]/[deltaSign]/[topReasonId] üzerinden `stringResource` ile çözülür; bu tip
 * doğrudan kullanıcıya gösterilecek serbest metin TAŞIMAZ (yalnızca sayısal/enum veri).
 *
 * @param score toplam skor (0..100) — [confidence] LOW ise UI sayıyı GÖSTERMEZ ("Veri birikiyor"),
 *   ama alan yine de taşınır (log/debug amaçlı, null değildir çünkü snapshot her zaman bir skor üretir).
 * @param statusBand skor bandına göre nötr durum etiketi — bkz. [PulseStatusBand].
 * @param delta [DigitalPulseSnapshot.scoreDelta] — önceki tam ISO haftasına göre fark, ilk hafta null.
 * @param topReasonId en büyük etkiye sahip sebep — [DigitalPulseSnapshot.score.reasons] içinden
 *   |delta| değeri en büyük olan seçilir; sebep yoksa null.
 * @param topReason [topReasonId] ile aynı seçimin tam [PulseScoreReason]'ı (value/delta dahil) —
 *   Döngü D04: [PulseReasonPresenter] sayı içeren etiketler (ör. "Kategorisiz 8 uygulama")
 *   üretebilsin diye eklendi. [topReasonId] geriye dönük uyumluluk için ayrıca tutulur.
 * @param confidence [DigitalPulseSnapshot.score.confidence] — LOW iken skor yerine "Veri birikiyor".
 * @param freshness [DataFreshness] — STALE'de "Son güncelleme X dk önce", UNAVAILABLE'da CTA.
 * @param staleMinutes freshness=STALE olduğunda geçen dakika sayısı (UI metni için); aksi halde null.
 */
data class HomePulseSummary(
    val score: Int?,
    val statusBand: PulseStatusBand?,
    val delta: Int?,
    val topReasonId: PulseReasonId?,
    val topReason: PulseScoreReason? = null,
    val confidence: DataConfidence,
    val freshness: DataFreshness,
    val staleMinutes: Long? = null,
) {
    /** Kart tıklanabilir mi — UNAVAILABLE dışında her zaman true (Wrapped rapor açılır). */
    val isActionable: Boolean get() = freshness != DataFreshness.UNAVAILABLE

    /** LOW confidence'ta sayı yerine "Veri birikiyor" gösterilir — skor kesinmiş gibi sunulmaz. */
    val shouldHideScore: Boolean get() = confidence == DataConfidence.LOW || score == null
}

/** Skor bandı → nötr durum etiketi (roadmap satır 1398-1406). Yargılayıcı dil YOK. */
enum class PulseStatusBand {
    EXCELLENT,   // 80-100 "Çok iyi"
    GOOD,        // 65-79  "İyi"
    BALANCED,    // 50-64  "Dengeli"
    NEEDS_FOCUS, // 35-49  "Dikkat gerekiyor"
    IMPROVING,   // 0-34   "İyileştirme alanı var"
    ;

    companion object {
        fun forScore(score: Int): PulseStatusBand = when {
            score >= 80 -> EXCELLENT
            score >= 65 -> GOOD
            score >= 50 -> BALANCED
            score >= 35 -> NEEDS_FOCUS
            else -> IMPROVING
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * [DigitalPulseSnapshot] → [HomePulseSummary]. Saf fonksiyon, test edilebilir.
 *
 * @param freshness çağıran taraf [com.armutlu.apporganizer.domain.common.DataFreshnessResolver]
 *   ile snapshot.computedAt'tan hesaplar (bu fonksiyon kendi Clock'unu tutmaz — H03 pattern'i).
 * @param nowMillis STALE dakika hesabı için "şimdi" — test edilebilirlik için enjekte edilir.
 */
fun toHomePulseSummary(
    snapshot: DigitalPulseSnapshot?,
    freshness: DataFreshness,
    nowMillis: Long,
): HomePulseSummary {
    if (snapshot == null) {
        return HomePulseSummary(
            score = null,
            statusBand = null,
            delta = null,
            topReasonId = null,
            confidence = DataConfidence.LOW,
            freshness = DataFreshness.UNAVAILABLE,
            staleMinutes = null,
        )
    }

    val total = snapshot.score.total
    val topReason = snapshot.score.reasons
        .maxByOrNull { kotlin.math.abs(it.delta) }
        ?.takeIf { it.delta != 0 }

    val staleMinutes = if (freshness == DataFreshness.STALE) {
        ((nowMillis - snapshot.computedAt).coerceAtLeast(0L) / MILLIS_PER_MINUTE)
    } else {
        null
    }

    return HomePulseSummary(
        score = total,
        statusBand = PulseStatusBand.forScore(total),
        delta = snapshot.scoreDelta,
        topReasonId = topReason?.id,
        topReason = topReason,
        confidence = snapshot.score.confidence,
        freshness = freshness,
        staleMinutes = staleMinutes,
    )
}
