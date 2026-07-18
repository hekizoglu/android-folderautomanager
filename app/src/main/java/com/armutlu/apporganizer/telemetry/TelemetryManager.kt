package com.armutlu.apporganizer.telemetry

import android.content.Context

interface AnalyticsGateway {
    fun log(event: TelemetryEvent)
}

interface CrashGateway {
    fun setKey(key: String, value: String)
    fun log(messageCode: String)
    fun recordNonFatal(code: HealthIssueCode, context: CrashContext, throwable: Throwable? = null)
}

interface PerformanceGateway {
    fun start(name: String): PerformanceTrace
}

interface PerformanceTrace {
    fun putAttribute(name: String, value: String)
    fun stop()
}

enum class PerformanceTraceName(val wireValue: String) {
    APP_COLD_START("app_cold_start"), HOME_SCREEN_READY("home_screen_ready"),
    HOME_SHELL_READY("home_shell_ready"), HOME_DASHBOARD_READY("home_dashboard_ready"),
    HOME_FOLDER_PAGE_READY("home_folder_page_ready"), HOME_PAGE_SWITCH("home_page_switch"),
    GLOBAL_SEARCH("global_search"), APP_CATALOG_RECONCILE("app_catalog_reconcile"),
    CLASSIFICATION_RUN("classification_run"), USAGE_SYNC("usage_sync"),
    FILE_INDEX("file_index"), HEALTH_REPORT_GENERATION("health_report_generation")
}

enum class PerformanceResult(val wireValue: String) { SUCCESS("success"), FAILURE("failure"), PARTIAL("partial") }

enum class TestDeviceTag(val wireValue: String?) {
    NONE(null), QA_PRIMARY_PHONE("qa_primary_phone"), QA_CLEAN_INSTALL_PHONE("qa_clean_install_phone"),
    QA_STRESS_PHONE("qa_stress_phone"), QA_TABLET("qa_tablet")
}

internal fun interface DailyEventLimiter {
    fun tryAcquire(): Boolean
}

internal fun interface DailyNonFatalLimiter {
    fun tryAcquire(code: HealthIssueCode): Boolean
}

/** Single, fail-closed entry point for all remote telemetry. */
object TelemetryManager {
    private const val DAILY_EVENT_LIMIT = 500
    @Volatile private var collectionEnabled = false
    @Volatile private var analytics: AnalyticsGateway = NoOpAnalyticsGateway
    @Volatile private var crash: CrashGateway = NoOpCrashGateway
    @Volatile private var performance: PerformanceGateway = NoOpPerformanceGateway
    @Volatile private var limiter: DailyEventLimiter = InMemoryDailyEventLimiter(DAILY_EVENT_LIMIT)
    @Volatile private var nonFatalLimiter: DailyNonFatalLimiter = InMemoryDailyNonFatalLimiter()
    @Volatile private var testDeviceTag: TestDeviceTag = TestDeviceTag.NONE
    private val activeTraces = mutableSetOf<PerformanceTraceName>()

    fun isCollectionEnabled(): Boolean = collectionEnabled

    fun setCollectionEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val services = runCatching { FirebaseCollectionServices(appContext) }.getOrNull()
        applyCollectionState(enabled, services ?: NoOpCollectionServices)
        if (enabled && services != null) {
            limiter = SharedPreferencesDailyEventLimiter(appContext, DAILY_EVENT_LIMIT)
            analytics = FirebaseAnalyticsGateway(appContext) { testDeviceTag }
            crash = FirebaseCrashGateway()
            nonFatalLimiter = SharedPreferencesDailyNonFatalLimiter(appContext)
            performance = FirebasePerformanceGateway()
        } else {
            resetGateways()
        }
    }

    fun log(event: TelemetryEvent) {
        if (collectionEnabled && LocalTelemetryStore.recordIfAggregated(event)) return
        logDirect(event)
    }

    internal fun logDirect(event: TelemetryEvent) {
        if (!collectionEnabled || !TelemetryEventValidator.isValid(event) || !limiter.tryAcquire()) return
        runCatching { analytics.log(event) }
    }

    fun setCrashKey(key: String, value: String) {
        if (collectionEnabled) runCatching { crash.setKey(key, value) }
    }

    fun logHealth(messageCode: String) {
        if (collectionEnabled) runCatching { crash.log(messageCode) }
    }

    fun recordNonFatal(code: HealthIssueCode, context: CrashContext, throwable: Throwable? = null) {
        if (collectionEnabled && nonFatalLimiter.tryAcquire(code)) {
            runCatching { crash.recordNonFatal(code, context, throwable) }
        }
    }

    fun <T> trace(name: PerformanceTraceName, block: () -> T): T = runTrace(name, block)

    suspend fun <T> traceSuspending(name: PerformanceTraceName, block: suspend () -> T): T {
        val trace = beginTrace(name) ?: return block()
        return try { block() } finally { endTrace(name, trace) }
    }

    private fun <T> runTrace(name: PerformanceTraceName, block: () -> T): T {
        val trace = beginTrace(name) ?: return block()
        return try { block() } finally { endTrace(name, trace) }
    }

    private fun beginTrace(name: PerformanceTraceName): PerformanceTrace? {
        if (!collectionEnabled) return null
        synchronized(activeTraces) { if (!activeTraces.add(name)) return null }
        return runCatching { performance.start(name.wireValue) }.getOrElse {
            synchronized(activeTraces) { activeTraces.remove(name) }
            null
        }
    }

    private fun endTrace(name: PerformanceTraceName, trace: PerformanceTrace) {
        runCatching { trace.stop() }
        synchronized(activeTraces) { activeTraces.remove(name) }
    }

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
        dailyNonFatalLimiter: DailyNonFatalLimiter = InMemoryDailyNonFatalLimiter(),
    ) {
        collectionEnabled = enabled
        analytics = analyticsGateway
        crash = crashGateway
        performance = performanceGateway
        limiter = dailyLimiter
        nonFatalLimiter = dailyNonFatalLimiter
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
    override fun recordNonFatal(code: HealthIssueCode, context: CrashContext, throwable: Throwable?) = Unit
}


private class InMemoryDailyNonFatalLimiter : DailyNonFatalLimiter {
    private var day = currentDay()
    private val sentCodes = mutableSetOf<HealthIssueCode>()
    @Synchronized override fun tryAcquire(code: HealthIssueCode): Boolean {
        val today = currentDay()
        if (today != day) { day = today; sentCodes.clear() }
        return sentCodes.add(code)
    }
    private fun currentDay(): Long = System.currentTimeMillis() / 86_400_000L
}

private class SharedPreferencesDailyNonFatalLimiter(context: Context) : DailyNonFatalLimiter {
    private val preferences = context.getSharedPreferences("crashlytics_non_fatal_rate_limit", Context.MODE_PRIVATE)
    @Synchronized override fun tryAcquire(code: HealthIssueCode): Boolean {
        val today = System.currentTimeMillis() / 86_400_000L
        val key = code.name.lowercase()
        if (preferences.getLong(key, -1L) == today) return false
        preferences.edit().putLong(key, today).apply()
        return true
    }
}
internal object NoOpPerformanceGateway : PerformanceGateway {
    override fun start(name: String): PerformanceTrace = object : PerformanceTrace {
        override fun putAttribute(name: String, value: String) = Unit
        override fun stop() = Unit
    }
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
