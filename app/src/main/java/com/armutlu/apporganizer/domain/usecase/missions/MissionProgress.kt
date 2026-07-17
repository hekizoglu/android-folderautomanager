package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Gorev ilerleme goruntusu (Dongu M03 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 852-941).
 *
 * [MissionProgressCalculator] tarafindan [MissionEvaluation] + [MissionProgressKind] ciftinden
 * uretilir. UI'ya dogrudan Turkce metin tasimaz — text alanlari [MissionTextSpec] uzerinden
 * resource id + argumanlar tasir.
 *
 * @param currentValue gorevin izledigi anlik deger (dakika, adet vb.) — veri yoksa null.
 * @param targetValue gorevin hedef/esik degeri — veri yoksa null.
 * @param remainingValue hedefe kalan mesafe — negatif olamaz (asilmissa null, [exceededValue] kullanilir).
 * @param progressFraction 0f..1f araliginda sinirlandirilmis ilerleme orani — hesaplanamiyorsa null.
 * @param exceededValue ust sinir asildiginda asilan miktar (>= 0) — asilmadiysa null.
 * @param currentTextRes "Su an: ..." metni icin spec — veri yoksa null.
 * @param remainingTextRes "Kalan: ..." metni icin spec — veri yoksa veya negatifse null.
 * @param progressTextRes "Limitin %50'si kullanildi" gibi ozet metin spec'i — veri yoksa null.
 */
data class MissionProgress(
    val currentValue: Long?,
    val targetValue: Long?,
    val remainingValue: Long?,
    val progressFraction: Float?,
    val exceededValue: Long?,
    val currentTextRes: MissionTextSpec?,
    val remainingTextRes: MissionTextSpec?,
    val progressTextRes: MissionTextSpec?,
)
