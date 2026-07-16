package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLongPressSheetTest {
    @Test
    fun `edit action dismisses sheet before opening editor`() {
        val events = mutableListOf<String>()

        openHomeLayoutEditor(
            onDismiss = { events += "dismiss" },
            onEditHomeLayout = { events += "edit" },
        )

        assertEquals(listOf("dismiss", "edit"), events)
    }
}
