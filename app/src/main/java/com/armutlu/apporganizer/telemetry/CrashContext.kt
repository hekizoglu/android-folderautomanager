package com.armutlu.apporganizer.telemetry

/** Closed, privacy-safe Crashlytics context. Values must be buckets or fixed codes. */
data class CrashContext(
    val appVersion: String,
    val androidApiBucket: String,
    val deviceClass: String,
    val classificationMode: String,
    val classificationConsistent: Boolean,
    val pendingReviewBucket: String,
    val fileIndexStatus: String,
    val notificationListenerEnabled: Boolean,
    val usageAccessEnabled: Boolean,
    val activeFeature: String,
    val lastOperationCode: String,
    val healthScoreBucket: String,
) {
    fun asCustomKeys(): Map<String, String> = linkedMapOf(
        "app_version" to appVersion,
        "android_api_bucket" to androidApiBucket,
        "device_class" to deviceClass,
        "classification_mode" to classificationMode,
        "classification_consistent" to classificationConsistent.toString(),
        "pending_review_bucket" to pendingReviewBucket,
        "file_index_status" to fileIndexStatus,
        "notification_listener_enabled" to notificationListenerEnabled.toString(),
        "usage_access_enabled" to usageAccessEnabled.toString(),
        "active_feature" to activeFeature,
        "last_operation_code" to lastOperationCode,
        "health_score_bucket" to healthScoreBucket,
    )
}
