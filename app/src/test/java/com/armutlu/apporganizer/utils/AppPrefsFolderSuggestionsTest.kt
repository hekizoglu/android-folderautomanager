package com.armutlu.apporganizer.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPrefsFolderSuggestionsTest {

    @Test
    fun `yeni kurulumda klasor onerileri varsayilan olarak acik doner`() {
        val result = AppPrefs.resolveFolderSuggestionsEnabled(
            hasStoredValue = false,
            storedValue = false
        )

        assertTrue(result)
    }

    @Test
    fun `kayitli klasor onerisi tercihi varsa korunur`() {
        val result = AppPrefs.resolveFolderSuggestionsEnabled(
            hasStoredValue = true,
            storedValue = false
        )

        assertFalse(result)
    }
}
