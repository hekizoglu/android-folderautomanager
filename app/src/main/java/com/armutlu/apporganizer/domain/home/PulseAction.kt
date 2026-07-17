package com.armutlu.apporganizer.domain.home

/**
 * Dijital Yaşam kartındaki bir skor nedeninin (PulseScoreReason) kullanıcıyı
 * götürebileceği eylem (Döngü D04 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1500-1549).
 *
 * [MissionAction] ile aynı desen: bu sealed interface yalnızca "hangi eylem" bilgisini
 * taşır, route/Intent çözümü [PulseActionRouter] içinde TEK yerde yapılır.
 */
sealed interface PulseAction {
    /** Sınıflandırma inceleme ekranını aç (kategorisiz/belirsiz uygulamalar). */
    data object OpenClassificationReview : PulseAction

    /** Bildirim analiz raporunu aç. */
    data object OpenNotificationReport : PulseAction

    /** Uygulama listesini aç (ör. uzun süredir kullanılmayanlar). */
    data object OpenAppList : PulseAction

    /** Haftalık/Wrapped raporu aç. */
    data object OpenWeeklyReport : PulseAction

    /** Görevler ekranını aç. */
    data object OpenMissions : PulseAction

    /** Eylem gerektirmeyen neden (ör. pozitif/nötr durum). */
    data object None : PulseAction
}
