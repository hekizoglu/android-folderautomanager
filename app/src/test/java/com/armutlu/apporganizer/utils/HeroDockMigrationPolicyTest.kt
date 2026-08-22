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

    @Test fun `tekrarlari siler ve dort slotu asmaz`() {
        // buildHeroDockItems kasıtlı olarak 4 slotla sınırlı (5. slot varsayılan
        // doldurmada boş bırakılır, kullanıcı addToDock ile 5.'yi kendi ekler —
        // DockPrefs.kt:75 yorumu: "İlk 4 slot döndür, 5. slot boş").
        val result = DockPrefs.buildHeroDockItems(
            current = listOf("a", "b", "a"),
            fallbackPackages = listOf("b", "c", "d", "e", "f"),
        )
        assertEquals(listOf("a", "b", "c", "d"), result)
    }

    @Test fun `kurulu olmayan ilk adaylar fallback slotlarini engellemez`() {
        val unavailable = setOf("gone", "missing")
        val result = DockPrefs.buildHeroDockItems(
            current = listOf("gone", "phone", "missing"),
            fallbackPackages = listOf("camera", "browser", "messages", "maps"),
            isEligible = { it !in unavailable },
        )
        assertEquals(listOf("phone", "camera", "browser", "messages"), result)
    }

    @Test fun `persist siniri klasor bosluk ve tekrarlari kabul etmez`() {
        val result = DockPrefs.sanitizeHeroDockItems(
            listOf("phone", "", "folder:social", "phone", "camera", "browser", "messages", "maps", "extra"),
        )
        assertEquals(listOf("phone", "camera", "browser", "messages", "maps"), result)
    }
}
