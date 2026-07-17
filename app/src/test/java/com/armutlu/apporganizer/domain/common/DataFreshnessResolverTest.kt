package com.armutlu.apporganizer.domain.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * DataFreshnessResolver — Dongu H03 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 557-598). Sabit [Clock] enjeksiyonuyla sinir deger senaryolari.
 */
class DataFreshnessResolverTest {

    private val nowInstant: Instant = Instant.parse("2026-07-17T12:00:00Z")

    private fun resolverAt(instant: Instant = nowInstant): DataFreshnessResolver =
        DataFreshnessResolver(Clock.fixed(instant, ZoneOffset.UTC))

    private val nowMs: Long = nowInstant.toEpochMilli()

    @Test
    fun `null computedAt is UNAVAILABLE`() {
        val resolver = resolverAt()
        assertEquals(DataFreshness.UNAVAILABLE, resolver.resolve(null))
    }

    @Test
    fun `zero age is LIVE`() {
        val resolver = resolverAt()
        assertEquals(DataFreshness.LIVE, resolver.resolve(nowMs))
    }

    @Test
    fun `exactly 5 minutes age is LIVE (boundary inclusive)`() {
        val resolver = resolverAt()
        val computedAt = nowMs - DataFreshnessResolver.LIVE_MAX_AGE_MS
        assertEquals(DataFreshness.LIVE, resolver.resolve(computedAt))
    }

    @Test
    fun `5 minutes plus 1 ms age is RECENT`() {
        val resolver = resolverAt()
        val computedAt = nowMs - (DataFreshnessResolver.LIVE_MAX_AGE_MS + 1)
        assertEquals(DataFreshness.RECENT, resolver.resolve(computedAt))
    }

    @Test
    fun `exactly 30 minutes age is RECENT (boundary inclusive)`() {
        val resolver = resolverAt()
        val computedAt = nowMs - DataFreshnessResolver.RECENT_MAX_AGE_MS
        assertEquals(DataFreshness.RECENT, resolver.resolve(computedAt))
    }

    @Test
    fun `30 minutes plus 1 ms age is STALE`() {
        val resolver = resolverAt()
        val computedAt = nowMs - (DataFreshnessResolver.RECENT_MAX_AGE_MS + 1)
        assertEquals(DataFreshness.STALE, resolver.resolve(computedAt))
    }

    @Test
    fun `future timestamp (clock skew) is LIVE`() {
        val resolver = resolverAt()
        val computedAt = nowMs + (60 * 1000L)
        assertEquals(DataFreshness.LIVE, resolver.resolve(computedAt))
    }

    @Test
    fun `24 hours old is STALE`() {
        val resolver = resolverAt()
        val computedAt = nowMs - (24 * 60 * 60 * 1000L)
        assertEquals(DataFreshness.STALE, resolver.resolve(computedAt))
    }
}
