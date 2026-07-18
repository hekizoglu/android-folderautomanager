package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döngü P14 — `buildHomePageIndicatorItems`in saf birim testleri (Compose/Android bağımlılığı
 * yoktur). `UnifiedHomePageIndicator`in Dashboard/klasör sayfası ayrımını doğru modellediğini,
 * seçili durumun doğru item'a düştüğünü ve `dashboardEnabledForPager=false` (Dashboard'sız plan)
 * durumunda tüm item'ların standart nokta (isDashboard=false) kaldığını (roadmap kabul kriteri:
 * "görünüm bugünkünden FARKSIZ") doğrular.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P14 (satır 1115-1171).
 */
class HomePageIndicatorItemTest {

    private fun folder(categoryId: String, order: Int = 0): AppFolder =
        AppFolder(
            category = Category(categoryId = categoryId, categoryName = categoryId, displayOrder = order),
            apps = emptyList(),
        )

    private fun folderPage(index: Int, categoryId: String?): HomePageSpec.FolderPage =
        HomePageSpec.FolderPage(
            pageIndex = index,
            firstFolderCategoryId = categoryId,
            folders = categoryId?.let { listOf(folder(it)) } ?: emptyList(),
        )

    @Test fun `bos sayfa listesi bos item listesi doner`() {
        assertEquals(emptyList<HomePageIndicatorItem>(), buildHomePageIndicatorItems(emptyList(), 0))
    }

    @Test fun `Dashboard artı 2 klasor sayfasi dogru item modeline cevrilir`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))

        val items = buildHomePageIndicatorItems(pages, selectedPageIndex = 0)

        assertEquals(3, items.size)
        assertTrue(items[0].isDashboard)
        assertEquals(null, items[0].folderNumber)
        assertFalse(items[1].isDashboard)
        assertEquals(1, items[1].folderNumber)
        assertFalse(items[2].isDashboard)
        assertEquals(2, items[2].folderNumber)
        // Dashboard hariç toplam klasör sayfası sayısı (M) her item'da aynı.
        items.forEach { assertEquals(2, it.folderPageCount) }
    }

    @Test fun `secili sayfa dogru item a isaretlenir digerleri secili degil`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))

        val items = buildHomePageIndicatorItems(pages, selectedPageIndex = 2)

        assertFalse(items[0].isSelected)
        assertFalse(items[1].isSelected)
        assertTrue(items[2].isSelected)
    }

    @Test fun `sinir disi selectedPageIndex guvenli sinira cekilir`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))

        val itemsOver = buildHomePageIndicatorItems(pages, selectedPageIndex = 99)
        assertTrue(itemsOver.last().isSelected)

        val itemsUnder = buildHomePageIndicatorItems(pages, selectedPageIndex = -3)
        assertTrue(itemsUnder.first().isSelected)
    }

    @Test fun `Dashboard kapali klasik modda tum itemlar standart nokta kalir (gorunum farksiz)`() {
        // dashboardEnabledForPager=false -> HomePagePlanner.buildPages Dashboard eklemez
        // (bkz. HomePagePlannerTest, HomePagerHostTest "Dashboard kapatildiginda..."), bu yüzden
        // pages listesinde hiç HomePageSpec.Dashboard elemanı yoktur.
        val pages = listOf(folderPage(0, "a"), folderPage(1, "b"), folderPage(2, "c"))

        val items = buildHomePageIndicatorItems(pages, selectedPageIndex = 1)

        assertEquals(3, items.size)
        items.forEach { assertFalse("isDashboard hep false olmali", it.isDashboard) }
        assertEquals(1, items[0].folderNumber)
        assertEquals(2, items[1].folderNumber)
        assertEquals(3, items[2].folderNumber)
        assertTrue(items[1].isSelected)
    }

    @Test fun `tek sayfa Dashboard olmadan bile item modeli uretir (composable pageCount lt 2 icin gizler)`() {
        // Not: gerçek gizleme kararı UnifiedHomePageIndicator composable'ında (pages.size <= 1
        // -> hiçbir şey çizilmez), saf model fonksiyonu her zaman veri üretir.
        val pages = listOf(folderPage(0, "a"))
        val items = buildHomePageIndicatorItems(pages, selectedPageIndex = 0)
        assertEquals(1, items.size)
        assertTrue(items[0].isSelected)
        assertEquals(1, items[0].folderPageCount)
    }

    @Test fun `categoryId olmayan bos klasor sayfasi da folderNumber alir (index bazli degil sira bazli)`() {
        val pages = listOf(folderPage(0, null), folderPage(1, "b"))
        val items = buildHomePageIndicatorItems(pages, selectedPageIndex = 0)
        assertEquals(1, items[0].folderNumber)
        assertEquals(2, items[1].folderNumber)
    }
}
