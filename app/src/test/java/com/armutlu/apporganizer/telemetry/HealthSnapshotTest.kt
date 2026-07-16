package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthSnapshotTest {
    @Test
    fun issueCatalog_isClosedAndComplete() {
        assertEquals(
            setOf(
                "CLASSIFICATION_COUNT_MISMATCH", "INVALID_WORK_SCHEDULE",
                "WORK_ENABLED_BUT_MISSING", "WORK_DISABLED_BUT_SCHEDULED",
                "WORKER_REPEATED_FAILURE", "FILE_INDEX_STALE", "USAGE_SYNC_STALE",
                "NOTIFICATION_LISTENER_STALE", "DATABASE_READ_FAILURE",
                "SEARCH_LATENCY_HIGH", "BACKUP_FAILURE", "REQUIRED_PERMISSION_MISSING",
            ),
            HealthIssueCode.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun snapshotSchema_containsOnlyApprovedSummaryFields() {
        val names = HealthSnapshot::class.java.declaredFields
            .filterNot { it.isSynthetic || it.name.startsWith("$") }
            .map { it.name }
            .toSet()
        assertEquals(
            setOf(
                "healthScoreBucket", "appVersion", "androidApiBucket",
                "classificationConsistent", "pendingReviewBucket", "searchLatencyBucket",
                "staleWorkerBucket", "failedWorkerBucket", "fileIndexAgeBucket",
                "notificationListenerActive", "usageAccessActive", "localCrashCountBucket",
                "warningCodes",
            ),
            names,
        )
        val forbidden = listOf("package", "appName", "contact", "person", "fileName", "notificationContent", "query")
        assertFalse(names.any { name -> forbidden.any { name.contains(it, ignoreCase = true) } })
    }

    @Test
    fun warningCodes_areStableScalarValues() {
        val snapshot = sampleSnapshot(
            setOf(HealthIssueCode.SEARCH_LATENCY_HIGH, HealthIssueCode.FILE_INDEX_STALE),
        )
        assertEquals(listOf("FILE_INDEX_STALE", "SEARCH_LATENCY_HIGH"), snapshot.warningCodeValues())
        assertTrue(snapshot.warningCodeValues().all { it.matches(Regex("[A-Z_]+")) })
    }

    private fun sampleSnapshot(codes: Set<HealthIssueCode>) = HealthSnapshot(
        healthScoreBucket = "80_100", appVersion = "1.3.63", androidApiBucket = "35_plus",
        classificationConsistent = true, pendingReviewBucket = "1_5",
        searchLatencyBucket = "0_50_ms", staleWorkerBucket = "0",
        failedWorkerBucket = "0", fileIndexAgeBucket = "0_24h",
        notificationListenerActive = true, usageAccessActive = true,
        localCrashCountBucket = "0", warningCodes = codes,
    )
}
