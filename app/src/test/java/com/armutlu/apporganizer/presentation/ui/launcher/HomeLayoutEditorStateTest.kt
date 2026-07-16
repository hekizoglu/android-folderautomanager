package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeSectionId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
