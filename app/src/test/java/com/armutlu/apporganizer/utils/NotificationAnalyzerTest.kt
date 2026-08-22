package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.domain.models.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NotificationAnalyzerTest {

    private fun eventAtHour(
        pkg: String,
        hourOfDay: Int,
        minuteOffset: Int = 0,
        category: NotificationCategory = NotificationCategory.OTHER,
        score: Int = category.defaultImportance,
        suppressed: Boolean = false,
    ): NotificationEvent {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minuteOffset)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return NotificationEvent(
            packageName = pkg,
            postedAt = cal.timeInMillis,
            category = category.name,
            importanceScore = score,
            wasSuppressed = suppressed,
        )
    }

    private fun eventsAtHour(
        pkg: String,
        hourOfDay: Int,
        count: Int,
        category: NotificationCategory = NotificationCategory.OTHER,
        score: Int = category.defaultImportance,
        suppressed: Boolean = false,
    ): List<NotificationEvent> =
        (0 until count).map { minute ->
            eventAtHour(
                pkg = pkg,
                hourOfDay = hourOfDay,
                minuteOffset = minute,
                category = category,
                score = score,
                suppressed = suppressed,
            )
        }

    @Test
    fun `empty events returns empty report`() {
        val report = NotificationAnalyzer.analyze(emptyList(), emptyMap(), emptyMap())

        assertEquals(0, report.totalNotifications)
        assertEquals(0, report.totalReceived)
        assertEquals(0, report.actionableCount)
        assertEquals(0, report.suppressedCount)
        assertTrue(report.categoryDistribution.isEmpty())
        assertTrue(report.appStats.isEmpty())
        assertTrue(report.mostTalkative.isEmpty())
        assertTrue(report.disturbing.isEmpty())
        assertTrue(report.distracting.isEmpty())
        assertTrue(report.topPromotionSources.isEmpty())
    }

    @Test
    fun `mostTalkative sorts apps by total count descending`() {
        val events = eventsAtHour("com.talkative", hourOfDay = 12, count = 20) +
            eventsAtHour("com.quiet", hourOfDay = 12, count = 3)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals(23, report.totalNotifications)
        assertEquals("com.talkative", report.mostTalkative.first().packageName)
        assertEquals(20, report.mostTalkative.first().total)
        assertEquals("com.quiet", report.mostTalkative[1].packageName)
    }

    @Test
    fun `mostTalkative caps at top 10 apps`() {
        val events = (1..15).flatMap { index ->
            eventsAtHour("com.app$index", hourOfDay = 12, count = index)
        }

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals(10, report.mostTalkative.size)
        assertEquals("com.app15", report.mostTalkative.first().packageName)
    }

    @Test
    fun `disturbing includes app with high night ratio`() {
        val events = eventsAtHour("com.nightowl", hourOfDay = 23, count = 12)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertTrue(report.disturbing.any { it.packageName == "com.nightowl" })
        assertTrue(report.disturbing.first().nightRatio > 0.3f)
        assertEquals(12, report.nightCount)
    }

    @Test
    fun `disturbing excludes daytime app with low night ratio`() {
        val events = (0 until 12).map { index ->
            eventAtHour("com.daytime", hourOfDay = 10 + (index % 8))
        }

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertTrue(report.disturbing.none { it.packageName == "com.daytime" })
    }

    @Test
    fun `disturbing includes app with hourly burst even without night activity`() {
        val events = eventsAtHour("com.burst", hourOfDay = 14, count = 6)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        val stats = report.appStats.first { it.packageName == "com.burst" }
        assertTrue(stats.maxBurstPerHour >= 5)
        assertEquals(0, stats.nightCount)
        assertTrue(report.disturbing.any { it.packageName == "com.burst" })
    }

    @Test
    fun `distracting includes high-notification low-usage app`() {
        val events = eventsAtHour("com.distracting", hourOfDay = 12, count = 20)
        val usage = mapOf("com.distracting" to 5 * 60_000L)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), usage)

        val stats = report.appStats.first { it.packageName == "com.distracting" }
        assertTrue(stats.distractionScore > 1f)
        assertTrue(report.distracting.any { it.packageName == "com.distracting" })
    }

    @Test
    fun `distracting excludes app with sufficient usage time`() {
        val events = eventsAtHour("com.wellused", hourOfDay = 12, count = 20)
        val usage = mapOf("com.wellused" to 120 * 60_000L)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), usage)

        assertTrue(report.distracting.none { it.packageName == "com.wellused" })
    }

    @Test
    fun `dailyCounts places today's events in the last index`() {
        val events = eventsAtHour("com.today", hourOfDay = 9, count = 4)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        val stats = report.appStats.first { it.packageName == "com.today" }
        assertEquals(7, stats.dailyCounts.size)
        assertEquals(4, stats.dailyCounts.last())
        assertEquals(4, stats.dailyCounts.sum())
    }

    @Test
    fun `appName falls back to package suffix when name unknown`() {
        val events = eventsAtHour("com.example.unknownapp", hourOfDay = 12, count = 1)

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals("unknownapp", report.appStats.first().appName)
    }

    @Test
    fun `appName uses provided display name when available`() {
        val events = eventsAtHour("com.instagram.android", hourOfDay = 12, count = 1)
        val names = mapOf("com.instagram.android" to "Instagram")

        val report = NotificationAnalyzer.analyze(events, names, emptyMap())

        assertEquals("Instagram", report.appStats.first().appName)
    }

    @Test
    fun `metadata summary separates actionable suppressed categories and high priority`() {
        val events = listOf(
            eventAtHour(
                pkg = "com.whatsapp",
                hourOfDay = 12,
                category = NotificationCategory.MESSAGING,
                score = 65,
            ),
            eventAtHour(
                pkg = "com.bank",
                hourOfDay = 13,
                category = NotificationCategory.FINANCE,
                score = 90,
            ),
            eventAtHour(
                pkg = "com.shop",
                hourOfDay = 14,
                category = NotificationCategory.PROMOTION,
                score = 15,
                suppressed = true,
            ),
        )

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals(3, report.totalReceived)
        assertEquals(2, report.actionableCount)
        assertEquals(1, report.suppressedCount)
        assertEquals(1, report.highPriorityCount)
        assertEquals(1, report.categoryDistribution[NotificationCategory.MESSAGING])
        assertEquals(1, report.categoryDistribution[NotificationCategory.FINANCE])
        assertEquals(1, report.categoryDistribution[NotificationCategory.PROMOTION])
    }

    @Test
    fun `topPromotionSources sorts by promotion count`() {
        val events = eventsAtHour(
            pkg = "com.shop.large",
            hourOfDay = 12,
            count = 4,
            category = NotificationCategory.PROMOTION,
            score = 15,
            suppressed = true,
        ) + eventsAtHour(
            pkg = "com.shop.small",
            hourOfDay = 13,
            count = 2,
            category = NotificationCategory.PROMOTION,
            score = 15,
            suppressed = true,
        )

        val report = NotificationAnalyzer.analyze(events, emptyMap(), emptyMap())

        assertEquals(listOf("com.shop.large", "com.shop.small"), report.topPromotionSources.map { it.packageName })
        assertEquals(4, report.topPromotionSources.first().promotionCount)
    }

    @Test
    fun `unknown stored category safely falls back to OTHER`() {
        val event = NotificationEvent(
            packageName = "com.legacy",
            postedAt = System.currentTimeMillis(),
            category = "BROKEN_VALUE",
            importanceScore = 35,
        )

        val report = NotificationAnalyzer.analyze(listOf(event), emptyMap(), emptyMap())

        assertEquals(1, report.categoryDistribution[NotificationCategory.OTHER])
        assertEquals(1, report.appStats.single().categoryCounts[NotificationCategory.OTHER])
    }
}
