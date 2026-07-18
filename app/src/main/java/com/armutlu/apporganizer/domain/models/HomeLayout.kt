package com.armutlu.apporganizer.domain.models

enum class HomeLayoutZone { HEADER, CONTENT, FOOTER }

enum class HomeSectionMovement { MOVABLE, RESTRICTED, FIXED }

enum class HomeSectionId(
    val defaultZone: HomeLayoutZone,
    val movement: HomeSectionMovement = HomeSectionMovement.MOVABLE,
    val hideable: Boolean = true,
) {
    CLOCK(HomeLayoutZone.CONTENT),
    MISSIONS_AND_SCORE(HomeLayoutZone.CONTENT),
    MAIN_SEARCH(HomeLayoutZone.HEADER),
    GOOGLE_SEARCH(HomeLayoutZone.CONTENT),
    FAVORITES(HomeLayoutZone.CONTENT),
    SUGGESTIONS(HomeLayoutZone.CONTENT),
    RECENT_NOTIFICATIONS(HomeLayoutZone.CONTENT),
    RECENT_APPS(HomeLayoutZone.CONTENT),
    ANDROID_WIDGETS(HomeLayoutZone.CONTENT),
    ASSISTANT_INSIGHTS(HomeLayoutZone.CONTENT),
    TICKER_OR_STATS(HomeLayoutZone.CONTENT),
    FOLDER_GRID(HomeLayoutZone.CONTENT, HomeSectionMovement.RESTRICTED, hideable = false),
    DOCK(HomeLayoutZone.FOOTER, HomeSectionMovement.FIXED, hideable = false),
    ;

    val required: Boolean get() = !hideable
    val movable: Boolean get() = movement == HomeSectionMovement.MOVABLE
}

data class HomeLayoutItem(
    val sectionId: HomeSectionId,
    val zone: HomeLayoutZone,
    val order: Int,
    val visible: Boolean,
    val locked: Boolean = sectionId.movement == HomeSectionMovement.FIXED,
) {
    init {
        require(order >= 0) { "Home layout order cannot be negative: $order" }
        require(sectionId.hideable || visible) {
            "Required home section cannot be hidden: ${sectionId.name}"
        }
    }
}

data class HomeLayoutConfig(
    val version: Int,
    val items: List<HomeLayoutItem>,
) {
    init {
        require(version > 0) { "Home layout version must be positive: $version" }
        val duplicateIds = items.groupingBy { it.sectionId }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) {
            "Home layout contains duplicate sections: ${duplicateIds.joinToString { it.name }}"
        }
        val presentIds = items.mapTo(mutableSetOf()) { it.sectionId }
        val missingRequired = HomeSectionId.entries.filter { it.required && it !in presentIds }
        require(missingRequired.isEmpty()) {
            "Home layout is missing required sections: ${missingRequired.joinToString { it.name }}"
        }
    }

    companion object {
        const val CURRENT_VERSION = 2

        /** The single source of truth for a new home layout. */
        val DEFAULT = HomeLayoutConfig(
            version = CURRENT_VERSION,
            items = listOf(
                // HEADER: MAIN_SEARCH is the sole header-zone section in v2.
                defaultItem(HomeSectionId.MAIN_SEARCH, 0),
                // CONTENT: Dashboard sections, order is zone-relative (P15 v2 — orders must be
                // unique WITHIN a zone; FOLDER_GRID shares CONTENT with the dashboard sections so
                // it needs its own non-colliding slot, placed after the visible defaults).
                defaultItem(HomeSectionId.CLOCK, 0),
                defaultItem(HomeSectionId.MISSIONS_AND_SCORE, 1),
                defaultItem(HomeSectionId.FAVORITES, 2),
                defaultItem(HomeSectionId.SUGGESTIONS, 3),
                defaultItem(HomeSectionId.RECENT_NOTIFICATIONS, 4),
                defaultItem(HomeSectionId.RECENT_APPS, 5),
                defaultItem(HomeSectionId.GOOGLE_SEARCH, 6, visible = false),
                defaultItem(HomeSectionId.ANDROID_WIDGETS, 7, visible = false),
                defaultItem(HomeSectionId.ASSISTANT_INSIGHTS, 8, visible = false),
                defaultItem(HomeSectionId.TICKER_OR_STATS, 9, visible = false),
                defaultItem(HomeSectionId.FOLDER_GRID, 10),
                // FOOTER: DOCK is fixed as the final entry.
                defaultItem(HomeSectionId.DOCK, 0),
            ),
        )

        private fun defaultItem(sectionId: HomeSectionId, order: Int, visible: Boolean = true) =
            HomeLayoutItem(sectionId, sectionId.defaultZone, order, visible)
    }
}

/**
 * P16 — editor'de MAIN_SEARCH için tek izin verilen etkileşim: HEADER (üstte sabit) ile FOOTER
 * (altta, dock üstünde sabit) arasında konum seçimi. MAIN_SEARCH ilgili zone'un TEK üyesi olarak
 * en başa (order=0) yerleşir; DOCK her zaman FOOTER'ın son sırasındadır (order=Int.MAX_VALUE ile
 * korunur, bkz. HomeSectionRenderer.homeZoneRenderPlan).
 */
fun HomeLayoutConfig.withSearchZone(zone: HomeLayoutZone): HomeLayoutConfig {
    require(zone == HomeLayoutZone.HEADER || zone == HomeLayoutZone.FOOTER) {
        "MAIN_SEARCH can only live in HEADER or FOOTER, not $zone"
    }
    val current = items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }
    if (current.zone == zone) return this
    return copy(items = items.map { item ->
        when (item.sectionId) {
            HomeSectionId.MAIN_SEARCH -> item.copy(zone = zone, order = 0)
            else -> item
        }
    })
}
