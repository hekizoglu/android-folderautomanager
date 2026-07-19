package com.armutlu.apporganizer.domain.usecase.missions

import com.armutlu.apporganizer.domain.common.DataFreshness

/**
 * Dongu M02 — tum gorev metriklerinin tek ve zaman tutarli anlik goruntusu
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 790-848).
 *
 * [MissionMetricSnapshotProvider] tarafindan tek `now` ve tek UsageStats okumasi ile uretilir;
 * `MissionsViewModel` bu snapshot'i tuketir, kendisi hesaplama yapmaz.
 *
 * Kullanim izni yoksa (veya UsageStats verisi alinamazsa) kullanim tabanli alanlar `null` olur —
 * gercek `0` (kullanim yok) ile veri-yok `null` ayrimi boylece korunur. Eylem sayaclari
 * (`classificationActionsToday`, `notificationReportViewedToday`, `positiveActionsThisWeek`)
 * UsageStats izninden bagimsiz TaskScore event tablosundan gelir, bu yuzden non-null'dur.
 */
data class MissionMetricSnapshot(
    /** Snapshot'in alindigi epoch-milli an — tum alanlar bu ana gore tutarlidir. */
    val capturedAt: Long,
    val screenTimeMinutesToday: Long?,
    val unlockCountToday: Int?,
    val usedAfter23Today: Boolean?,
    val firstUseAfter23At: Long?,
    val screenTimeMinutesThisWeek: Long?,
    val screenTimeMinutesPreviousWeek: Long?,
    val classificationActionsToday: Int,
    val notificationReportViewedToday: Boolean,
    val positiveActionsThisWeek: Int,
    val freshness: DataFreshness,
    // Dongu G3a — yeni gorev sinyalleri.
    /** Bugun bir klasor emoji/rengi ozellestirildi mi (TaskScore event tabanli). */
    val folderCustomizedToday: Boolean = false,
    /** Bu hafta haftalik rapor (Wrapped) acildi mi (TaskScore event tabanli). */
    val wrappedReportViewedThisWeek: Boolean = false,
    /**
     * Gunun ilk 30 dakikasinda (ilk kullanimdan itibaren) sosyal kategoride bir uygulama
     * acildi mi. null = veri yok (izin yok VEYA bugun henuz hic kullanim yok — pencere
     * baslamadi). Uygulama adi bu alanda ASLA tasinmaz, sadece kategori-bazli bayrak.
     */
    val socialAppOpenedInFirst30MinToday: Boolean? = null,
    /** Bugun Focus Mode'da biriken toplam dakika (AppPrefs basit sayaci, izin bagimsiz). */
    val focusModeMinutesToday: Long = 0L,
    /**
     * Dongu G1 (kisisel gorev hedefi) — bugun HARIC son (en fazla 7) TAMAMLANMIS gunun gunluk
     * ekran suresi (dk)/kilit acma listesi, eskiden yeniye sirali. Bugun donem bitmedigi icin
     * dahil edilmez (yaris kosulu: gun ortasinda hedef degismesin). Izin yoksa veya veri yoksa
     * bos liste doner — PersonalTargetCalculator zaten <3 gun icin null uretir (tanisma modu).
     */
    val screenTimeMinutesLast7CompletedDays: List<Long> = emptyList(),
    val unlockCountLast7CompletedDays: List<Long> = emptyList(),
    // Dongu G3b — uygulama-spesifik gorev (DAILY_APP_LIMIT). Paket adlari SADECE bu alanlarda
    // ve gorev BASLIGINDA gorunur, telemetriye ASLA gitmez (U02). packageDailyMinutesLast7Days
    // MissionSummaryUseCase'in aday secimi (AppLimitCandidateSelector) icin kullandigi ham veri;
    // appLimitUsageMinutesToday ise ONCEDEN secilmis (AppPrefs'te sabitlenmis) hedef paketin
    // BUGUNKU kullanim dakikasidir (evaluate() bunu dogrudan tuketir).
    /** Kategori-eligible (sosyal/oyun/video) paketlerin son 7 TAMAMLANMIS gunluk dakikalari. */
    val appLimitCandidates: List<AppLimitCandidateSelector.PackageUsageCandidate> = emptyList(),
    /** AppPrefs'te sabitlenmis hedef paketin BUGUNKU kullanim dakikasi (varsa). */
    val appLimitUsageMinutesToday: Long? = null,
)
