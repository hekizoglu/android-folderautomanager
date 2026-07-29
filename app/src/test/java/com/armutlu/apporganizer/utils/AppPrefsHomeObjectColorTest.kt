package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPrefsHomeObjectColorTest {

    @Test
    fun `gecerli mod oldugu gibi korunur`() {
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_DARK, AppPrefs.resolveHomeObjectColorMode("dark"))
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_LIGHT, AppPrefs.resolveHomeObjectColorMode("light"))
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_CUSTOM, AppPrefs.resolveHomeObjectColorMode("custom"))
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_AUTO, AppPrefs.resolveHomeObjectColorMode("auto"))
    }

    @Test
    fun `null mod auto varsayilanina duser`() {
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_AUTO, AppPrefs.resolveHomeObjectColorMode(null))
    }

    @Test
    fun `bilinmeyen veya bozuk mod auto varsayilanina duser`() {
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_AUTO, AppPrefs.resolveHomeObjectColorMode("rainbow"))
        assertEquals(AppPrefs.HOME_OBJECT_COLOR_MODE_AUTO, AppPrefs.resolveHomeObjectColorMode(""))
    }

    @Test
    fun `ozel renk alpha kanali her zaman opak zorlanir`() {
        val semiTransparent = 0x11223344
        val result = AppPrefs.resolveHomeObjectCustomColor(semiTransparent)

        assertEquals(0xFF, (result ushr 24) and 0xFF)
        // RGB kanalları değişmeden korunur.
        assertEquals(0x22, (result ushr 16) and 0xFF)
        assertEquals(0x33, (result ushr 8) and 0xFF)
        assertEquals(0x44, result and 0xFF)
    }

    @Test
    fun `zaten opak renk degismeden kalir`() {
        val opaque = 0xFF0F6F68.toInt()
        assertEquals(opaque, AppPrefs.resolveHomeObjectCustomColor(opaque))
    }
}
