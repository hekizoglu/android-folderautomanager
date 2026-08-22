package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderDragTest {

    // ── moveItem ────────────────────────────────────────────────────────────

    @Test
    fun `move item forward shifts middle elements`() {
        val result = moveItem(listOf("a", "b", "c", "d"), from = 0, to = 2)
        assertEquals(listOf("b", "c", "a", "d"), result)
    }

    @Test
    fun `move item backward shifts middle elements`() {
        val result = moveItem(listOf("a", "b", "c", "d"), from = 3, to = 1)
        assertEquals(listOf("a", "d", "b", "c"), result)
    }

    @Test
    fun `same index or invalid bounds return the original list`() {
        val list = listOf("a", "b", "c")
        assertEquals(list, moveItem(list, 1, 1))
        assertEquals(list, moveItem(list, -1, 2))
        assertEquals(list, moveItem(list, 0, 3))
        assertEquals(listOf(1), moveItem(listOf(1), 0, 0))
    }

    // ── hitTestFolderIndex ──────────────────────────────────────────────────
    // Hücreler: 2 sütun, 100x100px hücre, 10px aralık.
    // Satır 0: indeks 0 (x 0-100), indeks 1 (x 110-210)
    // Satır 1: indeks 2 (x 0-100), indeks 3 (x 110-210)

    private fun hit(from: Int, offset: Offset, tileCount: Int = 4) = hitTestFolderIndex(
        fromIndex = from,
        offset = offset,
        tileCount = tileCount,
        columns = 2,
        cellWidthPx = 100f,
        cellHeightPx = 100f,
        spacingPx = 10f,
    )

    @Test
    fun `no movement keeps the same index`() {
        assertEquals(0, hit(0, Offset.Zero))
        assertEquals(3, hit(3, Offset.Zero))
    }

    @Test
    fun `drag right onto neighbour selects it`() {
        // indeks 0'ın merkezi (50,50); +110px → merkez (160,50) → sütun 1, satır 0 → indeks 1
        assertEquals(1, hit(0, Offset(110f, 0f)))
    }

    @Test
    fun `drag down onto next row selects it`() {
        // indeks 0 merkezi (50,50); +110px y → (50,160) → satır 1, sütun 0 → indeks 2
        assertEquals(2, hit(0, Offset(0f, 110f)))
    }

    @Test
    fun `drag far outside clamps to grid bounds`() {
        assertEquals(3, hit(0, Offset(5000f, 5000f)))
        assertEquals(0, hit(3, Offset(-5000f, -5000f)))
    }

    @Test
    fun `last row partial grid clamps to last tile`() {
        // 3 kart, 2 sütun → satır 1 yalnız indeks 2'yi taşır; sağ-aşağı taşma indeks 2'de kalır
        assertEquals(2, hit(0, Offset(5000f, 5000f), tileCount = 3))
    }

    @Test
    fun `empty grid has no target`() {
        assertNull(
            hitTestFolderIndex(
                fromIndex = 0,
                offset = Offset.Zero,
                tileCount = 0,
                columns = 2,
                cellWidthPx = 100f,
                cellHeightPx = 100f,
                spacingPx = 10f,
            ),
        )
    }
}
