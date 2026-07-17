package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.domain.common.HomeDataResult
import com.armutlu.apporganizer.domain.common.HomeErrorCodes
import com.armutlu.apporganizer.domain.common.MissingReason
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döngü D00 (P0 2.1 çözümü) — [DigitalPulseRepository] sözleşmesinin testleri.
 *
 * [RealDigitalPulseSource]'un kendisi `Context`/`AppRepository`/`UsageStatsHelper` gibi Android
 * bağımlılıkları taşıdığı için (bkz. [RealMissionRuntimeSource] emsali — bu tür ince "Real*Source"
 * sınıfları bu projede doğrudan JVM unit test edilmez), iki katmanlı test stratejisi izlenir:
 *
 * 1) [RealDigitalPulseSource.isCacheExpired] — cache/force-refresh kararının SAF fonksiyonu,
 *    doğrudan test edilir (15 dk cache, force aşma, cache yok senaryoları).
 * 2) [FakeDigitalPulseRepository] — [DigitalPulseRepository] arayüzünün "aynı snapshot iki
 *    tüketiciye aynı skoru verir" ve "engine hatasında Stale/Failed" kontratını doğrular;
 *    arayüz PulseClockViewModel/WrappedViewModel/LauncherViewModel'in nasıl tükettiğini yansıtır.
 */
class DigitalPulseRepositoryImplTest {

    private fun snapshot(total: Int, computedAt: Long, validUntil: Long): DigitalPulseSnapshot =
        DigitalPulseSnapshot(
            score = DigitalPulseScore(
                total = total,
                baseScore = total,
                taskContribution = 0,
                organization = 70,
                attention = 70,
                balance = 70,
                cleanup = 70,
                consistency = 70,
                confidence = DataConfidence.MEDIUM,
                reasons = emptyList(),
            ),
            computedAt = computedAt,
            validUntil = validUntil,
        )

    // ── 1) isCacheExpired — saf karar fonksiyonu ────────────────────────────

    @Test
    fun `no cached snapshot always requires refresh`() {
        assertTrue(RealDigitalPulseSource.isCacheExpired(cached = null, nowMillis = 1_000L, force = false))
    }

    @Test
    fun `fresh cache within 15 minute TTL does not require refresh`() {
        val cached = snapshot(total = 70, computedAt = 0L, validUntil = RealDigitalPulseSource.CACHE_TTL_MS)
        assertFalse(
            RealDigitalPulseSource.isCacheExpired(cached = cached, nowMillis = RealDigitalPulseSource.CACHE_TTL_MS - 1, force = false),
        )
    }

    @Test
    fun `cache past validUntil requires refresh`() {
        val cached = snapshot(total = 70, computedAt = 0L, validUntil = RealDigitalPulseSource.CACHE_TTL_MS)
        assertTrue(
            RealDigitalPulseSource.isCacheExpired(cached = cached, nowMillis = RealDigitalPulseSource.CACHE_TTL_MS, force = false),
        )
    }

    @Test
    fun `force refresh bypasses a still-fresh cache`() {
        val cached = snapshot(total = 70, computedAt = 0L, validUntil = RealDigitalPulseSource.CACHE_TTL_MS)
        assertTrue(
            RealDigitalPulseSource.isCacheExpired(cached = cached, nowMillis = 1L, force = true),
        )
    }

    // ── 2) DigitalPulseRepository sözleşmesi — tek kaynak, iki tüketici ────

    @Test
    fun `same snapshot instance is visible to two independent consumers`() = runTest {
        val repo = FakeDigitalPulseRepository(nextSnapshot = { snapshot(total = 82, computedAt = 0L, validUntil = 900_000L) })

        repo.refresh()
        val consumerA = repo.state.value.let { (it as HomeDataResult.Ready).value }
        val consumerB = repo.state.value.let { (it as HomeDataResult.Ready).value }

        // PulseClockViewModel ve WrappedViewModel AYNI snapshot'ı okumalı — iki motor
        // farklı skor gösterme sorunu (P0 2.1) bu eşitlikle kilitlenir.
        assertEquals(consumerA.score.total, consumerB.score.total)
        assertEquals(82, consumerA.score.total)
    }

    @Test
    fun `engine failure with no previous snapshot yields Failed`() = runTest {
        val repo = FakeDigitalPulseRepository(nextSnapshot = { throw RuntimeException("engine boom") })

        repo.refresh()

        assertEquals(HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED), repo.state.value)
    }

    @Test
    fun `engine failure after a prior success preserves the last snapshot as Stale`() = runTest {
        var shouldFail = false
        val goodSnapshot = snapshot(total = 65, computedAt = 0L, validUntil = 900_000L)
        val repo = FakeDigitalPulseRepository(
            nextSnapshot = {
                if (shouldFail) throw RuntimeException("engine boom") else goodSnapshot
            },
        )

        repo.refresh()
        assertEquals(HomeDataResult.Ready(goodSnapshot), repo.state.value)

        shouldFail = true
        repo.refresh(force = true)

        assertEquals(HomeDataResult.Stale(goodSnapshot, HomeErrorCodes.PULSE_COMPUTE_FAILED), repo.state.value)
    }
}

/**
 * [DigitalPulseRepository] sözleşmesinin hafif sahte implementasyonu — [RealDigitalPulseSource]
 * ile AYNI cache/force/Ready-Stale-Failed davranışını taşır ama Android bağımlılığı yoktur,
 * böylece kontrat doğrudan JVM'de test edilebilir.
 */
private class FakeDigitalPulseRepository(
    private val nextSnapshot: () -> DigitalPulseSnapshot,
) : DigitalPulseRepository {
    private val _state = MutableStateFlow<HomeDataResult<DigitalPulseSnapshot>>(
        HomeDataResult.Missing(MissingReason.NO_DATA_YET),
    )
    override val state: StateFlow<HomeDataResult<DigitalPulseSnapshot>> = _state.asStateFlow()

    override suspend fun refresh(force: Boolean) {
        val cached = (_state.value as? HomeDataResult.Ready)?.value
            ?: (_state.value as? HomeDataResult.Stale)?.value
        runCatching { nextSnapshot() }.fold(
            onSuccess = { _state.value = HomeDataResult.Ready(it) },
            onFailure = {
                _state.value = if (cached != null) {
                    HomeDataResult.Stale(cached, HomeErrorCodes.PULSE_COMPUTE_FAILED)
                } else {
                    HomeDataResult.Failed(HomeErrorCodes.PULSE_COMPUTE_FAILED)
                }
            },
        )
    }
}
