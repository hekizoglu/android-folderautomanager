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

    // Döngü U03 — Sağlık raporu uyarı kodları (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
    // satır 2057-2112). Koordinatör kaynaklarının Stale/Failed olması dışında, sağlık raporunun
    // KENDİ toplama mantığının tespit ettiği durumlar için ayrı sabitler — serbest metin yerine
    // buradan raporlanır.
    /** MissionSettlementWorker'ın son planlanan sınırdan bu yana hiç çalışmadığı/gecikmiş olduğu durum. */
    const val MISSION_SETTLEMENT_STALE = "MISSION_SETTLEMENT_STALE"

    /** Görev kaynağı (MissionRuntimeRepository) koordinatörde Stale/Missing/Failed durumda. */
    const val MISSION_PROGRESS_DATA_STALE = "MISSION_PROGRESS_DATA_STALE"

    /** Dijital Nabız anlık görüntüsü (DigitalPulseRepository) koordinatörde Stale/Missing/Failed durumda. */
    const val PULSE_SNAPSHOT_STALE = "PULSE_SNAPSHOT_STALE"
    const val DIGITAL_LIFE_DATA_STALE = "DIGITAL_LIFE_DATA_STALE"
    const val DIGITAL_LIFE_LOW_CONFIDENCE = "DIGITAL_LIFE_LOW_CONFIDENCE"

    /** Ana ekran kartı ve Pulse Clock/Haftalık Rapor farklı skor kaynağından besleniyormuş gibi bir tutarsızlık tespit edildi. */
    const val PULSE_SOURCE_MISMATCH = "PULSE_SOURCE_MISMATCH"

    /** Şerit boş ama gösterilebilecek aksiyon alınabilir öğeler (aday) mevcuttu — sıralayıcı/bastırma katmanı hepsini elemiş olabilir. */
    const val TICKER_EMPTY_WITH_ACTIONABLE_ITEMS = "TICKER_EMPTY_WITH_ACTIONABLE_ITEMS"
}
