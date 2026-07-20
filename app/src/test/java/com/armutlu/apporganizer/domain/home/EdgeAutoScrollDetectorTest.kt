package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** Faz S4 — [detectEdgeScroll] saf fonksiyon testleri. */
class EdgeAutoScrollDetectorTest {

    @Test
    fun `left edge zone returns PREVIOUS`() {
        // 1000px genişlik, %12 sol bant = 0..120px arası
        assertEquals(EdgeScrollDirection.PREVIOUS, detectEdgeScroll(50f, 1000f))
    }

    @Test
    fun `right edge zone returns NEXT`() {
        // %12 sağ bant = 880..1000px arası
        assertEquals(EdgeScrollDirection.NEXT, detectEdgeScroll(950f, 1000f))
    }

    @Test
    fun `middle of screen returns NONE`() {
        assertEquals(EdgeScrollDirection.NONE, detectEdgeScroll(500f, 1000f))
    }

    @Test
    fun `exactly at left threshold (12 percent) is inclusive PREVIOUS`() {
        assertEquals(EdgeScrollDirection.PREVIOUS, detectEdgeScroll(120f, 1000f))
    }

    @Test
    fun `exactly at right threshold (88 percent) is inclusive NEXT`() {
        assertEquals(EdgeScrollDirection.NEXT, detectEdgeScroll(880f, 1000f))
    }

    @Test
    fun `just inside left threshold returns NONE`() {
        assertEquals(EdgeScrollDirection.NONE, detectEdgeScroll(120.5f, 1000f))
    }

    @Test
    fun `just inside right threshold returns NONE`() {
        assertEquals(EdgeScrollDirection.NONE, detectEdgeScroll(879.5f, 1000f))
    }

    @Test
    fun `custom edgeFraction widens the bands`() {
        // %25 bant genişliği ile 500px orta nokta hâlâ NONE, ama 200px artık PREVIOUS olur
        assertEquals(
            EdgeScrollDirection.PREVIOUS,
            detectEdgeScroll(200f, 1000f, edgeFraction = 0.25f)
        )
        assertEquals(
            EdgeScrollDirection.NONE,
            detectEdgeScroll(500f, 1000f, edgeFraction = 0.25f)
        )
    }

    @Test
    fun `zero screen width returns NONE`() {
        assertEquals(EdgeScrollDirection.NONE, detectEdgeScroll(10f, 0f))
    }

    @Test
    fun `pointer at absolute left edge (zero) returns PREVIOUS`() {
        assertEquals(EdgeScrollDirection.PREVIOUS, detectEdgeScroll(0f, 1000f))
    }

    @Test
    fun `pointer at absolute right edge returns NEXT`() {
        assertEquals(EdgeScrollDirection.NEXT, detectEdgeScroll(1000f, 1000f))
    }
}
