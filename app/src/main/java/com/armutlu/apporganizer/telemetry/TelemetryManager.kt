package com.armutlu.apporganizer.telemetry

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

/** Applies the single consent value to every Firebase measurement SDK. */
object TelemetryManager {
    @Volatile private var collectionEnabled = false

    fun isCollectionEnabled(): Boolean = collectionEnabled

    fun setCollectionEnabled(context: Context, enabled: Boolean) {
        applyCollectionState(enabled, FirebaseCollectionServices(context.applicationContext))
    }

    internal fun applyCollectionState(enabled: Boolean, services: CollectionServices) {
        // Close the app event gateway before asking SDKs to stop.
        if (!enabled) collectionEnabled = false
        services.setAnalyticsEnabled(enabled)
        services.setCrashlyticsEnabled(enabled)
        services.setPerformanceEnabled(enabled)
        if (enabled) collectionEnabled = true
    }
}

internal interface CollectionServices {
    fun setAnalyticsEnabled(enabled: Boolean)
    fun setCrashlyticsEnabled(enabled: Boolean)
    fun setPerformanceEnabled(enabled: Boolean)
}

private class FirebaseCollectionServices(context: Context) : CollectionServices {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val performance = FirebasePerformance.getInstance()

    override fun setAnalyticsEnabled(enabled: Boolean) = analytics.setAnalyticsCollectionEnabled(enabled)
    override fun setCrashlyticsEnabled(enabled: Boolean) = crashlytics.setCrashlyticsCollectionEnabled(enabled)
    override fun setPerformanceEnabled(enabled: Boolean) {
        performance.isPerformanceCollectionEnabled = enabled
    }
}
