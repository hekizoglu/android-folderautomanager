package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.usecase.missions.MissionTextSpec

/**
 * [com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason] -> kullanıcı metni + eylem
 * eşlemesi (Döngü D04 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır
 * 1500-1549). [MissionTextSpec] ile aynı "metin içermeyen model" deseni: [label] doğrudan
 * Türkçe/İngilizce string TAŞIMAZ, resource id + argüman taşır — çağıran taraf
 * `context.getString(label.resId, *label.args.toTypedArray())` ile çözer.
 *
 * @param label neden metni (resource id + sayısal argüman, ör. "Kategorisiz 8 uygulama").
 * @param action kullanıcıyı düzeltici ekrana götüren eylem — pozitif nedenlerde zorunlu
 *   değildir, [PulseAction.None] olabilir.
 * @param positive true: olumlu neden (skoru yükseltiyor), false: olumsuz, null: nötr/bilgi.
 */
data class PresentedPulseReason(
    val label: MissionTextSpec,
    val action: PulseAction,
    val positive: Boolean?,
)
