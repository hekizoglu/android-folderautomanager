package com.armutlu.apporganizer.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo

data class WidgetSuggestion(
    val packageName: String,
    val appName: String,
    val widgetCount: Int,
    val usageCount: Long
)

object WidgetSuggestionEngine {

    fun getSuggestions(context: Context, apps: List<AppInfo>, topN: Int = 5): List<WidgetSuggestion> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        val providers = runCatching { manager.installedProviders }.getOrDefault(emptyList())

        // Paket → widget sayısı haritası
        val widgetsByPkg = providers
            .groupingBy { it.provider.packageName }
            .eachCount()

        // Kullanıcının en çok kullandığı uygulamaları sırala, widget'ı olanları filtrele
        return apps
            .filter { !it.isHidden && !it.isSystemApp && widgetsByPkg.containsKey(it.packageName) }
            .sortedByDescending { it.usageCount }
            .take(topN)
            .map { app ->
                WidgetSuggestion(
                    packageName = app.packageName,
                    appName = app.appName,
                    widgetCount = widgetsByPkg[app.packageName] ?: 0,
                    usageCount = app.usageCount
                )
            }
    }
}
