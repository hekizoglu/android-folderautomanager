package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryEventValidatorTest {
    private val expectedCatalog = mapOf(
        "onboarding_started" to setOf("entry_type"),
        "onboarding_step" to setOf("step", "result"),
        "onboarding_completed" to setOf("duration_bucket"),
        "permission_result" to setOf("permission_type", "result"),
        "search_performed" to setOf("result_bucket", "latency_bucket", "source_mix"),
        "search_result_opened" to setOf("source_type", "position_bucket"),
        "quick_action_used" to setOf("action_type", "source_type"),
        "folder_opened" to setOf("folder_type", "app_count_bucket"),
        "classification_reviewed" to setOf("decision", "confidence_bucket", "source_type"),
        "classification_corrected" to setOf("source_type", "confidence_bucket", "target_type"),
        "mission_viewed" to setOf("mission_type"),
        "mission_completed" to setOf("mission_type", "reward_bucket"),
        "report_viewed" to setOf("report_type"),
        "widget_added" to setOf("widget_type"),
        "health_warning" to setOf("warning_code", "severity", "version"),
        "daily_usage_summary" to setOf(
            "search_count_bucket",
            "folder_open_count_bucket",
            "mission_complete_bucket",
            "report_view_bucket",
            "widget_active",
            "top_feature"
        ),
        "daily_health_summary" to setOf(
            "health_score_bucket",
            "warning_count_bucket",
            "classification_consistent",
            "worker_failure_bucket",
            "search_latency_bucket",
            "file_index_age_bucket"
        ),
        "home_mission_card_viewed" to setOf("mission_type", "status"),
        "home_mission_card_opened" to setOf("mission_type", "status"),
        "mission_progress_viewed" to setOf("mission_type", "progress_bucket"),
        "mission_card_completed" to setOf("mission_type"),
        "mission_card_failed" to setOf("mission_type"),
        "home_pulse_card_viewed" to setOf("score_bucket", "confidence"),
        "home_pulse_card_opened" to setOf("score_bucket", "confidence"),
        "ticker_impression" to setOf("item_type", "position_bucket"),
        "ticker_opened" to setOf("item_type", "position_bucket"),
        "ticker_dismissed" to setOf("item_type"),
        "ticker_snoozed" to setOf("item_type"),
        "ticker_type_disabled" to setOf("item_type"),
        "ticker_manual_next" to setOf("item_type"),
        "ticker_auto_advanced" to setOf("item_type"),
    )

    @Test fun `catalog is exactly the roadmap allowlist`() {
        assertEquals(expectedCatalog, TelemetryEventValidator.catalog.mapValues { it.value.keys })
    }

    @Test fun `unknown events extra parameters and free text are rejected`() {
        assertFalse(TelemetryEventValidator.isValidPayload("app_started", emptyMap()))
        assertFalse(TelemetryEventValidator.isValidPayload("search_performed", mapOf("query" to "private text")))
        assertFalse(TelemetryEventValidator.isValidPayload("folder_opened", mapOf("folder_type" to "private name", "app_count_bucket" to "1_5")))
    }

    @Test fun `typed events contain only bounded values`() {
        val events = listOf(
            TelemetryEvent.OnboardingStarted(TelemetryEvent.EntryType.FIRST_LAUNCH),
            TelemetryEvent.SearchPerformed(TelemetryEvent.ResultBucket.ONE_TO_FIVE, TelemetryEvent.LatencyBucket.UNDER_100_MS, TelemetryEvent.SearchSourceMix.APPS_ONLY),
            TelemetryEvent.FolderOpened(TelemetryEvent.FolderType.USER_CREATED, FolderAppCountBucket.TWENTY_ONE_PLUS),
            TelemetryEvent.HealthWarning(TelemetryEvent.WarningCode.DATA_STALE, TelemetryEvent.Severity.WARNING, TelemetryEvent.VersionBucket.CURRENT)
        )
        assertTrue(events.all(TelemetryEventValidator::isValid))
        assertTrue(TelemetryEventValidator.catalog.values.flatMap { it.values }.flatten().all { it.length <= 40 })
    }

    @Test fun `event and parameter names satisfy firebase rules`() {
        val validName = Regex("^[a-z][a-z0-9_]{0,39}$")
        assertTrue(TelemetryEventValidator.catalog.keys.all(validName::matches))
        assertTrue(TelemetryEventValidator.catalog.values.flatMap { it.keys }.all(validName::matches))
        assertTrue(TelemetryEventValidator.catalog.keys.none { it.startsWith("firebase_") || it.startsWith("google_") || it.startsWith("ga_") })
    }

    // Döngü U02 — ana ekran görev/skor/şerit telemetrisi: yasak alanların (isim/paket/bildirim
    // metni/dosya adı) hiçbir yolla event'e giremediğini ve tipli API'nin yalnız kapalı enum
    // değerleri gönderdiğini doğrular.
    @Test fun `home intelligence events contain only bounded values`() {
        val events = listOf(
            TelemetryEvent.HomeMissionCardViewed(TelemetryEvent.HomeMissionType.CLASSIFICATION_REVIEW, TelemetryEvent.HomeMissionStatus.IN_PROGRESS),
            TelemetryEvent.HomeMissionCardOpened(TelemetryEvent.HomeMissionType.USAGE_REPORT, TelemetryEvent.HomeMissionStatus.AT_RISK),
            TelemetryEvent.MissionProgressViewed(TelemetryEvent.HomeMissionType.NOTIFICATION_REPORT, TelemetryEvent.ProgressBucket.HIGH),
            TelemetryEvent.MissionCardCompleted(TelemetryEvent.HomeMissionType.NONE),
            TelemetryEvent.MissionCardFailed(TelemetryEvent.HomeMissionType.UNAVAILABLE),
            TelemetryEvent.HomePulseCardViewed(TelemetryEvent.ScoreBucket.S60_79, TelemetryEvent.ConfidenceBucket.MEDIUM),
            TelemetryEvent.HomePulseCardOpened(TelemetryEvent.ScoreBucket.S80_100, TelemetryEvent.ConfidenceBucket.HIGH),
            TelemetryEvent.TickerImpression(TelemetryEvent.TickerItemType.MISSION_PROGRESS, TelemetryEvent.PositionBucket.FIRST),
            TelemetryEvent.TickerOpened(TelemetryEvent.TickerItemType.PULSE_CHANGE, TelemetryEvent.PositionBucket.TWO_TO_FIVE),
            TelemetryEvent.TickerDismissed(TelemetryEvent.TickerItemType.CONTEXTUAL_SUGGESTION),
            TelemetryEvent.TickerSnoozed(TelemetryEvent.TickerItemType.FEATURE_DISCOVERY),
            TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.WEEKLY_REPORT),
            TelemetryEvent.TickerManualNext(TelemetryEvent.TickerItemType.ACTION_REQUIRED),
            TelemetryEvent.TickerAutoAdvanced(TelemetryEvent.TickerItemType.CRITICAL_HEALTH),
        )
        assertTrue(events.all(TelemetryEventValidator::isValid))
    }

    @Test fun `home intelligence events reject free text substituted for enum wire values`() {
        assertFalse(TelemetryEventValidator.isValidPayload("home_mission_card_opened", mapOf("mission_type" to "Bildirim İncelemesi", "status" to "in_progress")))
        assertFalse(TelemetryEventValidator.isValidPayload("ticker_dismissed", mapOf("item_type" to "com.whatsapp")))
        assertFalse(TelemetryEventValidator.isValidPayload("ticker_impression", mapOf("item_type" to "mission_progress", "position_bucket" to "first", "title" to "Unutulan uygulama: Spotify")))
    }
}
