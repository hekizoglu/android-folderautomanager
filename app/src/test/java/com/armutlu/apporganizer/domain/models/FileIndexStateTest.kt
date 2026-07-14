package com.armutlu.apporganizer.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0.3: computeFileIndexState() saf mantik testleri.
 * Android bağımlılığı yok — AppPrefs/PackageManager'a dokunmadan
 * sealed durum eşlemesi doğrulanır.
 */
class FileIndexStateTest {

    @Test
    fun `source disabled returns Disabled regardless of other flags`() {
        val state = computeFileIndexState(
            sourceEnabled = false,
            hasPermission = true,
            isIndexing = true,
            lastFailureReason = "boom",
            itemCount = 10,
            lastIndexedAt = 12345L,
        )
        assertEquals(FileIndexState.Disabled, state)
    }

    @Test
    fun `source enabled without permission returns PermissionRequired`() {
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = false,
            isIndexing = false,
            lastFailureReason = null,
            itemCount = 0,
            lastIndexedAt = 0L,
        )
        assertEquals(FileIndexState.PermissionRequired, state)
    }

    @Test
    fun `permission missing takes priority over stale ready data`() {
        // Kullanıcı izni sonradan geri aldıysa (Settings > App info) eski itemCount/lastIndexedAt
        // hala AppPrefs'te olabilir — ama PermissionRequired öncelikli olmalı (sahte "hazır" gösterilmemeli)
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = false,
            isIndexing = false,
            lastFailureReason = null,
            itemCount = 42,
            lastIndexedAt = 99999L,
        )
        assertEquals(FileIndexState.PermissionRequired, state)
    }

    @Test
    fun `indexing in progress returns Indexing`() {
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = true,
            isIndexing = true,
            lastFailureReason = null,
            itemCount = 0,
            lastIndexedAt = 0L,
        )
        assertTrue(state is FileIndexState.Indexing)
    }

    @Test
    fun `never indexed and never failed defaults to Indexing (bootstrap not yet run)`() {
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = true,
            isIndexing = false,
            lastFailureReason = null,
            itemCount = 0,
            lastIndexedAt = 0L,
        )
        assertTrue(state is FileIndexState.Indexing)
    }

    @Test
    fun `successful index returns Ready with itemCount and lastIndexedAt`() {
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = true,
            isIndexing = false,
            lastFailureReason = null,
            itemCount = 128,
            lastIndexedAt = 1_700_000_000_000L,
        )
        assertEquals(FileIndexState.Ready(128, 1_700_000_000_000L), state)
    }

    @Test
    fun `failure with no prior successful index returns Failed`() {
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = true,
            isIndexing = false,
            lastFailureReason = "MediaStore query timeout",
            itemCount = 0,
            lastIndexedAt = 0L,
        )
        assertEquals(FileIndexState.Failed("MediaStore query timeout"), state)
    }

    @Test
    fun `failure after a prior successful index still returns Ready (stale data preferred over error)`() {
        // Reindex denemesi başarısız oldu ama önceki başarılı indeks hala geçerli —
        // kullanıcıya "hata" yerine son bilinen iyi durumu göstermek daha faydalı.
        val state = computeFileIndexState(
            sourceEnabled = true,
            hasPermission = true,
            isIndexing = false,
            lastFailureReason = "MediaStore query timeout",
            itemCount = 50,
            lastIndexedAt = 1_700_000_000_000L,
        )
        assertEquals(FileIndexState.Ready(50, 1_700_000_000_000L), state)
    }
}
