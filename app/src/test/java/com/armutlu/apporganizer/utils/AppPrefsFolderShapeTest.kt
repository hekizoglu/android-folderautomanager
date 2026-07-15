package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPrefsFolderShapeTest {

    @Test
    fun `yeni kurulumda varsayilan klasor sekli rounded doner`() {
        val result = AppPrefs.resolveFolderShapePreference(
            hasStoredValue = false,
            storedShape = "circle"
        )

        assertEquals(AppPrefs.DEFAULT_FOLDER_SHAPE, result)
        assertEquals("rounded", result)
    }

    @Test
    fun `kayitli klasor sekli varsa korunur`() {
        val result = AppPrefs.resolveFolderShapePreference(
            hasStoredValue = true,
            storedShape = "circle"
        )

        assertEquals("circle", result)
    }
}
