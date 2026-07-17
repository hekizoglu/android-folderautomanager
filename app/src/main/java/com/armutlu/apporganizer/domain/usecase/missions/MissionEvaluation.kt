package com.armutlu.apporganizer.domain.usecase.missions

/**
 * MissionEngine.evaluate() sonucu — yapilandirilmis gorev degerlendirmesi
 * (Dongu M00 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md).
 *
 * @param status gorevin guncel yasam dongusu durumu.
 * @param currentValue gorevin izledigi anlik deger (dakika, adet vb.) — veri yoksa null.
 * @param targetValue gorevin hedef/esik degeri — gorev tipine gore sabit.
 * @param remainingValue hedefe kalan mesafe (ust sinir gorevlerinde "kalan kota",
 * eylem sayisi gorevlerinde "kalan eylem") — hesaplanamiyorsa null.
 * @param failureReasonCode FAILED durumunda makine-okunur sebep kodu (UI/analiz icin), yoksa null.
 */
data class MissionEvaluation(
    val status: MissionStatus,
    val currentValue: Long?,
    val targetValue: Long?,
    val remainingValue: Long?,
    val failureReasonCode: String? = null,
)
