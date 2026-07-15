package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun buildHourlyList_limitsToSixItemsFromCurrentHour() {
        val result = WeatherRepository.buildHourlyListFromArrays(
            times = listOf(
                "2026-07-15T09:00",
                "2026-07-15T10:00",
                "2026-07-15T11:00",
                "2026-07-15T12:00",
                "2026-07-15T13:00",
                "2026-07-15T14:00",
                "2026-07-15T15:00",
            ),
            temps = listOf(21, 22, 23, 24, 25, 26, 27),
            startIndex = 1,
        )

        assertEquals(listOf("10", "11", "12", "13", "14", "15"), result.map { it.hourLabel })
        assertEquals(listOf(22, 23, 24, 25, 26, 27), result.map { it.tempC })
    }

    @Test
    fun isCacheStale_returnsFalseWithinTtl() {
        val now = 1_000_000L
        val isStale = WeatherRepository.isCacheStale(
            fetchedAt = now - WeatherRepository.CACHE_TTL_FOR_TEST_MS + 1_000L,
            nowMillis = now,
        )

        assertFalse(isStale)
    }

    @Test
    fun isCacheStale_returnsTrueAfterTtl() {
        val now = 2_000_000L
        val isStale = WeatherRepository.isCacheStale(
            fetchedAt = now - WeatherRepository.CACHE_TTL_FOR_TEST_MS - 1L,
            nowMillis = now,
        )

        assertTrue(isStale)
    }
}
