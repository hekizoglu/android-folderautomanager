package com.armutlu.apporganizer.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics sarmalayıcı — Firebase başlatılamadıysa (google-services.json yok)
 * tüm event çağrıları sessizce no-op olur. UI akışları (klasör açma, uygulama başlatma)
 * analytics yüzünden ASLA çökmez.
 */
object AppAnalytics {

    // FirebaseApp yoksa null — her logEvent güvenli şekilde atlanır
    private val analytics: FirebaseAnalytics? by lazy {
        runCatching {
            if (FirebaseApp.getApps(appContext ?: return@runCatching null).isEmpty()) null
            else Firebase.analytics
        }.getOrNull()
    }

    @Volatile private var appContext: Context? = null

    private fun log(event: String, params: Bundle? = null) {
        runCatching { analytics?.logEvent(event, params) }
    }

    fun appStarted(context: Context) {
        appContext = context.applicationContext
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
        log("app_started", Bundle().apply {
            putString("version", versionName)
            putLong("timestamp", System.currentTimeMillis())
        })
    }

    fun folderOpened(categoryId: String, categoryName: String) {
        log("folder_opened", Bundle().apply {
            putString("category_id", categoryId)
            putString("category_name", categoryName)
        })
    }

    // Not: package_name kasıtlı olarak GÖNDERİLMEZ — hangi uygulamaları kullandığı
    // Firebase'e (üçüncü taraf) sızdırılmaz; privacy-first vaadiyle çelişir (Data Safety uyumu).
    fun appLaunched(source: String) {
        // source: "home", "folder", "all_apps", "suggestions", "favorites", "recent"
        log("app_launched", Bundle().apply {
            putString("source", source)
        })
    }

    fun allAppsOpened() {
        log("all_apps_opened")
    }

    fun categoryReclassified(fromCategory: String, toCategory: String) {
        // Kullanıcı bir uygulamanın kategorisini değiştirince — öğrenme sinyali
        log("category_reclassified", Bundle().apply {
            putString("from_category", fromCategory)
            putString("to_category", toCategory)
        })
    }

    fun shortcutUsed(shortcutId: String) {
        log("shortcut_used", Bundle().apply {
            putString("shortcut_id", shortcutId)
        })
    }

    fun searchPerformed(query: String, resultCount: Int) {
        log("search_performed", Bundle().apply {
            putString("query_length", query.length.coerceAtMost(5).toString())
            putInt("result_count", resultCount)
        })
    }
}
