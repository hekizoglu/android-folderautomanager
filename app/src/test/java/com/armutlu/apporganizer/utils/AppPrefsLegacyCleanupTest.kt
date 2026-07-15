package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AppPrefsLegacyCleanupTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        every { editor.remove("folder_blur") } returns editor

        prefs = mockk(relaxed = true)
        every { prefs.edit() } returns editor

        context = mockk(relaxed = true)
        every { context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
    }

    @Test
    fun `clearLegacyFolderBlurPreference removes deprecated key`() {
        AppPrefs.clearLegacyFolderBlurPreference(context)

        verify(exactly = 1) { editor.remove("folder_blur") }
        verify(exactly = 1) { editor.apply() }
    }
}
