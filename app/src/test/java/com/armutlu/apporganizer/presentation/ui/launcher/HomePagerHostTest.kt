package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P05 — `HomePagerHost`in saf yardımcı fonksiyonlarının (`anchorForCurrentPage`,
 * `clampPageToSafeBounds`) birim testleri. Compose `HorizontalPager` gövdesi (key/fade/scale)
 * derleme + mevcut instrumented/manuel testlerle doğrulanır — bu dosya sadece Android/Compose
 * bağımlılığı olmayan saf mantığı kapsar (HomePagePlannerTest.kt / HomePageAnchorResolverTest.kt
 * ile aynı desen).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P05 (satır 611-670).
 */
class HomePagerHostTest {

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

    // ── spec -> sayfa index eşlemesi / anchor yazma yönü (anchorForCurrentPage) ────────────

    @Test fun `ilk sayfa Dashboard ise anchor Dashboard doner`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(HomePageAnchor.Dashboard, anchorForCurrentPage(pages, 0))
    }

    @Test fun `ikinci sayfa ilk klasor chunk ise anchor Folder ile categoryId tasir`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(HomePageAnchor.Folder("a"), anchorForCurrentPage(pages, 1))
        assertEquals(HomePageAnchor.Folder("b"), anchorForCurrentPage(pages, 2))
    }

    @Test fun `Dashboard kapatildiginda page 0 ilk klasor sayfasi olur`() {
        // dashboardEnabled=false -> HomePagePlanner.buildPages Dashboard eklemez, ilk eleman
        // doğrudan ilk FolderPage olur (bkz. HomePagePlannerTest "Dashboard acik/kapali").
        val pages = listOf(folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(HomePageAnchor.Folder("a"), anchorForCurrentPage(pages, 0))
    }

    @Test fun `categoryId olmayan bos klasor sayfasi PageIndex fallback kullanir`() {
        val pages = listOf(folderPage(0, null))
        assertEquals(HomePageAnchor.PageIndex(0), anchorForCurrentPage(pages, 0))
    }

    @Test fun `sinir disi rawPage guvenli sinira cekilip cozulur`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))
        // rawPage=99 -> clamp edilip son sayfaya (index 1, folder a) düşer.
        assertEquals(HomePageAnchor.Folder("a"), anchorForCurrentPage(pages, 99))
        assertEquals(HomePageAnchor.Dashboard, anchorForCurrentPage(pages, -5))
    }

    @Test fun `bos sayfa listesinde PageIndex 0 doner`() {
        assertEquals(HomePageAnchor.PageIndex(0), anchorForCurrentPage(emptyList(), 3))
    }

    // ── Page listesi değiştiğinde current page güvenli sınıra çekilme (clampPageToSafeBounds) ─

    @Test fun `liste kucculunce current page son sayfaya cekilir`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))
        assertEquals(1, clampPageToSafeBounds(pages, 5))
    }

    @Test fun `liste bosaltilirsa 0 doner`() {
        assertEquals(0, clampPageToSafeBounds(emptyList(), 4))
    }

    @Test fun `negatif current page 0 a cekilir`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(0, clampPageToSafeBounds(pages, -1))
    }

    @Test fun `sinirlar icindeki current page degismez`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(1, clampPageToSafeBounds(pages, 1))
    }

    // ── Anchor yazma + HomePageAnchorResolver okuma köprüsü tutarlılığı (round-trip) ────────

    @Test fun `yazilan anchor ayni sayfa planinda ayni index e cozulur (round trip)`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        for (index in pages.indices) {
            val anchor = anchorForCurrentPage(pages, index)
            assertEquals(
                "index $index icin round-trip basarisiz",
                index,
                HomePageAnchorResolver.resolve(pages, anchor)
            )
        }
    }
}
