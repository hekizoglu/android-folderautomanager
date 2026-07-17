package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [TickerRanker] testleri (Döngü T02 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satır 1654-1723): öncelik, TTL, dedupe, tür kotası, boş durum, en fazla 3 öğe sınırı.
 */
class TickerRankerTest {

    private val fixedNow = 1_000_000L

    private fun item(
        id: String,
        type: SmartTickerType = SmartTickerType.CONTEXTUAL_SUGGESTION,
        priority: Int = 10,
        expiresAt: Long? = null,
        suggestionKey: String? = null,
    ) = SmartTickerItem(
        id = id,
        type = type,
        title = "title-$id",
        icon = "📰",
        priority = priority,
        createdAt = fixedNow,
        expiresAt = expiresAt,
        suggestionKey = suggestionKey,
    )

    @Test
    fun emptyInput_producesEmptyOutput() {
        val result = TickerRanker.rank(candidates = emptyList(), now = fixedNow)
        assertTrue(result.isEmpty())
    }

    @Test
    fun expiredItems_areFiltered() {
        val expired = item("expired", expiresAt = fixedNow - 1)
        val valid = item("valid", expiresAt = fixedNow + 1_000)

        val result = TickerRanker.rank(candidates = listOf(expired, valid), now = fixedNow)

        assertEquals(listOf(valid), result)
    }

    @Test
    fun priorityOrder_isPreserved_whenNoPenaltiesApply() {
        val low = item("low", priority = 10)
        val high = item("high", priority = 90)
        val mid = item("mid", priority = 50)

        // Farklı türlerden olmalı — aksi halde tür kotası (max 1/tür) devreye girer ve karşılaştırma
        // anlamsızlaşır.
        val a = high.copy(type = SmartTickerType.ACTION_REQUIRED)
        val b = mid.copy(type = SmartTickerType.PULSE_CHANGE)
        val c = low.copy(type = SmartTickerType.FEATURE_DISCOVERY)

        val result = TickerRanker.rank(candidates = listOf(c, a, b), now = fixedNow)

        assertEquals(listOf(a, b, c), result)
    }

    @Test
    fun duplicateDedupeKey_keepsOnlyOne() {
        val first = item("a", suggestionKey = "same_key", priority = 20)
        val second = item("b", suggestionKey = "same_key", priority = 5)

        val result = TickerRanker.rank(candidates = listOf(first, second), now = fixedNow)

        assertEquals(1, result.size)
        assertEquals("a", result.single().id)
    }

    @Test
    fun typeQuota_limitsToOnePerType_exceptCriticalHealth() {
        val suggestion1 = item("s1", type = SmartTickerType.CONTEXTUAL_SUGGESTION, priority = 50)
        val suggestion2 = item("s2", type = SmartTickerType.CONTEXTUAL_SUGGESTION, priority = 40)
        val critical1 = item("c1", type = SmartTickerType.CRITICAL_HEALTH, priority = 100)
        val critical2 = item("c2", type = SmartTickerType.CRITICAL_HEALTH, priority = 99)

        val result = TickerRanker.rank(
            candidates = listOf(suggestion1, suggestion2, critical1, critical2),
            now = fixedNow,
        )

        // MAX_VISIBLE = 3: iki kritik + tek suggestion (tür kotası suggestion2'yi eler).
        assertEquals(3, result.size)
        assertEquals(listOf("c1", "c2", "s1"), result.map { it.id })
    }

    @Test
    fun maxThreeItems_evenWhenManyCandidatesQualify() {
        val candidates = SmartTickerType.entries.map { type ->
            item(id = type.name, type = type, priority = 10)
        }

        val result = TickerRanker.rank(candidates = candidates, now = fixedNow)

        assertEquals(TickerRanker.MAX_VISIBLE, result.size)
    }

    @Test
    fun suppressedItems_areExcluded() {
        val suppressed = item("suppressed", suggestionKey = "dismissed_key")
        val visible = item("visible", suggestionKey = "other_key")

        val result = TickerRanker.rank(
            candidates = listOf(suppressed, visible),
            now = fixedNow,
            isSuppressed = { it.suggestionKey == "dismissed_key" },
        )

        assertEquals(listOf(visible), result)
    }

    @Test
    fun shownToday_appliesPenalty_canDropBelowOtherCandidate() {
        val shownToday = item("shown_today", type = SmartTickerType.ACTION_REQUIRED, priority = 40, suggestionKey = "k1")
        val fresh = item("fresh", type = SmartTickerType.PULSE_CHANGE, priority = 20, suggestionKey = "k2")

        val history = TickerHistory(lastShownAt = mapOf("k1" to fixedNow - 1_000))

        val result = TickerRanker.rank(
            candidates = listOf(shownToday, fresh),
            history = history,
            now = fixedNow,
        )

        // 40 - 35 (bugün gösterildi cezası) = 5 < 20 -> fresh önde olmalı.
        assertEquals(listOf(fresh, shownToday), result)
    }

    @Test
    fun repeatedThreeTimesInWindow_appliesHeavyPenalty() {
        val repeated = item("repeated", type = SmartTickerType.ACTION_REQUIRED, priority = 90, suggestionKey = "k1")
        val other = item("other", type = SmartTickerType.PULSE_CHANGE, priority = 30, suggestionKey = "k2")

        val threeDaysMs = 3L * 24 * 3600 * 1000
        val history = TickerHistory(
            showTimestamps = mapOf(
                "k1" to listOf(
                    fixedNow - threeDaysMs / 2,
                    fixedNow - threeDaysMs / 3,
                    fixedNow - threeDaysMs / 4,
                ),
            ),
        )

        val result = TickerRanker.rank(
            candidates = listOf(repeated, other),
            history = history,
            now = fixedNow,
        )

        // 90 - 70 (3 gunde 3+ kez gosterildi) = 20 < 30 -> other onde olmali.
        assertEquals(listOf(other, repeated), result)
    }
}
