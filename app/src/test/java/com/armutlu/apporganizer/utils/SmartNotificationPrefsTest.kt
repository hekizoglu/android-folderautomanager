package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.NotificationBadgeMode
import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartNotificationPrefsTest {

    @Test
    fun `fresh install enables engine by default`() {
        assertTrue(SmartNotificationPrefs.initialEngineEnabled(existingInstall = false))
    }

    @Test
    fun `upgrade install preserves classic behavior by disabling engine initially`() {
        assertFalse(SmartNotificationPrefs.initialEngineEnabled(existingInstall = true))
    }

    @Test
    fun `settings defaults enable privacy and promotion protection`() {
        val settings = SmartNotificationSettings.defaults(engineEnabled = true)

        assertTrue(settings.engineEnabled)
        assertTrue(settings.filterPromotions)
        assertTrue(settings.hideSensitiveContent)
        assertEquals(NotificationCategory.entries.toSet(), settings.visibleCategories)
        assertEquals(NotificationBadgeMode.CLASSIC_APP, settings.badgeMode)
    }
}
