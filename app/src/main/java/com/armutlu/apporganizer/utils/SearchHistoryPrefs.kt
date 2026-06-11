package com.armutlu.apporganizer.utils

import android.content.Context

object SearchHistoryPrefs {
    private const val PREFS = "search_history"
    private const val KEY   = "recent_queries"
    private const val MAX   = 5

    fun getHistory(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return raw.split("||").filter { it.isNotBlank() }
    }

    fun addQuery(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val current = getHistory(context).toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val saved = current.take(MAX).joinToString("||")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, saved).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }
}
