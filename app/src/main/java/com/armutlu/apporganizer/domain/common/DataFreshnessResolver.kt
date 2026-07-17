package com.armutlu.apporganizer.domain.common

import java.time.Clock

/**
 * Bir `computedAt`/`lastUpdatedAt` epoch-milli zaman damgasindan [DataFreshness] uretir
 * (Dongu H03 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satir 557-598).
 *
 * [clock] test edilebilirlik icin enjekte edilir (sabit Clock ile deterministik test,
 * bkz. [com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver] ile ayni pattern).
 */
class DataFreshnessResolver(
    private val clock: Clock,
) {

    companion object {
        /** Bu sureden daha yeni ise [DataFreshness.LIVE] (sinir dahil). */
        const val LIVE_MAX_AGE_MS: Long = 5 * 60 * 1000L

        /** Bu sureden daha yeni ise [DataFreshness.RECENT] (sinir dahil), aksi halde STALE. */
        const val RECENT_MAX_AGE_MS: Long = 30 * 60 * 1000L
    }

    /**
     * [computedAt] null ise [DataFreshness.UNAVAILABLE]. Gelecekteki bir zaman damgasi
     * (clock skew, negatif yas) [DataFreshness.LIVE] olarak kabul edilir.
     */
    fun resolve(computedAt: Long?): DataFreshness {
        if (computedAt == null) return DataFreshness.UNAVAILABLE

        val ageMs = clock.millis() - computedAt
        return when {
            ageMs <= LIVE_MAX_AGE_MS -> DataFreshness.LIVE
            ageMs <= RECENT_MAX_AGE_MS -> DataFreshness.RECENT
            else -> DataFreshness.STALE
        }
    }
}
