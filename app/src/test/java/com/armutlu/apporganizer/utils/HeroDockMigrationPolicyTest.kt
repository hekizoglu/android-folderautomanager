package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class HeroDockMigrationPolicyTest {
    @Test fun `klasorleri hero docktan cikarir ve sirayi korur`() {
        val result = DockPrefs.buildHeroDockItems(
            current = listOf("phone", "folder:social", "camera"),
            fallbackPackages = listOf("browser", "messages"),
        )
        assertEquals(listOf("phone", "camera", "browser", "messages"), result)
    }

    @Test fun `tekrarlari siler ve bes slotu asmaz`() {
        val result = DockPrefs.buildHeroDockItems(
            current = listOf("a", "b", "a"),
            fallbackPackages = listOf("b", "c", "d", "e", "f"),
        )
        assertEquals(listOf("a", "b", "c", "d", "e"), result)
    }
}
