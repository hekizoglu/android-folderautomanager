package com.armutlu.apporganizer.utils

import android.content.Context
import androidx.core.content.edit
import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId

object HomeLayoutPrefs {
    internal const val PREFS_NAME = "home_layout_prefs"
    internal const val KEY_HEADER_ORDER = "header_order"
    internal const val KEY_FOOTER_ORDER = "footer_order"
    internal const val KEY_HIDDEN_SECTIONS = "hidden_sections"
    internal const val KEY_LAYOUT_VERSION = "layout_version"
    internal const val KEY_CUSTOMIZED = "customized"

    data class State(val config: HomeLayoutConfig, val customized: Boolean)

    internal data class LegacySettings(
        val searchPosition: String,
        val widgetsVisible: Boolean,
        val favoritesVisible: Boolean,
        val suggestionsVisible: Boolean,
        val recentNotificationsVisible: Boolean,
        val recentAppsVisible: Boolean,
        val assistantVisible: Boolean,
        val tickerVisible: Boolean,
        val missionsVisible: Boolean,
        val mainSearchVisible: Boolean,
        val googleSearchVisible: Boolean = true,
    )

    internal data class StoredLayout(
        val headerOrder: String?, val footerOrder: String?, val hiddenSections: String?,
        val version: Int?, val customized: Boolean?,
    )

    fun read(context: Context): State {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasLayout = listOf(KEY_HEADER_ORDER, KEY_FOOTER_ORDER, KEY_HIDDEN_SECTIONS,
            KEY_LAYOUT_VERSION, KEY_CUSTOMIZED).any(prefs::contains)
        if (!hasLayout) {
            val migrated = migrateLegacy(readLegacy(context))
            write(context, migrated)
            return migrated
        }
        return sanitize(StoredLayout(
            runCatching { prefs.getString(KEY_HEADER_ORDER, null) }.getOrNull(),
            runCatching { prefs.getString(KEY_FOOTER_ORDER, null) }.getOrNull(),
            runCatching { prefs.getString(KEY_HIDDEN_SECTIONS, null) }.getOrNull(),
            runCatching { prefs.getInt(KEY_LAYOUT_VERSION, 0) }.getOrNull(),
            runCatching { prefs.getBoolean(KEY_CUSTOMIZED, false) }.getOrNull(),
        ))
    }

    fun write(context: Context, state: State) {
        val clean = sanitize(state)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_HEADER_ORDER, clean.config.idsIn(HomeLayoutZone.HEADER).joinToString(","))
            putString(KEY_FOOTER_ORDER, clean.config.idsIn(HomeLayoutZone.FOOTER).joinToString(","))
            putString(KEY_HIDDEN_SECTIONS, clean.config.items.filterNot { it.visible }.joinToString(",") { it.sectionId.name })
            putInt(KEY_LAYOUT_VERSION, HomeLayoutConfig.CURRENT_VERSION)
            putBoolean(KEY_CUSTOMIZED, clean.customized)
        }
    }

    internal fun sanitize(stored: StoredLayout): State {
        val present = stored.headerOrder != null || stored.footerOrder != null || stored.hiddenSections != null ||
            stored.version != null || stored.customized != null
        if (!present) return State(HomeLayoutConfig.DEFAULT, false)
        val suppliedHeader = parseIds(stored.headerOrder).filter { it.allowedIn(HomeLayoutZone.HEADER) }.distinct()
        val suppliedFooter = parseIds(stored.footerOrder).filter { it.allowedIn(HomeLayoutZone.FOOTER) }.distinct()
        val searchZone = if (HomeSectionId.MAIN_SEARCH in suppliedFooter &&
            HomeSectionId.MAIN_SEARCH !in suppliedHeader) HomeLayoutZone.FOOTER else HomeLayoutZone.HEADER
        val orders = mapOf(
            HomeLayoutZone.HEADER to sanitizeOrder(suppliedHeader, HomeLayoutZone.HEADER, searchZone),
            HomeLayoutZone.FOOTER to sanitizeOrder(suppliedFooter, HomeLayoutZone.FOOTER, searchZone),
        )
        val hidden = parseIds(stored.hiddenSections).filterTo(mutableSetOf()) { it.hideable }
        val items = HomeLayoutConfig.DEFAULT.items.map { item ->
            val zone = if (item.sectionId == HomeSectionId.MAIN_SEARCH) searchZone else item.zone
            val index = orders[zone]?.indexOf(item.sectionId)?.takeIf { it >= 0 }
            item.copy(zone = zone, order = index ?: item.order, visible = item.sectionId !in hidden)
        }
        return State(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items), stored.customized == true)
    }

    internal fun sanitize(state: State): State {
        val byId = state.config.items.associateBy { it.sectionId }
        val items = HomeLayoutConfig.DEFAULT.items.map { default ->
            val candidate = byId[default.sectionId]
            default.copy(
                zone = candidate?.zone?.takeIf { default.sectionId.allowedIn(it) } ?: default.zone,
                order = candidate?.takeIf { default.sectionId.allowedIn(it.zone) }?.order ?: default.order,
                visible = if (default.sectionId.hideable) candidate?.visible ?: default.visible else true,
            )
        }.groupBy { it.zone }.values.flatMap { zoneItems ->
            zoneItems.sortedWith(compareBy({ it.order }, { HomeSectionId.entries.indexOf(it.sectionId) }))
                .mapIndexed { index, item -> item.copy(order = index) }
        }
        return State(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items), state.customized)
    }

    internal fun initialState(stored: StoredLayout?, legacy: LegacySettings): State =
        stored?.let(::sanitize) ?: migrateLegacy(legacy)

    internal fun migrateLegacy(legacy: LegacySettings): State {
        val searchZone = if (legacy.searchPosition == AppPrefs.SEARCH_BAR_POS_TOP) {
            HomeLayoutZone.HEADER
        } else {
            HomeLayoutZone.FOOTER
        }
        val visibility = mapOf(
            HomeSectionId.ANDROID_WIDGETS to legacy.widgetsVisible,
            HomeSectionId.FAVORITES to legacy.favoritesVisible,
            HomeSectionId.SUGGESTIONS to legacy.suggestionsVisible,
            HomeSectionId.RECENT_NOTIFICATIONS to legacy.recentNotificationsVisible,
            HomeSectionId.RECENT_APPS to legacy.recentAppsVisible,
            HomeSectionId.ASSISTANT_INSIGHTS to legacy.assistantVisible,
            HomeSectionId.TICKER_OR_STATS to legacy.tickerVisible,
            HomeSectionId.MISSIONS_AND_SCORE to legacy.missionsVisible,
            HomeSectionId.MAIN_SEARCH to legacy.mainSearchVisible,
            HomeSectionId.GOOGLE_SEARCH to legacy.googleSearchVisible,
        )
        val items = HomeLayoutConfig.DEFAULT.items.map { item ->
            when (item.sectionId) {
                HomeSectionId.MAIN_SEARCH -> item.copy(zone = searchZone, order = if (searchZone == HomeLayoutZone.FOOTER) 0 else item.order,
                    visible = visibility.getValue(item.sectionId))
                HomeSectionId.DOCK -> item.copy(order = if (searchZone == HomeLayoutZone.FOOTER) 1 else 0)
                else -> item.copy(visible = visibility[item.sectionId] ?: item.visible)
            }
        }
        return sanitize(State(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items), customized = false))
    }

    private fun readLegacy(context: Context) = LegacySettings(
        searchPosition = AppPrefs.getSearchBarPosition(context),
        widgetsVisible = AppPrefs.isWidgetAreaEnabled(context),
        favoritesVisible = AppPrefs.isFavoritesEnabled(context),
        suggestionsVisible = AppPrefs.isSuggestionsEnabled(context),
        recentNotificationsVisible = AppPrefs.isRecentNotificationAppsRowEnabled(context),
        recentAppsVisible = AppPrefs.isRecentAppsEnabled(context),
        assistantVisible = AppPrefs.isAssistantCardsEnabled(context),
        tickerVisible = AppPrefs.isTickerEnabled(context),
        missionsVisible = AppPrefs.isMissionsEnabled(context),
        mainSearchVisible = AppPrefs.isHomeAppSearchEnabled(context) || AppPrefs.isHomeSearchEnabled(context),
    )

    private fun sanitizeOrder(supplied: List<HomeSectionId>, zone: HomeLayoutZone,
        searchZone: HomeLayoutZone): List<HomeSectionId> {
        val defaults = HomeLayoutConfig.DEFAULT.items.filter {
            (if (it.sectionId == HomeSectionId.MAIN_SEARCH) searchZone else it.zone) == zone
        }.map { it.sectionId }
        return supplied + defaults.filterNot { it in supplied }
    }

    private fun HomeSectionId.allowedIn(zone: HomeLayoutZone): Boolean =
        defaultZone == zone || (this == HomeSectionId.MAIN_SEARCH && zone == HomeLayoutZone.FOOTER)

    private fun parseIds(raw: String?): List<HomeSectionId> = raw.orEmpty().split(',').mapNotNull { token ->
        HomeSectionId.entries.firstOrNull { it.name == token.trim() }
    }

    private fun HomeLayoutConfig.idsIn(zone: HomeLayoutZone) = items.filter { it.zone == zone }
        .sortedBy { it.order }.map { it.sectionId }
}
