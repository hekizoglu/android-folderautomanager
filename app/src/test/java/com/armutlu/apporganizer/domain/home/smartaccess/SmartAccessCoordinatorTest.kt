package com.armutlu.apporganizer.domain.home.smartaccess

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartAccessCoordinatorTest {
    private fun app(pkg: String, lastUsed: Long = 1L, hidden: Boolean = false) = AppInfo(
        packageName = pkg,
        appName = pkg,
        lastUsedTimestamp = lastUsed,
        isHidden = hidden,
    )

    @Test fun `zaman dilimi sinyali deterministik siralanir`() {
        val result = SmartAccessCoordinator.rankNow(
            apps = listOf(app("first"), app("second")),
            slotPackages = listOf("first", "second"),
            frequencyScores = emptyMap(),
            weekdayScores = emptyMap(),
            ownPackageName = "launcher",
            nowMillis = 10L,
        )
        assertEquals(listOf("first", "second"), result.map { it.packageName })
    }

    @Test fun `fallback tekrar ve gizli uygulama uretmez`() {
        val visible = app("visible", 9L)
        val result = SmartAccessCoordinator.rankNow(
            apps = listOf(app("hidden", hidden = true)),
            slotPackages = emptyList(),
            frequencyScores = emptyMap(),
            weekdayScores = emptyMap(),
            ownPackageName = "launcher",
            nowMillis = 10L,
            favoritesFallback = listOf(visible),
            recentFallback = listOf(visible),
        )
        assertEquals(listOf("visible"), result.map { it.packageName })
    }
}
