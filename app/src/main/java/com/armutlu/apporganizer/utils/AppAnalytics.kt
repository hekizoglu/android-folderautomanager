package com.armutlu.apporganizer.utils

import android.content.Context
import android.os.Bundle
import com.armutlu.apporganizer.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AppAnalytics {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    fun appStarted(context: Context) {
        analytics.logEvent("app_started", Bundle().apply {
            putString("version", BuildConfig.VERSION_NAME)
            putLong("timestamp", System.currentTimeMillis())
        })
    }

    fun folderOpened(categoryId: String, categoryName: String) {
        analytics.logEvent("folder_opened", Bundle().apply {
            putString("category_id", categoryId)
            putString("category_name", categoryName)
        })
    }

    fun appLaunched(packageName: String, source: String) {
        // source: "home", "folder", "all_apps", "suggestions", "favorites", "recent"
        analytics.logEvent("app_launched", Bundle().apply {
            putString("package_name", packageName)
            putString("source", source)
        })
    }

    fun allAppsOpened() {
        analytics.logEvent("all_apps_opened", null)
    }

    fun categoryReclassified(packageName: String, fromCategory: String, toCategory: String) {
        // Kullanıcı bir uygulamanın kategorisini değiştirince — öğrenme sinyali
        analytics.logEvent("category_reclassified", Bundle().apply {
            putString("package_name", packageName)
            putString("from_category", fromCategory)
            putString("to_category", toCategory)
        })
    }

    fun shortcutUsed(packageName: String, shortcutId: String) {
        analytics.logEvent("shortcut_used", Bundle().apply {
            putString("package_name", packageName)
            putString("shortcut_id", shortcutId)
        })
    }

    fun searchPerformed(query: String, resultCount: Int) {
        analytics.logEvent("search_performed", Bundle().apply {
            putString("query_length", query.length.coerceAtMost(5).toString())
            putInt("result_count", resultCount)
        })
    }
}
