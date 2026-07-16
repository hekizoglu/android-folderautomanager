package com.armutlu.apporganizer.utils

import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutPrefsTest {
    private fun legacy(
        searchPosition: String = AppPrefs.SEARCH_BAR_POS_TOP,
        visible: Boolean = true,
    ) = HomeLayoutPrefs.LegacySettings(searchPosition, visible, visible, visible, visible,
        visible, visible, visible, visible, visible, visible)

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

    @Test fun `legacy TOP keeps search in header and migrates visible toggles`() {
        val state = HomeLayoutPrefs.migrateLegacy(legacy(visible = false))
        val search = state.config.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }
        assertEquals(HomeLayoutZone.HEADER, search.zone)
        assertFalse(search.visible)
        assertFalse(state.config.items.single { it.sectionId == HomeSectionId.FAVORITES }.visible)
        assertFalse(state.config.items.single { it.sectionId == HomeSectionId.ANDROID_WIDGETS }.visible)
        assertTrue(state.config.items.single { it.sectionId == HomeSectionId.FOLDER_GRID }.visible)
    }

    @Test fun `legacy BOTTOM puts search immediately before dock`() {
        val state = HomeLayoutPrefs.migrateLegacy(legacy(AppPrefs.SEARCH_BAR_POS_BOTTOM))
        val footer = state.config.items.filter { it.zone == HomeLayoutZone.FOOTER }.sortedBy { it.order }
        assertEquals(listOf(HomeSectionId.MAIN_SEARCH, HomeSectionId.DOCK), footer.map { it.sectionId })
    }

    @Test fun `stored layout wins over changed legacy settings`() {
        val stored = HomeLayoutPrefs.StoredLayout("MAIN_SEARCH,CLOCK", "DOCK", "FAVORITES", 1, true)
        val state = HomeLayoutPrefs.initialState(stored, legacy(AppPrefs.SEARCH_BAR_POS_BOTTOM, visible = true))
        assertEquals(HomeLayoutZone.HEADER, state.config.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.zone)
        assertFalse(state.config.items.single { it.sectionId == HomeSectionId.FAVORITES }.visible)
        assertTrue(state.customized)
    }

    @Test fun `old stored layout appends newly introduced section`() {
        val state = HomeLayoutPrefs.initialState(
            HomeLayoutPrefs.StoredLayout("CLOCK,MAIN_SEARCH", "DOCK", "", 1, false), legacy())
        assertTrue(HomeSectionId.GOOGLE_SEARCH in state.config.items.map { it.sectionId })
        assertEquals(HomeSectionId.entries.size, state.config.items.size)
    }
}
