package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockAppDao: AppDao
    private lateinit var mockCategoryDao: CategoryDao
    private lateinit var mockClassifier: AppClassifier
    private lateinit var repository: AppRepository

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockAppDao = mockk(relaxed = true)
        mockCategoryDao = mockk(relaxed = true)
        mockClassifier = mockk(relaxed = true)

        every { mockAppDao.getAllAppsFlow() } returns appsFlow
        every { mockAppDao.getAllAppsSortedByUsage() } returns appsFlow
        every { mockAppDao.getSystemApps() } returns appsFlow
        every { mockAppDao.getUserApps() } returns appsFlow
        every { mockAppDao.getHiddenApps() } returns appsFlow
        every { mockAppDao.getUncategorizedApps() } returns appsFlow

        repository = AppRepository(mockAppDao, mockCategoryDao, mockClassifier)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun app(pkg: String, name: String, cat: String = "social") =
        AppInfo(packageName = pkg, appName = name, categoryId = cat)

    // ── getAllApps ────────────────────────────────────────────────────────────

    @Test
    fun `getAllApps returns dao result`() = runTest {
        coEvery { mockAppDao.getAllApps() } returns listOf(app("com.a", "App A"))

        val result = repository.getAllApps()

        assertEquals(1, result.size)
        assertEquals("App A", result[0].appName)
    }

    @Test
    fun `getAllApps returns empty list on exception`() = runTest {
        coEvery { mockAppDao.getAllApps() } throws RuntimeException("DB error")

        val result = repository.getAllApps()

        assertTrue(result.isEmpty())
    }

    // ── getAppByPackageName ───────────────────────────────────────────────────

    @Test
    fun `getAppByPackageName returns correct app`() = runTest {
        val expected = app("com.test", "Test App")
        coEvery { mockAppDao.getAppByPackageName("com.test") } returns expected

        val result = repository.getAppByPackageName("com.test")

        assertEquals(expected, result)
    }

    @Test
    fun `getAppByPackageName returns null when not found`() = runTest {
        coEvery { mockAppDao.getAppByPackageName(any()) } returns null

        val result = repository.getAppByPackageName("com.nonexistent")

        assertNull(result)
    }

    // ── insertApps ────────────────────────────────────────────────────────────

    @Test
    fun `insertApps classifies each app via classifier`() = runTest {
        val apps = listOf(
            app("com.instagram.android", "Instagram", "other"),
            app("com.pubg.mobile", "PUBG", "other")
        )
        every { mockClassifier.classifyApp(match { it.packageName == "com.instagram.android" }) } returns "social"
        every { mockClassifier.classifyApp(match { it.packageName == "com.pubg.mobile" }) } returns "games"

        repository.insertApps(apps)
        advanceUntilIdle()

        coVerify {
            mockAppDao.insertApps(match { list ->
                list.any { it.packageName == "com.instagram.android" && it.categoryId == "social" } &&
                list.any { it.packageName == "com.pubg.mobile" && it.categoryId == "games" }
            })
        }
    }

    @Test
    fun `insertApps silently handles exception`() = runTest {
        coEvery { mockAppDao.insertApps(any()) } throws RuntimeException("insert failed")
        every { mockClassifier.classifyApp(any()) } returns "social"

        // Should not throw
        repository.insertApps(listOf(app("com.a", "App A")))
        advanceUntilIdle()
    }

    // ── updateAppCategory ─────────────────────────────────────────────────────

    @Test
    fun `updateAppCategory delegates to dao`() = runTest {
        repository.updateAppCategory("com.test.app", "productivity")
        advanceUntilIdle()

        coVerify { mockAppDao.updateAppCategory("com.test.app", "productivity", any()) }
    }

    @Test
    fun `updateAppCategory silently handles exception`() = runTest {
        coEvery { mockAppDao.updateAppCategory(any(), any()) } throws RuntimeException("dao error")

        repository.updateAppCategory("com.test.app", "productivity")
        advanceUntilIdle()
    }

    // ── updateAppsCategory ────────────────────────────────────────────────────

    @Test
    fun `updateAppsCategory delegates to dao with list`() = runTest {
        val packages = listOf("com.a", "com.b", "com.c")

        repository.updateAppsCategory(packages, "games")
        advanceUntilIdle()

        coVerify { mockAppDao.updateAppsCategory(packages, "games", any()) }
    }

    // ── deleteApp ─────────────────────────────────────────────────────────────

    @Test
    fun `deleteApp calls deleteAppByPackageName`() = runTest {
        repository.deleteApp("com.test.app")
        advanceUntilIdle()

        coVerify { mockAppDao.deleteAppByPackageName("com.test.app") }
    }

    // ── clearAllApps ──────────────────────────────────────────────────────────

    @Test
    fun `clearAllApps calls deleteAllApps`() = runTest {
        repository.clearAllApps()
        advanceUntilIdle()

        coVerify { mockAppDao.deleteAllApps() }
    }

    // ── countApps ─────────────────────────────────────────────────────────────

    @Test
    fun `countApps returns dao count`() = runTest {
        coEvery { mockAppDao.countApps() } returns 42

        val result = repository.countApps()

        assertEquals(42, result)
    }

    @Test
    fun `countApps returns 0 on exception`() = runTest {
        coEvery { mockAppDao.countApps() } throws RuntimeException("count error")

        val result = repository.countApps()

        assertEquals(0, result)
    }

    // ── countAppsByCategory ───────────────────────────────────────────────────

    @Test
    fun `countAppsByCategory returns dao count for category`() = runTest {
        coEvery { mockAppDao.countAppsByCategory("social") } returns 7

        val result = repository.countAppsByCategory("social")

        assertEquals(7, result)
    }

    @Test
    fun `countAppsByCategory returns 0 on exception`() = runTest {
        coEvery { mockAppDao.countAppsByCategory(any()) } throws RuntimeException("error")

        val result = repository.countAppsByCategory("social")

        assertEquals(0, result)
    }

    // ── appExists ─────────────────────────────────────────────────────────────

    @Test
    fun `appExists returns true when dao returns true`() = runTest {
        coEvery { mockAppDao.appExists("com.test") } returns true

        assertTrue(repository.appExists("com.test"))
    }

    @Test
    fun `appExists returns false when dao returns false`() = runTest {
        coEvery { mockAppDao.appExists("com.ghost") } returns false

        assertFalse(repository.appExists("com.ghost"))
    }

    // ── syncInstalledApps ─────────────────────────────────────────────────────

    @Test
    fun `syncInstalledApps inserts only new apps`() = runTest {
        coEvery { mockAppDao.getAllPackageNames() } returns listOf("com.existing")
        every { mockClassifier.classifyApp(any()) } returns "social"

        val installed = listOf(
            app("com.existing", "Existing"),
            app("com.new", "New App")
        )

        repository.syncInstalledApps(installed)
        advanceUntilIdle()

        coVerify {
            mockAppDao.insertApps(match { list ->
                list.size == 1 && list[0].packageName == "com.new"
            })
        }
    }

    @Test
    fun `syncInstalledApps removes uninstalled apps`() = runTest {
        coEvery { mockAppDao.getAllPackageNames() } returns listOf("com.installed", "com.uninstalled")
        every { mockClassifier.classifyApp(any()) } returns "social"

        val installed = listOf(app("com.installed", "Still Here"))

        repository.syncInstalledApps(installed)
        advanceUntilIdle()

        coVerify { mockAppDao.deleteAppByPackageName("com.uninstalled") }
    }

    @Test
    fun `syncInstalledApps with all new apps inserts all`() = runTest {
        coEvery { mockAppDao.getAllPackageNames() } returns emptyList()
        every { mockClassifier.classifyApp(any()) } returns "social"

        val apps = listOf(app("com.a", "App A"), app("com.b", "App B"))

        repository.syncInstalledApps(apps)
        advanceUntilIdle()

        coVerify {
            mockAppDao.insertApps(match { it.size == 2 })
        }
    }

    @Test
    fun `syncInstalledApps silently handles exception`() = runTest {
        coEvery { mockAppDao.getAllPackageNames() } throws RuntimeException("dao error")

        repository.syncInstalledApps(listOf(app("com.a", "App A")))
        advanceUntilIdle()
    }

    // ── getAllAppsFlow ────────────────────────────────────────────────────────

    @Test
    fun `getAllAppsFlow emits dao flow values`() = runTest {
        val apps = listOf(app("com.a", "App A"))
        appsFlow.value = apps

        val result = repository.getAllAppsFlow().first()

        assertEquals(apps, result)
    }

    // ── updateAppHidden ───────────────────────────────────────────────────────

    @Test
    fun `updateAppHidden delegates to dao`() = runTest {
        repository.updateAppHidden("com.test", true)
        advanceUntilIdle()

        coVerify { mockAppDao.updateAppHidden("com.test", true) }
    }

    // ── updateNotificationCount ───────────────────────────────────────────────

    @Test
    fun `updateNotificationCount delegates to dao`() = runTest {
        repository.updateNotificationCount("com.telegram", 5)
        advanceUntilIdle()

        coVerify { mockAppDao.updateNotificationCount("com.telegram", 5) }
    }

    @Test
    fun `updateNotificationCounts delegates batch to dao`() = runTest {
        val counts = mapOf("com.telegram" to 5, "com.whatsapp" to 2)

        repository.updateNotificationCounts(counts)
        advanceUntilIdle()

        coVerify { mockAppDao.updateNotificationCounts(counts) }
    }

    @Test
    fun `updateNotificationTexts delegates batch to dao`() = runTest {
        val texts = mapOf("com.telegram" to "Yeni mesaj", "com.mail" to "Inbox")

        repository.updateNotificationTexts(texts)
        advanceUntilIdle()

        coVerify { mockAppDao.updateNotificationTexts(texts) }
    }

    // ── resetAllCategories ────────────────────────────────────────────────────

    @Test
    fun `resetAllCategories calls dao resetAllAppCategories`() = runTest {
        repository.resetAllCategories()
        advanceUntilIdle()

        coVerify { mockAppDao.resetAllAppCategories() }
    }
}
