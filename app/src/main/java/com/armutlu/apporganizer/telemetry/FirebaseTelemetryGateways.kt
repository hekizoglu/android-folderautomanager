package com.armutlu.apporganizer.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

internal class FirebaseAnalyticsGateway(
    context: Context,
    private val testDeviceTag: () -> TestDeviceTag,
) : AnalyticsGateway {
    private val delegate = FirebaseAnalytics.getInstance(context)
    override fun log(event: TelemetryEvent) {
        val params = Bundle().apply {
            event.parameters.forEach { (key, value) -> putString(key, value) }
            testDeviceTag().wireValue?.let { putString("test_device_tag", it) }
        }.takeUnless { it.isEmpty }
        delegate.logEvent(event.eventName, params)
    }
}

internal class FirebaseCrashGateway : CrashGateway {
    private val delegate = FirebaseCrashlytics.getInstance()
    override fun setKey(key: String, value: String) = delegate.setCustomKey(key, value)
    override fun log(messageCode: String) = delegate.log(messageCode)
    override fun recordNonFatal(code: HealthIssueCode, context: CrashContext, throwable: Throwable?) {
        context.asCustomKeys().forEach(delegate::setCustomKey)
        delegate.setCustomKey("health_issue_code", code.name.lowercase())
        delegate.recordException(throwable ?: IllegalStateException(code.name))
    }
}

internal class FirebasePerformanceGateway : PerformanceGateway {
    private val delegate = FirebasePerformance.getInstance()
    override fun <T> trace(name: String, block: () -> T): T {
        val trace = delegate.newTrace(name)
        trace.start()
        return try { block() } finally { trace.stop() }
    }
}

internal class FirebaseCollectionServices(context: Context) : CollectionServices {
    init { check(FirebaseApp.getApps(context).isNotEmpty()) }
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val performance = FirebasePerformance.getInstance()
    override fun setAnalyticsEnabled(enabled: Boolean) = analytics.setAnalyticsCollectionEnabled(enabled)
    override fun setCrashlyticsEnabled(enabled: Boolean) = crashlytics.setCrashlyticsCollectionEnabled(enabled)
    override fun setPerformanceEnabled(enabled: Boolean) { performance.isPerformanceCollectionEnabled = enabled }
}
