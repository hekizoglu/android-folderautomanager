package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Faz S2 — FolderFreeGrid'in dayandığı saf yardımcı fonksiyonlar (sütun sayısı hesabı,
 * itemId/screenIndex üretimi). Compose Layout/gesture kısmı bu projede unit test edilmiyor
 * (bkz. HomeLayoutMathTest yorumu) — sadece saf mantık kapsanıyor.
 */
class FolderFreeGridTest {

    // --- computeFreeGridColumns ---

    @Test
    fun `genis alanda birden fazla sutun sigar`() {
        // 320dp genislik, 76dp min hucre -> 4 sutun
        val columns = computeFreeGridColumns(availableWidth = 320.dp, minCellSize = 76.dp)
        assertEquals(4, columns)
    }

    @Test
    fun `dar alanda en az bir sutun garanti edilir`() {
        val columns = computeFreeGridColumns(availableWidth = 10.dp, minCellSize = 76.dp)
        assertEquals(1, columns)
    }

    @Test
    fun `sifir genislikte bile en az bir sutun doner`() {
        val columns = computeFreeGridColumns(availableWidth = 0.dp, minCellSize = 76.dp)
        assertEquals(1, columns)
    }

    @Test
    fun `tam bolunen genislik dogru sutun sayisini verir`() {
        // 456dp / 76dp = 6 tam
        val columns = computeFreeGridColumns(availableWidth = 456.dp, minCellSize = 76.dp)
        assertEquals(6, columns)
    }

    // --- folderScreenIndex ---

    @Test
    fun `ayni categoryId her zaman ayni screenIndex uretir`() {
        val a = folderScreenIndex("productivity")
        val b = folderScreenIndex("productivity")
        assertEquals(a, b)
    }

    @Test
    fun `farkli categoryId farkli screenIndex uretir`() {
        val a = folderScreenIndex("productivity")
        val b = folderScreenIndex("social")
        assert(a != b)
    }

    // --- folderGridItemId ---

    @Test
    fun `itemId klasor ve paket adini birlestirir`() {
        val id = folderGridItemId("productivity", "com.example.app")
        assertEquals("folder_productivity_com.example.app", id)
    }

    @Test
    fun `farkli klasorlerde ayni paket farkli itemId uretir`() {
        val idA = folderGridItemId("productivity", "com.example.app")
        val idB = folderGridItemId("social", "com.example.app")
        assert(idA != idB)
    }
}
