package com.armutlu.apporganizer.domain.usecase.pulse

/**
 * Döngü D00 — Dijital Nabız için TEK kaynaktan üretilen anlık görüntü.
 * [DigitalPulseRepository] tarafından [DigitalPulseEngine.compute] çağrılarak üretilir;
 * ana ekran kartı, Pulse Clock ve Haftalık Rapor AYNI [score] değerini bu tipten okur
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md D00, satır 1253-1309).
 *
 * @param score DigitalPulseEngine.compute() çıktısı — tek doğruluk kaynağı, burada
 *   yeniden hesaplanmaz veya değiştirilmez.
 * @param computedAt bu snapshot'ın hesaplandığı epoch-milli zaman damgası.
 * @param validUntil cache süresinin (15 dk) dolacağı epoch-milli zaman damgası —
 *   [DigitalPulseRepository] bu sınırı referans alır, tüketiciler doğrudan kullanmaz.
 * @param previousScore Döngü D01 — bir önceki TAM ISO haftasının kapanış skoru (0-100),
 *   ilk hafta veya henüz kapanmış hafta yoksa null ("Veri birikiyor" — UI D02'de ele alınır).
 *   [com.armutlu.apporganizer.utils.PulseHistoryPrefs] tarafından üretilir, gerçek takvim
 *   haftasına dayanır (eski günlük/7-gün rotasyonu YERİNE).
 * @param scoreDelta [score.total] - [previousScore], [previousScore] null ise null.
 */
data class DigitalPulseSnapshot(
    val score: DigitalPulseScore,
    val computedAt: Long,
    val validUntil: Long,
    val previousScore: Int? = null,
    val scoreDelta: Int? = null,
)
