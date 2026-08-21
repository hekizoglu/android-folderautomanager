package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderQuickLaunchResolverTest {

    private fun app(pkg: String, launchCount: Long = 0L, lastUsed: Long = 0L, name: String = pkg, hidden: Boolean = false) =
        AppInfo(packageName = pkg, appName = name, launchCount = launchCount, lastUsedTimestamp = lastUsed, isHidden = hidden)

    @Test
    fun `most launched app wins`() {
        val apps = listOf(
            app("com.a", launchCount = 3),
            app("com.b", launchCount = 41),
            app("com.c", launchCount = 12),
        )
        assertEquals("com.b", FolderQuickLaunchResolver.resolve(apps)?.packageName)
    }

    @Test
    fun `hidden apps never win even with highest usage`() {
        val apps = listOf(
            app("com.hidden", launchCount = 999, hidden = true),
            app("com.visible", launchCount = 1),
        )
        assertEquals("com.visible", FolderQuickLaunchResolver.resolve(apps)?.packageName)
    }

    @Test
    fun `ties break by last used then name for determinism`() {
        val byLastUsed = listOf(
            app("com.old", launchCount = 5, lastUsed = 100),
            app("com.new", launchCount = 5, lastUsed = 200),
        )
        assertEquals("com.new", FolderQuickLaunchResolver.resolve(byLastUsed)?.packageName)

        val byName = listOf(
            app("com.zeta", launchCount = 5, lastUsed = 100),
            app("com.alpha", launchCount = 5, lastUsed = 100),
        )
        assertEquals("com.zeta", FolderQuickLaunchResolver.resolve(byName)?.packageName)
    }

    @Test
    fun `empty or fully hidden folder has no quick launch target`() {
        assertNull(FolderQuickLaunchResolver.resolve(emptyList()))
        assertNull(FolderQuickLaunchResolver.resolve(listOf(app("com.h", hidden = true))))
    }
}
