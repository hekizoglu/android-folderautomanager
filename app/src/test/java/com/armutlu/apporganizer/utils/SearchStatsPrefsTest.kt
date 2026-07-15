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

    @Test
    fun `arama oranlari elle hesaplanan degerlerle aynidir`() {
        val summary = summary(
            totalSearches = 24,
            zeroResultCount = 1,
            avgLatencyMs = 25,
            avgQueryLength = 5.4,
            totalClicks = 10,
            firstResultClicks = 6,
        )

        assertEquals(
            "total=24, zero=1, zeroRate=4.2%, avgLatencyMs=25",
            SearchDiagnosticsFormatter.counterLine(summary),
        )
        assertEquals(
            "totalClicks=10, clickThroughRate=41.7%, firstResultClicks=6, firstResultRate=60.0%",
            SearchDiagnosticsFormatter.interactionLine(summary),
        )
        assertEquals("5.4 karakter", SearchDiagnosticsFormatter.avgQueryLengthLine(summary))
    }

    @Test
    fun `sifir aramada oranlar sifir olur`() {
        val summary = summary()

        assertEquals(
            "total=0, zero=0, zeroRate=0.0%, avgLatencyMs=0",
            SearchDiagnosticsFormatter.counterLine(summary),
        )
        assertEquals(
            "totalClicks=0, clickThroughRate=0.0%, firstResultClicks=0, firstResultRate=0.0%",
            SearchDiagnosticsFormatter.interactionLine(summary),
        )
    }

    @Test
    fun `kaynak ve aksiyon map siralamasi deterministiktir`() {
        val summary = summary(
            clickCountsByType = mapOf("file" to 1, "app" to 6, "contact" to 2),
            actionCounts = mapOf("WHATSAPP" to 1, "CALL" to 1, "OPEN_APP" to 6),
        )

        assertEquals("app=6, contact=2, file=1", SearchDiagnosticsFormatter.sourceLine(summary))
        assertEquals("CALL=1, OPEN_APP=6, WHATSAPP=1", SearchDiagnosticsFormatter.actionLine(summary))
    }

    @Test
    fun `bos kaynak ve aksiyon mapleri tire dondurur`() {
        val summary = summary()

        assertEquals("-", SearchDiagnosticsFormatter.sourceLine(summary))
        assertEquals("-", SearchDiagnosticsFormatter.actionLine(summary))
    }

    private fun summary(
        totalSearches: Int = 0,
        zeroResultCount: Int = 0,
        avgLatencyMs: Long = 0,
        avgQueryLength: Double = 0.0,
        clickCountsByType: Map<String, Int> = emptyMap(),
        totalClicks: Int = 0,
        firstResultClicks: Int = 0,
        actionCounts: Map<String, Int> = emptyMap(),
    ) = SearchStatsPrefs.Summary(
        totalSearches = totalSearches,
        zeroResultCount = zeroResultCount,
        avgLatencyMs = avgLatencyMs,
        avgQueryLength = avgQueryLength,
        clickCountsByType = clickCountsByType,
        totalClicks = totalClicks,
        firstResultClicks = firstResultClicks,
        actionCounts = actionCounts,
    )
}
