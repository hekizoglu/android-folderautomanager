package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderChunksTest {

    private fun tiles(n: Int) = (1..n).map {
        FolderTileState(
            categoryId = "cat$it",
            title = "Cat $it",
            emoji = "📁",
            colorHex = "#000000",
            appCount = 1,
            previewPackages = emptyList(),
            notificationTotal = 0,
            hasUrgentNotification = false,
            quickLaunchPackage = null,
        )
    }

    @Test
    fun `chunks respect page size and column count`() {
        // pageSize 8, 2 sütun → sayfa başına 4 satır*2 = 8 klasör
        val pages = folderChunks(tiles(20), pageSize = 8, columns = 2)
        assertEquals(3, pages.size)
        assertEquals(8, pages[0].size)
        assertEquals(8, pages[1].size)
        assertEquals(4, pages[2].size)
    }

    @Test
    fun `page size rounds up to full rows`() {
        // pageSize 7, 2 sütun → ceil(7/2)=4 satır → 8 klasör/sayfa (tam satır korunur)
        val pages = folderChunks(tiles(9), pageSize = 7, columns = 2)
        assertEquals(2, pages.size)
        assertEquals(8, pages[0].size)
        assertEquals(1, pages[1].size)
    }

    @Test
    fun `empty or invalid input yields no pages`() {
        assertTrue(folderChunks(emptyList(), pageSize = 8, columns = 2).isEmpty())
        assertTrue(folderChunks(tiles(5), pageSize = 0, columns = 2).isEmpty())
        assertTrue(folderChunks(tiles(5), pageSize = 8, columns = 0).isEmpty())
    }
}
