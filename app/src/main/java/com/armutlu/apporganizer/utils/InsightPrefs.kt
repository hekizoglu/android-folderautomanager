package com.armutlu.apporganizer.utils

import android.content.Context

object InsightPrefs {
    private const val PREFS = "insight_prefs"
    private const val KEY_HISTORY = "insight_history"
    private const val HISTORY_SIZE = 6  // son 6 kart tekrar etmesin

    fun getHistory(context: Context): Set<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    fun addToHistory(context: Context, ids: List<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = getHistory(context).toMutableList()
        current.addAll(ids)
        val trimmed = current.takeLast(HISTORY_SIZE)
        prefs.edit().putString(KEY_HISTORY, trimmed.joinToString(",")).apply()
    }
}
