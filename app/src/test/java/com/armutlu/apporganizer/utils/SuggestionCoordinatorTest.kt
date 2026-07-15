package com.armutlu.apporganizer.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionCoordinatorTest {

    private val candidate = SuggestionCandidate(
        dedupeKey = "new_install_com.example.app",
        highValue = true,
        timeSensitive = true,
    )

    @Test
    fun `ticker is blocked when same suggestion was shown as task card recently`() {
        val store = FakeSuggestionHistoryStore().apply {
            recordShown(SuggestionChannel.TASK_CARD, candidate.dedupeKey, 1_000L)
        }

        val allowed = SuggestionCoordinator.canShow(
            candidate = candidate,
            channel = SuggestionChannel.TICKER,
            store = store,
            nowMillis = 2_000L,
        )

        assertFalse(allowed)
    }

    @Test
    fun `system notification requires high value and time sensitivity`() {
        val store = FakeSuggestionHistoryStore()

        val allowed = SuggestionCoordinator.canShow(
            candidate = SuggestionCandidate(
                dedupeKey = "weekly_tip",
                highValue = false,
                timeSensitive = false,
            ),
            channel = SuggestionChannel.SYSTEM_NOTIFICATION,
            store = store,
            nowMillis = 5_000L,
        )

        assertFalse(allowed)
    }

    @Test
    fun `recent rejection blocks all channels during rejection cooldown`() {
        val store = FakeSuggestionHistoryStore().apply {
            recordRejected(candidate.dedupeKey, 10_000L)
        }

        val allowed = SuggestionCoordinator.canShow(
            candidate = candidate,
            channel = SuggestionChannel.TASK_CARD,
            store = store,
            nowMillis = 20_000L,
        )

        assertFalse(allowed)
    }

    @Test
    fun `suggestion can reappear after cooldown expires`() {
        val store = FakeSuggestionHistoryStore().apply {
            recordShown(SuggestionChannel.TASK_CARD, candidate.dedupeKey, 0L)
        }

        val allowed = SuggestionCoordinator.canShow(
            candidate = candidate,
            channel = SuggestionChannel.TICKER,
            store = store,
            nowMillis = 7L * 60L * 60L * 1000L,
        )

        assertTrue(allowed)
    }

    private class FakeSuggestionHistoryStore : SuggestionHistoryStore {
        private val shown = mutableMapOf<Pair<SuggestionChannel, String>, Long>()
        private val rejected = mutableMapOf<String, Long>()

        override fun getLastShownAt(channel: SuggestionChannel, dedupeKey: String): Long? =
            shown[channel to dedupeKey]

        override fun getLastRejectedAt(dedupeKey: String): Long? = rejected[dedupeKey]

        override fun recordShown(channel: SuggestionChannel, dedupeKey: String, shownAt: Long) {
            shown[channel to dedupeKey] = shownAt
        }

        override fun recordRejected(dedupeKey: String, rejectedAt: Long) {
            rejected[dedupeKey] = rejectedAt
        }
    }
}
