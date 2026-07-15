package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryPrefsTest {

    @Test
    fun dedupeAndPrepend_movesSameQueryToFront_withoutDuplicating() {
        val existing = listOf(
            item(query = "spotify", title = "Spotify"),
            item(query = "maps", title = "Maps"),
        )

        val updated = SearchHistoryPrefs.dedupeAndPrepend(
            existing = existing,
            incoming = item(query = "spotify", title = "Spotify Premium", atMillis = 2L),
            maxItems = 3,
        )

        assertEquals(listOf("spotify", "maps"), updated.map { it.query })
        assertEquals("Spotify Premium", updated.first().title)
    }

    @Test
    fun dedupeAndPrepend_trimsToMaxItems() {
        val existing = listOf(
            item(query = "one"),
            item(query = "two"),
            item(query = "three"),
        )

        val updated = SearchHistoryPrefs.dedupeAndPrepend(
            existing = existing,
            incoming = item(query = "zero"),
            maxItems = 3,
        )

        assertEquals(listOf("zero", "one", "two"), updated.map { it.query })
    }

    @Test
    fun parseJson_returnsEmptyList_forInvalidPayload() {
        assertTrue(SearchHistoryPrefs.parseJson("not-json").isEmpty())
    }

    @Test
    fun dedupeAndPrepend_blankQuery_dedupesBySourceIdentity() {
        val existing = listOf(
            item(query = "", title = "Spotify", sourceId = "pkg.spotify", atMillis = 1L),
            item(query = "maps", title = "Maps", sourceId = "pkg.maps", atMillis = 2L),
        )

        val updated = SearchHistoryPrefs.dedupeAndPrepend(
            existing = existing,
            incoming = item(query = "", title = "Spotify", sourceId = "pkg.spotify", atMillis = 3L),
            maxItems = 3,
        )

        assertEquals(2, updated.size)
        assertEquals("pkg.spotify", updated.first().sourceId)
        assertEquals(3L, updated.first().atMillis)
    }

    private fun item(
        query: String,
        title: String = query,
        sourceId: String = "pkg.$query",
        atMillis: Long = 1L,
    ) = SearchHistoryPrefs.SearchHistoryItem(
        query = query,
        title = title,
        sourceType = "app",
        sourceId = sourceId,
        atMillis = atMillis,
    )
}
