package com.armutlu.apporganizer.utils

import android.content.Context

object WorkerTelemetryPrefs {
    const val KEY_FAILURE_CODE = "failure_code"
    const val FAILURE_PERMISSION_DENIED = "PERMISSION_DENIED"
    const val FAILURE_IO_ERROR = "IO_ERROR"
    const val FAILURE_DATABASE_ERROR = "DATABASE_ERROR"
    const val FAILURE_STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE"
    const val FAILURE_REPLAN_CANCELLED_WORK = "REPLAN_CANCELLED_WORK"
    const val FAILURE_REPLAN_FAILED_WORK = "REPLAN_FAILED_WORK"
    const val FAILURE_UNKNOWN = "UNKNOWN"
    const val RESULT_UNKNOWN = "UNKNOWN"
    const val RESULT_SUCCESS = "SUCCESS"
    const val RESULT_FAILED = "FAILED"

    private const val PREFS_NAME = "worker_telemetry_prefs"

    data class Snapshot(
        val workerName: String,
        val lastRequestedAt: Long,
        val lastStartedAt: Long,
        val lastFinishedAt: Long,
        val lastCompletedAt: Long,
        val lastSucceededAt: Long,
        val lastFailedAt: Long,
        val lastResult: String,
        val lastFailureCode: String,
        val lastDurationMs: Long,
        val successCount: Int,
        val failureCount: Int,
    ) {
        val pending: Boolean = lastRequestedAt > lastCompletedAt
    }

    fun markRequested(context: Context, workerName: String, now: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(key(workerName, "lastRequestedAt"), now)
            .apply()
    }

    fun markStarted(context: Context, workerName: String, now: Long = System.currentTimeMillis()): Long {
        prefs(context).edit()
            .putLong(key(workerName, "lastStartedAt"), now)
            .apply()
        return now
    }

    fun markSucceeded(
        context: Context,
        workerName: String,
        startedAt: Long,
        finishedAt: Long = System.currentTimeMillis(),
    ) {
        val p = prefs(context)
        p.edit()
            .putLong(key(workerName, "lastFinishedAt"), finishedAt)
            .putLong(key(workerName, "lastSucceededAt"), finishedAt)
            .putString(key(workerName, "lastResult"), RESULT_SUCCESS)
            .putLong(key(workerName, "lastDurationMs"), duration(startedAt, finishedAt))
            .putInt(key(workerName, "successCount"), p.getInt(key(workerName, "successCount"), 0) + 1)
            .apply()
    }

    fun markFailed(
        context: Context,
        workerName: String,
        startedAt: Long,
        failureCode: String = FAILURE_UNKNOWN,
        finishedAt: Long = System.currentTimeMillis(),
    ) {
        val p = prefs(context)
        p.edit()
            .putLong(key(workerName, "lastFinishedAt"), finishedAt)
            .putLong(key(workerName, "lastFailedAt"), finishedAt)
            .putString(key(workerName, "lastResult"), RESULT_FAILED)
            .putString(key(workerName, "lastFailureCode"), sanitizeFailureCode(failureCode))
            .putLong(key(workerName, "lastDurationMs"), duration(startedAt, finishedAt))
            .putInt(key(workerName, "failureCount"), p.getInt(key(workerName, "failureCount"), 0) + 1)
            .apply()
    }

    fun getSnapshot(context: Context, workerName: String): Snapshot {
        val p = prefs(context)
        return Snapshot(
            workerName = workerName,
            lastRequestedAt = p.getLong(key(workerName, "lastRequestedAt"), 0L),
            lastStartedAt = p.getLong(key(workerName, "lastStartedAt"), 0L),
            lastFinishedAt = p.getLong(key(workerName, "lastFinishedAt"), 0L),
            lastCompletedAt = p.getLong(key(workerName, "lastFinishedAt"), 0L),
            lastSucceededAt = p.getLong(key(workerName, "lastSucceededAt"), 0L),
            lastFailedAt = p.getLong(key(workerName, "lastFailedAt"), 0L),
            lastResult = p.getString(key(workerName, "lastResult"), RESULT_UNKNOWN) ?: RESULT_UNKNOWN,
            lastFailureCode = p.getString(key(workerName, "lastFailureCode"), "-") ?: "-",
            lastDurationMs = p.getLong(key(workerName, "lastDurationMs"), 0L),
            successCount = p.getInt(key(workerName, "successCount"), 0),
            failureCount = p.getInt(key(workerName, "failureCount"), 0),
        )
    }

    internal fun duration(startedAt: Long, finishedAt: Long): Long =
        (finishedAt - startedAt).coerceAtLeast(0L)

    internal fun sanitizeFailureCode(code: String): String {
        val normalized = code.trim().uppercase().replace(Regex("[^A-Z0-9_]"), "_")
        return normalized.ifBlank { FAILURE_UNKNOWN }.take(64)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(workerName: String, suffix: String): String =
        "${workerName}_$suffix"
}
