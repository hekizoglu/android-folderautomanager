package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetEditModeTest {
    @Test fun normalModeKeepsWidgetsLiveAndDisablesReorder() {
        assertEquals(WidgetInteractionMode.LIVE, widgetInteractionMode(editMode = false, widgetCount = 3))
    }

    @Test fun editModeBlocksSingleWidgetAndEnablesMultipleWidgetReorder() {
        assertEquals(WidgetInteractionMode.EDIT_SINGLE, widgetInteractionMode(editMode = true, widgetCount = 0))
        assertEquals(WidgetInteractionMode.EDIT_SINGLE, widgetInteractionMode(editMode = true, widgetCount = 1))
        assertEquals(WidgetInteractionMode.EDIT_REORDER, widgetInteractionMode(editMode = true, widgetCount = 2))
    }

    @Test fun widgetMoveHandlesEmptySingleAndMultipleLists() {
        assertEquals(emptyList<Int>(), moveWidget(emptyList(), 1, 1))
        assertEquals(listOf(7), moveWidget(listOf(7), 7, 1))
        assertEquals(listOf(2, 1, 3), moveWidget(listOf(1, 2, 3), 2, -1))
        assertEquals(listOf(1, 3, 2), moveWidget(listOf(1, 2, 3), 2, 1))
        assertEquals(listOf(1, 2), moveWidget(listOf(1, 2), 99, 1))
    }
}
