package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId

internal data class HomeSectionRenderEntry(val sectionId: HomeSectionId, val stableKey: String)

internal fun homeSectionRenderPlan(items: List<HomeLayoutItem>): List<HomeSectionRenderEntry> =
    items.asSequence()
        .filter(HomeLayoutItem::visible)
        .sortedBy(HomeLayoutItem::order)
        .map { HomeSectionRenderEntry(it.sectionId, "home-section-${it.sectionId.name}") }
        .toList()

internal data class HomeZoneRenderPlan(
    val header: List<HomeSectionRenderEntry>,
    val content: List<HomeSectionRenderEntry>,
    val footer: List<HomeSectionRenderEntry>,
)

/**
 * Converts the persisted layout into the three physical home-screen zones.
 * Required sections are rendered defensively even if an older/corrupt caller supplies
 * the wrong zone or visibility, and the dock is always the final footer entry.
 */
internal fun homeZoneRenderPlan(config: HomeLayoutConfig): HomeZoneRenderPlan {
    val byId = config.items.associateBy(HomeLayoutItem::sectionId)
    fun protectedItem(sectionId: HomeSectionId): HomeLayoutItem {
        val item = requireNotNull(byId[sectionId])
        return when (sectionId) {
            HomeSectionId.FOLDER_GRID -> item.copy(zone = HomeLayoutZone.CONTENT, visible = true)
            HomeSectionId.DOCK -> item.copy(zone = HomeLayoutZone.FOOTER, order = Int.MAX_VALUE, visible = true)
            else -> item
        }
    }
    val protectedItems = config.items.map { protectedItem(it.sectionId) }
    fun plan(zone: HomeLayoutZone) = homeSectionRenderPlan(protectedItems.filter { it.zone == zone })
    return HomeZoneRenderPlan(
        header = plan(HomeLayoutZone.HEADER),
        content = plan(HomeLayoutZone.CONTENT),
        footer = plan(HomeLayoutZone.FOOTER),
    )
}

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
