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
 */
data class DigitalPulseSnapshot(
    val score: DigitalPulseScore,
    val computedAt: Long,
    val validUntil: Long,
)
