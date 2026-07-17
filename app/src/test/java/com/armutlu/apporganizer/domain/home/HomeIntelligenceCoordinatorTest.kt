package com.armutlu.apporganizer.domain.home

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * HomeIntelligenceCoordinator testleri (Döngü H02).
 *
 * Koordinatör sadece orkestrasyon yapar — bu testler skor/görev hesabı DEĞİL,
 * eşzamanlılık kontrolü (tek işe düşme), kaynak bazlı hata izolasyonu ve
 * son başarılı state'in korunmasını doğrular.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeIntelligenceCoordinatorTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pulseRepo: DigitalPulseRepository
    private lateinit var missionRepo: MissionRuntimeRepository
    private lateinit var tickerEngine: SmartTickerEngine

    private val pulseState = MutableStateFlow(PulseSourceState())
    private val missionState = MutableStateFlow(MissionSourceState())
    private val tickerState = MutableStateFlow(TickerSourceState())

    private lateinit var coordinator: HomeIntelligenceCoordinator

    @Before
    fun setup() {
        pulseRepo = mockk()
        missionRepo = mockk()
        tickerEngine = mockk()

        every { pulseRepo.state } returns pulseState
        every { missionRepo.state } returns missionState
        every { tickerEngine.state } returns tickerState

        coEvery { pulseRepo.refresh() } returns Unit
        coEvery { missionRepo.refresh() } returns Unit
        coEvery { tickerEngine.refresh() } returns Unit

        coordinator = HomeIntelligenceCoordinator(
            digitalPulseRepository = pulseRepo,
            missionRuntimeRepository = missionRepo,
            smartTickerEngine = tickerEngine,
            ioDispatcher = testDispatcher,
        )
    }

    // 1) Eşzamanlı 5 refresh çağrısı → kaynaklar 1 kez çağrılır.
    @Test
    fun `concurrent refresh calls collapse into a single execution`() = runTest(testDispatcher) {
        val jobs = (1..5).map {
            async { coordinator.refresh(RefreshReason.MANUAL_REFRESH) }
        }
        advanceUntilIdle()
        jobs.forEach { it.await() }

        coVerify(exactly = 1) { pulseRepo.refresh() }
        coVerify(exactly = 1) { missionRepo.refresh() }
        coVerify(exactly = 1) { tickerEngine.refresh() }
    }

    // 2) Bir kaynak exception atınca diğer iki kaynağın yeni değeri state'e girer,
    //    hatalı kaynağın son başarılı değeri korunur.
    @Test
    fun `one source failing does not block the other two from updating`() = runTest(testDispatcher) {
        // Önce başarılı bir refresh ile mission alanına gerçek bir değer koy.
        val originalMission = MissionSourceState(missions = listOf("mission-original"))
        missionState.value = originalMission
        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()
        assertEquals(originalMission, coordinator.state.value.mission)

        // Şimdi mission kaynağı hata atsın, pulse/ticker ise yeni değerler yayınlasın.
        coEvery { missionRepo.refresh() } throws RuntimeException("boom")
        val newPulse = PulseSourceState(snapshot = "pulse-v1")
        val newTicker = TickerSourceState(items = listOf("ticker-item"))
        pulseState.value = newPulse
        tickerState.value = newTicker

        coordinator.refresh(RefreshReason.MANUAL_REFRESH)
        advanceUntilIdle()

        val state = coordinator.state.value
        assertEquals(newPulse, state.pulse)
        assertEquals(newTicker, state.ticker)
        // mission kaynağı hata verdi — son başarılı değeri (originalMission) korunur, kaybolmaz.
        assertEquals(originalMission, state.mission)
    }

    // 3) Geçici hata sırasında önceki başarılı state kaybolmaz.
    @Test
    fun `previous successful state is preserved across a transient failure`() = runTest(testDispatcher) {
        // İlk başarılı refresh — mission değeri state'e girer.
        val firstMission = MissionSourceState(missions = listOf("mission-1"))
        missionState.value = firstMission
        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()
        assertEquals(firstMission, coordinator.state.value.mission)

        // İkinci refresh'te mission kaynağı geçici hata atar — mission alanı hala firstMission olmalı.
        coEvery { missionRepo.refresh() } throws RuntimeException("transient")
        coordinator.refresh(RefreshReason.MANUAL_REFRESH)
        advanceUntilIdle()

        assertEquals(firstMission, coordinator.state.value.mission)
        assertTrue("hata sonrası state boşalmamalı", coordinator.state.value.mission != null)
    }

    // 4) refresh reason'ın state'e yansıması.
    @Test
    fun `refresh reason is reflected in state after completion`() = runTest(testDispatcher) {
        assertNull(coordinator.state.value.lastRefreshReason)

        coordinator.refresh(RefreshReason.NOTIFICATION_EVENT)
        advanceUntilIdle()

        assertEquals(RefreshReason.NOTIFICATION_EVENT, coordinator.state.value.lastRefreshReason)
        assertNotNull(coordinator.state.value.lastRefreshAt)
        assertTrue("refresh bitince loading false olmalı", !coordinator.state.value.loading)
    }
}
