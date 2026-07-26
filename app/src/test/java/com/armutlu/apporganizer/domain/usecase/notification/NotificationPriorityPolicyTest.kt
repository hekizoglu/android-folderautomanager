package com.armutlu.apporganizer.domain.usecase.notification

import com.armutlu.apporganizer.domain.models.NotificationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPriorityPolicyTest {

    private val classifier = NotificationClassifierUseCase()

    @Test
    fun `authentication stays at least 80 even with minimum system priority`() {
        val result = classify(
            packageName = "com.instagram.android",
            title = "Login alert",
            text = "Your login code is 123456",
            systemPriority = -100,
        )

        assertEquals(NotificationCategory.FINANCE, result.category)
        assertTrue(result.isSensitive)
        assertTrue(result.importanceScore >= 80)
        assertFalse(result.shouldSuppress)
    }

    @Test
    fun `promotion cannot escape suppression with urgency security or high priority`() {
        val result = classify(
            packageName = "com.akbank.android.apps.akbank_direkt",
            title = "Critical action required",
            text = "Urgent security sale: claim your coupon now",
            systemPriority = 100,
        )

        assertEquals(NotificationCategory.PROMOTION, result.category)
        assertTrue(result.importanceScore < 40)
        assertTrue(result.shouldSuppress)
    }

    @Test
    fun `authentication wins over promotion words`() {
        val result = classify(
            packageName = "com.trendyol.android",
            title = "Security code",
            text = "Verification code 774411 for your special offer",
            systemPriority = -2,
        )

        assertEquals(NotificationCategory.FINANCE, result.category)
        assertTrue(result.importanceScore >= 80)
        assertFalse(result.shouldSuppress)
    }

    @Test
    fun `normal messaging remains visible at minimum system priority`() {
        val result = classify(
            packageName = "com.whatsapp",
            title = "Ali",
            text = "Toplantı tamamlandı",
            systemPriority = -100,
        )

        assertEquals(NotificationCategory.MESSAGING, result.category)
        assertTrue(result.importanceScore >= 50)
        assertFalse(result.shouldSuppress)
    }

    @Test
    fun `system priority is clamped to Android supported range`() {
        val high = classify("com.example.notes", "Note", "General information", 100)
        val maxSupported = classify("com.example.notes", "Note", "General information", 2)
        val low = classify("com.example.notes", "Note", "General information", -100)
        val minSupported = classify("com.example.notes", "Note", "General information", -2)

        assertEquals(maxSupported.importanceScore, high.importanceScore)
        assertEquals(minSupported.importanceScore, low.importanceScore)
        assertTrue(high.importanceScore in 0..100)
        assertTrue(low.importanceScore in 0..100)
    }

    @Test
    fun `low value language lowers otherwise generic notification score`() {
        val regular = classify(
            packageName = "com.example.news",
            title = "Update",
            text = "General information",
        )
        val lowValue = classify(
            packageName = "com.example.news",
            title = "Newsletter",
            text = "Recommended for you",
        )

        assertEquals(NotificationCategory.OTHER, regular.category)
        assertEquals(NotificationCategory.OTHER, lowValue.category)
        assertTrue(lowValue.importanceScore < regular.importanceScore)
    }

    private fun classify(
        packageName: String,
        title: String,
        text: String,
        systemPriority: Int = 0,
    ) = classifier.classify(
        key = "$packageName-$title",
        packageName = packageName,
        title = title,
        text = text,
        timestamp = 1L,
        systemPriority = systemPriority,
    )
}
