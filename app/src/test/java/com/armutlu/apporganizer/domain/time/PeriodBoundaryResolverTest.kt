package com.armutlu.apporganizer.domain.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * PeriodBoundaryResolver — Dongu H01 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 423-489). Sabit [Clock] enjeksiyonuyla deterministik senaryolar.
 */
class PeriodBoundaryResolverTest {

    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun resolverAt(localDateTime: LocalDateTime, zoneId: ZoneId = istanbul): PeriodBoundaryResolver {
        val instant = localDateTime.atZone(zoneId).toInstant()
        return PeriodBoundaryResolver(Clock.fixed(instant, zoneId), zoneId)
    }

    @Test
    fun `monday 00_00 is the week start`() {
        // 2026-07-13 Pazartesi
        val monday = LocalDate.of(2026, 7, 13)
        assertEquals(java.time.DayOfWeek.MONDAY, monday.dayOfWeek)

        val resolver = resolverAt(monday.atTime(0, 0, 0))
        val week = resolver.currentIsoWeek()

        assertEquals(monday.toEpochDay(), week.weekStartEpochDay)
        assertEquals(monday.toEpochDay(), week.epochDay)
        assertEquals(monday.atStartOfDay(istanbul).toInstant().toEpochMilli(), week.startInclusive)
    }

    @Test
    fun `sunday 23_59 belongs to the same week as the preceding monday`() {
        val monday = LocalDate.of(2026, 7, 13)
        val sunday = monday.plusDays(6) // 2026-07-19 Pazar
        assertEquals(java.time.DayOfWeek.SUNDAY, sunday.dayOfWeek)

        val resolver = resolverAt(sunday.atTime(23, 59, 0))
        val week = resolver.currentIsoWeek()

        assertEquals(monday.toEpochDay(), week.weekStartEpochDay)
        // Pazar hala haftanin bitis sinirindan (bir sonraki Pazartesi 00:00) once.
        val nextMonday = monday.plusWeeks(1)
        assertEquals(nextMonday.atStartOfDay(istanbul).toInstant().toEpochMilli(), week.endExclusive)
    }

    @Test
    fun `same instant can fall on different local days across time zones`() {
        // Istanbul'da gece yarisindan hemen sonra (00:30) - New York'ta hala bir onceki gunun aksami.
        val istanbulMidnight = LocalDate.of(2026, 7, 14).atTime(0, 30, 0).atZone(istanbul).toInstant()
        val newYork = ZoneId.of("America/New_York")

        val istanbulResolver = PeriodBoundaryResolver(Clock.fixed(istanbulMidnight, istanbul), istanbul)
        val newYorkResolver = PeriodBoundaryResolver(Clock.fixed(istanbulMidnight, newYork), newYork)

        val istanbulDay = istanbulResolver.currentDay()
        val newYorkDay = newYorkResolver.currentDay()

        assertNotEquals(istanbulDay.epochDay, newYorkDay.epochDay)
        assertEquals(LocalDate.of(2026, 7, 14).toEpochDay(), istanbulDay.epochDay)
        assertEquals(LocalDate.of(2026, 7, 13).toEpochDay(), newYorkDay.epochDay)
    }

    @Test
    fun `dst spring-forward day in berlin is 23 hours not 24`() {
        // Europe/Berlin 2026: saat ileri alma 29 Mart Pazar 02:00 -> 03:00 (23 saatlik gun).
        val berlin = ZoneId.of("Europe/Berlin")
        val dstDay = LocalDate.of(2026, 3, 29)
        val resolver = resolverAt(dstDay.atTime(10, 0, 0), berlin)

        val day = resolver.currentDay()
        val durationMs = day.endExclusive - day.startInclusive
        val twentyThreeHoursMs = 23L * 60 * 60 * 1000
        val twentyFourHoursMs = 24L * 60 * 60 * 1000

        assertEquals(twentyThreeHoursMs, durationMs)
        assertNotEquals(twentyFourHoursMs, durationMs)
    }

    @Test
    fun `dst fall-back day in berlin is 25 hours not 24`() {
        // Europe/Berlin 2026: saat geri alma 25 Ekim Pazar 03:00 -> 02:00 (25 saatlik gun).
        val berlin = ZoneId.of("Europe/Berlin")
        val dstDay = LocalDate.of(2026, 10, 25)
        val resolver = resolverAt(dstDay.atTime(10, 0, 0), berlin)

        val day = resolver.currentDay()
        val durationMs = day.endExclusive - day.startInclusive
        val twentyFiveHoursMs = 25L * 60 * 60 * 1000
        val twentyFourHoursMs = 24L * 60 * 60 * 1000

        assertEquals(twentyFiveHoursMs, durationMs)
        assertNotEquals(twentyFourHoursMs, durationMs)
    }

    @Test
    fun `midnight boundary produces different epochDay just before and after`() {
        val day = LocalDate.of(2026, 7, 15)
        val justBefore = resolverAt(day.atTime(23, 59, 59, 999_000_000))
        val justAfter = resolverAt(day.plusDays(1).atTime(0, 0, 0, 0))

        val beforeBoundary = justBefore.currentDay()
        val afterBoundary = justAfter.currentDay()

        assertEquals(day.toEpochDay(), beforeBoundary.epochDay)
        assertEquals(day.plusDays(1).toEpochDay(), afterBoundary.epochDay)
        assertNotEquals(beforeBoundary.epochDay, afterBoundary.epochDay)
    }

    @Test
    fun `currentDay has null weekStartEpochDay`() {
        val resolver = resolverAt(LocalDate.of(2026, 7, 15).atTime(12, 0, 0))
        assertNull(resolver.currentDay().weekStartEpochDay)
    }

    @Test
    fun `previousIsoWeek starts exactly 7 days before current week monday`() {
        // Mevcut hafta: Persembe 2026-07-16 (haftasi 2026-07-13 Pazartesi baslar).
        val thursday = LocalDate.of(2026, 7, 16)
        val resolver = resolverAt(thursday.atTime(9, 0, 0))

        val currentWeek = resolver.currentIsoWeek()
        val previousWeek = resolver.previousIsoWeek()

        assertEquals(currentWeek.weekStartEpochDay!! - 7, previousWeek.weekStartEpochDay)
        assertEquals(currentWeek.startInclusive - previousWeek.startInclusive, 7L * 24 * 60 * 60 * 1000)
        assertEquals(previousWeek.endExclusive, currentWeek.startInclusive)
    }

    @Test
    fun `nextLocalMidnight returns the instant of the following day boundary`() {
        val today = LocalDate.of(2026, 7, 15)
        val resolver = resolverAt(today.atTime(14, 30, 0))

        val expected = today.plusDays(1).atStartOfDay(istanbul).toInstant()
        assertEquals(expected, resolver.nextLocalMidnight())
    }

    @Test
    fun `nextWeekBoundary returns the instant of the following monday`() {
        val wednesday = LocalDate.of(2026, 7, 15) // haftasi 2026-07-13 Pazartesi baslar
        val resolver = resolverAt(wednesday.atTime(14, 30, 0))

        val nextMonday = LocalDate.of(2026, 7, 20)
        val expected = nextMonday.atStartOfDay(istanbul).toInstant()
        assertEquals(expected, resolver.nextWeekBoundary())
    }

    @Test
    fun `nextLocalMidnight across dst spring-forward is still correct wall-clock instant`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val dayBeforeDst = LocalDate.of(2026, 3, 28)
        val resolver = resolverAt(dayBeforeDst.atTime(23, 0, 0), berlin)

        val expected = dayBeforeDst.plusDays(1).atStartOfDay(berlin).toInstant()
        assertEquals(expected, resolver.nextLocalMidnight())
    }
}
