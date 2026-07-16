package com.armutlu.apporganizer.presentation.viewmodel

import com.armutlu.apporganizer.telemetry.FirebaseConnectionTestResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsageDataViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `sharing change is retained in state and persisted`() {
        var persisted = false
        val model = UsageDataViewModel(false, { persisted = it }) { result(ConnectionTestStatus.SUCCESS) }

        model.setSharingEnabled(true)

        assertEquals(true, model.uiState.value.sharingEnabled)
        assertEquals(true, persisted)
    }

    @Test
    fun `second connection click is ignored while testing`() = runTest(dispatcher) {
        val result = CompletableDeferred<FirebaseConnectionTestResult>()
        var calls = 0
        val model = UsageDataViewModel(false, {}) {
            calls++
            result.await()
        }

        model.runConnectionTest()
        model.runConnectionTest()
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(ConnectionTestStatus.TESTING, model.uiState.value.connectionStatus)
        result.complete(result(ConnectionTestStatus.PARTIAL_SUCCESS))
        advanceUntilIdle()
        assertEquals(ConnectionTestStatus.PARTIAL_SUCCESS, model.uiState.value.connectionStatus)
        assertFalse(model.uiState.value.sharingEnabled)
    }

    private fun result(status: ConnectionTestStatus) = FirebaseConnectionTestResult(
        configurationOk = true,
        networkAvailable = true,
        firebaseRoundTripOk = true,
        analyticsQueued = true,
        crashlyticsReady = true,
        performanceReady = true,
        testedAt = 1L,
        status = status,
        safeErrorCode = null,
    )
}
