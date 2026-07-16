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
}
