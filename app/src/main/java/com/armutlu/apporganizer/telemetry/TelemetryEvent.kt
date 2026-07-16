package com.armutlu.apporganizer.telemetry

sealed class TelemetryEvent(
    val eventName: String,
    internal val parameters: Map<String, String> = emptyMap()
) {
    data object AppStarted : TelemetryEvent("app_started")
    data object FolderOpened : TelemetryEvent("folder_opened")
    data class AppLaunched(val source: Source) : TelemetryEvent(
        "app_launched", mapOf("source" to source.wireValue)
    )
    data object AllAppsOpened : TelemetryEvent("all_apps_opened")
    data object CategoryReclassified : TelemetryEvent("category_reclassified")
    data object ShortcutUsed : TelemetryEvent("shortcut_used")
    data class SearchPerformed(val queryLength: QueryLengthBucket, val resultCount: CountBucket) :
        TelemetryEvent("search_performed", mapOf(
            "query_length_bucket" to queryLength.wireValue,
            "result_count_bucket" to resultCount.wireValue
        ))

    enum class Source(val wireValue: String) {
        HOME("home"), FOLDER("folder"), ALL_APPS("all_apps"),
        SUGGESTIONS("suggestions"), FAVORITES("favorites"), RECENT("recent");

        companion object {
            fun from(value: String): Source? = entries.firstOrNull { it.wireValue == value }
        }
    }
}
