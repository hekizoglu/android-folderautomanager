package com.armutlu.apporganizer.utils

import android.content.Context
import com.armutlu.apporganizer.telemetry.FolderAppCountBucket
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
        // Intentionally not part of the first-version analytics catalog.
    }

    fun folderOpened(folderType: TelemetryEvent.FolderType, appCount: FolderAppCountBucket) {
        log(TelemetryEvent.FolderOpened(folderType, appCount))
    }

    // Not: package_name kasıtlı olarak GÖNDERİLMEZ — hangi uygulamaları kullandığı
    // Firebase'e (üçüncü taraf) sızdırılmaz; privacy-first vaadiyle çelişir (Data Safety uyumu).
    fun appLaunched(source: String) {
        // Intentionally omitted: exact app launches are outside the approved catalog.
    }

    fun allAppsOpened() {
        // Intentionally not part of the first-version analytics catalog.
    }

    fun homePageViewed(
        pageType: TelemetryEvent.HomePageType,
        pagePosition: TelemetryEvent.HomePagePositionBucket,
        navigationSource: TelemetryEvent.HomeNavigationSource,
        searchPosition: TelemetryEvent.HomeSearchPosition,
        startMode: TelemetryEvent.HomeStartMode,
        deviceClass: TelemetryEvent.HomeTelemetryDeviceClass,
    ) {
        log(
            TelemetryEvent.HomePageViewed(
                pageType = pageType,
                pagePosition = pagePosition,
                navigationSource = navigationSource,
                searchPosition = searchPosition,
                startMode = startMode,
                deviceClass = deviceClass,
            )
        )
    }

    fun homePageSwiped(
        pageType: TelemetryEvent.HomePageType,
        pagePosition: TelemetryEvent.HomePagePositionBucket,
        deviceClass: TelemetryEvent.HomeTelemetryDeviceClass,
    ) {
        log(TelemetryEvent.HomePageSwiped(pageType, pagePosition, deviceClass))
    }

    fun homeSearchOpened(
        searchPosition: TelemetryEvent.HomeSearchPosition,
        pageType: TelemetryEvent.HomePageType,
    ) {
        log(TelemetryEvent.HomeSearchOpened(searchPosition, pageType))
    }

    fun allAppsOpenedFromPage(
        pageType: TelemetryEvent.HomePageType,
        pagePosition: TelemetryEvent.HomePagePositionBucket,
        deviceClass: TelemetryEvent.HomeTelemetryDeviceClass,
    ) {
        log(TelemetryEvent.AllAppsOpenedFromPage(pageType, pagePosition, deviceClass))
    }

    fun homeButtonNavigation(
        startMode: TelemetryEvent.HomeStartMode,
        targetPageType: TelemetryEvent.HomePageType,
    ) {
        log(TelemetryEvent.HomeButtonNavigation(startMode, targetPageType))
    }

    fun homeStartModeChanged(startMode: TelemetryEvent.HomeStartMode) {
        log(TelemetryEvent.HomeStartModeChanged(startMode))
    }

    fun smartDashboardToggled(enabled: Boolean) {
        log(
            TelemetryEvent.SmartDashboardToggled(
                if (enabled) TelemetryEvent.ToggleState.ENABLED else TelemetryEvent.ToggleState.DISABLED
            )
        )
    }

    fun categoryReclassified(
        sourceType: TelemetryEvent.SourceType,
        resultType: TelemetryEvent.TargetType,
        confidence: TelemetryEvent.ConfidenceBucket
    ) {
        log(TelemetryEvent.ClassificationCorrected(sourceType, confidence, resultType))
    }

    fun shortcutUsed(shortcutId: String) {
        // Raw shortcut identifiers are deliberately never logged.
    }

    fun searchPerformed(
        resultCount: TelemetryEvent.ResultBucket,
        latency: TelemetryEvent.LatencyBucket,
        sourceMix: TelemetryEvent.SearchSourceMix
    ) {
        log(TelemetryEvent.SearchPerformed(
            result = resultCount,
            latency = latency,
            sourceMix = sourceMix
        ))
    }
}
