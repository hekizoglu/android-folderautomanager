package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelemetryManagerTest {
    @Before
    fun resetGateway() {
        TelemetryManager.configureForTest(enabled = false)
    }

    @Test
    fun `new process starts with collection disabled`() {
        assertFalse(TelemetryManager.isCollectionEnabled())
    }

    @Test
    fun `enabling consent enables every firebase service`() {
        val services = FakeServices()
        TelemetryManager.applyCollectionState(true, services)

        assertTrue(TelemetryManager.isCollectionEnabled())
        assertEquals(listOf("analytics:true", "crashlytics:true", "performance:true"), services.calls)
    }

    @Test
    fun `withdrawing consent immediately closes gateway and disables every service`() {
        val services = FakeServices()
        TelemetryManager.applyCollectionState(true, services)
        services.calls.clear()

        TelemetryManager.applyCollectionState(false, services)

        assertFalse(TelemetryManager.isCollectionEnabled())
        assertEquals(listOf("analytics:false", "crashlytics:false", "performance:false"), services.calls)
    }

    @Test
    fun `analytics is no-op without consent`() {
        val gateway = FakeAnalyticsGateway()
        TelemetryManager.configureForTest(enabled = false, analyticsGateway = gateway)

        TelemetryManager.log(TelemetryEvent.ReportViewed(TelemetryEvent.ReportType.HEALTH))

        assertTrue(gateway.events.isEmpty())
    }

    @Test
    fun `analytics validates and applies daily limiter before gateway`() {
        val gateway = FakeAnalyticsGateway()
        var permits = 1
        TelemetryManager.configureForTest(
            enabled = true,
            analyticsGateway = gateway,
            dailyLimiter = DailyEventLimiter { permits-- > 0 },
        )

        val accepted = TelemetryEvent.ReportViewed(TelemetryEvent.ReportType.HEALTH)
        TelemetryManager.log(accepted)
        TelemetryManager.log(TelemetryEvent.WidgetAdded(TelemetryEvent.WidgetType.SEARCH))

        assertEquals(listOf(accepted), gateway.events)
    }

    @Test
    fun `gateway failure never escapes into caller`() {
        TelemetryManager.configureForTest(
            enabled = true,
            analyticsGateway = object : AnalyticsGateway {
                override fun log(event: TelemetryEvent) = error("sdk unavailable")
            },
        )

        TelemetryManager.log(TelemetryEvent.ReportViewed(TelemetryEvent.ReportType.HEALTH))
    }

    @Test
    fun `fixed test device tags contain no free text`() {
        assertEquals(5, TestDeviceTag.entries.size)
        assertTrue(TestDeviceTag.entries.all { it.wireValue == null || it.wireValue!!.matches(Regex("[a-z_]+")) })
    }

    @Test
    fun `performance is no-op without consent and only allows fixed trace names`() {
        val gateway = FakePerformanceGateway()
        TelemetryManager.configureForTest(enabled = false, performanceGateway = gateway)
        assertEquals("ok", TelemetryManager.trace(PerformanceTraceName.GLOBAL_SEARCH) { "ok" })
        assertTrue(gateway.started.isEmpty())
        assertEquals(12, PerformanceTraceName.entries.size)
    }

    @Test
    fun `same performance trace is not nested or duplicated`() {
        val gateway = FakePerformanceGateway()
        TelemetryManager.configureForTest(enabled = true, performanceGateway = gateway)
        TelemetryManager.trace(PerformanceTraceName.GLOBAL_SEARCH) {
            TelemetryManager.trace(PerformanceTraceName.GLOBAL_SEARCH) { Unit }
        }
        assertEquals(listOf("global_search"), gateway.started)
        assertEquals(1, gateway.stopped)
    }

    private class FakeServices : CollectionServices {
        val calls = mutableListOf<String>()
        override fun setAnalyticsEnabled(enabled: Boolean) { calls += "analytics:$enabled" }
        override fun setCrashlyticsEnabled(enabled: Boolean) { calls += "crashlytics:$enabled" }
        override fun setPerformanceEnabled(enabled: Boolean) { calls += "performance:$enabled" }
    }

    private class FakeAnalyticsGateway : AnalyticsGateway {
        val events = mutableListOf<TelemetryEvent>()
        override fun log(event: TelemetryEvent) { events += event }
    }

    private class FakePerformanceGateway : PerformanceGateway {
        val started = mutableListOf<String>()
        var stopped = 0
        override fun start(name: String): PerformanceTrace {
            started += name
            return object : PerformanceTrace {
                override fun putAttribute(name: String, value: String) = Unit
                override fun stop() { stopped++ }
            }
        }
    }
}
