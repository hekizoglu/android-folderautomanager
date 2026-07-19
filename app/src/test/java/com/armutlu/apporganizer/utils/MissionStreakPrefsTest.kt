package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MissionStreakPrefs.advancePure — Dongu G4 (GOREV_SISTEMI_AKILLI_GELISTIRME_PLANI.md).
 * Saf fonksiyonu (Android bagimliligi yok) dogrudan test eder: ardisik gun, bosluk+dondurma,
 * bosluk+hak yok, golden seri, idempotent ayni gun, hafta basinda hakkin tazelenmesi.
 */
class MissionStreakPrefsTest {

    private val initial = MissionStreakPrefs.PureStreakState(
        currentStreak = 0,
        bestStreak = 0,
        goldenStreak = 0,
        lastCountedEpochDay = -1L,
        lastFrozenEpochDay = -1L,
        freezeWeek = -1L,
        freezeUsedThisWeek = false,
    )

    @Test
    fun `first day with at least one completed mission starts streak at 1`() {
        val result = MissionStreakPrefs.advancePure(
            before = initial,
            epochDay = 100L,
            completedCount = 1,
            totalCount = 3,
            weekStartEpochDay = 98L,
        )
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.bestStreak)
        assertEquals(0, result.goldenStreak) // 1/3 tamamlandi, 3/3 degil -> golden yok
        assertEquals(100L, result.lastCountedEpochDay)
    }

    @Test
    fun `first day with zero completed missions does not start a streak`() {
        val result = MissionStreakPrefs.advancePure(
            before = initial,
            epochDay = 100L,
            completedCount = 0,
            totalCount = 3,
            weekStartEpochDay = 98L,
        )
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.bestStreak)
        assertEquals(100L, result.lastCountedEpochDay) // yine de idempotency icin gun kaydedilir
    }

    @Test
    fun `consecutive days increment the streak`() {
        var state = initial
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 2, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 102L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)

        assertEquals(3, state.currentStreak)
        assertEquals(3, state.bestStreak)
    }

    @Test
    fun `bestStreak never decreases when current streak resets`() {
        var state = initial
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(2, state.bestStreak)

        // 3+ gun bosluk (hak da yok sayilir cunku gap=2 degil, direkt kirilma senaryosu).
        state = MissionStreakPrefs.advancePure(state, epochDay = 110L, completedCount = 1, totalCount = 3, weekStartEpochDay = 105L)
        assertEquals(1, state.currentStreak) // nazik sifirlama — yeni seri 1'den basliyor
        assertEquals(2, state.bestStreak) // en iyi seri korunuyor, azalmiyor
    }

    @Test
    fun `one day gap with unused freeze auto-consumes and preserves streak`() {
        var state = initial
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(2, state.currentStreak)

        // epochDay 102 hic sayilmadi (kacirildi) -> 103'te advance cagriliyor, gap=2.
        state = MissionStreakPrefs.advancePure(state, epochDay = 103L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)

        assertEquals(3, state.currentStreak) // seri korunup devam etti
        assertEquals(102L, state.lastFrozenEpochDay) // aradaki gun donduruldu olarak isaretlendi
        assertTrue(state.freezeUsedThisWeek)
    }

    @Test
    fun `one day gap without available freeze breaks the streak gently`() {
        var state = initial
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        // Hakki once baska bir boslukta harca.
        state = MissionStreakPrefs.advancePure(state, epochDay = 103L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        assertTrue(state.freezeUsedThisWeek)
        val streakBeforeSecondGap = state.currentStreak

        // Ayni hafta icinde ikinci bir 1-gunluk bosluk -> hak kalmadi, seri kirilir (ceza yok, sifirdan).
        state = MissionStreakPrefs.advancePure(state, epochDay = 105L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)

        assertEquals(1, state.currentStreak) // yeni seri bugun basliyor
        assertTrue(streakBeforeSecondGap > 1) // onceki serinin gercekten kirildigini dogrula
    }

    @Test
    fun `two or more day gap breaks the streak with no punishment language state`() {
        var state = initial
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)

        // 3 gun sonra (gap=3) devam — dondurma sadece 1 gunluk boslugu affeder.
        state = MissionStreakPrefs.advancePure(state, epochDay = 104L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)

        assertEquals(1, state.currentStreak)
    }

    @Test
    fun `golden streak counts only 3-of-3 consecutive days separately from currentStreak`() {
        var state = initial
        // Gun 1: 1/3 tamamlandi -> currentStreak ilerler, golden ilerlemez.
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(1, state.currentStreak)
        assertEquals(0, state.goldenStreak)

        // Gun 2: 3/3 tamamlandi -> golden 1'den baslar.
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 3, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(2, state.currentStreak)
        assertEquals(1, state.goldenStreak)

        // Gun 3: yine 3/3 -> golden 2'ye cikar.
        state = MissionStreakPrefs.advancePure(state, epochDay = 102L, completedCount = 3, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(3, state.currentStreak)
        assertEquals(2, state.goldenStreak)

        // Gun 4: 2/3 -> golden sifirlanir, currentStreak devam eder.
        state = MissionStreakPrefs.advancePure(state, epochDay = 103L, completedCount = 2, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(4, state.currentStreak)
        assertEquals(0, state.goldenStreak)
    }

    @Test
    fun `same epoch day called twice is idempotent via read-level guard`() {
        // advancePure kendisi idempotency guard'i icermez (bu MissionStreakPrefs.advance'te,
        // SharedPreferences seviyesinde yapilir) — burada saf fonksiyonun AYNI girdiyle AYNI
        // ciktiyi urettigini (deterministik, yan etkisiz) dogruluyoruz.
        val first = MissionStreakPrefs.advancePure(initial, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        val second = MissionStreakPrefs.advancePure(initial, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        assertEquals(first, second)
    }

    @Test
    fun `freeze right refreshes when the week changes`() {
        var state = initial
        state = MissionStreakPrefs.advancePure(state, epochDay = 100L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        state = MissionStreakPrefs.advancePure(state, epochDay = 101L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        // Hakki bu hafta harca.
        state = MissionStreakPrefs.advancePure(state, epochDay = 103L, completedCount = 1, totalCount = 3, weekStartEpochDay = 98L)
        assertTrue(state.freezeUsedThisWeek)

        // Yeni hafta baslar (weekStartEpochDay degisir) -> hak tazelenmis olmali, ikinci bir
        // 1-gunluk bosluk yine affedilebilir.
        state = MissionStreakPrefs.advancePure(state, epochDay = 104L, completedCount = 1, totalCount = 3, weekStartEpochDay = 105L)
        val streakAfterNewWeekStart = state.currentStreak
        assertFalse(state.freezeUsedThisWeek)

        state = MissionStreakPrefs.advancePure(state, epochDay = 106L, completedCount = 1, totalCount = 3, weekStartEpochDay = 105L)
        assertEquals(streakAfterNewWeekStart + 1, state.currentStreak) // hak tekrar kullanilip seri korundu
        assertTrue(state.freezeUsedThisWeek)
    }
}
