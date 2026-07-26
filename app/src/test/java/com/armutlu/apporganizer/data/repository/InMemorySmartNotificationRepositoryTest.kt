package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.SmartNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemorySmartNotificationRepositoryTest {

    @Test
    fun `replaceActive ranks items and derives actionable counts`() = runTest {
        val repository = repository()
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
    fun `read timestamp hides only older active notifications and keeps active snapshot`() = runTest {
        val readSource = FakeReadStateSource()
        val repository = InMemorySmartNotificationRepository(readSource)
        repository.replaceActive(
            listOf(
                smart("old", "com.whatsapp", NotificationCategory.MESSAGING, 60, 100L),
                smart("new", "com.whatsapp", NotificationCategory.MESSAGING, 65, 300L),
            )
        )

        readSource.state.value = mapOf("com.whatsapp" to 200L)
        awaitCondition { repository.actionablePackageCounts.value["com.whatsapp"] == 1 }

        assertEquals(2, repository.activeNotifications.value.size)
        assertEquals(1, repository.actionablePackageCounts.value["com.whatsapp"])
        assertEquals(1, repository.categoryCounts.value[NotificationCategory.MESSAGING])

        readSource.state.value = mapOf("com.whatsapp" to 400L)
        awaitCondition { repository.actionablePackageCounts.value.isEmpty() }

        assertEquals(2, repository.activeNotifications.value.size)
        assertTrue(repository.actionablePackageCounts.value.isEmpty())
    }

    @Test
    fun `new notification after read time becomes actionable again`() = runTest {
        val readSource = FakeReadStateSource(mapOf("com.whatsapp" to 200L))
        val repository = InMemorySmartNotificationRepository(readSource)

        repository.replaceActive(
            listOf(smart("new", "com.whatsapp", NotificationCategory.MESSAGING, 65, 201L))
        )

        assertEquals(mapOf("com.whatsapp" to 1), repository.actionablePackageCounts.value)
    }

    @Test
    fun `remove recomputes all derived state`() = runTest {
        val repository = repository()
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
        val repository = repository()
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
        val repository = repository()
        repository.replaceActive(
            listOf(
                smart("same-key", "com.first", NotificationCategory.OTHER, 30, 1L),
                smart("same-key", "com.second", NotificationCategory.FINANCE, 90, 2L),
            )
        )

        assertEquals(1, repository.activeNotifications.value.size)
        assertEquals("com.first", repository.activeNotifications.value.single().packageName)
    }

    private fun repository(): InMemorySmartNotificationRepository =
        InMemorySmartNotificationRepository(FakeReadStateSource())

    private fun awaitCondition(condition: () -> Boolean) {
        repeat(100) {
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition was not met")
    }

    private class FakeReadStateSource(
        initial: Map<String, Long> = emptyMap(),
    ) : NotificationReadStateSource {
        val state = MutableStateFlow(initial)
        override val lastReadAt: StateFlow<Map<String, Long>> = state
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
