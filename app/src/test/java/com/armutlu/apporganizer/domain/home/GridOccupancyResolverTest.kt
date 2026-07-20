package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Faz S — S1 saf domain matematiği testleri (Context/Room/Compose bağımlılığı yok). */
class GridOccupancyResolverTest {

    private val bounds = GridBounds(columns = 4, rows = 4)

    @Test
    fun `bos gridde ilk hucre 0,0 bulunur`() {
        val result = findFirstFreeCell(occupied = emptyList(), bounds = bounds)
        assertEquals(GridPosition(0, 0, 1, 1), result)
    }

    @Test
    fun `dolu hucreler varken bir sonraki bos hucre dogru bulunur`() {
        val occupied = listOf(GridPosition(0, 0, 1, 1))
        val result = findFirstFreeCell(occupied = occupied, bounds = bounds)
        assertEquals(GridPosition(1, 0, 1, 1), result)
    }

    @Test
    fun `ilk satir tamamen doluysa ikinci satirdan bos hucre bulunur`() {
        val occupied = (0 until bounds.columns).map { x -> GridPosition(x, 0, 1, 1) }
        val result = findFirstFreeCell(occupied = occupied, bounds = bounds)
        assertEquals(GridPosition(0, 1, 1, 1), result)
    }

    @Test
    fun `span 2x2 icin yeterli bitisik bos alan gerekir`() {
        // Ilk satirin tamami (0,0)-(3,0) dolu — 2x2 span ilk satirda hicbir x'te baslayamaz
        // (spanY=2 her aday icin 2. satirin da bos olmasini gerektirir), siradaki uygun konum
        // ikinci satirin basi (0,1) olmali.
        val occupied = (0 until bounds.columns).map { x -> GridPosition(x, 0, 1, 1) }
        val result = findFirstFreeCell(occupied = occupied, bounds = bounds, spanX = 2, spanY = 2)
        assertEquals(GridPosition(0, 1, 2, 2), result)
    }

    @Test
    fun `grid tamamen doluysa null doner`() {
        val occupied = mutableListOf<GridPosition>()
        for (y in 0 until bounds.rows) {
            for (x in 0 until bounds.columns) {
                occupied.add(GridPosition(x, y, 1, 1))
            }
        }
        val result = findFirstFreeCell(occupied = occupied, bounds = bounds)
        assertNull(result)
    }

    @Test
    fun `span grid siginmiyorsa null doner`() {
        val result = findFirstFreeCell(occupied = emptyList(), bounds = GridBounds(columns = 1, rows = 1), spanX = 2, spanY = 2)
        assertNull(result)
    }

    @Test
    fun `cakisan iki dikdortgen hasOverlap true doner`() {
        val a = GridPosition(0, 0, 2, 2)
        val b = GridPosition(1, 1, 2, 2)
        assertTrue(hasOverlap(a, b))
    }

    @Test
    fun `cakismayan iki dikdortgen hasOverlap false doner`() {
        val a = GridPosition(0, 0, 1, 1)
        val b = GridPosition(1, 0, 1, 1)
        assertFalse(hasOverlap(a, b))
    }

    @Test
    fun `komsu ama cakismayan dikdortgenler false doner`() {
        val a = GridPosition(0, 0, 2, 2)
        val b = GridPosition(2, 0, 2, 2)
        assertFalse(hasOverlap(a, b))
    }

    @Test
    fun `sinir disi yerlesim isValidPlacement false doner`() {
        val candidate = GridPosition(cellX = 3, cellY = 3, spanX = 2, spanY = 2)
        assertFalse(isValidPlacement(candidate, emptyList(), bounds))
    }

    @Test
    fun `negatif koordinatli yerlesim isValidPlacement false doner`() {
        val candidate = GridPosition(cellX = -1, cellY = 0, spanX = 1, spanY = 1)
        assertFalse(isValidPlacement(candidate, emptyList(), bounds))
    }

    @Test
    fun `sinirlar icinde ve cakismayan yerlesim isValidPlacement true doner`() {
        val occupied = listOf(GridPosition(0, 0, 1, 1))
        val candidate = GridPosition(1, 0, 1, 1)
        assertTrue(isValidPlacement(candidate, occupied, bounds))
    }

    @Test
    fun `mevcut ogeyle cakisan yerlesim isValidPlacement false doner`() {
        val occupied = listOf(GridPosition(0, 0, 2, 2))
        val candidate = GridPosition(1, 1, 1, 1)
        assertFalse(isValidPlacement(candidate, occupied, bounds))
    }
}
