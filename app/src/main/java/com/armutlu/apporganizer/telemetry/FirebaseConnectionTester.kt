package com.armutlu.apporganizer.telemetry

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import com.armutlu.apporganizer.presentation.viewmodel.ConnectionTestStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.perf.FirebasePerformance
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class FirebaseConnectionTestResult(
    val configurationOk: Boolean,
    val networkAvailable: Boolean,
    val firebaseRoundTripOk: Boolean,
    val analyticsQueued: Boolean,
    val crashlyticsReady: Boolean,
    val performanceReady: Boolean,
    val testedAt: Long,
    val status: ConnectionTestStatus,
    val safeErrorCode: String?,
)

internal interface FirebaseConnectionDependencies {
    fun configurationOk(): Boolean
    fun networkAvailable(): Boolean
    suspend fun roundTrip()
    fun queueAnalytics()
    fun logCrashlytics()
    fun runPerformanceTrace()
    fun persist(result: FirebaseConnectionTestResult)
    fun now(): Long
}

class FirebaseConnectionTester internal constructor(
    private val dependencies: FirebaseConnectionDependencies,
) {
    constructor(context: Context) : this(AndroidFirebaseConnectionDependencies(context.applicationContext))

    suspend fun test(): FirebaseConnectionTestResult {
        val testedAt = dependencies.now()
        if (!runCatching { dependencies.configurationOk() }.getOrDefault(false)) {
            return result(testedAt, safeErrorCode = FIREBASE_NOT_CONFIGURED)
        }
        if (!runCatching { dependencies.networkAvailable() }.getOrDefault(false)) {
            return result(testedAt, configurationOk = true, safeErrorCode = NETWORK_UNAVAILABLE)
        }
        if (runCatching { dependencies.roundTrip() }.isFailure) {
            return result(testedAt, configurationOk = true, networkAvailable = true, safeErrorCode = INSTALLATION_ID_FAILED)
        }

        val analyticsQueued = runCatching { dependencies.queueAnalytics() }.isSuccess
        val crashlyticsReady = runCatching { dependencies.logCrashlytics() }.isSuccess
        val performanceReady = runCatching { dependencies.runPerformanceTrace() }.isSuccess
        val error = when {
            !analyticsQueued -> ANALYTICS_NOT_AVAILABLE
            !crashlyticsReady -> CRASHLYTICS_NOT_AVAILABLE
            !performanceReady -> PERFORMANCE_NOT_AVAILABLE
            else -> null
        }
        return result(testedAt, true, true, true, analyticsQueued, crashlyticsReady, performanceReady, error)
    }

    private fun result(
        testedAt: Long,
        configurationOk: Boolean = false,
        networkAvailable: Boolean = false,
        firebaseRoundTripOk: Boolean = false,
        analyticsQueued: Boolean = false,
        crashlyticsReady: Boolean = false,
        performanceReady: Boolean = false,
        safeErrorCode: String? = null,
    ): FirebaseConnectionTestResult {
        val status = when {
            firebaseRoundTripOk && safeErrorCode == null -> ConnectionTestStatus.SUCCESS
            firebaseRoundTripOk -> ConnectionTestStatus.PARTIAL_SUCCESS
            else -> ConnectionTestStatus.FAILED
        }
        val value = FirebaseConnectionTestResult(configurationOk, networkAvailable, firebaseRoundTripOk, analyticsQueued,
            crashlyticsReady, performanceReady, testedAt, status, safeErrorCode)
        runCatching { dependencies.persist(value) }
        return value
    }

    companion object {
        const val FIREBASE_NOT_CONFIGURED = "FIREBASE_NOT_CONFIGURED"
        const val NETWORK_UNAVAILABLE = "NETWORK_UNAVAILABLE"
        const val INSTALLATION_ID_FAILED = "INSTALLATION_ID_FAILED"
        const val ANALYTICS_NOT_AVAILABLE = "ANALYTICS_NOT_AVAILABLE"
        const val CRASHLYTICS_NOT_AVAILABLE = "CRASHLYTICS_NOT_AVAILABLE"
        const val PERFORMANCE_NOT_AVAILABLE = "PERFORMANCE_NOT_AVAILABLE"
    }
}

private class AndroidFirebaseConnectionDependencies(private val context: Context) : FirebaseConnectionDependencies {
    override fun configurationOk(): Boolean = FirebaseApp.getApps(context).any { app ->
        app.options.applicationId.isNotBlank() && app.options.apiKey.isNotBlank() && app.options.projectId?.isNotBlank() == true
    }

    override fun networkAvailable(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override suspend fun roundTrip() {
        // The force-refreshed bearer token proves backend registration, then is discarded.
        FirebaseInstallations.getInstance().getToken(true).awaitResult()
    }

    override fun queueAnalytics() {
        FirebaseAnalytics.getInstance(context).logEvent("telemetry_connection_test", Bundle())
    }

    override fun logCrashlytics() { FirebaseCrashlytics.getInstance().log("connection_test") }

    override fun runPerformanceTrace() {
        FirebasePerformance.getInstance().newTrace("firebase_connection_test").apply { start(); stop() }
    }

    override fun persist(result: FirebaseConnectionTestResult) {
        context.getSharedPreferences("firebase_connection_test", Context.MODE_PRIVATE).edit()
            .putBoolean("configuration_ok", result.configurationOk)
            .putBoolean("network_available", result.networkAvailable)
            .putBoolean("firebase_round_trip_ok", result.firebaseRoundTripOk)
            .putBoolean("analytics_queued", result.analyticsQueued)
            .putBoolean("crashlytics_ready", result.crashlyticsReady)
            .putBoolean("performance_ready", result.performanceReady)
            .putLong("tested_at", result.testedAt)
            .putString("status", result.status.name)
            .putString("safe_error_code", result.safeErrorCode)
            .apply()
    }

    override fun now(): Long = System.currentTimeMillis()
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
