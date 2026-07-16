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
        )
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
}
