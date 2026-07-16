package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutPrefsTest {
    @Test fun `missing storage returns canonical default`() {
        assertEquals(HomeLayoutPrefs.State(HomeLayoutConfig.DEFAULT, false),
            HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(null, null, null, null, null)))
    }

    @Test fun `corrupt duplicate unknown and wrong-zone ids are sanitized`() {
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(
            "RECENT_APPS,UNKNOWN,RECENT_APPS,DOCK,CLOCK", "CLOCK,DOCK,BOGUS",
            "CLOCK,FOLDER_GRID,DOCK,UNKNOWN", -9, true))
        val header = state.config.items.filter { it.zone == HomeLayoutZone.HEADER }.sortedBy { it.order }
        assertEquals(HomeSectionId.RECENT_APPS, header[0].sectionId)
        assertEquals(HomeSectionId.CLOCK, header[1].sectionId)
        assertEquals(HomeSectionId.entries.toSet(), state.config.items.map { it.sectionId }.toSet())
        assertFalse(state.config.items.single { it.sectionId == HomeSectionId.CLOCK }.visible)
        assertTrue(state.config.items.single { it.sectionId == HomeSectionId.FOLDER_GRID }.visible)
        assertTrue(state.config.items.single { it.sectionId == HomeSectionId.DOCK }.visible)
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, state.config.version)
        assertTrue(state.customized)
    }

    @Test fun `old incomplete layout appends new sections in default order`() {
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout("FAVORITES", "", "", 0, false))
        val header = state.config.items.filter { it.zone == HomeLayoutZone.HEADER }.sortedBy { it.order }.map { it.sectionId }
        assertEquals(HomeSectionId.FAVORITES, header.first())
        assertEquals(HomeSectionId.entries.count { it.defaultZone == HomeLayoutZone.HEADER }, header.size)
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, state.config.version)
    }

    @Test fun `write boundary repairs conflicting zone and version`() {
        val malformed = HomeLayoutConfig.DEFAULT.copy(version = 99, items = HomeLayoutConfig.DEFAULT.items.map {
            if (it.sectionId == HomeSectionId.CLOCK) it.copy(zone = HomeLayoutZone.FOOTER, order = 99) else it
        })
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.State(malformed, true))
        assertEquals(HomeLayoutZone.HEADER, state.config.items.single { it.sectionId == HomeSectionId.CLOCK }.zone)
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, state.config.version)
        assertTrue(state.customized)
    }
}
