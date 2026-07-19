package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCacheTest {

    @Test
    fun `app search matches package and app file aliases`() {
        SearchCache.warmApps(
            listOf(
                AppInfo(
                    packageName = "com.google.chromeremotedesktop",
                    appName = "Uzak Masaustu",
                    appFileName = "remote-desktop"
                )
            )
        )

        val results = SearchCache.searchApps("remote desktop", maxResults = 5)

        assertTrue(results.isNotEmpty())
        assertEquals("com.google.chromeremotedesktop", results.first().packageName)
    }
}
