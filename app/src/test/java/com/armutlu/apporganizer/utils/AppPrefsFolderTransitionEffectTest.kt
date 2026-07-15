package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPrefsFolderTransitionEffectTest {

    @Test
    fun `legacy transition values migrate to android smooth`() {
        assertEquals(
            AppPrefs.FOLDER_TRANSITION_ANDROID_SMOOTH,
            AppPrefs.resolveFolderTransitionEffectPreference("page_turn")
        )
        assertEquals(
            AppPrefs.FOLDER_TRANSITION_ANDROID_SMOOTH,
            AppPrefs.resolveFolderTransitionEffectPreference("slide_parallax")
        )
    }

    @Test
    fun `ios zoom fade stays selectable`() {
        assertEquals(
            AppPrefs.FOLDER_TRANSITION_IOS_ZOOM_FADE,
            AppPrefs.resolveFolderTransitionEffectPreference("zoom_fade")
        )
        assertEquals(
            AppPrefs.FOLDER_TRANSITION_IOS_ZOOM_FADE,
            AppPrefs.resolveFolderTransitionEffectPreference(AppPrefs.FOLDER_TRANSITION_IOS_ZOOM_FADE)
        )
    }
}
