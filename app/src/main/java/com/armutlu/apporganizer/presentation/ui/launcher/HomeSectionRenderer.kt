package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeSectionId

internal data class HomeSectionRenderEntry(val sectionId: HomeSectionId, val stableKey: String)

internal fun homeSectionRenderPlan(items: List<HomeLayoutItem>): List<HomeSectionRenderEntry> =
    items.asSequence()
        .filter(HomeLayoutItem::visible)
        .sortedBy(HomeLayoutItem::order)
        .map { HomeSectionRenderEntry(it.sectionId, "home-section-${it.sectionId.name}") }
        .toList()

/** Stateless bridge used while legacy HomeScreen sections move to HomeLayoutConfig. */
@Composable
internal fun HomeSectionRenderer(
    items: List<HomeLayoutItem>,
    content: @Composable (HomeSectionId) -> Unit,
) {
    homeSectionRenderPlan(items).forEach { entry ->
        key(entry.stableKey) { content(entry.sectionId) }
    }
}
