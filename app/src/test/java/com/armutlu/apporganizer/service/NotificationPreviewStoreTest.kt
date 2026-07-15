package com.armutlu.apporganizer.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPreviewStoreTest {

    @Test
    fun `summarize returns count label when content is disabled`() {
        val previews = listOf(
            NotificationPreview("a", "pkg", 2L, "Yeni mesaj"),
            NotificationPreview("b", "pkg", 1L, "Toplanti basliyor"),
        )

        val summary = NotificationPreviewStore.summarize(previews, count = 2, showContent = false)

        assertEquals("2 bildirim", summary)
    }

    @Test
    fun `summarize keeps only latest two previews in descending order`() {
        val previews = listOf(
            NotificationPreview("a", "pkg", 1L, "Ilk"),
            NotificationPreview("b", "pkg", 3L, "Ucuncu"),
            NotificationPreview("c", "pkg", 2L, "Ikinci"),
        )

        val summary = NotificationPreviewStore.summarize(previews, count = 3, showContent = true)

        assertEquals("Ucuncu  •  Ikinci", summary)
    }

    @Test
    fun `removePreview drops package when last active preview disappears`() {
        val current = mapOf("pkg" to listOf(NotificationPreview("a", "pkg", 1L, "Mesaj")))

        val updated = NotificationPreviewStore.removePreview(current, "pkg", "a")

        assertEquals(emptyMap<String, List<NotificationPreview>>(), updated)
    }

    @Test
    fun `extractPreview returns null when notification has no visible content`() {
        assertNull(
            NotificationPreviewStore.extractPreview(
                TestStatusBarNotificationFactory.create(
                    packageName = "pkg",
                    key = "n1",
                    title = "",
                    text = "",
                    bigText = "",
                )
            )
        )
    }
}
