package com.armutlu.apporganizer.telemetry

/** Privacy-safe, low-cardinality health summary. It must never contain user content. */
data class HealthSnapshot(
    val healthScoreBucket: String,
    val appVersion: String,
    val androidApiBucket: String,
    val classificationConsistent: Boolean,
    val pendingReviewBucket: String,
    val searchLatencyBucket: String,
    val staleWorkerBucket: String,
    val failedWorkerBucket: String,
    val fileIndexAgeBucket: String,
    val notificationListenerActive: Boolean,
    val usageAccessActive: Boolean,
    val localCrashCountBucket: String,
    val warningCodes: Set<HealthIssueCode>,
)

enum class HealthIssueCode {
    CLASSIFICATION_COUNT_MISMATCH,
    INVALID_WORK_SCHEDULE,
    WORK_ENABLED_BUT_MISSING,
    WORK_DISABLED_BUT_SCHEDULED,
    WORKER_REPEATED_FAILURE,
    FILE_INDEX_STALE,
    USAGE_SYNC_STALE,
    NOTIFICATION_LISTENER_STALE,
    DATABASE_READ_FAILURE,
    SEARCH_LATENCY_HIGH,
    BACKUP_FAILURE,
    REQUIRED_PERMISSION_MISSING,
}

/** Codes are emitted individually; Firebase Analytics does not support array event parameters. */
fun HealthSnapshot.warningCodeValues(): List<String> = warningCodes
    .map(HealthIssueCode::name)
    .sorted()
