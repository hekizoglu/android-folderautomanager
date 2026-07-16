package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TelemetryManagerTest {
    @Before
    fun resetGateway() {
        TelemetryManager.applyCollectionState(false, FakeServices())
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

    private class FakeServices : CollectionServices {
        val calls = mutableListOf<String>()
        override fun setAnalyticsEnabled(enabled: Boolean) { calls += "analytics:$enabled" }
        override fun setCrashlyticsEnabled(enabled: Boolean) { calls += "crashlytics:$enabled" }
        override fun setPerformanceEnabled(enabled: Boolean) { calls += "performance:$enabled" }
    }
}
