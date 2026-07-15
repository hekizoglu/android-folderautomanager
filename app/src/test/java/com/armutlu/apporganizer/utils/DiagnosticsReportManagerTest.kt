package com.armutlu.apporganizer.utils

import androidx.work.WorkInfo
import com.armutlu.apporganizer.domain.usecase.classify.AttentionReason
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsReportManagerTest {

    private val now = 1_800_000_000_000L

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
        assertTrue(report.contains("Kullanici uygulamasi toplam: 87"))
        assertTrue(report.contains("Sayac toplami: 87"))
        assertTrue(report.contains("Tutarlilik: OK"))
        assertTrue(report.contains("Dikkat nedenleri:"))
        assertTrue(report.contains("REVIEW_PENDING=5"))
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

    @Test
    fun oneShotSucceeded_doesNotRenderNextDate() {
        val text = workerNextRunText(
            state = WorkInfo.State.SUCCEEDED,
            kind = WorkerKind.ONE_SHOT,
            nextScheduleTimeMillis = Long.MAX_VALUE,
            now = now,
        )

        assertFalse(text.orEmpty().contains("next="))
        assertFalse(text.orEmpty().contains("292278994"))
    }

    @Test
    fun oneShotSucceeded_rendersNoNextRunMessage() {
        val text = workerNextRunText(
            state = WorkInfo.State.SUCCEEDED,
            kind = WorkerKind.ONE_SHOT,
            nextScheduleTimeMillis = Long.MAX_VALUE,
            now = now,
        )

        assertEquals("tamamlandi, sonraki calisma yok", text)
    }

    @Test
    fun periodicEnqueued_rendersReasonableNextDate() {
        val text = workerNextRunText(
            state = WorkInfo.State.ENQUEUED,
            kind = WorkerKind.PERIODIC,
            nextScheduleTimeMillis = now + 60_000L,
            now = now,
        )

        assertTrue(text.orEmpty().startsWith("next="))
        assertFalse(text.orEmpty().contains("292278994"))
    }

    @Test
    fun longMaxValue_isTreatedAsNoNextRun() {
        val text = workerNextRunText(
            state = WorkInfo.State.ENQUEUED,
            kind = WorkerKind.PERIODIC,
            nextScheduleTimeMillis = Long.MAX_VALUE,
            now = now,
        )

        assertEquals("sonraki calisma yok", text)
    }

    @Test
    fun farFutureSentinel_isNotFormattedAsDate() {
        val farFuture = now + java.util.concurrent.TimeUnit.DAYS.toMillis(3651L)

        val text = workerNextRunText(
            state = WorkInfo.State.ENQUEUED,
            kind = WorkerKind.PERIODIC,
            nextScheduleTimeMillis = farFuture,
            now = now,
        )

        assertEquals("sonraki calisma yok", text)
        assertFalse(text.orEmpty().contains("next="))
    }

    @Test
    fun failedWork_doesNotRenderNextDate() {
        val text = workerNextRunText(
            state = WorkInfo.State.FAILED,
            kind = WorkerKind.PERIODIC,
            nextScheduleTimeMillis = now + 60_000L,
            now = now,
        )

        assertEquals("basarisiz", text)
        assertFalse(text.orEmpty().contains("next="))
    }

    @Test
    fun runningWork_rendersCurrentlyRunning() {
        val text = workerNextRunText(
            state = WorkInfo.State.RUNNING,
            kind = WorkerKind.PERIODIC,
            nextScheduleTimeMillis = now + 60_000L,
            now = now,
        )

        assertEquals("su anda calisiyor", text)
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
        classificationDiagnostics = ClassificationDiagnostics(
            totalUserApps = 87,
            hiddenUserApps = 4,
            automaticAccepted = 68,
            needsAttention = 5,
            snoozed = 0,
            confirmed = 4,
            corrected = 0,
            skipped = 2,
            uncategorized = 8,
            invalidOrUnknown = 0,
            reconciledTotal = 87,
            isConsistent = true,
            attentionByReason = AttentionReason.entries.associateWith { reason ->
                if (reason == AttentionReason.REVIEW_PENDING) 5 else 0
            },
        ),
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
