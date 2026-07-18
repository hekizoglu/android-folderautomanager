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

    // ── Döngü P13 — plan degisince current page semantik yeniden cozumu (resolvePageAfterPlanChange) ─

    @Test fun `klasor reorder sonrasi ayni klasorun yeni index ine takip eder`() {
        // Onceki plan: Dashboard, a, b, c -> kullanici "b" sayfasinda (index 2).
        val previousPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"), folderPage(2, "c"))
        // Reorder sonrasi: b en basa alindi -> Dashboard, b, a, c.
        val newPages = listOf(HomePageSpec.Dashboard, folderPage(0, "b"), folderPage(1, "a"), folderPage(2, "c"))

        val resolved = resolvePageAfterPlanChange(previousPages, previousPageIndex = 2, newPages = newPages)

        assertEquals(1, resolved) // "b" artik index 1'de -> oraya takip eder, index 2'de kalmaz (c olurdu).
    }

    @Test fun `page size degisince ayni klasoru iceren yeni chunk a cozulur`() {
        // pageSize 8 -> tek FolderPage: Dashboard, [a,b,c,d,e]. Kullanici "d" nin sayfasinda (index 1).
        val previousPages = listOf(
            HomePageSpec.Dashboard,
            HomePageSpec.FolderPage(pageIndex = 0, firstFolderCategoryId = "a", folders = listOf(folder("a"), folder("b"), folder("c"), folder("d"), folder("e"))),
        )
        // pageSize 8 -> 4: Dashboard, [a,b,c,d], [e]. "d" hala ilk chunk'ta (firstFolderCategoryId "a").
        val newPages = listOf(
            HomePageSpec.Dashboard,
            HomePageSpec.FolderPage(pageIndex = 0, firstFolderCategoryId = "a", folders = listOf(folder("a"), folder("b"), folder("c"), folder("d"))),
            HomePageSpec.FolderPage(pageIndex = 1, firstFolderCategoryId = "e", folders = listOf(folder("e"))),
        )

        val resolved = resolvePageAfterPlanChange(previousPages, previousPageIndex = 1, newPages = newPages)

        // Onceki sayfanin anchor'i Folder("a") (chunk'in ilk klasoru) -> yeni planda hala index 1.
        assertEquals(1, resolved)
    }

    @Test fun `goruntulenen klasor silinince ayni chunk taki baska klasore degil dashboard a duser`() {
        // Onceki plan: Dashboard, a, b. Kullanici "b" sayfasinda (index 2).
        val previousPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        // "b" silindi -> yeni plan: Dashboard, a.
        val newPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))

        val resolved = resolvePageAfterPlanChange(previousPages, previousPageIndex = 2, newPages = newPages)

        assertEquals(0, resolved) // HomePageAnchorResolver kurali: silinen klasor -> Dashboard (varsa).
    }

    @Test fun `dashboard kapatilinca ilk klasor sayfasina duser`() {
        val previousPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        val newPages = listOf(folderPage(0, "a"), folderPage(1, "b")) // dashboardEnabled=false

        val resolved = resolvePageAfterPlanChange(previousPages, previousPageIndex = 0, newPages = newPages)

        assertEquals(0, resolved) // Dashboard anchor + Dashboard yok -> ilk sayfa.
    }

    @Test fun `dashboard acilinca kullanicinin klasoru korunur ilk sayfaya zorlanmaz`() {
        val previousPages = listOf(folderPage(0, "a"), folderPage(1, "b"))
        val newPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))

        val resolved = resolvePageAfterPlanChange(previousPages, previousPageIndex = 1, newPages = newPages)

        assertEquals(2, resolved) // "b" klasoru yeni planda index 2'ye kaydi -> oraya takip eder.
    }

    @Test fun `plan degismezse (ayni stableKey dizisi) index degismeden dondurulur`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        val resolved = resolvePageAfterPlanChange(pages, previousPageIndex = 2, newPages = pages)
        assertEquals(2, resolved)
    }

    @Test fun `onceki plan bossa (ilk composition) previousPageIndex guvenli sinira cekilerek kullanilir`() {
        val newPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))
        assertEquals(1, resolvePageAfterPlanChange(emptyList(), previousPageIndex = 1, newPages = newPages))
        assertEquals(1, resolvePageAfterPlanChange(emptyList(), previousPageIndex = 99, newPages = newPages)) // clamp
    }

    @Test fun `yeni plan bossa 0 doner`() {
        val previousPages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))
        assertEquals(0, resolvePageAfterPlanChange(previousPages, previousPageIndex = 1, newPages = emptyList()))
    }

    @Test fun `bos klasor sayfasindaki PageIndex fallback yeni planda da index korur`() {
        // categoryId olmayan (bos) klasor sayfasi anchorForCurrentPage'de PageIndex fallback kullanir.
        val previousPages = listOf(folderPage(0, null))
        val newPages = listOf(folderPage(0, null), folderPage(1, "a"))
        assertEquals(0, resolvePageAfterPlanChange(previousPages, previousPageIndex = 0, newPages = newPages))
    }

    // ── Döngü P19 — TalkBack sayfa başlığı (homePagerCurrentPageDescription) ────────────────

    private fun folderLabelFormat(n: Int, m: Int) = "Klasör sayfası $n / $m"

    @Test fun `Dashboard sayfasindayken dashboard etiketi doner`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        val result = homePagerCurrentPageDescription(pages, 0, "Akıllı Ana Ekran", ::folderLabelFormat)
        assertEquals("Akıllı Ana Ekran", result)
    }

    @Test fun `klasor sayfasindayken N M formatinda etiket doner`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals("Klasör sayfası 1 / 2", homePagerCurrentPageDescription(pages, 1, "Akıllı Ana Ekran", ::folderLabelFormat))
        assertEquals("Klasör sayfası 2 / 2", homePagerCurrentPageDescription(pages, 2, "Akıllı Ana Ekran", ::folderLabelFormat))
    }

    @Test fun `Dashboard kapaliyken ilk sayfa klasor etiketi 1 M doner`() {
        val pages = listOf(folderPage(0, "a"), folderPage(1, "b"))
        assertEquals("Klasör sayfası 1 / 2", homePagerCurrentPageDescription(pages, 0, "Akıllı Ana Ekran", ::folderLabelFormat))
    }

    @Test fun `bos sayfa listesinde dashboard etiketi fallback olarak doner`() {
        val result = homePagerCurrentPageDescription(emptyList(), 0, "Akıllı Ana Ekran", ::folderLabelFormat)
        assertEquals("Akıllı Ana Ekran", result)
    }

    @Test fun `sinir disi currentPage guvenli sinira cekilip dogru etiketi uretir`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        // rawPage=99 -> clamp edilip son sayfaya (index 2, klasor b) duser.
        assertEquals("Klasör sayfası 2 / 2", homePagerCurrentPageDescription(pages, 99, "Akıllı Ana Ekran", ::folderLabelFormat))
    }
}
