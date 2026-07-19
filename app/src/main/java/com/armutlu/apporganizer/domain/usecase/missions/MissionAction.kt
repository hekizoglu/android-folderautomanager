package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Gorev tamamlama eylemi (Dongu M05 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md). Her gorev, kullaniciyi
 * tamamlayacagi ekrana tek dokunusla goturen bir eylemle eslenir. Route/Intent cozumu
 * burada DEGIL — tek yerde [MissionActionRouter] icinde yapilir; bu sealed interface
 * sadece "hangi eylem" bilgisini tasir.
 */
sealed interface MissionAction {
    /** Sinif landirma inceleme ekranini ac (belirsiz/CAT_OTHER uygulamalar). */
    data object OpenClassificationReview : MissionAction

    /** Bildirim analiz raporunu ac. */
    data object OpenNotificationReport : MissionAction

    /** Ekran suresi / kullanim raporunu ac. */
    data object OpenUsageReport : MissionAction

    /** Kullanim erisimi izni verilmemis — Android sistem ayarina yonlendir. */
    data object OpenSettingsUsageAccess : MissionAction

    /**
     * Gece kullanmama gorevi — sistem Rahatsiz Etmeyin/Bedtime ayarina yonlendir (Dongu G2).
     * UI katmani Intent'i cozemezse (resolveActivity null — cihaz desteklemiyorsa)
     * [OpenUsageReport] hedefine dusurur; bu karar domain katmaninda DEGIL, MissionsScreen'de
     * Intent cozumlemesiyle verilir (Android API'si JVM testte kullanilamaz).
     */
    data object OpenDoNotDisturbSettings : MissionAction

    /** Eylem gerektirmeyen gorev (orn. pasif/otomatik izlenen gorevler). */
    data object None : MissionAction

    /**
     * Dongu G3b — uygulama-spesifik gorevde (DAILY_APP_LIMIT) uzun basis: hedef uygulamanin
     * Android "Uygulama Bilgisi" ekranini ac (Settings.ACTION_APPLICATION_DETAILS_SETTINGS +
     * paket URI). [packageName] SADECE bu action nesnesinde tasinir, telemetriye YAZILMAZ.
     */
    data class OpenAppInfo(val packageName: String) : MissionAction
}
