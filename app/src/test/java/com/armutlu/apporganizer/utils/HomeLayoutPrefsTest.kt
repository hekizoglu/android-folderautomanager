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
        // version=-9 (< 2, no contentOrder) triggers the v1->v2 legacy path: RECENT_APPS and CLOCK
        // (today's CONTENT-default sections) are pulled out of the legacy HEADER_ORDER string into
        // CONTENT, preserving their relative order.
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(
            "RECENT_APPS,UNKNOWN,RECENT_APPS,DOCK,CLOCK", "CLOCK,DOCK,BOGUS",
            "CLOCK,FOLDER_GRID,DOCK,UNKNOWN", -9, true))
        val content = state.config.items.filter { it.zone == HomeLayoutZone.CONTENT }.sortedBy { it.order }
        assertEquals(HomeSectionId.RECENT_APPS, content[0].sectionId)
        assertEquals(HomeSectionId.CLOCK, content[1].sectionId)
        assertEquals(HomeSectionId.entries.toSet(), state.config.items.map { it.sectionId }.toSet())
        assertFalse(state.config.items.single { it.sectionId == HomeSectionId.CLOCK }.visible)
        assertTrue(state.config.items.single { it.sectionId == HomeSectionId.FOLDER_GRID }.visible)
        assertTrue(state.config.items.single { it.sectionId == HomeSectionId.DOCK }.visible)
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, state.config.version)
        assertTrue(state.customized)
    }

    @Test fun `old incomplete layout appends new sections in default order`() {
        // version=0 (< 2, no contentOrder) -> legacy path: FAVORITES (CONTENT-default today) is
        // pulled from HEADER_ORDER into CONTENT; MAIN_SEARCH stays the only HEADER-default section.
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout("FAVORITES", "", "", 0, false))
        val content = state.config.items.filter { it.zone == HomeLayoutZone.CONTENT }.sortedBy { it.order }.map { it.sectionId }
        assertEquals(HomeSectionId.FAVORITES, content.first())
        assertEquals(HomeSectionId.entries.count { it.defaultZone == HomeLayoutZone.CONTENT }, content.size)
        val header = state.config.items.filter { it.zone == HomeLayoutZone.HEADER }.map { it.sectionId }
        assertEquals(listOf(HomeSectionId.MAIN_SEARCH), header)
        assertEquals(HomeLayoutConfig.CURRENT_VERSION, state.config.version)
    }

    @Test fun `v1 header dashboard sections migrate to content preserving order`() {
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(
            headerOrder = "MAIN_SEARCH,SUGGESTIONS,CLOCK,FAVORITES",
            footerOrder = "DOCK",
            hiddenSections = "",
            version = 1,
            customized = true,
        ))
        assertEquals(2, state.config.version)
        val content = state.config.items.filter { it.zone == HomeLayoutZone.CONTENT }.sortedBy { it.order }.map { it.sectionId }
        assertEquals(listOf(HomeSectionId.SUGGESTIONS, HomeSectionId.CLOCK, HomeSectionId.FAVORITES), content.take(3))
        assertEquals(HomeLayoutZone.HEADER, state.config.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.zone)
        assertTrue(HomeSectionId.FOLDER_GRID in content)
        assertFalse(HomeSectionId.MAIN_SEARCH in content)
    }

    @Test fun `v2 content order is idempotent across repeated sanitize passes`() {
        val once = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(
            headerOrder = "MAIN_SEARCH", footerOrder = "DOCK",
            hiddenSections = "", version = 2, customized = true,
            contentOrder = "FAVORITES,CLOCK,SUGGESTIONS,FOLDER_GRID",
        ))
        val twice = HomeLayoutPrefs.sanitize(once)
        assertEquals(once.config.items.map { it.sectionId to it.zone to it.order }.toSet(),
            twice.config.items.map { it.sectionId to it.zone to it.order }.toSet())
        val content = once.config.items.filter { it.zone == HomeLayoutZone.CONTENT }.sortedBy { it.order }.map { it.sectionId }
        assertEquals(listOf(HomeSectionId.FAVORITES, HomeSectionId.CLOCK, HomeSectionId.SUGGESTIONS), content.take(3))
    }

    @Test fun `v2 stored layout with content order is not re-migrated from header`() {
        // version=2 with an explicit (even empty-ish) contentOrder should NOT re-trigger the v1
        // legacy header-partition path — header entries stay in HEADER only if allowed there.
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(
            headerOrder = "MAIN_SEARCH,CLOCK", footerOrder = "DOCK",
            hiddenSections = "", version = 2, customized = false,
            contentOrder = "CLOCK,FAVORITES",
        ))
        // CLOCK is not allowedIn(HEADER) anymore, so it is dropped from the header candidate list
        // (not moved) — sanitizeOrder falls back to defaults, and CLOCK's position is governed by
        // suppliedContent instead.
        assertEquals(HomeLayoutZone.CONTENT, state.config.items.single { it.sectionId == HomeSectionId.CLOCK }.zone)
        assertEquals(listOf(HomeSectionId.MAIN_SEARCH), state.config.items.filter { it.zone == HomeLayoutZone.HEADER }.map { it.sectionId })
    }

    @Test fun `corrupt v2 backup with malformed content order falls back to safe default order`() {
        val restored = HomeLayoutPrefs.fromBackupFields(HomeLayoutPrefs.BackupFields(
            version = 2,
            headerOrder = "MAIN_SEARCH",
            footerOrder = "DOCK",
            contentOrder = "UNKNOWN,CLOCK,CLOCK,,BOGUS",
            hiddenSections = null,
            customized = true,
        ))
        assertEquals(HomeSectionId.entries.toSet(), restored.config.items.map { it.sectionId }.toSet())
        assertEquals(HomeSectionId.CLOCK, restored.config.items.filter { it.zone == HomeLayoutZone.CONTENT }
            .sortedBy { it.order }.first().sectionId)
        assertEquals(2, restored.config.version)
    }

    @Test fun `write boundary repairs conflicting zone and version`() {
        val malformed = HomeLayoutConfig.DEFAULT.copy(version = 99, items = HomeLayoutConfig.DEFAULT.items.map {
            if (it.sectionId == HomeSectionId.CLOCK) it.copy(zone = HomeLayoutZone.FOOTER, order = 99) else it
        })
        val state = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.State(malformed, true))
        // CLOCK is not allowedIn(FOOTER) (its defaultZone is CONTENT) so the bogus zone is dropped
        // and it falls back to its canonical default zone.
        assertEquals(HomeLayoutZone.CONTENT, state.config.items.single { it.sectionId == HomeSectionId.CLOCK }.zone)
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

    @Test fun `corrupt backup fields are sanitized at restore boundary`() {
        val restored = HomeLayoutPrefs.fromBackupFields(HomeLayoutPrefs.BackupFields(
            version = -7,
            headerOrder = "UNKNOWN,CLOCK,CLOCK,DOCK",
            footerOrder = "MAIN_SEARCH,BOGUS",
            hiddenSections = "CLOCK,FOLDER_GRID,UNKNOWN",
            customized = true,
        ))

        assertEquals(HomeLayoutConfig.CURRENT_VERSION, restored.config.version)
        assertEquals(HomeSectionId.entries.toSet(), restored.config.items.map { it.sectionId }.toSet())
        assertFalse(restored.config.items.single { it.sectionId == HomeSectionId.CLOCK }.visible)
        assertTrue(restored.config.items.single { it.sectionId == HomeSectionId.FOLDER_GRID }.visible)
        assertEquals(HomeLayoutZone.FOOTER, restored.config.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }.zone)
    }

    @Test fun `v2 backup round-trip preserves customized content order exactly`() {
        // P15 kabul kriteri: BackupManager uyumu (P02 pattern) — toBackupFields/fromBackupFields
        // export/import round-trip'i, CONTENT sırasını da dahil ederek kaybetmemeli.
        val customized = HomeLayoutPrefs.sanitize(HomeLayoutPrefs.StoredLayout(
            headerOrder = "MAIN_SEARCH", footerOrder = "DOCK",
            hiddenSections = "GOOGLE_SEARCH", version = 2, customized = true,
            contentOrder = "SUGGESTIONS,FAVORITES,CLOCK,RECENT_APPS,RECENT_NOTIFICATIONS,MISSIONS_AND_SCORE,FOLDER_GRID",
        ))
        val fields = HomeLayoutPrefs.toBackupFields(customized)
        val restored = HomeLayoutPrefs.fromBackupFields(fields)

        assertEquals(customized.config.items.map { it.sectionId to it.zone to it.order to it.visible }.toSet(),
            restored.config.items.map { it.sectionId to it.zone to it.order to it.visible }.toSet())
        assertEquals(customized.customized, restored.customized)
        val restoredContent = restored.config.items.filter { it.zone == HomeLayoutZone.CONTENT }
            .sortedBy { it.order }.map { it.sectionId }
        // Supplied order is honored first; sections absent from contentOrder (GOOGLE_SEARCH,
        // ANDROID_WIDGETS, ASSISTANT_INSIGHTS, TICKER_OR_STATS) are appended in default order —
        // same "old layout appends new sections" contract as HEADER/FOOTER.
        assertEquals(
            listOf(HomeSectionId.SUGGESTIONS, HomeSectionId.FAVORITES, HomeSectionId.CLOCK,
                HomeSectionId.RECENT_APPS, HomeSectionId.RECENT_NOTIFICATIONS,
                HomeSectionId.MISSIONS_AND_SCORE, HomeSectionId.FOLDER_GRID,
                HomeSectionId.GOOGLE_SEARCH, HomeSectionId.ANDROID_WIDGETS,
                HomeSectionId.ASSISTANT_INSIGHTS, HomeSectionId.TICKER_OR_STATS),
            restoredContent,
        )
    }

    @Test fun `diagnostics summary contains only typed safe layout values`() {
        val summary = HomeLayoutPrefs.diagnosticsSummary(
            HomeLayoutPrefs.migrateLegacy(legacy()), widgetCount = -3, dockItemCount = 5)

        assertEquals(HomeLayoutConfig.CURRENT_VERSION, summary.version)
        assertTrue(summary.headerOrder.all { it in HomeSectionId.entries })
        assertTrue(summary.hiddenSections.all { it in HomeSectionId.entries })
        assertEquals(0, summary.widgetCount)
        assertEquals(5, summary.dockItemCount)
    }
}
