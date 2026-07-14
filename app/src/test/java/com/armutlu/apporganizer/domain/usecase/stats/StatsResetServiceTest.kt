package com.armutlu.apporganizer.domain.usecase.stats

import android.content.Context
import android.content.SharedPreferences
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * P0.4: StatsResetService kapsam seçim mantığı testleri.
 *
 * DAO çağrıları AppRepository mock'u üzerinden doğrulanır (repository zaten AppDao/NotificationEventDao'yu
 * sarmalıyor — bkz. AppRepositoryTest.kt). SharedPreferences tabanlı kapsamlar (WRAPPED_SNAPSHOTS,
 * MISSION_PROGRESS) relaxed mockk Context ile doğrulanır; gerçek SharedPreferences davranışı
 * Robolectric gerektirir ve bu projede kullanılmıyor (bkz. SearchStatsPrefsTest.kt).
 */
class StatsResetServiceTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var mockRepository: AppRepository

    @Before
    fun setup() {
        mockEditor = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.clear() } returns mockEditor

        mockContext = mockk(relaxed = true)
        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs

        mockRepository = mockk(relaxed = true)
        coEvery { mockRepository.getAllApps() } returns listOf(
            AppInfo(packageName = "com.example.a", appName = "A"),
            AppInfo(packageName = "com.example.b", appName = "B")
        )
    }

    @Test
    fun `sadece USAGE_COUNTERS secilirse sadece resetAllUsageCounters cagrilir`() = runTest {
        val results = StatsResetService.reset(
            mockContext, mockRepository, setOf(StatsResetService.Scope.USAGE_COUNTERS)
        )

        coVerify(exactly = 1) { mockRepository.resetAllUsageCounters() }
        coVerify(exactly = 0) { mockRepository.resetAllLastUsedTimestamps() }
        coVerify(exactly = 0) { mockRepository.clearAllNotificationEvents() }
        assertEquals(1, results.size)
        assertTrue(results.single().success)
        assertEquals(StatsResetService.Scope.USAGE_COUNTERS, results.single().scope)
    }

    @Test
    fun `sadece LAST_USED_TIMESTAMPS secilirse sadece resetAllLastUsedTimestamps cagrilir`() = runTest {
        StatsResetService.reset(
            mockContext, mockRepository, setOf(StatsResetService.Scope.LAST_USED_TIMESTAMPS)
        )

        coVerify(exactly = 1) { mockRepository.resetAllLastUsedTimestamps() }
        coVerify(exactly = 0) { mockRepository.resetAllUsageCounters() }
    }

    @Test
    fun `NOTIFICATION_HISTORY secilirse events, texts ve counts sifirlanir`() = runTest {
        StatsResetService.reset(
            mockContext, mockRepository, setOf(StatsResetService.Scope.NOTIFICATION_HISTORY)
        )

        coVerify(exactly = 1) { mockRepository.clearAllNotificationEvents() }
        coVerify(exactly = 1) { mockRepository.clearAllNotificationTexts() }
        coVerify(exactly = 1) {
            mockRepository.updateNotificationCounts(
                mapOf("com.example.a" to 0, "com.example.b" to 0)
            )
        }
    }

    @Test
    fun `WRAPPED_SNAPSHOTS secilirse wrapped_prefs temizlenir`() = runTest {
        StatsResetService.reset(
            mockContext, mockRepository, setOf(StatsResetService.Scope.WRAPPED_SNAPSHOTS)
        )

        coVerify(exactly = 0) { mockRepository.resetAllUsageCounters() }
        io.mockk.verify { mockContext.getSharedPreferences("wrapped_prefs", Context.MODE_PRIVATE) }
        io.mockk.verify { mockEditor.clear() }
    }

    @Test
    fun `MISSION_PROGRESS secilirse mission_prefs temizlenir`() = runTest {
        StatsResetService.reset(
            mockContext, mockRepository, setOf(StatsResetService.Scope.MISSION_PROGRESS)
        )

        io.mockk.verify { mockContext.getSharedPreferences("mission_prefs", Context.MODE_PRIVATE) }
        io.mockk.verify { mockEditor.clear() }
    }

    @Test
    fun `birden fazla kapsam secilince hepsi calisir ve sonuc listesi boyutu eslesir`() = runTest {
        val scopes = setOf(
            StatsResetService.Scope.USAGE_COUNTERS,
            StatsResetService.Scope.LAST_USED_TIMESTAMPS,
            StatsResetService.Scope.MISSION_PROGRESS
        )

        val results = StatsResetService.reset(mockContext, mockRepository, scopes)

        coVerify(exactly = 1) { mockRepository.resetAllUsageCounters() }
        coVerify(exactly = 1) { mockRepository.resetAllLastUsedTimestamps() }
        assertEquals(3, results.size)
        assertTrue(results.all { it.success })
    }

    @Test
    fun `bos kapsam seti secilirse hicbir DAO cagrisi yapilmaz`() = runTest {
        val results = StatsResetService.reset(mockContext, mockRepository, emptySet())

        assertTrue(results.isEmpty())
        coVerify(exactly = 0) { mockRepository.resetAllUsageCounters() }
        coVerify(exactly = 0) { mockRepository.resetAllLastUsedTimestamps() }
        coVerify(exactly = 0) { mockRepository.clearAllNotificationEvents() }
    }

    @Test
    fun `bir kapsam basarisiz olursa digerleri yine de denenir`() = runTest {
        coEvery { mockRepository.resetAllUsageCounters() } throws RuntimeException("db hatasi")

        val results = StatsResetService.reset(
            mockContext,
            mockRepository,
            setOf(StatsResetService.Scope.USAGE_COUNTERS, StatsResetService.Scope.LAST_USED_TIMESTAMPS)
        )

        coVerify(exactly = 1) { mockRepository.resetAllLastUsedTimestamps() }
        val usageResult = results.single { it.scope == StatsResetService.Scope.USAGE_COUNTERS }
        val lastUsedResult = results.single { it.scope == StatsResetService.Scope.LAST_USED_TIMESTAMPS }
        assertFalse(usageResult.success)
        assertTrue(lastUsedResult.success)
    }
}
