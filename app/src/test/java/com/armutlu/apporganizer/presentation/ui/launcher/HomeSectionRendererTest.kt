package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSectionRendererTest {
    @Test
    fun `render plan keeps visible sections ordered with stable keys`() {
        val items = listOf(
            item(HomeSectionId.ANDROID_WIDGETS, 2),
            item(HomeSectionId.GOOGLE_SEARCH, 0),
            item(HomeSectionId.ASSISTANT_INSIGHTS, 1, visible = false),
        )
        assertEquals(
            listOf(
                HomeSectionRenderEntry(HomeSectionId.GOOGLE_SEARCH, "home-section-GOOGLE_SEARCH"),
                HomeSectionRenderEntry(HomeSectionId.ANDROID_WIDGETS, "home-section-ANDROID_WIDGETS"),
            ),
            homeSectionRenderPlan(items),
        )
    }

    @Test
    fun `contextual rows map to independently renderable section ids`() {
        assertEquals(HomeSectionId.FAVORITES, HomeContextualRowKind.FAVORITES.sectionId())
        assertEquals(HomeSectionId.SUGGESTIONS, HomeContextualRowKind.SUGGESTIONS.sectionId())
        assertEquals(HomeSectionId.RECENT_NOTIFICATIONS, HomeContextualRowKind.RECENT_NOTIFICATIONS.sectionId())
        assertEquals(HomeSectionId.RECENT_APPS, HomeContextualRowKind.RECENT_APPS.sectionId())
    }

    @Test
    fun `zone plan follows migrated bottom search and keeps dock last`() {
        val items = HomeLayoutConfig.DEFAULT.items.map {
            when (it.sectionId) {
                HomeSectionId.MAIN_SEARCH -> it.copy(zone = HomeLayoutZone.FOOTER, order = 0)
                HomeSectionId.DOCK -> it.copy(order = 1)
                else -> it
            }
        }

        val plan = homeZoneRenderPlan(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items))

        assertEquals(HomeSectionId.MAIN_SEARCH, plan.footer.first().sectionId)
        assertEquals(HomeSectionId.DOCK, plan.footer.last().sectionId)
        // P15 v2: CONTENT now hosts all Dashboard sections, not just FOLDER_GRID — it must still be
        // present (protected) among them.
        assertEquals(true, plan.content.any { it.sectionId == HomeSectionId.FOLDER_GRID })
        assertEquals(false, plan.header.any { it.sectionId == HomeSectionId.MAIN_SEARCH })
    }

    @Test
    fun `zone plan protects folder grid and dock from misplaced config`() {
        val items = HomeLayoutConfig.DEFAULT.items.map {
            when (it.sectionId) {
                HomeSectionId.FOLDER_GRID -> it.copy(zone = HomeLayoutZone.HEADER, order = 99)
                HomeSectionId.DOCK -> it.copy(zone = HomeLayoutZone.HEADER, order = 0)
                else -> it
            }
        }

        val plan = homeZoneRenderPlan(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items))

        // P15 v2: CONTENT now hosts all Dashboard sections, not just FOLDER_GRID — it must still be
        // forced back into CONTENT (protected) regardless of the misplaced HEADER zone in the input.
        assertEquals(true, plan.content.any { it.sectionId == HomeSectionId.FOLDER_GRID })
        assertEquals(HomeSectionId.DOCK, plan.footer.last().sectionId)
        assertEquals(false, plan.header.any { it.sectionId == HomeSectionId.FOLDER_GRID || it.sectionId == HomeSectionId.DOCK })
    }

    private fun item(sectionId: HomeSectionId, order: Int, visible: Boolean = true) =
        HomeLayoutItem(sectionId, sectionId.defaultZone, order, visible)
}
