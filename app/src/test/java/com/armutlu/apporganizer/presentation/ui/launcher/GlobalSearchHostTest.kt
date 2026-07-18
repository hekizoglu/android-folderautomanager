package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.FileIndexState
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Döngü P08 — `GlobalSearchHost` state türetiminin ([computeGlobalSearchUiState]) birim testleri
 * (roadmap satır 805-866 "Testler" bölümü). Compose bağımlılığı olmayan saf mantık — host'un
 * kendisi (Composable) instrumented/manuel testlerle doğrulanır.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P08 (satır 805-866).
 */
class GlobalSearchHostTest {

    private fun doc(id: String, source: SourceType = SourceType.APP): SearchDocument =
        SearchDocument(
            sourceId = id,
            sourceType = source.key,
            title = id,
            subtitle = "",
            iconKey = id,
            sourceGroup = source.key,
            lastModified = 0L,
        )

    // ── query temizlenince overlay kapanır ──────────────────────────────────────────────────

    @Test
    fun `bos query overlay kapali ve pasif`() {
        val state = computeGlobalSearchUiState(
            query = "",
            fullscreenOpen = false,
            fullscreenEnabled = true,
            resultGroups = emptyMap(),
            filesIndexState = FileIndexState.Disabled,
        )
        assertFalse(state.active)
        assertFalse(state.overlayVisible)
        assertFalse(state.fullscreenVisible)
    }

    @Test
    fun `yazi girilince aktif ve overlay acik olur`() {
        val state = computeGlobalSearchUiState(
            query = "a",
            fullscreenOpen = false,
            fullscreenEnabled = true,
            resultGroups = emptyMap(),
            filesIndexState = FileIndexState.Disabled,
        )
        assertTrue(state.active)
        assertTrue(state.overlayVisible)
    }

    // ── fullscreen açıkken inline overlay görünmez (mevcut HomeAppSearchBar davranışı) ──────

    @Test
    fun `fullscreen acikken inline overlay gizlenir`() {
        val state = computeGlobalSearchUiState(
            query = "abc",
            fullscreenOpen = true,
            fullscreenEnabled = true,
            resultGroups = emptyMap(),
            filesIndexState = FileIndexState.Disabled,
        )
        assertTrue(state.fullscreenVisible)
        assertFalse("Fullscreen açıkken inline sonuç overlay'i render edilmemeli", state.overlayVisible)
    }

    @Test
    fun `fullscreen ayari kapaliysa fullscreenOpen true olsa bile gorunmez`() {
        val state = computeGlobalSearchUiState(
            query = "abc",
            fullscreenOpen = true,
            fullscreenEnabled = false,
            resultGroups = emptyMap(),
            filesIndexState = FileIndexState.Disabled,
        )
        assertFalse(state.fullscreenVisible)
        // fullscreen gerçekte görünmüyorsa inline overlay tekrar devreye girer
        assertTrue(state.overlayVisible)
    }

    // ── sonuç grupları ve dosya indeks durumu birebir taşınır ───────────────────────────────

    @Test
    fun `sonuc gruplari ve dosya indeks durumu degistirilmeden tasinir`() {
        val groups = mapOf(SourceType.APP to listOf(doc("pkg.a"), doc("pkg.b")))
        val state = computeGlobalSearchUiState(
            query = "ab",
            fullscreenOpen = false,
            fullscreenEnabled = true,
            resultGroups = groups,
            filesIndexState = FileIndexState.Indexing(0.5f),
        )
        assertEquals(groups, state.resultGroups)
        assertEquals(FileIndexState.Indexing(0.5f), state.filesIndexState)
    }

    @Test
    fun `query trim edilmeden state a birebir yansir - LauncherViewModel debounce zaten trim eder`() {
        // computeGlobalSearchUiState kendi başına trim yapmaz — searchQuery zaten
        // LauncherViewModel.searchResults debounce zincirinde trim edilmiş halde gelir; burada
        // sadece "aktiflik" kararı query'nin boş olup olmadığına bakar (roadmap madde: "İki
        // harften kısa query sonuç çağrısı yapmaz" davranışı searchResults map'inin boş
        // dönmesiyle zaten sağlanır — bu fonksiyon o kararı tekrar üretmez, sadece taşır).
        val state = computeGlobalSearchUiState(
            query = "  ",
            fullscreenOpen = false,
            fullscreenEnabled = true,
            resultGroups = emptyMap(),
            filesIndexState = FileIndexState.Disabled,
        )
        assertTrue("Boşluk karakteri de 'yazılmış' sayılır — trim kararı LauncherViewModel'de", state.active)
    }

    // ── Empty companion default'u tutarlı ────────────────────────────────────────────────────

    @Test
    fun `Empty durumu tamamen pasiftir`() {
        val empty = GlobalSearchUiState.Empty
        assertEquals("", empty.query)
        assertFalse(empty.active)
        assertFalse(empty.overlayVisible)
        assertFalse(empty.fullscreenVisible)
        assertTrue(empty.resultGroups.isEmpty())
        assertEquals(FileIndexState.Disabled, empty.filesIndexState)
    }
}
