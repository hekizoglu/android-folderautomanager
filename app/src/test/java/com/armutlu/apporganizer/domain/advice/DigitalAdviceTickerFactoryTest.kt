package com.armutlu.apporganizer.domain.advice

import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.home.TickerAction
import com.armutlu.apporganizer.domain.home.TickerRanker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P9 — [DigitalAdviceTickerFactory]. Roadmap §9/§11.3: tavsiye CONTEXTUAL_SUGGESTION tipinde
 * üretilir, suggestionKey [DigitalAdvice.suggestionKey] ile birebir aynı, mevcut TickerRanker
 * tür-başı kotasına ve suppression'a tabi olur — ikinci bir suppression deposu YAZILMAZ.
 */
class DigitalAdviceTickerFactoryTest {

    private fun advice(id: String = "goal_exceeded") = DigitalAdvice(
        id = id,
        type = DigitalAdviceType.GOAL_EXCEEDED,
        priority = 2,
        titleRes = 1,
        messageRes = 2,
        action = DigitalAdviceAction.OpenCategoryGoals,
        suggestionKey = "advice_$id",
        createdAt = 0L,
    )

    @Test
    fun `null advice produces no candidate`() {
        val items = DigitalAdviceTickerFactory.candidate(null, "title", "subtitle", nowMillis = 1000L)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `advice produces exactly one CONTEXTUAL_SUGGESTION candidate with matching suggestionKey`() {
        val items = DigitalAdviceTickerFactory.candidate(advice(), "title", "subtitle", nowMillis = 1000L)
        assertEquals(1, items.size)
        assertEquals(SmartTickerType.CONTEXTUAL_SUGGESTION, items.single().type)
        assertEquals("advice_goal_exceeded", items.single().suggestionKey)
    }

    @Test
    fun `action is mapped to the correct typed ticker action`() {
        val items = DigitalAdviceTickerFactory.candidate(advice(), "title", "subtitle", nowMillis = 1000L)
        assertEquals(TickerAction.OpenDashboard, items.single().action)
    }

    @Test
    fun `same suggestionKey is suppressed on repeated ranking within the window`() {
        val nowMillis = 10_000L
        val item = DigitalAdviceTickerFactory.candidate(advice(), "title", "subtitle", nowMillis).single()

        // Ilk gosterim: suppress edilmez.
        val firstRank = TickerRanker.rank(
            candidates = listOf(item),
            now = nowMillis,
            isSuppressed = { false },
        )
        assertEquals(1, firstRank.size)

        // Ikinci cagri: caller (SuggestionCoordinator benzeri) ayni suggestionKey icin true
        // dondurdugunde (roadmap: cross-channel suppression) oge tamamen elenir.
        val suppressedRank = TickerRanker.rank(
            candidates = listOf(item),
            now = nowMillis,
            isSuppressed = { it.suggestionKey == "advice_goal_exceeded" },
        )
        assertTrue(suppressedRank.isEmpty())
    }

    @Test
    fun `only one CONTEXTUAL_SUGGESTION item survives ranking even if multiple candidates share the type`() {
        val nowMillis = 10_000L
        val adviceItem = DigitalAdviceTickerFactory.candidate(advice(), "title", "subtitle", nowMillis).single()
        val otherContextualItem = adviceItem.copy(id = "other_contextual", suggestionKey = "other_contextual_key", priority = 20)

        val ranked = TickerRanker.rank(
            candidates = listOf(adviceItem, otherContextualItem),
            now = nowMillis,
            isSuppressed = { false },
        )
        assertEquals(
            "TickerRanker MAX_PER_TYPE=1 kotasi CONTEXTUAL_SUGGESTION icin de gecerli olmali",
            1,
            ranked.count { it.type == SmartTickerType.CONTEXTUAL_SUGGESTION },
        )
    }
}
