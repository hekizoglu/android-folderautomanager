package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döngü P01 — HomePagePlanner saf fonksiyon testleri.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm 3.2 / Döngü P01.
 * HomeLayoutMath.pageCount ile çapraz tutarlılık da burada doğrulanır.
 */
class HomePagePlannerTest {

    @Test fun `hero uretim girisi dashboardu her zaman sayfa sifira koyar`() {
        val emptyPages = HomePagePlanner.buildHeroPages(emptyList(), pageSize = 8)
        val populatedPages = HomePagePlanner.buildHeroPages(folders(9), pageSize = 8)

        assertEquals(HomePageSpec.Dashboard, emptyPages.first())
        assertEquals(HomePageSpec.Dashboard, populatedPages.first())
    }

    private fun folder(categoryId: String, order: Int = 0): AppFolder =
        AppFolder(
            category = Category(
                categoryId = categoryId,
                categoryName = categoryId,
                displayOrder = order,
            ),
            apps = emptyList(),
        )

    private fun folders(count: Int): List<AppFolder> =
        (0 until count).map { folder("cat_$it", it) }

    // --- 0 klasör ---

    @Test
    fun `0 klasor Dashboard acik tek sayfa doner`() {
        val pages = HomePagePlanner.buildPages(folders = emptyList(), pageSize = 8, dashboardEnabled = true)

        assertEquals(1, pages.size)
        assertTrue(pages[0] is HomePageSpec.Dashboard)
    }

    @Test
    fun `0 klasor Dashboard kapali fallback bos klasor sayfasi doner`() {
        val pages = HomePagePlanner.buildPages(folders = emptyList(), pageSize = 8, dashboardEnabled = false)

        assertEquals(1, pages.size)
        val page = pages[0]
        assertTrue(page is HomePageSpec.FolderPage)
        page as HomePageSpec.FolderPage
        assertEquals(0, page.pageIndex)
        assertTrue(page.folders.isEmpty())
    }

    // --- 8 klasör / pageSize 8 ---

    @Test
    fun `8 klasor pageSize 8 Dashboard arti 1 klasor sayfasi`() {
        val pages = HomePagePlanner.buildPages(folders = folders(8), pageSize = 8, dashboardEnabled = true)

        assertEquals(2, pages.size)
        assertTrue(pages[0] is HomePageSpec.Dashboard)
        val folderPage = pages[1] as HomePageSpec.FolderPage
        assertEquals(8, folderPage.folders.size)
        assertEquals(0, folderPage.pageIndex)
    }

    // --- 9 klasör / pageSize 8 ---

    @Test
    fun `9 klasor pageSize 8 Dashboard arti 2 klasor sayfasi`() {
        val pages = HomePagePlanner.buildPages(folders = folders(9), pageSize = 8, dashboardEnabled = true)

        assertEquals(3, pages.size)
        assertTrue(pages[0] is HomePageSpec.Dashboard)
        val firstFolderPage = pages[1] as HomePageSpec.FolderPage
        val secondFolderPage = pages[2] as HomePageSpec.FolderPage
        assertEquals(8, firstFolderPage.folders.size)
        assertEquals(1, secondFolderPage.folders.size)
        assertEquals(0, firstFolderPage.pageIndex)
        assertEquals(1, secondFolderPage.pageIndex)
    }

    // --- Dashboard kapalı klasik mod ---

    @Test
    fun `Dashboard kapali klasik mod yalniz klasor sayfalari doner`() {
        val pages = HomePagePlanner.buildPages(folders = folders(9), pageSize = 8, dashboardEnabled = false)

        assertEquals(2, pages.size)
        assertTrue(pages.all { it is HomePageSpec.FolderPage })
        assertEquals(0, (pages[0] as HomePageSpec.FolderPage).pageIndex)
        assertEquals(1, (pages[1] as HomePageSpec.FolderPage).pageIndex)
    }

    // --- 1 / 16 / 17 klasör ek senaryolar ---

    @Test
    fun `1 klasor pageSize 8 Dashboard arti tek klasor sayfasi`() {
        val pages = HomePagePlanner.buildPages(folders = folders(1), pageSize = 8, dashboardEnabled = true)

        assertEquals(2, pages.size)
        assertEquals(1, (pages[1] as HomePageSpec.FolderPage).folders.size)
    }

    @Test
    fun `16 klasor pageSize 8 Dashboard arti 2 tam klasor sayfasi`() {
        val pages = HomePagePlanner.buildPages(folders = folders(16), pageSize = 8, dashboardEnabled = true)

        assertEquals(3, pages.size)
        assertEquals(8, (pages[1] as HomePageSpec.FolderPage).folders.size)
        assertEquals(8, (pages[2] as HomePageSpec.FolderPage).folders.size)
    }

    @Test
    fun `17 klasor pageSize 8 Dashboard arti 3 klasor sayfasi son sayfa 1 klasor`() {
        val pages = HomePagePlanner.buildPages(folders = folders(17), pageSize = 8, dashboardEnabled = true)

        assertEquals(4, pages.size)
        assertEquals(8, (pages[1] as HomePageSpec.FolderPage).folders.size)
        assertEquals(8, (pages[2] as HomePageSpec.FolderPage).folders.size)
        assertEquals(1, (pages[3] as HomePageSpec.FolderPage).folders.size)
    }

    // --- Stable key sözleşmesi ---

    @Test
    fun `Stable key klasor sirasina gore tutarli`() {
        val order1 = folders(9)
        val order2 = order1.reversed()

        val pages1 = HomePagePlanner.buildPages(order1, pageSize = 8, dashboardEnabled = true)
        val pages2 = HomePagePlanner.buildPages(order2, pageSize = 8, dashboardEnabled = true)

        // Aynı giriş sırasıyla tekrar çağrıldığında aynı stableKey listesi üretilmeli (deterministik).
        val pages1Again = HomePagePlanner.buildPages(order1, pageSize = 8, dashboardEnabled = true)
        assertEquals(pages1.map { it.stableKey }, pages1Again.map { it.stableKey })

        // Sıra değişince ilk klasör sayfasının stableKey'i de değişir (ilk kategori değiştiği için).
        assertTrue(pages1[1].stableKey != pages2[1].stableKey)
    }

    @Test
    fun `stableKey her zaman benzersiz`() {
        val pages = HomePagePlanner.buildPages(folders(17), pageSize = 8, dashboardEnabled = true)
        val keys = pages.map { it.stableKey }

        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `FolderPage stableKey ilk klasorun categoryId sini kullanir`() {
        val pages = HomePagePlanner.buildPages(folders(9), pageSize = 8, dashboardEnabled = true)

        val firstFolderPage = pages[1] as HomePageSpec.FolderPage
        assertEquals("folder:cat_0", firstFolderPage.stableKey)
    }

    // --- pageSize güvenlik ---

    @Test
    fun `pageSize sifir veya negatif gelirse guvenli minimum kullanilir`() {
        val zeroPages = HomePagePlanner.buildPages(folders(9), pageSize = 0, dashboardEnabled = true)
        val negativePages = HomePagePlanner.buildPages(folders(9), pageSize = -5, dashboardEnabled = true)

        // Güvenli minimum HomeLayoutMath.MIN_VISIBLE_FOLDERS (4) kullanılır; çakma/crash olmaz.
        assertTrue(zeroPages.size > 1)
        assertEquals(zeroPages.map { it.stableKey }, negativePages.map { it.stableKey })
    }

    // --- HomeLayoutMath.pageCount ile çapraz tutarlılık ---

    @Test
    fun `klasor sayfasi adedi HomeLayoutMath pageCount ile tutarli`() {
        // folderCount=0 haric: Dashboard acikken klasor yoksa ayri bos klasor sayfasi
        // uretilmez (Dashboard tek basina yeterli sayilir) — bu senaryo asagidaki ayri
        // testte ("0 klasor Dashboard acik tek sayfa doner") zaten dogrulanıyor.
        val scenarios = listOf(1, 8, 9, 16, 17)
        val pageSize = 8

        scenarios.forEach { folderCount ->
            val pages = HomePagePlanner.buildPages(folders(folderCount), pageSize = pageSize, dashboardEnabled = true)
            val folderPageCount = pages.count { it is HomePageSpec.FolderPage }
            val expected = HomeLayoutMath.pageCount(folderCount, pageSize)

            assertEquals(
                "folderCount=$folderCount icin klasor sayfasi adedi HomeLayoutMath.pageCount ile eslesmeli",
                expected,
                folderPageCount,
            )
        }
    }

    @Test
    fun `dashboard kapali klasik modda da HomeLayoutMath pageCount ile tutarli`() {
        val scenarios = listOf(0, 1, 8, 9, 16, 17)
        val pageSize = 8

        scenarios.forEach { folderCount ->
            val pages = HomePagePlanner.buildPages(folders(folderCount), pageSize = pageSize, dashboardEnabled = false)
            val expected = HomeLayoutMath.pageCount(folderCount, pageSize)

            assertEquals(
                "folderCount=$folderCount icin sayfa adedi HomeLayoutMath.pageCount ile eslesmeli",
                expected,
                pages.size,
            )
        }
    }
}
