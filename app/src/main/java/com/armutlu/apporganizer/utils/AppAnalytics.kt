package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.telemetry.CountBucket
import com.armutlu.apporganizer.telemetry.FolderAppCountBucket
import com.armutlu.apporganizer.telemetry.QueryLengthBucket
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryManager

/**
 * Firebase Analytics sarmalayıcı — Firebase başlatılamadıysa (google-services.json yok)
 * tüm event çağrıları sessizce no-op olur. UI akışları (klasör açma, uygulama başlatma)
 * analytics yüzünden ASLA çökmez.
 */
object AppAnalytics {

    // FirebaseApp yoksa null — her logEvent güvenli şekilde atlanır
    private fun log(event: TelemetryEvent) {
        TelemetryManager.log(event)
    }

    fun appStarted(context: Context) {
        log(TelemetryEvent.AppStarted)
    }

    fun folderOpened(folderType: TelemetryEvent.FolderType, appCount: FolderAppCountBucket) {
        log(TelemetryEvent.FolderOpened(folderType, appCount))
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

    fun categoryReclassified(
        sourceType: TelemetryEvent.CategorySourceType,
        resultType: TelemetryEvent.CategoryResultType,
        confidence: TelemetryEvent.ConfidenceBucket
    ) {
        log(TelemetryEvent.CategoryReclassified(sourceType, resultType, confidence))
    }

    fun shortcutUsed(shortcutId: String) {
        log(TelemetryEvent.ShortcutUsed)
    }

    fun searchPerformed(
        queryLength: QueryLengthBucket,
        resultCount: CountBucket,
        latency: TelemetryEvent.LatencyBucket,
        sourceMix: TelemetryEvent.SearchSourceMix
    ) {
        log(TelemetryEvent.SearchPerformed(
            queryLength = queryLength,
            resultCount = resultCount,
            latency = latency,
            sourceMix = sourceMix
        ))
    }
}
