package com.armutlu.apporganizer.domain.usecase.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class UsageSessionAggregatorTest {
    private val utc = ZoneId.of("UTC")
    private val base = Instant.parse("2026-01-10T00:00:00Z").toEpochMilli()

    @Test fun `single session is counted`() {
        val result = run(listOf(resume("a", "A", 1_000), pause("a", "A", 6_000)), 0, 10_000).single()
        assertEquals(1, result.launchCount)
        assertEquals(5_000, result.foregroundDurationMs)
        assertEquals(5_000, result.globalForegroundMs)
        assertFalse(result.isPartial)
    }

    @Test fun `duplicate resume and pause are idempotent`() {
        val events = listOf(resume("a", "A", 1_000), resume("a", "A", 2_000), pause("a", "A", 5_000), pause("a", "A", 7_000))
        val result = run(events, 0, 10_000).single()
        assertEquals(1, result.launchCount)
        assertEquals(4_000, result.foregroundDurationMs)
    }

    @Test fun `activity transition in one package does not double count`() {
        val events = listOf(resume("a", "A", 1_000), resume("a", "B", 2_000), pause("a", "A", 3_000), pause("a", "B", 6_000))
        val result = run(events, 0, 10_000).single()
        assertEquals(1, result.launchCount)
        assertEquals(5_000, result.foregroundDurationMs)
    }

    @Test fun `overlapping packages use union for global duration`() {
        val events = listOf(resume("a", "A", 1_000), resume("b", "B", 3_000), pause("a", "A", 5_000), pause("b", "B", 7_000))
        val result = run(events, 0, 10_000)
        assertEquals(4_000, result.first { it.packageName == "a" }.foregroundDurationMs)
        assertEquals(4_000, result.first { it.packageName == "b" }.foregroundDurationMs)
        assertTrue(result.all { it.globalForegroundMs == 6_000L })
    }

    @Test fun `lock freezes and unlock resumes active session`() {
        val events = listOf(resume("a", "A", 1_000), global(UsageEventType.KEYGUARD_SHOWN, 3_000), global(UsageEventType.KEYGUARD_HIDDEN, 8_000), pause("a", "A", 10_000))
        assertEquals(4_000, run(events, 0, 12_000).single().foregroundDurationMs)
    }

    @Test fun `shutdown closes all sessions`() {
        val events = listOf(resume("a", "A", 1_000), global(UsageEventType.DEVICE_SHUTDOWN, 4_000))
        val result = run(events, 0, 10_000).single()
        assertEquals(3_000, result.foregroundDurationMs)
        assertFalse(result.isPartial)
    }

    @Test fun `midnight and DST are split by actual elapsed time`() {
        val zone = ZoneId.of("Europe/Berlin")
        val start = Instant.parse("2026-03-28T22:30:00Z").toEpochMilli() // 23:30 local
        val end = Instant.parse("2026-03-29T02:30:00Z").toEpochMilli() // 04:30 local after jump
        val result = UsageSessionAggregator(zone).aggregate(
            listOf(UsageEvent("a", "A", UsageEventType.RESUMED, start), UsageEvent("a", "A", UsageEventType.PAUSED, end)),
            start, end,
        )
        assertEquals(2, result.size)
        assertEquals(30 * 60_000L, result[0].foregroundDurationMs)
        assertEquals(210 * 60_000L, result[1].foregroundDurationMs)
        assertEquals(0L, result[1].hourlyForegroundMs[2])
        assertEquals(4 * 60 * 60_000L, result.sumOf { it.foregroundDurationMs })
    }

    @Test fun `open session is clamped and marked partial`() {
        val result = run(listOf(resume("a", "A", -5_000)), 0, 10_000).single()
        assertEquals(10_000, result.foregroundDurationMs)
        assertEquals(0, result.launchCount)
        assertTrue(result.isPartial)
        assertEquals(24, result.hourlyForegroundMs.size)
    }

    @Test fun `session crossing only start boundary is partial`() {
        val result = run(listOf(resume("a", "A", -5_000), pause("a", "A", 5_000)), 0, 10_000).single()
        assertEquals(5_000, result.foregroundDurationMs)
        assertTrue(result.isPartial)
    }

    @Test fun `resume after shutdown starts a fresh session`() {
        val events = listOf(resume("a", "A", 1_000), global(UsageEventType.DEVICE_SHUTDOWN, 3_000), resume("a", "A", 5_000), pause("a", "A", 7_000))
        val result = run(events, 0, 10_000).single()
        assertEquals(2, result.launchCount)
        assertEquals(4_000, result.foregroundDurationMs)
    }

    @Test fun `terminal event wins before resume at identical timestamp`() {
        val events = listOf(resume("a", "A", 1_000), resume("a", "A", 5_000), pause("a", "A", 5_000), pause("a", "A", 8_000))
        val result = run(events, 0, 10_000).single()
        assertEquals(2, result.launchCount)
        assertEquals(7_000, result.foregroundDurationMs)
    }

    private fun run(events: List<UsageEvent>, start: Long, end: Long) =
        UsageSessionAggregator(utc).aggregate(events, base + start, base + end)

    private fun resume(pkg: String, cls: String, offset: Long) = UsageEvent(pkg, cls, UsageEventType.RESUMED, base + offset)
    private fun pause(pkg: String, cls: String, offset: Long) = UsageEvent(pkg, cls, UsageEventType.PAUSED, base + offset)
    private fun global(type: UsageEventType, offset: Long) = UsageEvent("", "", type, base + offset)
}
