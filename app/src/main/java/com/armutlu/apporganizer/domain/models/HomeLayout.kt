package com.armutlu.apporganizer.domain.models

enum class HomeLayoutZone { HEADER, CONTENT, FOOTER }

enum class HomeSectionMovement { MOVABLE, RESTRICTED, FIXED }

enum class HomeSectionId(
    val defaultZone: HomeLayoutZone,
    val movement: HomeSectionMovement = HomeSectionMovement.MOVABLE,
    val hideable: Boolean = true,
) {
    CLOCK(HomeLayoutZone.HEADER),
    MISSIONS_AND_SCORE(HomeLayoutZone.HEADER),
    MAIN_SEARCH(HomeLayoutZone.HEADER),
    GOOGLE_SEARCH(HomeLayoutZone.HEADER),
    FAVORITES(HomeLayoutZone.HEADER),
    SUGGESTIONS(HomeLayoutZone.HEADER),
    RECENT_NOTIFICATIONS(HomeLayoutZone.HEADER),
    RECENT_APPS(HomeLayoutZone.HEADER),
    ANDROID_WIDGETS(HomeLayoutZone.HEADER),
    ASSISTANT_INSIGHTS(HomeLayoutZone.HEADER),
    TICKER_OR_STATS(HomeLayoutZone.HEADER),
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
        const val CURRENT_VERSION = 1

        /** The single source of truth for a new home layout. */
        val DEFAULT = HomeLayoutConfig(
            version = CURRENT_VERSION,
            items = listOf(
                defaultItem(HomeSectionId.CLOCK, 0),
                defaultItem(HomeSectionId.MAIN_SEARCH, 1),
                defaultItem(HomeSectionId.MISSIONS_AND_SCORE, 2),
                defaultItem(HomeSectionId.FAVORITES, 3),
                defaultItem(HomeSectionId.SUGGESTIONS, 4),
                defaultItem(HomeSectionId.RECENT_NOTIFICATIONS, 5),
                defaultItem(HomeSectionId.RECENT_APPS, 6),
                defaultItem(HomeSectionId.GOOGLE_SEARCH, 7, visible = false),
                defaultItem(HomeSectionId.ANDROID_WIDGETS, 8, visible = false),
                defaultItem(HomeSectionId.ASSISTANT_INSIGHTS, 9, visible = false),
                defaultItem(HomeSectionId.TICKER_OR_STATS, 10, visible = false),
                defaultItem(HomeSectionId.FOLDER_GRID, 0),
                defaultItem(HomeSectionId.DOCK, 0),
            ),
        )

        private fun defaultItem(sectionId: HomeSectionId, order: Int, visible: Boolean = true) =
            HomeLayoutItem(sectionId, sectionId.defaultZone, order, visible)
    }
}
