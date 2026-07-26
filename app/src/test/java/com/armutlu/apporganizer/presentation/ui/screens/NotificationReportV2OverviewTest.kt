package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.NotificationCategory
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationReportV2OverviewTest {

    @Test
    fun `metrics calculate actionable and suppressed percentages`() {
        val metrics = NotificationReportV2Metrics.from(
            report(total = 10, actionable = 7, suppressed = 3, highPriority = 2, night = 4)
        )

        assertEquals(10, metrics.totalReceived)
        assertEquals(70, metrics.actionablePercent)
        assertEquals(30, metrics.suppressedPercent)
        assertEquals(2, metrics.highPriorityCount)
        assertEquals(4, metrics.nightCount)
    }

    @Test
    fun `empty report percentages remain zero`() {
        val metrics = NotificationReportV2Metrics.from(report(total = 0))

        assertEquals(0, metrics.actionablePercent)
        assertEquals(0, metrics.suppressedPercent)
    }

    @Test
    fun `invalid negative values are clamped for display`() {
        val metrics = NotificationReportV2Metrics.from(
            report(total = -5, actionable = -2, suppressed = -1, highPriority = -3, night = -4)
        )

        assertEquals(0, metrics.totalReceived)
        assertEquals(0, metrics.actionableCount)
        assertEquals(0, metrics.suppressedCount)
        assertEquals(0, metrics.highPriorityCount)
        assertEquals(0, metrics.nightCount)
    }

    @Test
    fun `all categories have report labels`() {
        NotificationCategory.values().forEach { category ->
            assertTrue(category.reportDisplayName().isNotBlank())
        }
    }

    private fun report(
        total: Int,
        actionable: Int = 0,
        suppressed: Int = 0,
        highPriority: Int = 0,
        night: Int = 0,
    ) = NotificationAnalyzer.Report(
        totalNotifications = total,
        appStats = emptyList(),
        mostTalkative = emptyList(),
        disturbing = emptyList(),
        distracting = emptyList(),
        totalReceived = total,
        actionableCount = actionable,
        suppressedCount = suppressed,
        highPriorityCount = highPriority,
        nightCount = night,
    )
}
