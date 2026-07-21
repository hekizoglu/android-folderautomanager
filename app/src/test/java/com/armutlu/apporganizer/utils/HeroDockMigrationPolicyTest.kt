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

    @Test fun `kurulu olmayan ilk adaylar fallback slotlarini engellemez`() {
        val unavailable = setOf("gone", "missing")
        val result = DockPrefs.buildHeroDockItems(
            current = listOf("gone", "phone", "missing"),
            fallbackPackages = listOf("camera", "browser", "messages", "maps"),
            isEligible = { it !in unavailable },
        )
        assertEquals(listOf("phone", "camera", "browser", "messages", "maps"), result)
    }

    @Test fun `persist siniri klasor bosluk ve tekrarlari kabul etmez`() {
        val result = DockPrefs.sanitizeHeroDockItems(
            listOf("phone", "", "folder:social", "phone", "camera", "browser", "messages", "maps", "extra")
        )
        assertEquals(listOf("phone", "camera", "browser", "messages", "maps"), result)
    }
}
