package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.domain.models.SourceType
import org.json.JSONArray
import org.json.JSONObject

/**
 * P1.2 - Ana ekran tam ekran arama icin cihaz-ici kisa gecmis.
 *
 * Sadece kullanicinin launcher aramasinda sectigi son sorgular/sonuclar tutulur.
 * Max 3 kayit saklanir, ayni sorgu tekrar secilirse yeni kayit basa tasinir ve ciftlenmez.
 */
object SearchHistoryPrefs {
    private const val FILE_NAME = "search_history_prefs"
    private const val KEY_ITEMS = "search_history_items"
    const val MAX_ITEMS = 3

    data class SearchHistoryItem(
        val query: String,
        val title: String,
        val sourceType: String,
        val sourceId: String,
        val atMillis: Long,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun record(
        context: Context,
        query: String,
        title: String,
        sourceType: SourceType,
        sourceId: String,
        atMillis: Long = System.currentTimeMillis(),
    ) {
        val normalizedQuery = query.trim()
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || sourceId.isBlank()) return
        val updated = dedupeAndPrepend(
            existing = getAll(context),
            incoming = SearchHistoryItem(
                query = normalizedQuery,
                title = normalizedTitle,
                sourceType = sourceType.key,
                sourceId = sourceId,
                atMillis = atMillis,
            ),
            maxItems = MAX_ITEMS,
        )
        prefs(context).edit().putString(KEY_ITEMS, updated.toJson()).apply()
    }

    fun getAll(context: Context): List<SearchHistoryItem> {
        val raw = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        return parseJson(raw)
    }

    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_ITEMS).apply()
    }

    fun dedupeAndPrepend(
        existing: List<SearchHistoryItem>,
        incoming: SearchHistoryItem,
        maxItems: Int,
    ): List<SearchHistoryItem> {
        val filtered = existing.filterNot {
            when {
                incoming.query.isNotBlank() -> it.query.equals(incoming.query, ignoreCase = true)
                else -> it.sourceType == incoming.sourceType && it.sourceId == incoming.sourceId
            }
        }
        return listOf(incoming) + filtered.take(maxItems - 1)
    }

    fun List<SearchHistoryItem>.toJson(): String {
        val arr = JSONArray()
        for (item in this) {
            arr.put(
                JSONObject().apply {
                    put("query", item.query)
                    put("title", item.title)
                    put("sourceType", item.sourceType)
                    put("sourceId", item.sourceId)
                    put("atMillis", item.atMillis)
                }
            )
        }
        return arr.toString()
    }

    fun parseJson(raw: String): List<SearchHistoryItem> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val query = obj.optString("query", "").trim()
                    val title = obj.optString("title", "").trim()
                    val sourceType = obj.optString("sourceType", "").trim()
                    val sourceId = obj.optString("sourceId", "").trim()
                    if (title.isBlank() || sourceType.isBlank() || sourceId.isBlank()) continue
                    add(
                        SearchHistoryItem(
                            query = query,
                            title = title,
                            sourceType = sourceType,
                            sourceId = sourceId,
                            atMillis = obj.optLong("atMillis", 0L),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
