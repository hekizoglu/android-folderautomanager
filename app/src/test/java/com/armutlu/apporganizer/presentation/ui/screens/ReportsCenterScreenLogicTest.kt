package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsCenterScreenLogicTest {

    @Test
    fun `buildReportsCenterEntries_returns_single_unique_route_per_report`() {
        val entries = buildReportsCenterEntries(
            apps = sampleApps(),
            categories = Category.getDefaultCategories(),
            wrappedEnabled = true,
            privacyReportEnabled = true,
            diagnosticsGenerating = false,
            nowMs = NOW_MS,
        )

        assertEquals(entries.size, entries.map { it.route }.distinct().size)
        assertEquals(
            listOf(
                "dashboard",
                "usage_report",
                "notification_report",
                "diagnostics_report",
                "wrapped_report",
                "privacy_report",
            ),
            entries.map { it.route }
        )
    }

    @Test
    fun `buildReportsCenterEntries_keeps_disabled_reports_visible_with_reason`() {
        val entries = buildReportsCenterEntries(
            apps = sampleApps(),
            categories = Category.getDefaultCategories(),
            wrappedEnabled = false,
            privacyReportEnabled = false,
            diagnosticsGenerating = false,
            nowMs = NOW_MS,
        )

        val wrapped = entries.first { it.route == "wrapped_report" }
        val privacy = entries.first { it.route == "privacy_report" }

        assertFalse(wrapped.isAvailable)
        assertTrue(wrapped.unavailableReason!!.contains("ayarlardan", ignoreCase = true))
        assertFalse(privacy.isAvailable)
        assertTrue(privacy.unavailableReason!!.contains("etkinlestirilmeden", ignoreCase = true))
    }

    @Test
    fun `formatReportTimestamp_returns_relative_labels_for_recent_data`() {
        assertEquals("son 1 saat icinde", formatReportTimestamp(NOW_MS - 20 * 60 * 1000L, NOW_MS))
        assertEquals("bugun", formatReportTimestamp(NOW_MS - 3 * 60 * 60 * 1000L, NOW_MS))
        assertEquals("dun", formatReportTimestamp(NOW_MS - 26 * 60 * 60 * 1000L, NOW_MS))
        assertEquals("3 gun once", formatReportTimestamp(NOW_MS - 3 * 24 * 60 * 60 * 1000L, NOW_MS))
    }

    @Test
    fun `buildReportsCenterEntries_marks_notification_report_empty_when_no_data`() {
        val entries = buildReportsCenterEntries(
            apps = listOf(
                app("pkg.a", "App A", notificationCount = 0),
                app("pkg.b", "App B", notificationCount = 0),
            ),
            categories = Category.getDefaultCategories(),
            wrappedEnabled = true,
            privacyReportEnabled = true,
            diagnosticsGenerating = false,
            nowMs = NOW_MS,
        )

        val notification = entries.first { it.route == "notification_report" }
        assertTrue(notification.lastUpdated.contains("Bos durum"))
    }

    private fun sampleApps(): List<AppInfo> = listOf(
        app(
            pkg = "pkg.alpha",
            name = "Alpha",
            lastUsed = NOW_MS - 2 * 60 * 60 * 1000L,
            lastUpdatedTime = NOW_MS - 90 * 60 * 1000L,
            notificationCount = 2,
        ),
        app(
            pkg = "pkg.beta",
            name = "Beta",
            hidden = true,
            lastUsed = NOW_MS - 3 * 24 * 60 * 60 * 1000L,
            lastUpdatedTime = NOW_MS - 2 * 24 * 60 * 60 * 1000L,
            notificationCount = 0,
        ),
    )

    private fun app(
        pkg: String,
        name: String,
        hidden: Boolean = false,
        lastUsed: Long = 0L,
        lastUpdatedTime: Long = 0L,
        notificationCount: Int = 0,
    ) = AppInfo(
        packageName = pkg,
        appName = name,
        categoryId = "social",
        isHidden = hidden,
        lastUsedTimestamp = lastUsed,
        lastUpdatedTime = lastUpdatedTime,
        lastUpdated = lastUpdatedTime,
        notificationCount = notificationCount,
    )

    companion object {
        private const val NOW_MS = 1_800_000_000_000L
    }
}
