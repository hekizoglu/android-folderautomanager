package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FolderNotificationStateTest {

    @Test
    fun `live badge count is applied to folder grid apps`() {
        val apps = listOf(AppInfo(packageName = "com.chat", appName = "Chat"))

        val result = apps.withLiveNotificationState(
            badgeCounts = mapOf("com.chat" to 3),
            latestTexts = emptyMap(),
        )

        assertEquals(3, result.single().notificationCount)
    }

    @Test
    fun `stored notification state is preserved when live state has no entry`() {
        val app = AppInfo(
            packageName = "com.mail",
            appName = "Mail",
            notificationCount = 2,
            notificationText = "2 bildirim",
        )

        val result = listOf(app).withLiveNotificationState(
            badgeCounts = emptyMap(),
            latestTexts = emptyMap(),
        )

        assertSame(app, result.single())
    }

    @Test
    fun `live notification text is applied without losing stored count`() {
        val apps = listOf(
            AppInfo(
                packageName = "com.mail",
                appName = "Mail",
                notificationCount = 2,
                notificationText = "Eski",
            )
        )

        val result = apps.withLiveNotificationState(
            badgeCounts = emptyMap(),
            latestTexts = mapOf("com.mail" to "Yeni bildirim"),
        )

        assertEquals(2, result.single().notificationCount)
        assertEquals("Yeni bildirim", result.single().notificationText)
    }
}
