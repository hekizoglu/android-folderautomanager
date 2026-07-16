package com.armutlu.apporganizer.telemetry

object TelemetryEventValidator {
    private const val MAX_EVENT_NAME_LENGTH = 40
    private const val MAX_PARAMETER_VALUE_LENGTH = 40
    private val snakeCase = Regex("^[a-z][a-z0-9_]{0,39}$")
    private val forbiddenKeys = setOf(
        "query", "name", "phone", "path", "package", "package_name",
        "notification_text", "folder_name", "category_name", "file_name", "email"
    )
    private val catalog = mapOf(
        "app_started" to emptyMap(),
        "folder_opened" to mapOf(
            "folder_type" to TelemetryEvent.FolderType.entries.map { it.wireValue }.toSet(),
            "app_count_bucket" to FolderAppCountBucket.entries.map { it.wireValue }.toSet()
        ),
        "app_launched" to mapOf("source" to TelemetryEvent.Source.entries.map { it.wireValue }.toSet()),
        "all_apps_opened" to emptyMap(),
        "category_reclassified" to mapOf(
            "source_type" to TelemetryEvent.CategorySourceType.entries.map { it.wireValue }.toSet(),
            "result_type" to TelemetryEvent.CategoryResultType.entries.map { it.wireValue }.toSet(),
            "confidence_bucket" to TelemetryEvent.ConfidenceBucket.entries.map { it.wireValue }.toSet()
        ),
        "shortcut_used" to emptyMap(),
        "search_performed" to mapOf(
            "query_length_bucket" to QueryLengthBucket.entries.map { it.wireValue }.toSet(),
            "result_count_bucket" to CountBucket.entries.map { it.wireValue }.toSet(),
            "latency_bucket" to TelemetryEvent.LatencyBucket.entries.map { it.wireValue }.toSet(),
            "source_mix" to TelemetryEvent.SearchSourceMix.entries.map { it.wireValue }.toSet()
        )
    )

    fun isValid(event: TelemetryEvent): Boolean = isValidPayload(event.eventName, event.parameters)

    /** Public for boundary adapters/tests; production logging accepts only [TelemetryEvent]. */
    fun isValidPayload(eventName: String, parameters: Map<String, String>): Boolean {
        if (eventName.length > MAX_EVENT_NAME_LENGTH || !snakeCase.matches(eventName)) return false
        val schema = catalog[eventName] ?: return false
        if (parameters.size > 25 || parameters.keys.any { it in forbiddenKeys }) return false
        if (parameters.keys != schema.keys) return false
        return parameters.all { (key, value) ->
            snakeCase.matches(key) && value.length <= MAX_PARAMETER_VALUE_LENGTH &&
                schema[key]?.contains(value) == true
        }
    }
}
