package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Faz S3 — WidgetFreeGrid'in dayandığı saf yardımcı fonksiyonlar (itemId üretimi, widget
 * span hesabı). Compose Layout/gesture kısmı bu projede unit test edilmiyor (bkz.
 * FolderFreeGridTest yorumu) — sadece saf mantık kapsanıyor.
 */
class WidgetFreeGridTest {

    // --- widgetGridItemId ---

    @Test
    fun `itemId widget on eki ve id yi birlestirir`() {
        val id = widgetGridItemId(42)
        assertEquals("widget_42", id)
    }

    @Test
    fun `farkli widget id farkli itemId uretir`() {
        val idA = widgetGridItemId(1)
        val idB = widgetGridItemId(2)
        assert(idA != idB)
    }

    // --- computeWidgetSpan ---

    @Test
    fun `hucre boyutuna tam sigan widget 1x1 span alir`() {
        val span = computeWidgetSpan(minWidthDp = 56, minHeightDp = 56, cellSizeDp = 56)
        assertEquals(1, span.spanX)
        assertEquals(1, span.spanY)
    }

    @Test
    fun `buyuk widget birden fazla hucre kaplar`() {
        // 250dp genislik / 56dp hucre -> yukari yuvarlanir (5 hucre)
        val span = computeWidgetSpan(minWidthDp = 250, minHeightDp = 110, cellSizeDp = 56)
        assertEquals(5, span.spanX)
        assertEquals(2, span.spanY)
    }

    @Test
    fun `sifir veya negatif boyut en az 1x1 span garanti eder`() {
        val span = computeWidgetSpan(minWidthDp = 0, minHeightDp = 0, cellSizeDp = 56)
        assertEquals(1, span.spanX)
        assertEquals(1, span.spanY)
    }

    @Test
    fun `hucre boyutu sifirsa guvenli varsayilan 1x1 doner`() {
        val span = computeWidgetSpan(minWidthDp = 200, minHeightDp = 200, cellSizeDp = 0)
        assertEquals(1, span.spanX)
        assertEquals(1, span.spanY)
    }

    // --- WIDGET_GRID_SCREEN_INDEX ---

    @Test
    fun `widget screen index klasor hash carpismalarindan uzak sabit bir deger`() {
        // Klasör screenIndex'leri categoryId.hashCode() ile üretilir (İnt aralığında herhangi bir
        // değer olabilir) — WIDGET_GRID_SCREEN_INDEX belirgin şekilde ayrı bir sabit olmalı.
        assertEquals(-1_000_000, WIDGET_GRID_SCREEN_INDEX)
    }
}
