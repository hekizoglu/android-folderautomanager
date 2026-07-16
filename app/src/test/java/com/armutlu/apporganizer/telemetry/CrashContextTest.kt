package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashContextTest {
    private val context = CrashContext(
        appVersion = "1.3.64", androidApiBucket = "api_35_plus", deviceClass = "phone",
        classificationMode = "automatic", classificationConsistent = true,
        pendingReviewBucket = "zero", fileIndexStatus = "fresh",
        notificationListenerEnabled = false, usageAccessEnabled = true,
        activeFeature = "launcher", lastOperationCode = "worker_run", healthScoreBucket = "good",
    )

    @Test
    fun `custom keys exactly match privacy allowlist`() {
        assertEquals(EXPECTED_KEYS, context.asCustomKeys().keys)
        assertTrue(context.asCustomKeys().values.none { it.contains("@") || it.contains("/") })
    }

    @Test
    fun `non fatal requires consent and is limited once per code per day`() {
        val gateway = FakeCrashGateway()
        val acquired = mutableSetOf<HealthIssueCode>()
        TelemetryManager.configureForTest(
            enabled = false,
            crashGateway = gateway,
            dailyNonFatalLimiter = DailyNonFatalLimiter { acquired.add(it) },
        )
        TelemetryManager.recordNonFatal(HealthIssueCode.DATABASE_READ_FAILURE, context)
        assertTrue(gateway.codes.isEmpty())

        TelemetryManager.configureForTest(
            enabled = true,
            crashGateway = gateway,
            dailyNonFatalLimiter = DailyNonFatalLimiter { acquired.add(it) },
        )
        TelemetryManager.recordNonFatal(HealthIssueCode.DATABASE_READ_FAILURE, context)
        TelemetryManager.recordNonFatal(HealthIssueCode.DATABASE_READ_FAILURE, context)
        TelemetryManager.recordNonFatal(HealthIssueCode.BACKUP_FAILURE, context)
        assertEquals(listOf(HealthIssueCode.DATABASE_READ_FAILURE, HealthIssueCode.BACKUP_FAILURE), gateway.codes)
    }

    private class FakeCrashGateway : CrashGateway {
        val codes = mutableListOf<HealthIssueCode>()
        override fun setKey(key: String, value: String) = Unit
        override fun log(messageCode: String) = Unit
        override fun recordNonFatal(code: HealthIssueCode, context: CrashContext, throwable: Throwable?) { codes += code }
    }

    companion object {
        private val EXPECTED_KEYS = setOf(
            "app_version", "android_api_bucket", "device_class", "classification_mode",
            "classification_consistent", "pending_review_bucket", "file_index_status",
            "notification_listener_enabled", "usage_access_enabled", "active_feature",
            "last_operation_code", "health_score_bucket",
        )
    }
}
