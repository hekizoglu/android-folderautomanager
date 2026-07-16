package com.armutlu.apporganizer.telemetry

import android.content.Context

interface AnalyticsGateway {
    fun log(event: TelemetryEvent)
}

interface CrashGateway {
    fun setKey(key: String, value: String)
    fun log(messageCode: String)
    fun recordNonFatal(code: HealthIssueCode, throwable: Throwable? = null)
}

interface PerformanceGateway {
    fun <T> trace(name: String, block: () -> T): T
}

enum class HealthIssueCode { UNKNOWN }

enum class TestDeviceTag(val wireValue: String?) {
    NONE(null), QA_PRIMARY_PHONE("qa_primary_phone"), QA_CLEAN_INSTALL_PHONE("qa_clean_install_phone"),
    QA_STRESS_PHONE("qa_stress_phone"), QA_TABLET("qa_tablet")
}

internal fun interface DailyEventLimiter {
    fun tryAcquire(): Boolean
}

/** Single, fail-closed entry point for all remote telemetry. */
object TelemetryManager {
    private const val DAILY_EVENT_LIMIT = 500
    @Volatile private var collectionEnabled = false
    @Volatile private var analytics: AnalyticsGateway = NoOpAnalyticsGateway
    @Volatile private var crash: CrashGateway = NoOpCrashGateway
    @Volatile private var performance: PerformanceGateway = NoOpPerformanceGateway
    @Volatile private var limiter: DailyEventLimiter = InMemoryDailyEventLimiter(DAILY_EVENT_LIMIT)
    @Volatile private var testDeviceTag: TestDeviceTag = TestDeviceTag.NONE

    fun isCollectionEnabled(): Boolean = collectionEnabled

    fun setCollectionEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val services = runCatching { FirebaseCollectionServices(appContext) }.getOrNull()
        applyCollectionState(enabled, services ?: NoOpCollectionServices)
        if (enabled && services != null) {
            limiter = SharedPreferencesDailyEventLimiter(appContext, DAILY_EVENT_LIMIT)
            analytics = FirebaseAnalyticsGateway(appContext) { testDeviceTag }
            crash = FirebaseCrashGateway()
            performance = FirebasePerformanceGateway()
        } else {
            resetGateways()
        }
    }

    fun log(event: TelemetryEvent) {
        if (!collectionEnabled || !TelemetryEventValidator.isValid(event) || !limiter.tryAcquire()) return
        runCatching { analytics.log(event) }
    }

    fun setCrashKey(key: String, value: String) {
        if (collectionEnabled) runCatching { crash.setKey(key, value) }
    }

    fun logHealth(messageCode: String) {
        if (collectionEnabled) runCatching { crash.log(messageCode) }
    }

    fun recordNonFatal(code: HealthIssueCode, throwable: Throwable? = null) {
        if (collectionEnabled && limiter.tryAcquire()) runCatching { crash.recordNonFatal(code, throwable) }
    }

    fun <T> trace(name: String, block: () -> T): T =
        if (!collectionEnabled) block() else runCatching { performance.trace(name, block) }.getOrElse { block() }

    fun setTestDeviceTag(tag: TestDeviceTag) { testDeviceTag = tag }

    internal fun applyCollectionState(enabled: Boolean, services: CollectionServices) {
        if (!enabled) collectionEnabled = false
        val applied = runCatching {
            services.setAnalyticsEnabled(enabled)
            services.setCrashlyticsEnabled(enabled)
            services.setPerformanceEnabled(enabled)
        }.isSuccess
        collectionEnabled = enabled && applied
    }

    internal fun configureForTest(
        enabled: Boolean,
        analyticsGateway: AnalyticsGateway = NoOpAnalyticsGateway,
        crashGateway: CrashGateway = NoOpCrashGateway,
        performanceGateway: PerformanceGateway = NoOpPerformanceGateway,
        dailyLimiter: DailyEventLimiter = InMemoryDailyEventLimiter(DAILY_EVENT_LIMIT),
    ) {
        collectionEnabled = enabled
        analytics = analyticsGateway
        crash = crashGateway
        performance = performanceGateway
        limiter = dailyLimiter
    }

    private fun resetGateways() {
        analytics = NoOpAnalyticsGateway
        crash = NoOpCrashGateway
        performance = NoOpPerformanceGateway
    }
}

internal interface CollectionServices {
    fun setAnalyticsEnabled(enabled: Boolean)
    fun setCrashlyticsEnabled(enabled: Boolean)
    fun setPerformanceEnabled(enabled: Boolean)
}

private object NoOpCollectionServices : CollectionServices {
    override fun setAnalyticsEnabled(enabled: Boolean) = Unit
    override fun setCrashlyticsEnabled(enabled: Boolean) = Unit
    override fun setPerformanceEnabled(enabled: Boolean) = Unit
}

internal object NoOpAnalyticsGateway : AnalyticsGateway { override fun log(event: TelemetryEvent) = Unit }
internal object NoOpCrashGateway : CrashGateway {
    override fun setKey(key: String, value: String) = Unit
    override fun log(messageCode: String) = Unit
    override fun recordNonFatal(code: HealthIssueCode, throwable: Throwable?) = Unit
}
internal object NoOpPerformanceGateway : PerformanceGateway {
    override fun <T> trace(name: String, block: () -> T): T = block()
}

private class InMemoryDailyEventLimiter(private val limit: Int) : DailyEventLimiter {
    private var day = currentDay()
    private var count = 0
    @Synchronized override fun tryAcquire(): Boolean {
        val today = currentDay()
        if (today != day) { day = today; count = 0 }
        if (count >= limit) return false
        count++
        return true
    }
    private fun currentDay(): Long = System.currentTimeMillis() / 86_400_000L
}

private class SharedPreferencesDailyEventLimiter(context: Context, private val limit: Int) : DailyEventLimiter {
    private val preferences = context.getSharedPreferences("telemetry_rate_limit", Context.MODE_PRIVATE)
    @Synchronized override fun tryAcquire(): Boolean {
        val today = System.currentTimeMillis() / 86_400_000L
        val storedDay = preferences.getLong("day", -1L)
        val count = if (storedDay == today) preferences.getInt("count", 0) else 0
        if (count >= limit) return false
        preferences.edit().putLong("day", today).putInt("count", count + 1).apply()
        return true
    }
}
