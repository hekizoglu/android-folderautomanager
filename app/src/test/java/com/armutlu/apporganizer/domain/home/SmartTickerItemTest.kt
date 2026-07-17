package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SmartTickerItem] davranış testleri (Döngü T01) — dedupeKey türetimi ve expiresAt mantığı.
 */
class SmartTickerItemTest {

    private fun sample(
        suggestionKey: String? = null,
        expiresAt: Long? = null,
    ) = SmartTickerItem(
        id = "item-1",
        type = SmartTickerType.FEATURE_DISCOVERY,
        title = "Test",
        icon = "📰",
        priority = 5,
        createdAt = 1_000L,
        expiresAt = expiresAt,
        suggestionKey = suggestionKey,
    )

    @Test
    fun dedupeKey_usesSuggestionKey_whenPresent() {
        val item = sample(suggestionKey = "notification_summary")
        assertEquals("notification_summary", item.dedupeKey)
    }

    @Test
    fun dedupeKey_fallsBackToId_whenSuggestionKeyNull() {
        val item = sample(suggestionKey = null)
        assertEquals("item-1", item.dedupeKey)
    }

    @Test
    fun isExpired_false_whenExpiresAtNull() {
        val item = sample(expiresAt = null)
        assertFalse(item.isExpired(Long.MAX_VALUE))
    }

    @Test
    fun isExpired_false_beforeExpiry() {
        val item = sample(expiresAt = 10_000L)
        assertFalse(item.isExpired(9_999L))
    }

    @Test
    fun isExpired_true_atOrAfterExpiry() {
        val item = sample(expiresAt = 10_000L)
        assertTrue(item.isExpired(10_000L))
        assertTrue(item.isExpired(10_001L))
    }
}
