package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderEditModeTest {
    @Test fun normalModeKeepsContextMenuAndDisablesDrag() {
        assertEquals(FolderGestureMode.CONTEXT_MENU, folderGestureMode(editMode = false))
    }

    @Test fun editModeEnablesReorder() {
        assertEquals(FolderGestureMode.REORDER, folderGestureMode(editMode = true))
        assertEquals(listOf("b", "a", "c"), moveFolder(listOf("a", "b", "c"), "b", -1))
    }

    @Test fun folderMoveDoesNotCrossBounds() {
        val ids = listOf("a", "b")
        assertEquals(ids, moveFolder(ids, "a", -1))
        assertEquals(ids, moveFolder(ids, "b", 1))
    }
}
