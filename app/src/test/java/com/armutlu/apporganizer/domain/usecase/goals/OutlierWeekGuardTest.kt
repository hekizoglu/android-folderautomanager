package com.armutlu.apporganizer.domain.usecase.goals

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P2 — [OutlierWeekGuard]. Roadmap §4.5: son hafta 4 haftalık medyanın çok üstünde/altındaysa
 * ham değer olarak kullanılmaz.
 */
class OutlierWeekGuardTest {

    @Test
    fun `fewer than 4 prior weeks skips outlier protection entirely`() {
        val guarded = OutlierWeekGuard.guard(900L, listOf(300L, 310L, 290L))
        assertEquals(900L, guarded)
    }

    @Test
    fun `last week far above median is clamped to upper bound`() {
        // medyan(300,320,280,310) = 305 -> ust sinir 305*1.75 = 533 (Long ile kesme)
        val guarded = OutlierWeekGuard.guard(900L, listOf(300L, 320L, 280L, 310L))
        val median = 305.0
        val expectedUpper = (median * OutlierWeekGuard.UPPER_MULTIPLIER).toLong()
        assertEquals(expectedUpper, guarded)
    }

    @Test
    fun `last week far below median is clamped to lower bound`() {
        // medyan(300,320,280,310) = 305 -> alt sinir 305*0.60 = 183
        val guarded = OutlierWeekGuard.guard(10L, listOf(300L, 320L, 280L, 310L))
        val median = 305.0
        val expectedLower = (median * OutlierWeekGuard.LOWER_MULTIPLIER).toLong()
        assertEquals(expectedLower, guarded)
    }

    @Test
    fun `last week within normal range is returned unchanged`() {
        val guarded = OutlierWeekGuard.guard(310L, listOf(300L, 320L, 280L, 310L))
        assertEquals(310L, guarded)
    }

    @Test
    fun `zero median prior weeks skips protection to avoid division noise`() {
        val guarded = OutlierWeekGuard.guard(50L, listOf(0L, 0L, 0L, 0L))
        assertEquals(50L, guarded)
    }
}
