package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.telemetry.TelemetryEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tur 21: AllAppsDrawer'dan çıkarılan saf karar fonksiyonlarının testleri.
 */
class DrawerLogicTest {

    // ── searchResultBucket ───────────────────────────────────────────────────

    @Test
    fun `result bucket boundaries are exact`() {
        assertEquals(TelemetryEvent.ResultBucket.ZERO, searchResultBucket(0))
        assertEquals(TelemetryEvent.ResultBucket.ONE_TO_FIVE, searchResultBucket(1))
        assertEquals(TelemetryEvent.ResultBucket.ONE_TO_FIVE, searchResultBucket(5))
        assertEquals(TelemetryEvent.ResultBucket.SIX_TO_TWENTY, searchResultBucket(6))
        assertEquals(TelemetryEvent.ResultBucket.SIX_TO_TWENTY, searchResultBucket(20))
        assertEquals(TelemetryEvent.ResultBucket.TWENTY_ONE_PLUS, searchResultBucket(21))
        assertEquals(TelemetryEvent.ResultBucket.TWENTY_ONE_PLUS, searchResultBucket(500))
    }

    // ── searchSourceMix ─────────────────────────────────────────────────────

    @Test
    fun `source mix covers all combinations`() {
        // Yalnız uygulama isabeti
        assertEquals(
            TelemetryEvent.SearchSourceMix.APPS_ONLY,
            searchSourceMix(appHits = 3, categoryHits = 0, nonAppHits = 0),
        )
        // Uygulama + kategori/ayar/kişi/dosya karışık
        assertEquals(
            TelemetryEvent.SearchSourceMix.MIXED,
            searchSourceMix(appHits = 2, categoryHits = 1, nonAppHits = 4),
        )
        // Uygulama yok ama kategori var → MIXED
        assertEquals(
            TelemetryEvent.SearchSourceMix.MIXED,
            searchSourceMix(appHits = 0, categoryHits = 1, nonAppHits = 1),
        )
        // Yalnız uygulama dışı (dosya vb.) — kategori isabeti yok
        assertEquals(
            TelemetryEvent.SearchSourceMix.FILES_ONLY,
            searchSourceMix(appHits = 0, categoryHits = 0, nonAppHits = 2),
        )
        // Hiçbir şey yok
        assertEquals(
            TelemetryEvent.SearchSourceMix.OTHER,
            searchSourceMix(appHits = 0, categoryHits = 0, nonAppHits = 0),
        )
    }

    // ── shouldShowWebFallback ───────────────────────────────────────────────

    @Test
    fun `web fallback requires enabled toggle and long enough query and no groups`() {
        assertTrue(shouldShowWebFallback(enabled = true, trimmedQuery = "ankara", hasSearchGroups = false))
        // Kapalı tercih
        assertFalse(shouldShowWebFallback(enabled = false, trimmedQuery = "ankara", hasSearchGroups = false))
        // Kısa sorgu (kırpılmış < 2 karakter)
        assertFalse(shouldShowWebFallback(enabled = true, trimmedQuery = "a", hasSearchGroups = false))
        assertFalse(shouldShowWebFallback(enabled = true, trimmedQuery = "", hasSearchGroups = false))
        // Zaten sonuç grubu var
        assertFalse(shouldShowWebFallback(enabled = true, trimmedQuery = "ankara", hasSearchGroups = true))
    }
}
