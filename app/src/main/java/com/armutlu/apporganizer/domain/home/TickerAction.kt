package com.armutlu.apporganizer.domain.home

/**
 * [SmartTickerItem.action] — dokunulduğunda ticker öğesinin götüreceği hedef (Döngü T01).
 * [PulseAction]/[com.armutlu.apporganizer.domain.usecase.missions.MissionAction] ile AYNI
 * desen: bu sealed interface yalnızca "hangi eylem" bilgisini taşır, route/Intent çözümü
 * [TickerActionRouter] içinde TEK yerde yapılır — domain katmanı navigation modülüne bağımlı
 * kalmaz.
 */
sealed interface TickerAction {
    /** Belirli bir klasörü aç (kategori kartı içgörüleri). */
    data class OpenFolder(val categoryId: String) : TickerAction

    /** Belirli bir uygulamayı başlat (unutulan uygulama, uygulama bazlı içgörü). */
    data class OpenApp(val packageName: String) : TickerAction

    /** Uygulama listesini aç (ör. genel uygulama gezinme). */
    data object OpenAppList : TickerAction

    /** Belirsiz/düşük güvenli sınıflandırma inceleme ekranını aç. */
    data object OpenClassificationReview : TickerAction

    /** Bildirim analiz raporunu aç. */
    data object OpenNotificationReport : TickerAction

    /** Dashboard'u aç (genel içgörüler). */
    data object OpenDashboard : TickerAction

    /** Haftalık/Wrapped raporu aç. */
    data object OpenWeeklyReport : TickerAction

    /** Ayarlar ekranını (veya bir alt bölümünü) aç. */
    data class OpenSettings(val section: SettingsSection = SettingsSection.ROOT) : TickerAction

    /** Arama istatistikleri ekranını aç. */
    data object OpenSearchStats : TickerAction

    /** Raporlar merkezini aç. */
    data object OpenReportsCenter : TickerAction

    /** Kullanım/ekran süresi raporunu aç. */
    data object OpenUsageReport : TickerAction

    /** Görevler ekranını aç (Döngü T03 — görev kaynaklı ticker öğeleri). */
    data object OpenMissions : TickerAction

    /** Eylem gerektirmeyen/hedefsiz öğe (ör. salt bilgilendirme). */
    data object None : TickerAction
}

/** [TickerAction.OpenSettings] için alt bölüm seçimi — eski TickerSpec.routeKey karşılıkları. */
enum class SettingsSection {
    ROOT,
    LAUNCHER,
    NOTIFICATIONS,
    APPEARANCE,
    STATS,
    SEARCH,
}
