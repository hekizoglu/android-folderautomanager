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
        "health_warning" to mapOf("warning_code" to values(TelemetryEvent.WarningCode.entries.toTypedArray()), "severity" to values(TelemetryEvent.Severity.entries.toTypedArray()), "version" to values(TelemetryEvent.VersionBucket.entries.toTypedArray()))
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
