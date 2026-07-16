package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySummarySchemaTest {
    @Test fun `daily summary accepts buckets and rejects exact counts`() {
        val valid = TelemetryEvent.DailyUsageSummary(mapOf(
            "search_count_bucket" to "101_plus", "folder_open_count_bucket" to "11_20",
            "mission_complete_bucket" to "0", "report_view_bucket" to "1_5",
            "widget_active" to "false", "top_feature" to "search"
        ))
        val exact = TelemetryEvent.DailyUsageSummary(valid.parameters + ("search_count_bucket" to "347"))
        assertTrue(TelemetryEventValidator.isValid(valid))
        assertFalse(TelemetryEventValidator.isValid(exact))
    }

    @Test fun `health summary is closed and low cardinality`() {
        val event = TelemetryEvent.DailyHealthSummary(mapOf(
            "health_score_bucket" to "80_89", "warning_count_bucket" to "1_5",
            "classification_consistent" to "true", "worker_failure_bucket" to "0",
            "search_latency_bucket" to "under_50_ms", "file_index_age_bucket" to "under_24_hours"
        ))
        assertTrue(TelemetryEventValidator.isValid(event))
        assertFalse(TelemetryEventValidator.isValidPayload("daily_health_summary", event.parameters + ("file_name" to "secret")))
    }
}
