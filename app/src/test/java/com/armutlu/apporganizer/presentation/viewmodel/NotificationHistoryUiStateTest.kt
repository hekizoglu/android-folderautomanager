package com.armutlu.apporganizer.presentation.viewmodel

import com.armutlu.apporganizer.domain.models.NotificationHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationHistoryUiStateTest {

    private val entries = listOf(
        history(id = 3, packageName = "com.whatsapp", postedAt = 300L),
        history(id = 2, packageName = "com.android.settings", postedAt = 200L),
        history(id = 1, packageName = "com.whatsapp", postedAt = 100L),
    )

    @Test
    fun `build exposes app filters with counts and resolved names`() {
        val state = NotificationHistoryUiState.build(
            entries = entries,
            appNames = mapOf(
                "com.whatsapp" to "WhatsApp",
                "com.android.settings" to "Ayarlar",
            ),
            requestedPackageName = null,
        )

        assertEquals(3, state.totalCount)
        assertEquals(listOf("Ayarlar", "WhatsApp"), state.filters.map { it.appName })
        assertEquals(listOf(1, 2), state.filters.map { it.count })
        assertEquals(entries, state.entries)
        assertNull(state.selectedPackageName)
    }

    @Test
    fun `selected package only returns notifications from that app`() {
        val state = NotificationHistoryUiState.build(
            entries = entries,
            appNames = mapOf("com.whatsapp" to "WhatsApp"),
            requestedPackageName = "com.whatsapp",
        )

        assertEquals("com.whatsapp", state.selectedPackageName)
        assertEquals(listOf(3L, 1L), state.entries.map { it.id })
    }

    @Test
    fun `missing selected package safely falls back to all notifications`() {
        val state = NotificationHistoryUiState.build(
            entries = entries,
            appNames = emptyMap(),
            requestedPackageName = "com.deleted.app",
        )

        assertNull(state.selectedPackageName)
        assertEquals(entries, state.entries)
    }

    @Test
    fun `unknown package uses package tail as visible app name`() {
        assertEquals(
            "unknown",
            NotificationHistoryUiState.resolveAppName("com.example.unknown", emptyMap()),
        )
    }

    private fun history(
        id: Long,
        packageName: String,
        postedAt: Long,
    ) = NotificationHistoryEntity(
        id = id,
        packageName = packageName,
        title = "Başlık",
        text = "Metin",
        postedAt = postedAt,
    )
}
