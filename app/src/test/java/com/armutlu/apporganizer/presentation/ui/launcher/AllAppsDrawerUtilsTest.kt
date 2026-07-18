package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllAppsDrawerUtilsTest {

    @Test
    fun `drawer usage formatter uses foreground duration`() {
        assertEquals("1,0 sa", formatUsageMs(3_600_000L))
    }

    @Test
    fun `usage sidebar uses launch count instead of duration`() {
        val apps = listOf(
            AppInfo("com.long", "Long", usageCount = 1_000L, launchCount = 120L),
            AppInfo("com.often", "Often", usageCount = 1L, launchCount = 8L),
            AppInfo("com.rare", "Rare", usageCount = 9_999_999L, launchCount = 0L),
        )

        val entries = buildSidebarEntries(apps, AllAppsSortMode.USAGE)

        assertTrue(entries.any { it.label == "100x" && it.scrollIndex == 1 })
        assertTrue(entries.any { it.label == "5x" && it.scrollIndex == 2 })
        assertTrue(entries.none { it.label == "100x" && it.scrollIndex == 0 })
    }
}
