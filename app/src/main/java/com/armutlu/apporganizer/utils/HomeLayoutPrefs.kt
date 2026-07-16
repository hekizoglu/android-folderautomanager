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

    internal data class StoredLayout(
        val headerOrder: String?, val footerOrder: String?, val hiddenSections: String?,
        val version: Int?, val customized: Boolean?,
    )

    fun read(context: Context): State {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        val orders = mapOf(
            HomeLayoutZone.HEADER to sanitizeOrder(stored.headerOrder, HomeLayoutZone.HEADER),
            HomeLayoutZone.FOOTER to sanitizeOrder(stored.footerOrder, HomeLayoutZone.FOOTER),
        )
        val hidden = parseIds(stored.hiddenSections).filterTo(mutableSetOf()) { it.hideable }
        val items = HomeLayoutConfig.DEFAULT.items.map { item ->
            val index = orders[item.zone]?.indexOf(item.sectionId)?.takeIf { it >= 0 }
            item.copy(order = index ?: item.order, visible = item.sectionId !in hidden)
        }
        return State(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items), stored.customized == true)
    }

    internal fun sanitize(state: State): State {
        val byId = state.config.items.associateBy { it.sectionId }
        val items = HomeLayoutConfig.DEFAULT.items.map { default ->
            val candidate = byId[default.sectionId]
            default.copy(
                order = candidate?.takeIf { it.zone == default.zone }?.order ?: default.order,
                visible = if (default.sectionId.hideable) candidate?.visible ?: default.visible else true,
            )
        }.groupBy { it.zone }.values.flatMap { zoneItems ->
            zoneItems.sortedWith(compareBy({ it.order }, { HomeSectionId.entries.indexOf(it.sectionId) }))
                .mapIndexed { index, item -> item.copy(order = index) }
        }
        return State(HomeLayoutConfig(HomeLayoutConfig.CURRENT_VERSION, items), state.customized)
    }

    private fun sanitizeOrder(raw: String?, zone: HomeLayoutZone): List<HomeSectionId> {
        val supplied = parseIds(raw).filter { it.defaultZone == zone }.distinct()
        val defaults = HomeLayoutConfig.DEFAULT.idsIn(zone)
        return supplied + defaults.filterNot { it in supplied }
    }

    private fun parseIds(raw: String?): List<HomeSectionId> = raw.orEmpty().split(',').mapNotNull { token ->
        HomeSectionId.entries.firstOrNull { it.name == token.trim() }
    }

    private fun HomeLayoutConfig.idsIn(zone: HomeLayoutZone) = items.filter { it.zone == zone }
        .sortedBy { it.order }.map { it.sectionId }
}
