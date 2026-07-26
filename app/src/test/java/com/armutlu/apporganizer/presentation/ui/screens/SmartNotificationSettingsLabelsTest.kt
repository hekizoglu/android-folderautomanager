package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.NotificationBadgeMode
import com.armutlu.apporganizer.domain.models.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartNotificationSettingsLabelsTest {

    @Test
    fun `every badge mode has a visible name and description`() {
        NotificationBadgeMode.values().forEach { mode ->
            assertTrue(mode.displayName().isNotBlank())
            assertTrue(mode.description().isNotBlank())
        }
    }

    @Test
    fun `every notification category has a visible name and description`() {
        NotificationCategory.values().forEach { category ->
            assertTrue(category.displayName().isNotBlank())
            assertTrue(category.description().isNotBlank())
        }
    }

    @Test
    fun `finance and promotion labels remain distinct`() {
        assertEquals("Finans ve Güvenlik", NotificationCategory.FINANCE.displayName())
        assertEquals("Promosyonlar", NotificationCategory.PROMOTION.displayName())
    }
}
