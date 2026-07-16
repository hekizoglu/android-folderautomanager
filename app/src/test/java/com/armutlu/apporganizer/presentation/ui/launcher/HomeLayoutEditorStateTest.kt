package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeSectionId
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
        val moved = HomeLayoutConfig.DEFAULT.moveSection(HomeSectionId.MAIN_SEARCH, 1)

        assertEquals(2, moved.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.order)
        assertEquals(1, moved.items.single { it.sectionId == HomeSectionId.MISSIONS_AND_SCORE }.order)
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
}
