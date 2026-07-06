package com.armutlu.apporganizer.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * Firebase Analytics sarmalayıcı.
 *
 * google-services.json olmadan derlenen (skipGoogleServices) yapılandırmalarda
 * Firebase varsayılan uygulaması başlatılamaz. Bu durumda tüm event çağrıları
 * sessizce no-op olur — uygulama çökmez.
 */
object AppAnalytics {

    // FirebaseApp başlatılamamışsa null kalır; her çağrı güvenli şekilde atlanır.
    // Firebase.analytics, varsayılan FirebaseApp yoksa IllegalStateException fırlatır.
    private val analytics: FirebaseAnalytics? by lazy {
        try {
            Firebase.analytics
        } catch (e: Exception) {
            Timber.w(e, "FirebaseAnalytics kullanılamıyor")
            null
        }
    }

    private inline fun log(name: String, block: () -> Bundle?) {
        val a = analytics ?: return
        try {
            a.logEvent(name, block())
        } catch (e: Exception) {
            Timber.w(e, "Analytics event gönderilemedi: $name")
        }
    }

    fun appStarted(context: Context) {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
        log("app_started") {
            Bundle().apply {
                putString("version", versionName)
                putLong("timestamp", System.currentTimeMillis())
            }
        }
    }

    fun folderOpened(categoryId: String, categoryName: String) {
        log("folder_opened") {
            Bundle().apply {
                putString("category_id", categoryId)
                putString("category_name", categoryName)
            }
        }
    }

    fun appLaunched(packageName: String, source: String) {
        // source: "home", "folder", "all_apps", "suggestions", "favorites", "recent"
        log("app_launched") {
            Bundle().apply {
                putString("package_name", packageName)
                putString("source", source)
            }
        }
    }

    fun allAppsOpened() {
        log("all_apps_opened") { null }
    }

    fun categoryReclassified(packageName: String, fromCategory: String, toCategory: String) {
        // Kullanıcı bir uygulamanın kategorisini değiştirince — öğrenme sinyali
        log("category_reclassified") {
            Bundle().apply {
                putString("package_name", packageName)
                putString("from_category", fromCategory)
                putString("to_category", toCategory)
            }
        }
    }

    fun shortcutUsed(packageName: String, shortcutId: String) {
        log("shortcut_used") {
            Bundle().apply {
                putString("package_name", packageName)
                putString("shortcut_id", shortcutId)
            }
        }
    }

    fun searchPerformed(query: String, resultCount: Int) {
        log("search_performed") {
            Bundle().apply {
                putString("query_length", query.length.coerceAtMost(5).toString())
                putInt("result_count", resultCount)
            }
        }
    }
}
