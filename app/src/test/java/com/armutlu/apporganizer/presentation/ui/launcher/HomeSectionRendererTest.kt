package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.HomeLayoutItem
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

    private fun item(sectionId: HomeSectionId, order: Int, visible: Boolean = true) =
        HomeLayoutItem(sectionId, sectionId.defaultZone, order, visible)
}
