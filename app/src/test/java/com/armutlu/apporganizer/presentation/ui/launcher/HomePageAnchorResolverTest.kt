package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P02 — HomePageAnchorResolver saf fonksiyon testleri (anchor + sayfa planı -> index).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P02 (satır 449-501).
 */
class HomePageAnchorResolverTest {

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

    @Test fun `empty pages always resolves to 0`() {
        assertEquals(0, HomePageAnchorResolver.resolve(emptyList(), HomePageAnchor.Dashboard))
        assertEquals(0, HomePageAnchorResolver.resolve(emptyList(), HomePageAnchor.Folder("x")))
        assertEquals(0, HomePageAnchorResolver.resolve(emptyList(), HomePageAnchor.PageIndex(5)))
    }

    @Test fun `dashboard anchor resolves to dashboard page when present`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(0, HomePageAnchorResolver.resolve(pages, HomePageAnchor.Dashboard))
    }

    @Test fun `dashboard anchor but dashboard disabled falls back to first folder page`() {
        val pages = listOf(folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(0, HomePageAnchorResolver.resolve(pages, HomePageAnchor.Dashboard))
    }

    @Test fun `folder anchor resolves to matching folder page`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(2, HomePageAnchorResolver.resolve(pages, HomePageAnchor.Folder("b")))
    }

    @Test fun `folder anchor for deleted folder falls back to dashboard`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(0, HomePageAnchorResolver.resolve(pages, HomePageAnchor.Folder("deleted_cat")))
    }

    @Test fun `folder anchor for deleted folder without dashboard falls back to first page`() {
        val pages = listOf(folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(0, HomePageAnchorResolver.resolve(pages, HomePageAnchor.Folder("deleted_cat")))
    }

    @Test fun `page index within bounds resolves directly`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(1, HomePageAnchorResolver.resolve(pages, HomePageAnchor.PageIndex(1)))
    }

    @Test fun `page index out of upper bound clamps to last page`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"), folderPage(1, "b"))
        assertEquals(2, HomePageAnchorResolver.resolve(pages, HomePageAnchor.PageIndex(99)))
    }

    @Test fun `negative page index clamps to 0`() {
        val pages = listOf(HomePageSpec.Dashboard, folderPage(0, "a"))
        assertEquals(0, HomePageAnchorResolver.resolve(pages, HomePageAnchor.PageIndex(-5)))
    }
}
