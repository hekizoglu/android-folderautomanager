package com.armutlu.apporganizer.telemetry

sealed class TelemetryEvent(
    val eventName: String,
    internal val parameters: Map<String, String> = emptyMap()
) {
    data object AppStarted : TelemetryEvent("app_started")
    data class FolderOpened(val folderType: FolderType, val appCount: FolderAppCountBucket) :
        TelemetryEvent("folder_opened", mapOf(
            "folder_type" to folderType.wireValue,
            "app_count_bucket" to appCount.wireValue
        ))
    data class AppLaunched(val source: Source) : TelemetryEvent(
        "app_launched", mapOf("source" to source.wireValue)
    )
    data object AllAppsOpened : TelemetryEvent("all_apps_opened")
    data class CategoryReclassified(
        val sourceType: CategorySourceType,
        val resultType: CategoryResultType,
        val confidence: ConfidenceBucket
    ) : TelemetryEvent("category_reclassified", mapOf(
        "source_type" to sourceType.wireValue,
        "result_type" to resultType.wireValue,
        "confidence_bucket" to confidence.wireValue
    ))
    data object ShortcutUsed : TelemetryEvent("shortcut_used")
    data class SearchPerformed(
        val queryLength: QueryLengthBucket,
        val resultCount: CountBucket,
        val latency: LatencyBucket,
        val sourceMix: SearchSourceMix
    ) :
        TelemetryEvent("search_performed", mapOf(
            "query_length_bucket" to queryLength.wireValue,
            "result_count_bucket" to resultCount.wireValue,
            "latency_bucket" to latency.wireValue,
            "source_mix" to sourceMix.wireValue
        ))

    enum class FolderType(val wireValue: String) { SYSTEM("system"), AUTO("auto"), USER_CREATED("user_created") }
    enum class CategorySourceType(val wireValue: String) {
        AUTO("auto"), OTHER("other"), UNCATEGORIZED("uncategorized"), USER_CREATED("user_created")
    }
    enum class CategoryResultType(val wireValue: String) {
        BUILTIN("builtin"), USER_CREATED("user_created"), OTHER("other")
    }
    enum class ConfidenceBucket(val wireValue: String) { LOW("low"), MEDIUM("medium"), HIGH("high"), UNKNOWN("unknown") }
    enum class LatencyBucket(val wireValue: String) { UNDER_100_MS("under_100_ms"), MS_100_499("100_499_ms"), MS_500_PLUS("500_plus_ms"), UNKNOWN("unknown") }
    enum class SearchSourceMix(val wireValue: String) { APPS_ONLY("apps_only"), MIXED("mixed"), OTHER("other") }

    enum class Source(val wireValue: String) {
        HOME("home"), FOLDER("folder"), ALL_APPS("all_apps"),
        SUGGESTIONS("suggestions"), FAVORITES("favorites"), RECENT("recent");

        companion object {
            fun from(value: String): Source? = entries.firstOrNull { it.wireValue == value }
        }
    }
}
