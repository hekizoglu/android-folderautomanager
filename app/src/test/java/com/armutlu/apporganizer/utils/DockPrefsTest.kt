package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DockPrefsTest {

    @Test
    fun buildHeroDockItems_returns4Slots_leavesSlot5Empty() {
        val current = emptyList<String>()
        val fallbackPackages = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.google.android.GoogleCamera",
            "com.android.chrome"
        )

        val result = DockPrefs.buildHeroDockItems(current, fallbackPackages)

        assertEquals(4, result.size)
        assertEquals(fallbackPackages.take(4), result)
    }

    @Test
    fun buildHeroDockItems_capsAt4_ignoresExtraFallbacks() {
        val current = emptyList<String>()
        val fallbackPackages = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.google.android.GoogleCamera",
            "com.android.chrome",
            "com.example.extra"  // 5th slot should be ignored
        )

        val result = DockPrefs.buildHeroDockItems(current, fallbackPackages)

        assertEquals(4, result.size)
        assertEquals(
            listOf(
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.google.android.GoogleCamera",
                "com.android.chrome"
            ),
            result
        )
    }

    @Test
    fun sanitizeHeroDockItems_returns4Slots_leavesSlot5Empty() {
        val items = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.google.android.GoogleCamera",
            "com.android.chrome",
            "com.example.extra"  // Should be ignored
        )

        val result = DockPrefs.sanitizeHeroDockItems(items)

        assertEquals(4, result.size)
        assertEquals(
            listOf(
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.google.android.GoogleCamera",
                "com.android.chrome"
            ),
            result
        )
    }

    @Test
    fun sanitizeHeroDockItems_removesBlankAndFolder_thenCapsAt4() {
        val items = listOf(
            "com.google.android.dialer",
            "",  // blank
            "folder:CAT_WORK",  // folder item
            "com.google.android.apps.messaging",
            "com.google.android.GoogleCamera",
            "com.android.chrome"
        )

        val result = DockPrefs.sanitizeHeroDockItems(items)

        assertEquals(4, result.size)
        assertEquals(
            listOf(
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.google.android.GoogleCamera",
                "com.android.chrome"
            ),
            result
        )
    }
}
