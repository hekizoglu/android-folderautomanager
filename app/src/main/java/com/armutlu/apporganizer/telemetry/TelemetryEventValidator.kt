package com.armutlu.apporganizer.telemetry

object TelemetryEventValidator {
    private const val MAX_PARAMETER_VALUE_LENGTH = 40
    private val snakeCase = Regex("^[a-z][a-z0-9_]{0,39}$")
    private val reservedPrefixes = listOf("firebase_", "google_", "ga_")
    private val forbiddenKeys = setOf("query", "name", "phone", "path", "package", "package_name", "notification_text", "folder_name", "category_name", "file_name", "email")

    private fun values(entries: Array<out TelemetryEvent.WireValue>) = entries.map { it.wireValue }.toSet()
    internal val catalog: Map<String, Map<String, Set<String>>> = mapOf(
        "onboarding_started" to mapOf("entry_type" to values(TelemetryEvent.EntryType.entries.toTypedArray())),
        "onboarding_step" to mapOf("step" to values(TelemetryEvent.OnboardingStepType.entries.toTypedArray()), "result" to values(TelemetryEvent.Result.entries.toTypedArray())),
        "onboarding_completed" to mapOf("duration_bucket" to values(TelemetryEvent.DurationBucket.entries.toTypedArray())),
        "permission_result" to mapOf("permission_type" to values(TelemetryEvent.PermissionType.entries.toTypedArray()), "result" to values(TelemetryEvent.Result.entries.toTypedArray())),
        "search_performed" to mapOf("result_bucket" to values(TelemetryEvent.ResultBucket.entries.toTypedArray()), "latency_bucket" to values(TelemetryEvent.LatencyBucket.entries.toTypedArray()), "source_mix" to values(TelemetryEvent.SearchSourceMix.entries.toTypedArray())),
        "search_result_opened" to mapOf("source_type" to values(TelemetryEvent.SourceType.entries.toTypedArray()), "position_bucket" to values(TelemetryEvent.PositionBucket.entries.toTypedArray())),
        "quick_action_used" to mapOf("action_type" to values(TelemetryEvent.ActionType.entries.toTypedArray()), "source_type" to values(TelemetryEvent.SourceType.entries.toTypedArray())),
        "folder_opened" to mapOf("folder_type" to values(TelemetryEvent.FolderType.entries.toTypedArray()), "app_count_bucket" to values(FolderAppCountBucket.entries.toTypedArray())),
        "classification_reviewed" to mapOf("decision" to values(TelemetryEvent.Decision.entries.toTypedArray()), "confidence_bucket" to values(TelemetryEvent.ConfidenceBucket.entries.toTypedArray()), "source_type" to values(TelemetryEvent.SourceType.entries.toTypedArray())),
        "classification_corrected" to mapOf("source_type" to values(TelemetryEvent.SourceType.entries.toTypedArray()), "confidence_bucket" to values(TelemetryEvent.ConfidenceBucket.entries.toTypedArray()), "target_type" to values(TelemetryEvent.TargetType.entries.toTypedArray())),
        "mission_viewed" to mapOf("mission_type" to values(TelemetryEvent.MissionType.entries.toTypedArray())),
        "mission_completed" to mapOf("mission_type" to values(TelemetryEvent.MissionType.entries.toTypedArray()), "reward_bucket" to values(TelemetryEvent.RewardBucket.entries.toTypedArray())),
        "report_viewed" to mapOf("report_type" to values(TelemetryEvent.ReportType.entries.toTypedArray())),
        "widget_added" to mapOf("widget_type" to values(TelemetryEvent.WidgetType.entries.toTypedArray())),
        "health_warning" to mapOf("warning_code" to values(TelemetryEvent.WarningCode.entries.toTypedArray()), "severity" to values(TelemetryEvent.Severity.entries.toTypedArray()), "version" to values(TelemetryEvent.VersionBucket.entries.toTypedArray())),
        "daily_usage_summary" to mapOf(
            "search_count_bucket" to CountBucket.entries.map { it.wireValue }.toSet(), "folder_open_count_bucket" to CountBucket.entries.map { it.wireValue }.toSet(),
            "mission_complete_bucket" to CountBucket.entries.map { it.wireValue }.toSet(), "report_view_bucket" to CountBucket.entries.map { it.wireValue }.toSet(),
            "widget_active" to setOf("true", "false"), "top_feature" to setOf("search", "folder", "mission", "report", "none")
        ),
        "daily_health_summary" to mapOf(
            "health_score_bucket" to setOf("0_9", "10_19", "20_29", "30_39", "40_49", "50_59", "60_69", "70_79", "80_89", "90_100", "unknown"),
            "warning_count_bucket" to CountBucket.entries.map { it.wireValue }.toSet(), "classification_consistent" to setOf("true", "false", "unknown"),
            "worker_failure_bucket" to CountBucket.entries.map { it.wireValue }.toSet(), "search_latency_bucket" to setOf("under_50_ms", "50_99_ms", "100_249_ms", "250_499_ms", "500_999_ms", "1_3_sec", "over_3_sec", "unknown"),
            "file_index_age_bucket" to setOf("under_24_hours", "1_3_days", "4_7_days", "over_7_days", "unknown")
        ),
        // Döngü U02 — ana ekran görev/skor/şerit telemetrisi (roadmap satır 1992-2055).
        "home_mission_card_viewed" to mapOf("mission_type" to values(TelemetryEvent.HomeMissionType.entries.toTypedArray()), "status" to values(TelemetryEvent.HomeMissionStatus.entries.toTypedArray())),
        "home_mission_card_opened" to mapOf("mission_type" to values(TelemetryEvent.HomeMissionType.entries.toTypedArray()), "status" to values(TelemetryEvent.HomeMissionStatus.entries.toTypedArray())),
        "mission_progress_viewed" to mapOf("mission_type" to values(TelemetryEvent.HomeMissionType.entries.toTypedArray()), "progress_bucket" to values(TelemetryEvent.ProgressBucket.entries.toTypedArray())),
        "mission_card_completed" to mapOf("mission_type" to values(TelemetryEvent.HomeMissionType.entries.toTypedArray())),
        "mission_card_failed" to mapOf("mission_type" to values(TelemetryEvent.HomeMissionType.entries.toTypedArray())),
        "home_pulse_card_viewed" to mapOf("score_bucket" to values(TelemetryEvent.ScoreBucket.entries.toTypedArray()), "confidence" to values(TelemetryEvent.ConfidenceBucket.entries.toTypedArray())),
        "home_pulse_card_opened" to mapOf("score_bucket" to values(TelemetryEvent.ScoreBucket.entries.toTypedArray()), "confidence" to values(TelemetryEvent.ConfidenceBucket.entries.toTypedArray())),
        "ticker_impression" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray()), "position_bucket" to values(TelemetryEvent.PositionBucket.entries.toTypedArray())),
        "ticker_opened" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray()), "position_bucket" to values(TelemetryEvent.PositionBucket.entries.toTypedArray())),
        "ticker_dismissed" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray())),
        "ticker_snoozed" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray())),
        "ticker_type_disabled" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray())),
        "ticker_manual_next" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray())),
        "ticker_auto_advanced" to mapOf("item_type" to values(TelemetryEvent.TickerItemType.entries.toTypedArray())),
        "home_page_viewed" to mapOf(
            "page_type" to values(TelemetryEvent.HomePageType.entries.toTypedArray()),
            "page_position_bucket" to values(TelemetryEvent.HomePagePositionBucket.entries.toTypedArray()),
            "navigation_source" to values(TelemetryEvent.HomeNavigationSource.entries.toTypedArray()),
            "search_position" to values(TelemetryEvent.HomeSearchPosition.entries.toTypedArray()),
            "start_mode" to values(TelemetryEvent.HomeStartMode.entries.toTypedArray()),
            "device_class" to values(TelemetryEvent.HomeTelemetryDeviceClass.entries.toTypedArray()),
        ),
        "home_page_swiped" to mapOf(
            "page_type" to values(TelemetryEvent.HomePageType.entries.toTypedArray()),
            "page_position_bucket" to values(TelemetryEvent.HomePagePositionBucket.entries.toTypedArray()),
            "device_class" to values(TelemetryEvent.HomeTelemetryDeviceClass.entries.toTypedArray()),
        ),
        "home_start_mode_changed" to mapOf("start_mode" to values(TelemetryEvent.HomeStartMode.entries.toTypedArray())),
        "smart_dashboard_toggled" to mapOf("state" to values(TelemetryEvent.ToggleState.entries.toTypedArray())),
        "home_search_opened" to mapOf(
            "search_position" to values(TelemetryEvent.HomeSearchPosition.entries.toTypedArray()),
            "page_type" to values(TelemetryEvent.HomePageType.entries.toTypedArray()),
        ),
        "all_apps_opened_from_page" to mapOf(
            "page_type" to values(TelemetryEvent.HomePageType.entries.toTypedArray()),
            "page_position_bucket" to values(TelemetryEvent.HomePagePositionBucket.entries.toTypedArray()),
            "device_class" to values(TelemetryEvent.HomeTelemetryDeviceClass.entries.toTypedArray()),
        ),
        "home_button_navigation" to mapOf(
            "start_mode" to values(TelemetryEvent.HomeStartMode.entries.toTypedArray()),
            "target_page_type" to values(TelemetryEvent.HomePageType.entries.toTypedArray()),
        ),
    )

    fun isValid(event: TelemetryEvent): Boolean = isValidPayload(event.eventName, event.parameters)
    fun isValidPayload(eventName: String, parameters: Map<String, String>): Boolean {
        if (!snakeCase.matches(eventName) || reservedPrefixes.any(eventName::startsWith)) return false
        val schema = catalog[eventName] ?: return false
        if (parameters.size > 25 || parameters.keys.any { it in forbiddenKeys }) return false
        if (parameters.keys != schema.keys) return false
        return parameters.all { (key, value) -> snakeCase.matches(key) && value.length <= MAX_PARAMETER_VALUE_LENGTH && schema[key]?.contains(value) == true }
    }
}
