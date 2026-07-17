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
)
