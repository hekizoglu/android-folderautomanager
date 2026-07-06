package com.armutlu.apporganizer.utils

import android.content.Context
import java.util.Locale

/**
 * Arama geçmişi — 2 saat TTL'li: her girdi "query::epochMillis" formatında saklanır,
 * okurken süresi dolanlar temizlenir. Eski formatlı ("::"suz) girdiler süresi dolmuş sayılır.
 * AppPrefs.isSearchHistoryEnabled kapalıysa hiçbir şey kaydedilmez/dönmez.
 */
object SearchHistoryPrefs {
    private const val PREFS = "search_history"
    private const val KEY   = "recent_queries"
    private const val MAX   = 5
    private const val SEP   = "||"
    private const val TS_SEP = "::"
    private const val TTL_MS = 2 * 60 * 60 * 1000L  // 2 saat

    private val trLocale = Locale("tr")

    fun getHistory(context: Context): List<String> {
        if (!AppPrefs.isSearchHistoryEnabled(context)) return emptyList()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        val fresh = raw.split(SEP).mapNotNull { entry ->
            val idx = entry.lastIndexOf(TS_SEP)
            if (idx <= 0) return@mapNotNull null  // eski format → süresi dolmuş say
            val query = entry.substring(0, idx)
            val ts = entry.substring(idx + TS_SEP.length).toLongOrNull() ?: return@mapNotNull null
            if (query.isBlank() || now - ts > TTL_MS) null else query to ts
        }
        // Süresi dolanlar varsa listeyi budayıp geri yaz
        if (fresh.size != raw.split(SEP).count { it.isNotBlank() }) {
            persist(context, fresh)
        }
        return fresh.map { it.first }
    }

    fun addQuery(context: Context, query: String) {
        if (!AppPrefs.isSearchHistoryEnabled(context)) return
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val now = System.currentTimeMillis()
        val current = getHistoryWithTs(context)
            .filterNot { it.first.lowercase(trLocale) == trimmed.lowercase(trLocale) }
            .toMutableList()
        current.add(0, trimmed to now)
        persist(context, current.take(MAX))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }

    private fun getHistoryWithTs(context: Context): List<Pair<String, Long>> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        return raw.split(SEP).mapNotNull { entry ->
            val idx = entry.lastIndexOf(TS_SEP)
            if (idx <= 0) return@mapNotNull null
            val query = entry.substring(0, idx)
            val ts = entry.substring(idx + TS_SEP.length).toLongOrNull() ?: return@mapNotNull null
            if (query.isBlank() || now - ts > TTL_MS) null else query to ts
        }
    }

    private fun persist(context: Context, entries: List<Pair<String, Long>>) {
        val saved = entries.joinToString(SEP) { "${it.first}$TS_SEP${it.second}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, saved).apply()
    }
}
