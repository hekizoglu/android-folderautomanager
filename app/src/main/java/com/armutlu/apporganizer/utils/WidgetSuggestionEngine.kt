package com.armutlu.apporganizer.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WidgetSuggestion(
    val packageName: String,
    val appName: String,
    val widgetCount: Int,
    val launchCount: Long   // kez açıldı (adet) — gösterim için
)

object WidgetSuggestionEngine {

    // P1-19 FIX: suspend fun yapılıp Dispatchers.IO'da çalışsın (AppWidgetManager.installedProviders Main thread'de ANR riski)
    suspend fun getSuggestions(context: Context, apps: List<AppInfo>, topN: Int = 5): List<WidgetSuggestion> =
        withContext(Dispatchers.IO) {
            val manager = AppWidgetManager.getInstance(context) ?: return@withContext emptyList()
            val providers = runCatching { manager.installedProviders }.getOrDefault(emptyList())

            // Paket → widget sayısı haritası
            val widgetsByPkg = providers
                .groupingBy { it.provider.packageName }
                .eachCount()

            // Kullanıcının en çok kullandığı uygulamaları sırala, widget'ı olanları filtrele
            apps
                .filter { !it.isHidden && !it.isSystemApp && widgetsByPkg.containsKey(it.packageName) }
                .sortedByDescending { it.usageCount }   // ms bazlı sıralama (gerçek kullanım büyüklüğü)
                .take(topN)
                .map { app ->
                    WidgetSuggestion(
                        packageName = app.packageName,
                        appName = app.appName,
                        widgetCount = widgetsByPkg[app.packageName] ?: 0,
                        launchCount = app.launchCount
                    )
                }
        }
}
