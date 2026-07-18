package com.armutlu.apporganizer.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun `default config is the complete deterministic section source`() {
        val config = HomeLayoutConfig.DEFAULT
        assertEquals(HomeSectionId.entries.toSet(), config.items.map { it.sectionId }.toSet())
        // P15 v2: Dashboard section'ları artık CONTENT zone'una ait; HEADER yalnızca MAIN_SEARCH'ü
        // barındırır.
        assertEquals(
            listOf(HomeSectionId.MAIN_SEARCH),
            config.items.filter { it.zone == HomeLayoutZone.HEADER && it.visible }
                .sortedBy { it.order }.map { it.sectionId },
        )
        assertEquals(
            listOf(HomeSectionId.CLOCK, HomeSectionId.MISSIONS_AND_SCORE,
                HomeSectionId.FAVORITES, HomeSectionId.SUGGESTIONS,
                HomeSectionId.RECENT_NOTIFICATIONS, HomeSectionId.RECENT_APPS,
                HomeSectionId.FOLDER_GRID),
            config.items.filter { it.zone == HomeLayoutZone.CONTENT && it.visible }
                .sortedBy { it.order }.map { it.sectionId },
        )
    }

    @Test
    fun `section policies type required hideable and movement rules`() {
        assertFalse(HomeSectionId.FOLDER_GRID.hideable)
        assertEquals(HomeSectionMovement.RESTRICTED, HomeSectionId.FOLDER_GRID.movement)
        assertTrue(HomeSectionId.DOCK.required)
        assertFalse(HomeSectionId.DOCK.movable)
        assertTrue(HomeSectionId.CLOCK.hideable)
        assertTrue(HomeSectionId.CLOCK.movable)
    }

    @Test
    fun `duplicate section id is rejected`() {
        val dock = HomeLayoutConfig.DEFAULT.items.first { it.sectionId == HomeSectionId.DOCK }
        assertThrows(IllegalArgumentException::class.java) {
            HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION,
                HomeLayoutConfig.DEFAULT.items + dock.copy(order = 1))
        }
    }

    @Test
    fun `missing required section is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION,
                HomeLayoutConfig.DEFAULT.items.filterNot { it.sectionId == HomeSectionId.FOLDER_GRID })
        }
    }
}
