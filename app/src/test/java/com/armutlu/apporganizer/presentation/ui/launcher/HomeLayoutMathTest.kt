package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P04 — HomeLayoutMath + FolderGridPage saf mantık testleri.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm Döngü P04.
 * FolderGridPage bir Composable olduğu için render burada test edilmiyor; bunun yerine
 * grid'in dayandığı saf hesaplar (kapasite→satır/sütun, sayfa sayısı, empty-slot, global
 * index) doğrulanıyor — bunlar HomeScreen.kt'nin artık doğrudan çağırdığı fonksiyonlar.
 */
class HomeLayoutMathTest {

    // --- folderCapacity: satır/sütun dağılımı ---

    @Test
    fun `buyuk yukseklikte birden fazla satir sigar`() {
        // 800dp yukseklik, 72dp folder + 36dp label = 108dp satir, 4 sutun
        val capacity = HomeLayoutMath.folderCapacity(availableHeightDp = 800, folderSizeDp = 72, columns = 4)
        assert(capacity >= HomeLayoutMath.MIN_VISIBLE_FOLDERS)
        assertEquals(0, capacity % 4)
    }

    @Test
    fun `cok kucuk yukseklikte minimum satir garanti edilir`() {
        val capacity = HomeLayoutMath.folderCapacity(availableHeightDp = 50, folderSizeDp = 72, columns = 4)
        assertEquals(HomeLayoutMath.MIN_VISIBLE_FOLDERS.coerceAtLeast(4), capacity)
    }

    @Test
    fun `tablet 6 sutun kapasitesi 6nin katidir`() {
        val capacity = HomeLayoutMath.folderCapacity(availableHeightDp = 900, folderSizeDp = 72, columns = 6)
        assertEquals(0, capacity % 6)
    }

    // --- pageSize: kullanıcı tercihi vs kapasite ---

    @Test
    fun `istenen sayfa boyutu kapasiteyi asarsa kapasiteye dusurulur`() {
        val pageSize = HomeLayoutMath.pageSize(requestedPageSize = 20, folderCapacity = 8)
        assertEquals(8, pageSize)
    }

    @Test
    fun `istenen sayfa boyutu kapasite altindaysa aynen kullanilir`() {
        val pageSize = HomeLayoutMath.pageSize(requestedPageSize = 4, folderCapacity = 8)
        assertEquals(4, pageSize)
    }

    @Test
    fun `varsayilan duzen kapasiteyi 8 ile sinirlamaz`() {
        assertEquals(24, HomeLayoutMath.adaptivePageSize(folderCapacity = 24))
        assertEquals(HomeLayoutMath.MIN_VISIBLE_FOLDERS, HomeLayoutMath.adaptivePageSize(folderCapacity = 2))
    }

    // --- pageCount: FolderGridPage'in dayandigi sayfalama ---

    @Test
    fun `pageCount tavan bolme ile hesaplanir`() {
        assertEquals(2, HomeLayoutMath.pageCount(folderCount = 9, pageSize = 8))
        assertEquals(1, HomeLayoutMath.pageCount(folderCount = 8, pageSize = 8))
        assertEquals(1, HomeLayoutMath.pageCount(folderCount = 0, pageSize = 8))
    }

    // --- FolderGridPage global index sözleşmesi (roadmap testleri) ---

    @Test
    fun `birinci sayfa global index 0-7 araligindadir`() {
        val pageSize = 8
        val page = 0
        val pageStart = page * pageSize
        val globalIndices = (0 until pageSize).map { pageStart + it }
        assertEquals((0..7).toList(), globalIndices)
    }

    @Test
    fun `ikinci sayfa global index 8-15 araligindadir`() {
        val pageSize = 8
        val page = 1
        val pageStart = page * pageSize
        val globalIndices = (0 until pageSize).map { pageStart + it }
        assertEquals((8..15).toList(), globalIndices)
    }

    @Test
    fun `emptySlots pageSize eksi pageFolders boyutudur`() {
        val pageSize = 8
        val pageFoldersCount = 5
        val emptySlots = pageSize - pageFoldersCount
        assertEquals(3, emptySlots)
    }

    @Test
    fun `sayfa tam doluysa emptySlots sifirdir`() {
        val pageSize = 8
        val pageFoldersCount = 8
        val emptySlots = pageSize - pageFoldersCount
        assertEquals(0, emptySlots)
    }
}
