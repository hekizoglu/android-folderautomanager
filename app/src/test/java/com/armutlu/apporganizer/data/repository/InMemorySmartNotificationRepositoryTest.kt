package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemorySmartNotificationRepositoryTest {

    @Test
    fun `replaceActive ranks items and derives actionable counts`() = runTest {
        val repository = InMemorySmartNotificationRepository()
        repository.replaceActive(
            listOf(
                smart("msg-1", "com.whatsapp", NotificationCategory.MESSAGING, 60, 10L),
                smart("promo-1", "com.shop", NotificationCategory.PROMOTION, 15, 30L, suppressed = true),
                smart("finance-1", "com.bank", NotificationCategory.FINANCE, 90, 20L),
                smart("msg-2", "com.whatsapp", NotificationCategory.MESSAGING, 65, 40L),
            )
        )

        assertEquals(
            listOf("finance-1", "msg-2", "msg-1", "promo-1"),
            repository.activeNotifications.value.map { it.key },
        )
        assertEquals(2, repository.actionablePackageCounts.value["com.whatsapp"])
        assertEquals(1, repository.actionablePackageCounts.value["com.bank"])
        assertTrue("com.shop" !in repository.actionablePackageCounts.value)
        assertEquals(2, repository.categoryCounts.value[NotificationCategory.MESSAGING])
        assertEquals(1, repository.categoryCounts.value[NotificationCategory.FINANCE])
        assertEquals(1, repository.suppressedCount.value)
    }

    @Test
    fun `remove recomputes all derived state`() = runTest {
        val repository = InMemorySmartNotificationRepository()
        repository.replaceActive(
            listOf(
                smart("msg-1", "com.whatsapp", NotificationCategory.MESSAGING, 60, 10L),
                smart("msg-2", "com.whatsapp", NotificationCategory.MESSAGING, 65, 20L),
                smart("promo-1", "com.shop", NotificationCategory.PROMOTION, 10, 30L, suppressed = true),
            )
        )

        repository.remove("msg-2")
        repository.remove("promo-1")

        assertEquals(listOf("msg-1"), repository.activeNotifications.value.map { it.key })
        assertEquals(mapOf("com.whatsapp" to 1), repository.actionablePackageCounts.value)
        assertEquals(mapOf(NotificationCategory.MESSAGING to 1), repository.categoryCounts.value)
        assertEquals(0, repository.suppressedCount.value)
    }

    @Test
    fun `clearActive clears content and every derived flow`() = runTest {
        val repository = InMemorySmartNotificationRepository()
        repository.replaceActive(
            listOf(smart("finance-1", "com.bank", NotificationCategory.FINANCE, 90, 1L))
        )

        repository.clearActive()

        assertTrue(repository.activeNotifications.value.isEmpty())
        assertTrue(repository.actionablePackageCounts.value.isEmpty())
        assertTrue(repository.categoryCounts.value.isEmpty())
        assertEquals(0, repository.suppressedCount.value)
    }

    @Test
    fun `replaceActive keeps only one item for duplicate notification key`() = runTest {
        val repository = InMemorySmartNotificationRepository()
        repository.replaceActive(
            listOf(
                smart("same-key", "com.first", NotificationCategory.OTHER, 30, 1L),
                smart("same-key", "com.second", NotificationCategory.FINANCE, 90, 2L),
            )
        )

        assertEquals(1, repository.activeNotifications.value.size)
        assertEquals("com.first", repository.activeNotifications.value.single().packageName)
    }

    private fun smart(
        key: String,
        packageName: String,
        category: NotificationCategory,
        score: Int,
        timestamp: Long,
        suppressed: Boolean = false,
    ) = SmartNotification(
        key = key,
        packageName = packageName,
        title = "",
        text = "",
        category = category,
        importanceScore = score,
        timestamp = timestamp,
        isSensitive = category == NotificationCategory.FINANCE,
        shouldSuppress = suppressed,
    )
}
