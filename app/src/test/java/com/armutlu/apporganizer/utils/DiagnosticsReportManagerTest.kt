package com.armutlu.apporganizer.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportManagerTest {

    @Test
    fun renderReport_includesRequiredSectionsAndFields() {
        val report = renderReport(sampleSnapshot())

        assertTrue(report.contains("[Uygulama]"))
        assertTrue(report.contains("Surum: 1.3.36 (59)"))
        assertTrue(report.contains("Android: 16 (API 36)"))
        assertTrue(report.contains("[Izinler]"))
        assertTrue(report.contains("POST_NOTIFICATIONS: granted"))
        assertTrue(report.contains("[Uygulama Katalogu]"))
        assertTrue(report.contains("Son reconcile: 2026-07-15 16:59:40"))
        assertTrue(report.contains("[Siniflandirma]"))
        assertTrue(report.contains("Mod: MANUAL_REVIEW_ONLY"))
        assertTrue(report.contains("[Arama ve Indeks]"))
        assertTrue(report.contains("Arama sayaci: total=12, zero=3, avgLatencyMs=44, totalClicks=9, firstResultClicks=4"))
        assertTrue(report.contains("[Bildirimler]"))
        assertTrue(report.contains("Event sayisi son 7 gun: 11"))
        assertTrue(report.contains("[Misyon Motoru]"))
        assertTrue(report.contains("Son gorev olayi: Focus Sprint (delta=3, at=2026-07-15 15:00:00)"))
        assertTrue(report.contains("[Widgetler]"))
        assertTrue(report.contains("[Worker Ozeti]"))
        assertTrue(report.contains("Weekly digest: enabled=evet, work=ENQUEUED, attempts=0"))
        assertTrue(report.contains("[Kritik Hatalar]"))
        assertTrue(report.contains("1. safeMode=hayir, summary=IllegalStateException: sample"))
        assertTrue(report.contains("[Gizlilik Notu]"))
    }

    @Test
    fun renderReport_doesNotLeakPersonalDataFieldsByDefault() {
        val report = renderReport(sampleSnapshot())

        assertFalse(report.contains("com.example.secret"))
        assertFalse(report.contains("5551234567"))
        assertFalse(report.contains("Ayse Yilmaz"))
        assertFalse(report.contains("top secret search"))
        assertTrue(report.contains("paket listesi, bildirim metni, kisi adi/numarasi ve arama sorgulari icermez"))
    }

    private fun sampleSnapshot() = DiagnosticsReportSnapshot(
        generatedAt = "2026-07-15 16:59:40",
        appVersionName = "1.3.36",
        appVersionCode = 59,
        packageVersionName = "1.3.36",
        packageLongVersionCode = 59,
        deviceName = "Google Pixel",
        androidVersion = "16 (API 36)",
        notificationListenerEnabled = "evet",
        usageAccessEnabled = "evet",
        postNotificationsState = "granted",
        readContactsState = "denied",
        coarseLocationState = "granted",
        totalApps = 120,
        userApps = 87,
        systemApps = 33,
        hiddenApps = 4,
        categoryCount = 12,
        lastReconcileAt = "2026-07-15 16:59:40",
        lastUsageSyncAt = "2026-07-15 16:30:00",
        classificationMode = "MANUAL_REVIEW_ONLY",
        uncategorizedCount = 8,
        pendingReviewCount = 5,
        confirmedCount = 64,
        skippedCount = 2,
        searchSourcesLine = "apps=evet, categories=evet, settings=evet, contacts=hayir, files=evet",
        fileIndexItemCount = 215,
        fileIndexLastIndexedAt = "2026-07-15 16:45:00",
        fileIndexFailureReason = "-",
        searchCounterLine = "total=12, zero=3, avgLatencyMs=44, totalClicks=9, firstResultClicks=4",
        notificationAnalyticsEnabled = "evet",
        notificationTotal = 340,
        notificationLast7d = 11,
        missionsEnabled = "evet",
        wrappedEnabled = "evet",
        missionPrefsMigrated = "evet",
        totalStars = 42,
        latestMissionEvent = "Focus Sprint (delta=3, at=2026-07-15 15:00:00)",
        widgetSummary = "Kayitli widget id: 3, provider bulunan: 2",
        workerSummary = listOf(
            "Weekly digest: enabled=evet, work=ENQUEUED, attempts=0",
            "Files index periodic: enabled=evet, work=RUNNING, attempts=1",
        ),
        crashSummary = listOf(
            "1. safeMode=hayir, summary=IllegalStateException: sample",
        ),
    )
}
