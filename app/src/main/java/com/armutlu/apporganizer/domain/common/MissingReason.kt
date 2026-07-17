package com.armutlu.apporganizer.domain.common

/**
 * Bir veri kaynağının hiç sonuç üretememe nedeni — Döngü H04
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 602-637).
 *
 * [HomeDataResult.Missing] ile birlikte kullanılır; kaynak hata atmadı ama
 * anlamlı bir değer de üretemedi (izin yok, özellik kapalı, henüz veri yok).
 */
enum class MissingReason {
    /** Kullanım istatistikleri izni (UsageStatsManager) verilmemiş. */
    USAGE_PERMISSION_MISSING,

    /** Bildirim erişimi (NotificationListenerService) verilmemiş. */
    NOTIFICATION_ACCESS_MISSING,

    /** İlgili özellik kullanıcı tarafından ayarlardan kapatılmış. */
    FEATURE_DISABLED,

    /** Kaynak henüz hiç başarılı hesaplama/veri üretmedi (ilk çalıştırma). */
    NO_DATA_YET,
}
