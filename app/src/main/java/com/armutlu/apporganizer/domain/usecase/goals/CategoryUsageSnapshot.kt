package com.armutlu.apporganizer.domain.usecase.goals

import com.armutlu.apporganizer.domain.common.DataFreshness

/**
 * P1 — [CategoryUsageSnapshotProvider] tarafından üretilen, kategori bazlı kullanım verisinin
 * tek kaynağı. Adaptif kategori hedefleri (P2/P4) ve Dashboard UI (P5) AYNI snapshot'ı tüketir —
 * iki farklı hesaplama aynı ekranda farklı sayı göstermesin diye (bkz. roadmap S1/S9/S10).
 *
 * [previousWeekMinutesByCategory]/[currentWeekMinutesByCategory] ISO hafta (Pazartesi başlangıç)
 * sınırlarına göre toplanır — [com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver] ile.
 */
data class CategoryUsageSnapshot(
    val capturedAt: Long,
    val previousWeekMinutesByCategory: Map<String, Long>,
    val currentWeekMinutesByCategory: Map<String, Long>,
    val validDataDayCount: Int,
    val freshness: DataFreshness,
) {
    /** Veri yokken sıfır dakika VARSAYILMAZ — çağıran taraf önce [freshness]'ı kontrol etmeli. */
    fun previousWeekMinutes(categoryId: String): Long? =
        if (freshness == DataFreshness.UNAVAILABLE) null else previousWeekMinutesByCategory[categoryId] ?: 0L

    fun currentWeekMinutes(categoryId: String): Long? =
        if (freshness == DataFreshness.UNAVAILABLE) null else currentWeekMinutesByCategory[categoryId] ?: 0L
}
