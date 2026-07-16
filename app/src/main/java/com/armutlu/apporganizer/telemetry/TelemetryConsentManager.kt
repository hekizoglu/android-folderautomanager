package com.armutlu.apporganizer.telemetry

import android.content.Context
import com.armutlu.apporganizer.utils.AppPrefs

/** Persistent single source of truth for telemetry consent. */
object TelemetryConsentManager {
    const val CURRENT_CONSENT_VERSION = 1

    fun initialize(context: Context) {
        TelemetryManager.setCollectionEnabled(context, AppPrefs.isTelemetryEnabled(context))
        TelemetryDailySummaryWorker.sync(context, AppPrefs.isTelemetryEnabled(context))
    }

    fun setConsent(
        context: Context,
        enabled: Boolean,
        changedAt: Long = System.currentTimeMillis(),
    ) {
        AppPrefs.setTelemetryConsent(context, enabled, CURRENT_CONSENT_VERSION, changedAt)
        TelemetryManager.setCollectionEnabled(context, enabled)
        TelemetryDailySummaryWorker.sync(context, enabled)
    }
}
