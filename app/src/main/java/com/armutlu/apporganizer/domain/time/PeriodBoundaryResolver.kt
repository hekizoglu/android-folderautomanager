package com.armutlu.apporganizer.domain.time

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Tum gunluk/haftalik gorev ve skor hesaplarinin kullanmasi gereken TEK yerel zaman sinirlarini
 * uretir (Dongu H01 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md).
 *
 * Kurallar:
 * - Hafta PAZARTESI baslar (ISO-8601 ile uyumlu, [DayOfWeek.MONDAY]).
 * - Gun/hafta sinirlari sabit `24 * 60 * 60 * 1000` ile DEGIL, [ZonedDateTime] ile hesaplanir —
 *   DST gecislerinde 23/25 saatlik gunler dogru sinirlanir.
 * - `minSdk 26` oldugu icin `java.time` dogrudan kullanilir, desugaring gerekmez.
 *
 * [clock] ve [zoneId] test edilebilirlik icin enjekte edilir (sabit Clock ile deterministik test).
 */
class PeriodBoundaryResolver(
    private val clock: Clock,
    private val zoneId: ZoneId,
) {

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock).withZoneSameInstant(zoneId)

    /** Bulunulan yerel gunun sinirlarini dondurur (00:00:00.000 dahil - ertesi gun 00:00 haric). */
    fun currentDay(): PeriodBoundary = dayBoundary(now().toLocalDate())

    /** Bulunulan ISO haftanin (Pazartesi 00:00 - bir sonraki Pazartesi 00:00 haric) sinirlarini dondurur. */
    fun currentIsoWeek(): PeriodBoundary = weekBoundary(now().toLocalDate())

    /** Bir onceki ISO haftanin sinirlarini dondurur (mevcut haftanin Pazartesi'sinden tam 7 gun once baslar). */
    fun previousIsoWeek(): PeriodBoundary {
        val currentWeekStart = mondayOf(now().toLocalDate())
        return weekBoundary(currentWeekStart.minusWeeks(1))
    }

    /** Bir sonraki yerel gun sinirinin (gece yarisi) Instant'ini dondurur. */
    fun nextLocalMidnight(): Instant = currentDay().endExclusive.let { Instant.ofEpochMilli(it) }

    /** Bir sonraki hafta sinirinin (gelecek Pazartesi 00:00) Instant'ini dondurur. */
    fun nextWeekBoundary(): Instant = currentIsoWeek().endExclusive.let { Instant.ofEpochMilli(it) }

    private fun mondayOf(date: LocalDate): LocalDate =
        if (date.dayOfWeek == DayOfWeek.MONDAY) date
        else date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))

    private fun dayBoundary(date: LocalDate): PeriodBoundary {
        val startInclusive = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return PeriodBoundary(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            epochDay = date.toEpochDay(),
            weekStartEpochDay = null,
        )
    }

    private fun weekBoundary(anyDateInWeek: LocalDate): PeriodBoundary {
        val monday = mondayOf(anyDateInWeek)
        val nextMonday = monday.plusWeeks(1)
        val startInclusive = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = nextMonday.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return PeriodBoundary(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
            epochDay = monday.toEpochDay(),
            weekStartEpochDay = monday.toEpochDay(),
        )
    }
}
