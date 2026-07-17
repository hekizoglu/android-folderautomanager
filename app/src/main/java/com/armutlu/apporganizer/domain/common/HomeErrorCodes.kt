package com.armutlu.apporganizer.domain.common

/**
 * Ana ekran veri kaynakları için sabit hata/uyarı kodları — Döngü H04
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 602-637).
 *
 * Serbest hata metni yerine bu sabitler [HomeDataResult.Stale.warningCode] ve
 * [HomeDataResult.Failed.errorCode] alanlarında kullanılır — UI katmanı ve
 * loglama tutarlı, karşılaştırılabilir kodlar üzerinden çalışır.
 */
object HomeErrorCodes {
    const val USAGE_PERMISSION_MISSING = "USAGE_PERMISSION_MISSING"
    const val NOTIFICATION_DATA_UNAVAILABLE = "NOTIFICATION_DATA_UNAVAILABLE"
    const val PULSE_COMPUTE_FAILED = "PULSE_COMPUTE_FAILED"
    const val MISSION_METRIC_STALE = "MISSION_METRIC_STALE"
    const val MISSION_SETTLEMENT_FAILED = "MISSION_SETTLEMENT_FAILED"
}
