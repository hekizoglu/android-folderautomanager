package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentNotificationTotalTest {
    @Test fun `sums package counts`() {
        assertEquals(8, safeRecentNotificationTotal(mapOf("a" to 3, "b" to 5)))
    }

    @Test fun `negative counts do not reduce total`() {
        assertEquals(5, safeRecentNotificationTotal(mapOf("a" to -9, "b" to 5)))
    }

    @Test fun `caps total at int max`() {
        assertEquals(Int.MAX_VALUE, safeRecentNotificationTotal(mapOf("a" to Int.MAX_VALUE, "b" to Int.MAX_VALUE)))
    }
}
