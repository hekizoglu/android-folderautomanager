package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SearchStatsPrefs.computeEma() pure function testleri.
 * Android Context gerektiren getSummary/logSearch/logClick/logAction Robolectric olmadan
 * test edilemediginden, EMA hesabi ayri bir pure function'a cikarilarak test edilir.
 */
class SearchStatsPrefsTest {

    @Test
    fun `ilk olcum dogrudan yeni degeri doner`() {
        val result = SearchStatsPrefs.computeEma(currentAvg = 0.0, newValue = 100.0)
        assertEquals(100.0, result, 0.0001)
    }

    @Test
    fun `ema alpha 0_2 ile agirlikli ortalama hesaplar`() {
        // alpha=0.2 -> yeni = 0.2*newValue + 0.8*currentAvg
        val result = SearchStatsPrefs.computeEma(currentAvg = 100.0, newValue = 200.0)
        assertEquals(120.0, result, 0.0001)
    }

    @Test
    fun `ardisik olcumler zamanla yeni degere yaklasir`() {
        var avg = 0.0
        repeat(50) { avg = SearchStatsPrefs.computeEma(avg, 500.0) }
        // 50 tekrardan sonra 500'e cok yakinsamis olmali
        assertEquals(500.0, avg, 1.0)
    }

    @Test
    fun `ozel alpha degeri ile hesaplanabilir`() {
        val result = SearchStatsPrefs.computeEma(currentAvg = 10.0, newValue = 20.0, alpha = 0.5)
        assertEquals(15.0, result, 0.0001)
    }

    @Test
    fun `negatif olmayan degerlerde stabil kalir`() {
        val result = SearchStatsPrefs.computeEma(currentAvg = 5.0, newValue = 0.0)
        assertEquals(4.0, result, 0.0001)
    }
}
