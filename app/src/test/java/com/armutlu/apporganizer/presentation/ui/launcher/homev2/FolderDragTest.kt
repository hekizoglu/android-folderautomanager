package com.armutlu.apporganizer.presentation.ui.launcher.homev2

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

    // ── hitTestRowIndex (yatay satır düzeni: tek sütun) ─────────────────────
    // Satırlar: tek sütun, 100px yükseklik, 10px aralık.
    // Satır 0 merkez y=50, satır 1 merkez y=160, satır 2 merkez y=270.

    private fun hit(from: Int, offsetY: Float, rowCount: Int = 4) = hitTestRowIndex(
        fromIndex = from,
        offsetY = offsetY,
        rowCount = rowCount,
        rowHeightPx = 100f,
        spacingPx = 10f,
    )

    @Test
    fun `no movement keeps the same row`() {
        assertEquals(0, hit(0, 0f))
        assertEquals(3, hit(3, 0f))
    }

    @Test
    fun `drag down onto next row selects it`() {
        // satır 0 merkez y=50; +110px → y=160 → satır 1
        assertEquals(1, hit(0, 110f))
    }

    @Test
    fun `drag up onto previous row selects it`() {
        // satır 2 merkez y=270; -110px → y=160 → satır 1
        assertEquals(1, hit(2, -110f))
    }

    @Test
    fun `drag far outside clamps to list bounds`() {
        assertEquals(3, hit(0, 5000f))
        assertEquals(0, hit(3, -5000f))
    }

    @Test
    fun `partial last row clamps to last row`() {
        // 3 satır; aşağı taşma son satırda kalır
        assertEquals(2, hit(0, 5000f, rowCount = 3))
    }

    @Test
    fun `empty list has no target`() {
        assertNull(
            hitTestRowIndex(
                fromIndex = 0,
                offsetY = 0f,
                rowCount = 0,
                rowHeightPx = 100f,
                spacingPx = 10f,
            ),
        )
    }
}
