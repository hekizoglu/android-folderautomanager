package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPrefsIconScaleTest {

    @Test
    fun `yeni kurulum veya varsayilana don icon scale olarak 130 doner`() {
        val result = AppPrefs.resolveIconScalePreference(
            hasStoredValue = false,
            storedScale = 1.0f
        )

        assertEquals(AppPrefs.DEFAULT_ICON_SCALE, result, 0.0001f)
        assertEquals(1.3f, result, 0.0001f)
    }

    @Test
    fun `kayitli icon scale varsa korunur ve ezilmez`() {
        val result = AppPrefs.resolveIconScalePreference(
            hasStoredValue = true,
            storedScale = 1.0f
        )

        assertEquals(1.0f, result, 0.0001f)
    }
}
