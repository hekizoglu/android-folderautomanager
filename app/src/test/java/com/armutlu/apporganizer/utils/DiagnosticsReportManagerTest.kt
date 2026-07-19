package com.armutlu.apporganizer.utils

import androidx.work.WorkInfo
import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.MissingReason
import com.armutlu.apporganizer.domain.home.HomeIntelligenceHealthReport
import com.armutlu.apporganizer.domain.home.HomeIntelligenceState
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
        assertTrue(report.contains("Rapor standardi: v2 | AI analizine uygun yapilandirilmis cikti"))
        assertTrue(report.contains("[Yonetici Ozeti]"))
        assertTrue(report.contains("Genel durum:"))
        assertTrue(report.contains("[Oncelikli Aksiyonlar]"))
        assertTrue(report.contains("Surum: 1.3.36 (59)"))
        assertTrue(report.contains("Android: 16 (API 36)"))
        assertTrue(report.contains("[Izinler]"))
        assertTrue(report.contains("POST_NOTIFICATIONS: granted"))
        assertTrue(report.contains("Konum: gerekli=hayir, izin=denied, saglik=NORMAL_KULLANILMIYOR"))
        assertTrue(report.contains("[Depolama]"))
        assertTrue(report.contains("toplam=665 B"))
        assertTrue(report.contains("[Baslangic ve Cikis Sagligi]"))
        assertTrue(report.contains("ANR=2, lowMemory=1, nativeCrash=0; trace=rapora_dahil_degil"))
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
        assertTrue(report.contains("Arama sayaci: total=12, zero=3, zeroRate=25.0%, avgLatencyMs=44"))
        assertTrue(report.contains("Sonuc etkilesimi: totalClicks=9, clickThroughRate=75.0%, firstResultClicks=4, firstResultRate=44.4%"))
        assertTrue(report.contains("Tiklama kaynaklari: app=6, contact=2, file=1"))
        assertTrue(report.contains("Hizli aksiyonlar: CALL=1, OPEN_APP=6, WHATSAPP=1"))
        assertTrue(report.contains("Ortalama sorgu uzunlugu: 5.4 karakter"))
        assertTrue(report.contains("[Bildirimler]"))
        assertTrue(report.contains("Event sayisi son 7 gun: 11"))
        assertTrue(report.contains("Event sayisi son 24 saat: 3"))
        assertTrue(report.contains("Son event zamani: 2026-07-15 15:50:00"))
        assertTrue(report.contains("Tazelik: KONTROL_ONERISI: listener acik fakat olay yok"))
        assertTrue(report.contains("[Misyon Motoru]"))
        assertTrue(report.contains("Son gorev olayi: Focus Sprint (delta=3, at=2026-07-15 15:00:00)"))
        assertTrue(report.contains("Tamamlanan gunluk gorev: 8"))
        assertTrue(report.contains("Tamamlanan haftalik gorev: 3"))
        assertTrue(report.contains("Davranis degisikligi gorevi: 9"))
        assertTrue(report.contains("Goruntuleme gorevi: 2"))
        assertTrue(report.contains("Gorev skoru: pozitif=31, negatif=-4, net=27"))
        assertTrue(report.contains("Dijital yasam skoru toplam yildizdan bagimsizdir."))
        assertTrue(report.contains("Tekrar odul engeli: aktif"))
        assertTrue(report.contains("[Widgetler]"))
        assertTrue(report.contains("[Ana Ekran Yerlesimi]"))
        assertTrue(report.contains("Arama bolgesi: HEADER"))
        assertTrue(report.contains("Widget sayisi: 2"))
        assertTrue(report.contains("Dock oge sayisi: 5"))
        assertTrue(report.contains("[Ana Ekran Mimarisi]"))
        assertTrue(report.contains("Ana ekran modu: Akilli Dashboard"))
        assertTrue(report.contains("Baslangic sayfasi: Dashboard"))
        assertTrue(report.contains("Son sayfa turu: Dashboard"))
        assertTrue(report.contains("Toplam ana sayfa: 4"))
        assertTrue(report.contains("Klasor sayfasi: 3"))
        assertTrue(report.contains("Global arama konumu: Ust"))
        assertTrue(report.contains("Pager restore: OK"))
        assertTrue(report.contains("Pager v2 flag: false"))
        assertTrue(report.contains("Gesture policy: Normal"))
        assertTrue(report.contains("[Worker Ozeti]"))
        assertTrue(report.contains("Weekly digest: enabled=evet, work=ENQUEUED, attempts=0, durum=NORMAL"))
        assertTrue(report.contains("[Gorev Sistemi]"))
        assertTrue(report.contains("[Dijital Yasam]"))
        assertTrue(report.contains("[Akilli Nabiz]"))
        assertTrue(report.contains("[Ana Ekran Zeka Sagligi Uyarilari]"))
        assertTrue(report.contains("[AI Tani Paketi]"))
        assertTrue(report.contains("## AI_SUMMARY"))
        assertTrue(report.contains("## AI_ISSUES"))
        assertTrue(report.contains("## AI_METRICS"))
        assertTrue(report.contains("classification.totalUserApps=87"))
        assertTrue(report.contains("homeLayout.widgetCount=2, dockItemCount=5"))
        assertTrue(report.contains("homeArchitecture.totalPages=4, folderPages=3, searchPosition=Ust"))
        assertTrue(report.contains("severity=WARN | area=home_intelligence | signal=MISSION_PROGRESS_DATA_STALE"))
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
        assertTrue(report.contains("Gizlilik siniri: Paket adi, bildirim metni, kisi, dosya adi ve arama sorgusu yoktur."))
    }

    @Test
    fun renderReport_filesIndexPermissionBlockedDoesNotEmitPeriodicMissingIssue() {
        val report = renderReport(
            sampleSnapshot().copy(
                workerSummary = listOf(
                    "Files index periodic: enabled=evet, eligible=hayir(permission), work=yok, durum=NORMAL_KAPALI",
                ),
            ),
        )

        assertTrue(report.contains("Files index periodic: enabled=evet, eligible=hayir(permission), work=yok"))
        assertFalse(report.contains("signal=files_index_periodic_missing"))
        assertFalse(report.contains("evidence=Files index periodic: enabled=evet, eligible=evet, work=yok"))
    }

    @Test
    fun unusedDeniedPermission_isNormal() {
        assertTrue(permissionHealthLine("Konum", granted = false, needed = false).contains("NORMAL_KULLANILMIYOR"))
    }

    @Test
    fun enabledListenerWithoutEvents_isOnlyCheckRecommendation() {
        assertEquals("KONTROL_ONERISI: listener acik fakat olay yok", notificationFreshnessState(true, null))
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

    @Test
    fun workerPlanHealth_disabledAndMissingIsNormalClosed() {
        val health = workerPlanHealth(enabled = false, hasWork = false, kind = WorkerKind.PERIODIC)

        assertEquals(WorkerPlanHealth.NORMAL_KAPALI, health)
        assertEquals("NORMAL_KAPALI", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_enabledAndMissingIsError() {
        val health = workerPlanHealth(enabled = true, hasWork = false, kind = WorkerKind.PERIODIC)

        assertEquals(WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING, health)
        assertEquals("HATA: etkin fakat work bulunamadi", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_enabledAndMissingOneShotIsNormal() {
        val health = workerPlanHealth(enabled = true, hasWork = false, kind = WorkerKind.ONE_SHOT)

        assertEquals(WorkerPlanHealth.NORMAL, health)
        assertEquals("NORMAL", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_pendingOneShotRequestAndMissingWorkIsError() {
        val health = workerPlanHealth(
            enabled = true,
            hasWork = false,
            kind = WorkerKind.ONE_SHOT,
            requested = true,
        )

        assertEquals(WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING, health)
        assertEquals("HATA: etkin fakat work bulunamadi", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_disabledButScheduledIsWarning() {
        val health = workerPlanHealth(enabled = false, hasWork = true, kind = WorkerKind.PERIODIC)

        assertEquals(WorkerPlanHealth.WARNING_DISABLED_BUT_SCHEDULED, health)
        assertEquals("UYARI: kapali ozellik icin work mevcut", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_enabledAndScheduledIsNormal() {
        val health = workerPlanHealth(enabled = true, hasWork = true, kind = WorkerKind.PERIODIC)

        assertEquals(WorkerPlanHealth.NORMAL, health)
        assertEquals("NORMAL", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_completedOneShotAndMissingWorkIsCompletedNormal() {
        val health = workerPlanHealth(
            enabled = true,
            hasWork = false,
            kind = WorkerKind.ONE_SHOT,
            telemetry = emptyTelemetry().copy(successCount = 1, lastSucceededAt = 2_500L),
        )

        assertEquals(WorkerPlanHealth.NORMAL_TAMAMLANDI, health)
        assertEquals("NORMAL_TAMAMLANDI", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_runningOneShotIsRunningNormal() {
        val health = workerPlanHealth(
            enabled = true,
            hasWork = true,
            kind = WorkerKind.ONE_SHOT,
            states = listOf(WorkInfo.State.RUNNING),
        )

        assertEquals(WorkerPlanHealth.NORMAL_CALISIYOR, health)
        assertEquals("NORMAL_CALISIYOR", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_delayedQueuedOneShotIsWarning() {
        val health = workerPlanHealth(
            enabled = true,
            hasWork = true,
            kind = WorkerKind.ONE_SHOT,
            states = listOf(WorkInfo.State.ENQUEUED),
            nextScheduleTimes = listOf(now - 16 * 60 * 1000L),
            now = now,
        )

        assertEquals(WorkerPlanHealth.WARN_GECIKMIS, health)
        assertEquals("WARN_GECIKMIS", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_failedOneShotIsError() {
        val health = workerPlanHealth(
            enabled = true,
            hasWork = true,
            kind = WorkerKind.ONE_SHOT,
            states = listOf(WorkInfo.State.FAILED),
        )

        assertEquals(WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING, health)
        assertEquals("HATA: etkin fakat work bulunamadi", workerPlanHealthText(health))
    }

    @Test
    fun workerPlanHealth_cancelledRequiredOneShotIsError() {
        val health = workerPlanHealth(
            enabled = true,
            hasWork = true,
            kind = WorkerKind.ONE_SHOT,
            requested = true,
            states = listOf(WorkInfo.State.CANCELLED),
        )

        assertEquals(WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING, health)
        assertEquals("HATA: etkin fakat work bulunamadi", workerPlanHealthText(health))
    }

    @Test
    fun workerTelemetryText_emptySnapshotRendersMissingTelemetry() {
        val text = workerTelemetryText(emptyTelemetry(), ::testDate)

        assertEquals("telemetry=yok", text)
    }

    @Test
    fun workerTelemetryText_includesLastRunFields() {
        val text = workerTelemetryText(
            WorkerTelemetryPrefs.Snapshot(
                workerName = "weekly_digest",
                lastRequestedAt = 0L,
                lastStartedAt = 1_000L,
                lastFinishedAt = 2_500L,
                lastCompletedAt = 2_500L,
                lastSucceededAt = 2_500L,
                lastFailedAt = 0L,
                lastResult = WorkerTelemetryPrefs.RESULT_SUCCESS,
                lastFailureCode = "-",
                lastDurationMs = 1_500L,
                successCount = 2,
                failureCount = 0,
            ),
            ::testDate,
        )

        assertEquals(
            "lastStart=1000, lastSuccess=2500, lastFailure=-, lastResult=SUCCESS, durationMs=1500, success=2, failure=0, failureCode=-",
            text,
        )
    }

    @Test
    fun backupHealthLine_mapsClosedStateToNormalHealth() {
        val text = backupHealthLine(
            enabled = false,
            planHealth = WorkerPlanHealth.NORMAL_KAPALI,
            lastBackupAt = 0L,
            telemetry = emptyTelemetry(),
            formatDate = ::testDate,
        )

        assertEquals("Auto backup: tercih=kapali, saglik=NORMAL, sonYedek=-, sonHata=-", text)
    }

    @Test
    fun backupHealthLine_flagsEnabledButMissingWork() {
        val text = backupHealthLine(
            enabled = true,
            planHealth = WorkerPlanHealth.ERROR_ENABLED_BUT_MISSING,
            lastBackupAt = 3_000L,
            telemetry = emptyTelemetry(lastFailureCode = "IO_ERROR"),
            formatDate = ::testDate,
        )

        assertEquals("Auto backup: tercih=acik, saglik=HATA_PLANLANMAMIS, sonYedek=3000, sonHata=IO_ERROR", text)
    }

    @Test
    fun workerTelemetryPrefs_durationNeverNegative() {
        assertEquals(0L, WorkerTelemetryPrefs.duration(startedAt = 200L, finishedAt = 100L))
    }

    @Test
    fun workerTelemetryPrefs_sanitizesFailureCode() {
        assertEquals("PATH___SECRET", WorkerTelemetryPrefs.sanitizeFailureCode("path / secret"))
        assertEquals(WorkerTelemetryPrefs.FAILURE_UNKNOWN, WorkerTelemetryPrefs.sanitizeFailureCode("   "))
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
        permissionHealthSummary = listOf(
            "Konum: gerekli=hayir, izin=denied, saglik=NORMAL_KULLANILMIYOR",
            "Kisiler: gerekli=evet, izin=granted, saglik=NORMAL",
        ),
        storageSummary = "Room=100 B, WAL=200 B, SHM=300 B, cache=65 B, toplam=665 B",
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
        searchCounterLine = "total=12, zero=3, zeroRate=25.0%, avgLatencyMs=44",
        searchInteractionLine = "totalClicks=9, clickThroughRate=75.0%, firstResultClicks=4, firstResultRate=44.4%",
        searchClickSourcesLine = "app=6, contact=2, file=1",
        searchActionLine = "CALL=1, OPEN_APP=6, WHATSAPP=1",
        searchAvgQueryLengthLine = "5.4 karakter",
        notificationAnalyticsEnabled = "evet",
        notificationTotal = 340,
        notificationLast7d = 11,
        notificationLast24h = 3,
        notificationLatestAt = "2026-07-15 15:50:00",
        notificationFreshness = "KONTROL_ONERISI: listener acik fakat olay yok",
        exitSummary = "kayit=3, ANR=2, lowMemory=1, nativeCrash=0; trace=rapora_dahil_degil",
        startupSummary = "Son cold=420ms, warm=130ms, ana ekran hazir=350ms",
        missionsEnabled = "evet",
        wrappedEnabled = "evet",
        missionPrefsMigrated = "evet",
        totalStars = 42,
        latestMissionEvent = "Focus Sprint (delta=3, at=2026-07-15 15:00:00)",
        dailyMissionCompletions = 8,
        weeklyMissionCompletions = 3,
        behaviorMissionCompletions = 9,
        viewingMissionCompletions = 2,
        positiveTaskScore = 31,
        negativeTaskScore = -4,
        widgetSummary = "Kayitli widget id: 3, provider bulunan: 2",
        homeLayoutSummary = HomeLayoutPrefs.DiagnosticsSummary(
            version = 1,
            customized = true,
            headerOrder = listOf(
                com.armutlu.apporganizer.domain.models.HomeSectionId.CLOCK,
                com.armutlu.apporganizer.domain.models.HomeSectionId.MAIN_SEARCH,
            ),
            hiddenSections = listOf(com.armutlu.apporganizer.domain.models.HomeSectionId.GOOGLE_SEARCH),
            searchZone = com.armutlu.apporganizer.domain.models.HomeLayoutZone.HEADER,
            widgetCount = 2,
            dockItemCount = 5,
        ),
        homeStartPageMode = "SMART_DASHBOARD",
        homeLastPageAnchorType = "DASHBOARD",
        homeArchitecture = HomeArchitectureDiagnostics(
            homeMode = "Akilli Dashboard",
            startPage = "Dashboard",
            lastPageType = "Dashboard",
            totalPageCount = 4,
            folderPageCount = 3,
            globalSearchPosition = "Ust",
            pagerRestore = "OK",
            gesturePolicy = "Normal",
        ),
        workerSummary = listOf(
            "Weekly digest: enabled=evet, work=ENQUEUED, attempts=0, durum=NORMAL",
            "Files index periodic: enabled=evet, work=RUNNING, attempts=1",
        ),
        crashSummary = listOf(
            "1. safeMode=hayir, summary=IllegalStateException: sample",
        ),
        homeIntelligenceHealth = HomeIntelligenceHealthReport.build(
            HomeIntelligenceHealthReport.Input(
                homeIntelligenceState = HomeIntelligenceState(
                    pulse = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
                    mission = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
                    ticker = HomeDataResult.Missing(MissingReason.NO_DATA_YET),
                ),
                settlementLastSucceededAt = 0L,
                settlementLastFailedAt = 0L,
                settlementLastFailureCode = "-",
                settlementNextScheduledAt = null,
                pendingSettlementCount = 0,
                now = 1_800_000_000_000L,
            ),
        ),
    )

    private fun emptyTelemetry(lastFailureCode: String = "-") = WorkerTelemetryPrefs.Snapshot(
        workerName = "worker",
        lastRequestedAt = 0L,
        lastStartedAt = 0L,
        lastFinishedAt = 0L,
        lastCompletedAt = 0L,
        lastSucceededAt = 0L,
        lastFailedAt = 0L,
        lastResult = WorkerTelemetryPrefs.RESULT_UNKNOWN,
        lastFailureCode = lastFailureCode,
        lastDurationMs = 0L,
        successCount = 0,
        failureCount = 0,
    )

    private fun testDate(value: Long): String = if (value <= 0L) "-" else value.toString()
}
