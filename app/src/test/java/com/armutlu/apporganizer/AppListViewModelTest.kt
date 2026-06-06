package com.armutlu.apporganizer

import android.app.Application
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.data.remote.FetchResult
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.usecase.AppClassifier
import com.armutlu.apporganizer.presentation.ui.screens.SortOption
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

/**
 * Unit tests for AppListViewModel.
 *
 * Bağımlılıklar mockk ile sahtelenmiştir; AndroidViewModel için
 * Application da mocklanmıştır (Robolectric gerektirmez).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    // ── Test infrastructure ──────────────────────────────────────────────────

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockApplication: Application
    private lateinit var mockRepository: AppRepository
    private lateinit var mockClassifier: AppClassifier
    private lateinit var mockDbService: AppDatabaseService

    /** Flow that the fake repository emits apps through. */
    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    private lateinit var viewModel: AppListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockApplication = mockk(relaxed = true)
        mockRepository   = mockk(relaxed = true)
        mockClassifier   = mockk(relaxed = true)
        mockDbService    = mockk(relaxed = true)

        // Repository returns our controllable flow
        every { mockRepository.getAllAppsFlow() } returns appsFlow

        // AppDatabaseService returns a quick success so init does not block
        coEvery { mockDbService.fetchAndCache() } returns FetchResult.FromCache(0, 1)

        viewModel = AppListViewModel(
            application      = mockApplication,
            repository       = mockRepository,
            classifier       = mockClassifier,
            appDatabaseService = mockDbService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun appInfo(pkg: String, name: String, system: Boolean = false) = AppInfo(
        packageName = pkg,
        appName     = name,
        isSystemApp = system
    )

    // ── 1. setSearchQuery ─────────────────────────────────────────────────────

    @Test
    fun `setSearchQuery updates searchQuery flow`() = runTest {
        viewModel.setSearchQuery("chrome")
        advanceUntilIdle()
        assertEquals("chrome", viewModel.searchQuery.value)
    }

    @Test
    fun `setSearchQuery updates screenState searchQuery`() = runTest {
        advanceUntilIdle() // let init complete

        viewModel.setSearchQuery("maps")
        advanceUntilIdle()

        assertEquals("maps", viewModel.screenState.value.searchQuery)
    }

    @Test
    fun `setSearchQuery with empty string clears query`() = runTest {
        viewModel.setSearchQuery("something")
        advanceUntilIdle()
        viewModel.setSearchQuery("")
        advanceUntilIdle()

        assertEquals("", viewModel.searchQuery.value)
    }

    // ── 2. setSelectedCategory ────────────────────────────────────────────────

    @Test
    fun `setSelectedCategory updates selectedCategory flow`() = runTest {
        viewModel.setSelectedCategory("social")
        advanceUntilIdle()
        assertEquals("social", viewModel.selectedCategory.value)
    }

    @Test
    fun `setSelectedCategory updates screenState selectedCategory`() = runTest {
        advanceUntilIdle()

        viewModel.setSelectedCategory("games")
        advanceUntilIdle()

        assertEquals("games", viewModel.screenState.value.selectedCategory)
    }

    @Test
    fun `setSelectedCategory clears selectedApps`() = runTest {
        advanceUntilIdle()

        // Select an app first
        viewModel.toggleAppSelection("com.example.app")
        advanceUntilIdle()
        assertFalse(viewModel.selectedApps.value.isEmpty())

        // Change category — selection must be cleared
        viewModel.setSelectedCategory("productivity")
        advanceUntilIdle()

        assertTrue(viewModel.selectedApps.value.isEmpty())
    }

    // ── 3. setSortOption ──────────────────────────────────────────────────────

    @Test
    fun `setSortOption updates sortOption flow`() = runTest {
        viewModel.setSortOption(SortOption.NAME_DESC)
        advanceUntilIdle()
        assertEquals(SortOption.NAME_DESC, viewModel.sortOption.value)
    }

    @Test
    fun `setSortOption updates screenState sortBy`() = runTest {
        advanceUntilIdle()

        viewModel.setSortOption(SortOption.INSTALL_DATE_NEWEST)
        advanceUntilIdle()

        assertEquals(SortOption.INSTALL_DATE_NEWEST, viewModel.screenState.value.sortBy)
    }

    @Test
    fun `setSortOption default is NAME_ASC`() = runTest {
        advanceUntilIdle()
        assertEquals(SortOption.NAME_ASC, viewModel.sortOption.value)
    }

    // ── 4. toggleShowSystemApps ───────────────────────────────────────────────

    @Test
    fun `toggleShowSystemApps flips false to true`() = runTest {
        assertFalse(viewModel.showSystemApps.value)

        viewModel.toggleShowSystemApps()
        advanceUntilIdle()

        assertTrue(viewModel.showSystemApps.value)
    }

    @Test
    fun `toggleShowSystemApps flips true back to false`() = runTest {
        viewModel.toggleShowSystemApps()
        advanceUntilIdle()
        viewModel.toggleShowSystemApps()
        advanceUntilIdle()

        assertFalse(viewModel.showSystemApps.value)
    }

    @Test
    fun `toggleShowSystemApps updates screenState showSystemApps`() = runTest {
        advanceUntilIdle()

        viewModel.toggleShowSystemApps()
        advanceUntilIdle()

        assertTrue(viewModel.screenState.value.showSystemApps)
    }

    // ── 5. updateAppCategory ──────────────────────────────────────────────────

    @Test
    fun `updateAppCategory calls repository with correct args`() = runTest {
        coEvery { mockRepository.updateAppCategory(any(), any()) } returns Unit

        viewModel.updateAppCategory("com.example.app", "productivity")
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.updateAppCategory("com.example.app", "productivity") }
    }

    @Test
    fun `updateAppCategory repository error sets screenState error`() = runTest {
        advanceUntilIdle()
        coEvery { mockRepository.updateAppCategory(any(), any()) } throws RuntimeException("DB error")

        viewModel.updateAppCategory("com.example.app", "productivity")
        advanceUntilIdle()

        assertEquals("Failed to update category", viewModel.screenState.value.error)
    }

    @Test
    fun `clearError removes error from screenState`() = runTest {
        advanceUntilIdle()
        coEvery { mockRepository.updateAppCategory(any(), any()) } throws RuntimeException("DB error")
        viewModel.updateAppCategory("com.example.app", "productivity")
        advanceUntilIdle()

        viewModel.clearError()

        assertNull(viewModel.screenState.value.error)
    }

    // ── 6. clearSelectedApps (clearSelection) ─────────────────────────────────

    @Test
    fun `clearSelection empties selectedApps`() = runTest {
        viewModel.toggleAppSelection("com.app.one")
        viewModel.toggleAppSelection("com.app.two")
        advanceUntilIdle()
        assertEquals(2, viewModel.selectedApps.value.size)

        viewModel.clearSelection()
        advanceUntilIdle()

        assertTrue(viewModel.selectedApps.value.isEmpty())
    }

    @Test
    fun `clearSelection on already empty set stays empty`() = runTest {
        assertTrue(viewModel.selectedApps.value.isEmpty())
        viewModel.clearSelection()
        advanceUntilIdle()
        assertTrue(viewModel.selectedApps.value.isEmpty())
    }

    // ── 7. toggleAppSelection ─────────────────────────────────────────────────

    @Test
    fun `toggleAppSelection adds package when not selected`() = runTest {
        viewModel.toggleAppSelection("com.example.first")
        advanceUntilIdle()

        assertTrue(viewModel.selectedApps.value.contains("com.example.first"))
    }

    @Test
    fun `toggleAppSelection removes package when already selected`() = runTest {
        viewModel.toggleAppSelection("com.example.first")
        advanceUntilIdle()
        viewModel.toggleAppSelection("com.example.first")
        advanceUntilIdle()

        assertFalse(viewModel.selectedApps.value.contains("com.example.first"))
    }

    @Test
    fun `toggleAppSelection can select multiple packages`() = runTest {
        viewModel.toggleAppSelection("com.app.one")
        viewModel.toggleAppSelection("com.app.two")
        viewModel.toggleAppSelection("com.app.three")
        advanceUntilIdle()

        assertEquals(setOf("com.app.one", "com.app.two", "com.app.three"), viewModel.selectedApps.value)
    }

    @Test
    fun `toggleAppSelection removing one keeps others`() = runTest {
        viewModel.toggleAppSelection("com.app.one")
        viewModel.toggleAppSelection("com.app.two")
        advanceUntilIdle()

        viewModel.toggleAppSelection("com.app.one") // remove
        advanceUntilIdle()

        assertFalse(viewModel.selectedApps.value.contains("com.app.one"))
        assertTrue(viewModel.selectedApps.value.contains("com.app.two"))
    }
}
