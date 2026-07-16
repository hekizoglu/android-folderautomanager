package com.armutlu.apporganizer.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.armutlu.apporganizer.telemetry.CountBucket
import com.armutlu.apporganizer.telemetry.QueryLengthBucket
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryEventValidator

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

    private fun log(event: TelemetryEvent) {
        if (!TelemetryEventValidator.isValid(event)) return
        val params = event.parameters.takeIf { it.isNotEmpty() }?.let { values ->
            Bundle().apply { values.forEach { (key, value) -> putString(key, value) } }
        }
        runCatching { analytics?.logEvent(event.eventName, params) }
    }

    fun appStarted(context: Context) {
        appContext = context.applicationContext
        log(TelemetryEvent.AppStarted)
    }

    fun folderOpened(categoryId: String, categoryName: String) {
        log(TelemetryEvent.FolderOpened)
    }

    // Not: package_name kasıtlı olarak GÖNDERİLMEZ — hangi uygulamaları kullandığı
    // Firebase'e (üçüncü taraf) sızdırılmaz; privacy-first vaadiyle çelişir (Data Safety uyumu).
    fun appLaunched(source: String) {
        // source: "home", "folder", "all_apps", "suggestions", "favorites", "recent"
        TelemetryEvent.Source.from(source)?.let { log(TelemetryEvent.AppLaunched(it)) }
    }

    fun allAppsOpened() {
        log(TelemetryEvent.AllAppsOpened)
    }

    fun categoryReclassified(fromCategory: String, toCategory: String) {
        // Kullanıcı bir uygulamanın kategorisini değiştirince — öğrenme sinyali
        log(TelemetryEvent.CategoryReclassified)
    }

    fun shortcutUsed(shortcutId: String) {
        log(TelemetryEvent.ShortcutUsed)
    }

    fun searchPerformed(query: String, resultCount: Int) {
        log(TelemetryEvent.SearchPerformed(
            queryLength = QueryLengthBucket.from(query.length),
            resultCount = CountBucket.from(resultCount)
        ))
    }
}
