package com.armutlu.apporganizer.telemetry

import com.armutlu.apporganizer.presentation.viewmodel.ConnectionTestStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseConnectionTesterTest {
    @Test
    fun `missing configuration fails closed without touching Firebase services`() = runTest {
        val dependencies = FakeDependencies(configured = false)

        val result = FirebaseConnectionTester(dependencies).test()

        assertEquals(FirebaseConnectionTester.FIREBASE_NOT_CONFIGURED, result.safeErrorCode)
        assertEquals(ConnectionTestStatus.FAILED, result.status)
        assertFalse(dependencies.roundTripCalled)
        assertEquals(result, dependencies.persisted)
    }

    @Test
    fun `unvalidated network reports network unavailable`() = runTest {
        val dependencies = FakeDependencies(network = false)

        val result = FirebaseConnectionTester(dependencies).test()

        assertEquals(FirebaseConnectionTester.NETWORK_UNAVAILABLE, result.safeErrorCode)
        assertFalse(dependencies.roundTripCalled)
    }

    @Test
    fun `successful backend token refresh runs safe SDK probes`() = runTest {
        val dependencies = FakeDependencies()

        val result = FirebaseConnectionTester(dependencies).test()

        assertEquals(ConnectionTestStatus.SUCCESS, result.status)
        assertTrue(result.firebaseRoundTripOk)
        assertTrue(result.analyticsQueued)
        assertTrue(result.crashlyticsReady)
        assertTrue(result.performanceReady)
        assertEquals(1, dependencies.analyticsCalls)
        assertEquals(1, dependencies.crashlyticsCalls)
        assertEquals(1, dependencies.performanceCalls)
    }

    @Test
    fun `round trip failure never reports analytics queued`() = runTest {
        val dependencies = FakeDependencies(roundTripFailure = true)

        val result = FirebaseConnectionTester(dependencies).test()

        assertEquals(FirebaseConnectionTester.INSTALLATION_ID_FAILED, result.safeErrorCode)
        assertFalse(result.analyticsQueued)
        assertEquals(0, dependencies.analyticsCalls)
    }

    @Test
    fun `telemetry service failure after round trip reports partial success`() = runTest {
        val dependencies = FakeDependencies(analyticsFailure = true)

        val result = FirebaseConnectionTester(dependencies).test()

        assertEquals(ConnectionTestStatus.PARTIAL_SUCCESS, result.status)
        assertEquals(FirebaseConnectionTester.ANALYTICS_NOT_AVAILABLE, result.safeErrorCode)
        assertTrue(result.firebaseRoundTripOk)
        assertFalse(result.analyticsQueued)
        assertTrue(result.crashlyticsReady)
        assertTrue(result.performanceReady)
    }
}

private class FakeDependencies(
    private val configured: Boolean = true,
    private val network: Boolean = true,
    private val roundTripFailure: Boolean = false,
    private val analyticsFailure: Boolean = false,
    private val crashlyticsFailure: Boolean = false,
    private val performanceFailure: Boolean = false,
) : FirebaseConnectionDependencies {
    var roundTripCalled = false
    var analyticsCalls = 0
    var crashlyticsCalls = 0
    var performanceCalls = 0
    var persisted: FirebaseConnectionTestResult? = null

    override fun configurationOk() = configured
    override fun networkAvailable() = network
    override suspend fun roundTrip() {
        roundTripCalled = true
        if (roundTripFailure) error("safe synthetic failure")
    }
    override fun queueAnalytics() {
        analyticsCalls++
        if (analyticsFailure) error("analytics unavailable")
    }
    override fun logCrashlytics() {
        crashlyticsCalls++
        if (crashlyticsFailure) error("crashlytics unavailable")
    }
    override fun runPerformanceTrace() {
        performanceCalls++
        if (performanceFailure) error("performance unavailable")
    }
    override fun persist(result: FirebaseConnectionTestResult) { persisted = result }
    override fun now() = 123L
}
