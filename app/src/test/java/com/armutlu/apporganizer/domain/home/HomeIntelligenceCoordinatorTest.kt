package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
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
 * HomeIntelligenceCoordinator testleri (Döngü H02, H04).
 *
 * Koordinatör sadece orkestrasyon yapar — bu testler skor/görev hesabı DEĞİL,
 * eşzamanlılık kontrolü (tek işe düşme), kaynak bazlı hata izolasyonu ve
 * [HomeDataResult] sarmalayıcısının doğru üretilmesini doğrular (Döngü H04:
 * Ready/Stale/Missing/Failed geçişleri, tek/çift/üçlü kaynak hata kombinasyonları).
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
    //    hatalı kaynağın son başarılı değeri Stale olarak korunur.
    @Test
    fun `one source failing does not block the other two from updating`() = runTest(testDispatcher) {
        // Önce başarılı bir refresh ile mission alanına gerçek bir değer koy.
        val originalMission = MissionSourceState(missions = listOf("mission-original"))
        missionState.value = originalMission
        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()
        assertEquals(HomeDataResult.Ready(originalMission), coordinator.state.value.mission)

        // Şimdi mission kaynağı hata atsın, pulse/ticker ise yeni değerler yayınlasın.
        coEvery { missionRepo.refresh() } throws RuntimeException("boom")
        val newPulse = PulseSourceState(snapshot = "pulse-v1")
        val newTicker = TickerSourceState(items = listOf("ticker-item"))
        pulseState.value = newPulse
        tickerState.value = newTicker

        coordinator.refresh(RefreshReason.MANUAL_REFRESH)
        advanceUntilIdle()

        val state = coordinator.state.value
        assertEquals(HomeDataResult.Ready(newPulse), state.pulse)
        assertEquals(HomeDataResult.Ready(newTicker), state.ticker)
        // mission kaynağı hata verdi — son başarılı değeri (originalMission) Stale olarak korunur, kaybolmaz.
        assertEquals(
            HomeDataResult.Stale(originalMission, HomeErrorCodes.MISSION_SETTLEMENT_FAILED),
            state.mission,
        )
    }

    // 3) Geçici hata sırasında önceki başarılı state kaybolmaz (Stale olarak korunur).
    @Test
    fun `previous successful state is preserved across a transient failure`() = runTest(testDispatcher) {
        // İlk başarılı refresh — mission değeri state'e girer.
        val firstMission = MissionSourceState(missions = listOf("mission-1"))
        missionState.value = firstMission
        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()
        assertEquals(HomeDataResult.Ready(firstMission), coordinator.state.value.mission)

        // İkinci refresh'te mission kaynağı geçici hata atar — mission alanı Stale(firstMission) olmalı.
        coEvery { missionRepo.refresh() } throws RuntimeException("transient")
        coordinator.refresh(RefreshReason.MANUAL_REFRESH)
        advanceUntilIdle()

        val missionResult = coordinator.state.value.mission
        assertTrue("hata sonrası state boşalmamalı, Stale olmalı", missionResult is HomeDataResult.Stale)
        assertEquals(firstMission, (missionResult as HomeDataResult.Stale).value)
        assertEquals(HomeErrorCodes.MISSION_SETTLEMENT_FAILED, missionResult.warningCode)
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

    // 5) İlk refresh'te bir kaynak hata verirse (önceki değer yok) Failed döner.
    @Test
    fun `source failing on first refresh with no previous value yields Failed`() = runTest(testDispatcher) {
        coEvery { pulseRepo.refresh() } throws RuntimeException("first-time boom")

        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()

        assertEquals(
            HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED),
            coordinator.state.value.pulse,
        )
    }

    // 6) İki kaynak aynı anda hata verirse ikisi de Failed (önceki değer yoksa) döner,
    //    üçüncü kaynak Ready olarak güncellenir.
    @Test
    fun `two sources failing simultaneously both yield Failed while the third stays Ready`() = runTest(testDispatcher) {
        coEvery { pulseRepo.refresh() } throws RuntimeException("pulse boom")
        coEvery { missionRepo.refresh() } throws RuntimeException("mission boom")
        val newTicker = TickerSourceState(items = listOf("ticker-ok"))
        tickerState.value = newTicker

        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()

        val state = coordinator.state.value
        assertEquals(HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED), state.pulse)
        assertEquals(HomeDataResult.Failed(HomeErrorCodes.MISSION_SETTLEMENT_FAILED), state.mission)
        assertEquals(HomeDataResult.Ready(newTicker), state.ticker)
    }

    // 7) Üç kaynak da aynı anda hata verirse (önceki değer yok) hepsi Failed olur,
    //    ekran state'i tamamen boşalmaz (loading false'a döner, state null değil).
    @Test
    fun `all three sources failing simultaneously all yield Failed`() = runTest(testDispatcher) {
        coEvery { pulseRepo.refresh() } throws RuntimeException("pulse boom")
        coEvery { missionRepo.refresh() } throws RuntimeException("mission boom")
        coEvery { tickerEngine.refresh() } throws RuntimeException("ticker boom")

        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()

        val state = coordinator.state.value
        assertEquals(HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED), state.pulse)
        assertEquals(HomeDataResult.Failed(HomeErrorCodes.MISSION_SETTLEMENT_FAILED), state.mission)
        assertEquals(HomeDataResult.Failed(HomeErrorCodes.NOTIFICATION_DATA_UNAVAILABLE), state.ticker)
        assertTrue("hata sonrası loading false olmalı", !state.loading)
    }

    // 8) Üç kaynak da önce başarılı olup sonra üçü birden hata verirse hepsi Stale olur
    //    (önceki değerler korunur), state boşalmaz.
    @Test
    fun `all three sources failing after a prior success all yield Stale with previous values`() = runTest(testDispatcher) {
        val firstPulse = PulseSourceState(snapshot = "pulse-first")
        val firstMission = MissionSourceState(missions = listOf("mission-first"))
        val firstTicker = TickerSourceState(items = listOf("ticker-first"))
        pulseState.value = firstPulse
        missionState.value = firstMission
        tickerState.value = firstTicker

        coordinator.refresh(RefreshReason.APP_START)
        advanceUntilIdle()

        coEvery { pulseRepo.refresh() } throws RuntimeException("pulse boom")
        coEvery { missionRepo.refresh() } throws RuntimeException("mission boom")
        coEvery { tickerEngine.refresh() } throws RuntimeException("ticker boom")

        coordinator.refresh(RefreshReason.MANUAL_REFRESH)
        advanceUntilIdle()

        val state = coordinator.state.value
        assertEquals(HomeDataResult.Stale(firstPulse, HomeErrorCodes.PULSE_COMPUTE_FAILED), state.pulse)
        assertEquals(HomeDataResult.Stale(firstMission, HomeErrorCodes.MISSION_SETTLEMENT_FAILED), state.mission)
        assertEquals(HomeDataResult.Stale(firstTicker, HomeErrorCodes.NOTIFICATION_DATA_UNAVAILABLE), state.ticker)
    }

    // 9) İlk state Missing(NO_DATA_YET) — henüz hiçbir refresh çağrılmadı.
    @Test
    fun `initial state before any refresh is Missing with NO_DATA_YET`() {
        val freshCoordinator = HomeIntelligenceCoordinator(
            digitalPulseRepository = pulseRepo,
            missionRuntimeRepository = missionRepo,
            smartTickerEngine = tickerEngine,
            ioDispatcher = testDispatcher,
        )

        assertTrue(freshCoordinator.state.value.pulse is HomeDataResult.Missing)
        assertTrue(freshCoordinator.state.value.mission is HomeDataResult.Missing)
        assertTrue(freshCoordinator.state.value.ticker is HomeDataResult.Missing)
    }
}
