package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import com.armutlu.apporganizer.domain.models.withSearchZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutEditorStateTest {
    @Test
    fun freshDraftHasNoUnsavedChanges() {
        assertFalse(HomeLayoutEditorState(HomeLayoutConfig.DEFAULT).hasUnsavedChanges)
    }

    @Test
    fun changedDraftRequiresDiscardProtection() {
        val changed = HomeLayoutConfig.DEFAULT.copy(items = HomeLayoutConfig.DEFAULT.items.map {
            if (it.sectionId == HomeSectionId.FAVORITES) it.copy(visible = false) else it
        })

        assertTrue(HomeLayoutEditorState(HomeLayoutConfig.DEFAULT, changed).hasUnsavedChanges)
    }

    @Test
    fun optionalSectionCanBeHiddenAndShown() {
        val hidden = HomeLayoutConfig.DEFAULT.withSectionVisibility(HomeSectionId.FAVORITES, false)
        assertFalse(hidden.items.single { it.sectionId == HomeSectionId.FAVORITES }.visible)
        assertTrue(hidden.withSectionVisibility(HomeSectionId.FAVORITES, true).items.single {
            it.sectionId == HomeSectionId.FAVORITES
        }.visible)
    }

    @Test
    fun requiredSectionsCannotBeHidden() {
        val changed = HomeLayoutConfig.DEFAULT.withSectionVisibility(HomeSectionId.FOLDER_GRID, false)
        assertEquals(HomeLayoutConfig.DEFAULT, changed)
    }

    @Test
    fun resetOnlyReplacesLayoutDraftWithDefault() {
        val changed = HomeLayoutConfig.DEFAULT.withSectionVisibility(HomeSectionId.FAVORITES, false)
        val state = HomeLayoutEditorState(HomeLayoutConfig.DEFAULT, changed).resetDraft()
        assertEquals(HomeLayoutConfig.DEFAULT, state.draft)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun movableSectionReordersAndPersistsAsDraftOrder() {
        // P15 v2: MAIN_SEARCH is now the sole HEADER-zone section (moveSection only reorders within
        // a zone), so this test moves a CONTENT-zone section pair instead (CLOCK, MISSIONS_AND_SCORE).
        val moved = HomeLayoutConfig.DEFAULT.moveSection(HomeSectionId.CLOCK, 1)

        assertEquals(1, moved.items.single { it.sectionId == HomeSectionId.CLOCK }.order)
        assertEquals(0, moved.items.single { it.sectionId == HomeSectionId.MISSIONS_AND_SCORE }.order)
        assertTrue(HomeLayoutEditorState(HomeLayoutConfig.DEFAULT, moved).hasUnsavedChanges)
    }

    @Test
    fun fixedAndRestrictedSectionsCannotReorder() {
        assertEquals(HomeLayoutConfig.DEFAULT, HomeLayoutConfig.DEFAULT.moveSection(HomeSectionId.DOCK, -1))
        assertEquals(HomeLayoutConfig.DEFAULT, HomeLayoutConfig.DEFAULT.moveSection(HomeSectionId.FOLDER_GRID, -1))
    }

    @Test
    fun reorderDoesNotCrossZoneBoundary() {
        val lastHeader = HomeLayoutConfig.DEFAULT.items.filter { it.zone.name == "HEADER" && it.visible }.maxBy { it.order }
        assertEquals(HomeLayoutConfig.DEFAULT, HomeLayoutConfig.DEFAULT.moveSection(lastHeader.sectionId, 1))
    }

    @Test
    fun accessibilityMoveToTopStopsAtZoneBoundary() {
        val movedDown = HomeLayoutConfig.DEFAULT.moveSection(HomeSectionId.CLOCK, 1)
            .moveSection(HomeSectionId.CLOCK, 1)
        val movedTop = movedDown.moveSectionToZoneStart(HomeSectionId.CLOCK)

        assertEquals(0, movedTop.items.single { it.sectionId == HomeSectionId.CLOCK }.order)
        assertEquals(HomeLayoutConfig.DEFAULT, HomeLayoutConfig.DEFAULT.moveSectionToZoneStart(HomeSectionId.DOCK))
    }

    @Test
    fun dockReorderSupportsMixedAppAndFolderItems() {
        val items = listOf("app.one", "folder:social", "app.two")

        assertEquals(listOf("folder:social", "app.one", "app.two"), moveDockItem(items, "folder:social", -1))
        assertEquals(listOf("app.one", "app.two", "folder:social"), moveDockItem(items, "folder:social", 1))
    }

    @Test
    fun dockReorderKeepsListAtBoundariesAndForUnknownItems() {
        val items = listOf("app.one", "folder:social")

        assertEquals(items, moveDockItem(items, "app.one", -1))
        assertEquals(items, moveDockItem(items, "folder:social", 1))
        assertEquals(items, moveDockItem(items, "suggested.app", 1))
    }

    // P16 — HomeLayoutConfig.dashboardSectionItems(): editörün "Akıllı Ana Ekran bölümleri"
    // listesi. FOLDER_GRID kart olarak kaldırıldığı için bu listede hiç görünmemeli (roadmap
    // madde 2), MAIN_SEARCH/DOCK de "Global alanlar" bölümünde ayrı yönetildiği için burada yer
    // almamalı.

    @Test
    fun dashboardSectionItemsExcludesFolderGridSearchAndDock() {
        val sectionIds = HomeLayoutConfig.DEFAULT.dashboardSectionItems().map { it.sectionId }

        assertFalse(sectionIds.contains(HomeSectionId.FOLDER_GRID))
        assertFalse(sectionIds.contains(HomeSectionId.MAIN_SEARCH))
        assertFalse(sectionIds.contains(HomeSectionId.DOCK))
        assertTrue(sectionIds.contains(HomeSectionId.CLOCK))
        assertTrue(sectionIds.contains(HomeSectionId.FAVORITES))
    }

    @Test
    fun dashboardSectionItemsOnlyContainsContentZoneItems() {
        val items = HomeLayoutConfig.DEFAULT.dashboardSectionItems()

        assertTrue(items.all { it.zone == HomeLayoutZone.CONTENT })
    }

    @Test
    fun dashboardSectionReorderStaysWithinContentZoneAfterFolderGridIsSkipped() {
        // FOLDER_GRID (RESTRICTED, order 10) sits after the visible CONTENT defaults; moving the
        // last visible dashboard section (RECENT_APPS, order 5) forward must no-op because
        // FOLDER_GRID isn't movable — it never leaves the CONTENT zone or reorders past the grid.
        val moved = HomeLayoutConfig.DEFAULT.moveSection(HomeSectionId.RECENT_APPS, 1)

        assertEquals(HomeLayoutConfig.DEFAULT, moved)
    }

    // P16 — HomeLayoutConfig.withSearchZone(): editörün MAIN_SEARCH için tek etkileşimi (üst/alt
    // konum seçimi), DOCK ve diğer öğeleri etkilemeden yalnız MAIN_SEARCH'ün zone'unu değiştirir.

    @Test
    fun withSearchZoneMovesMainSearchToFooterAndBackToHeader() {
        val toFooter = HomeLayoutConfig.DEFAULT.withSearchZone(HomeLayoutZone.FOOTER)
        assertEquals(HomeLayoutZone.FOOTER, toFooter.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.zone)
        assertEquals(0, toFooter.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.order)

        val backToHeader = toFooter.withSearchZone(HomeLayoutZone.HEADER)
        assertEquals(HomeLayoutZone.HEADER, backToHeader.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.zone)
    }

    @Test
    fun withSearchZoneIsNoOpWhenAlreadyInTargetZone() {
        assertEquals(HomeLayoutConfig.DEFAULT, HomeLayoutConfig.DEFAULT.withSearchZone(HomeLayoutZone.HEADER))
    }

    @Test
    fun withSearchZoneDoesNotAffectDockOrOtherSections() {
        val toFooter = HomeLayoutConfig.DEFAULT.withSearchZone(HomeLayoutZone.FOOTER)
        val dockItem = toFooter.items.single { it.sectionId == HomeSectionId.DOCK }
        val defaultDockItem = HomeLayoutConfig.DEFAULT.items.single { it.sectionId == HomeSectionId.DOCK }

        assertEquals(defaultDockItem.zone, dockItem.zone)
        val otherSectionsUnchanged = HomeLayoutConfig.DEFAULT.items
            .filterNot { it.sectionId == HomeSectionId.MAIN_SEARCH }
        val otherSectionsAfter = toFooter.items.filterNot { it.sectionId == HomeSectionId.MAIN_SEARCH }
        assertEquals(otherSectionsUnchanged, otherSectionsAfter)
    }
}
