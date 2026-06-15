package com.armutlu.apporganizer.utils

import android.content.Context

object WidgetPrefs {
    private const val PREFS_NAME = "widget_prefs"
    private const val KEY_WIDGET_IDS = "widget_ids"

    fun getWidgetIds(context: Context): List<Int> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WIDGET_IDS, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun saveWidgetIds(context: Context, ids: List<Int>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WIDGET_IDS, ids.joinToString(",")).apply()
    }

    fun addWidgetId(context: Context, id: Int) {
        val current = getWidgetIds(context)
        if (id !in current) saveWidgetIds(context, current + id)
    }

    fun removeWidgetId(context: Context, id: Int) {
        saveWidgetIds(context, getWidgetIds(context).filter { it != id })
    }

    /**
     * Widget'ı hem AppWidgetHost'tan hem SharedPrefs'ten kaldırır.
     * Widget kaldırma akışında bu metodu çağır — sadece removeWidgetId() çağırma,
     * aksi halde AppWidgetHost ID'si serbest bırakılmaz ve kaynak sızıntısı oluşur.
     */
    fun deleteWidget(context: Context, id: Int) {
        WidgetHostManager.deleteId(context, id)
        removeWidgetId(context, id)
    }
}
