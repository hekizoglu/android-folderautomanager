package com.armutlu.apporganizer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.AppClassifier
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreenState
import com.armutlu.apporganizer.presentation.ui.screens.SortOption
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AppListViewModelTest {
    
    @MockK
    private lateinit var repository: AppRepository
    
    @MockK
    private lateinit var classifier: AppClassifier
    
    private lateinit var viewModel: AppListViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock repository to return sample data
        val sampleApps = listOf(
            AppInfo("com.facebook.katana", "Facebook", "social"),
            AppInfo("com.minecraft", "Minecraft", "games"),
            AppInfo("com.google.android.apps.docs", "Google Docs", "productivity"),
        )
        
        coEvery { repository.getAllAppsFlow() } returns flowOf(sampleApps)
    }
    
    @After
    fun tearDown() {
        // Cleanup if needed
    }
    
    @Test
    fun testViewModelInitialization() = runTest {
        viewModel = AppListViewModel(repository, classifier)
        
        // Give time for init to complete
        Thread.sleep(100)
        
        val state = viewModel.screenState.value
        assertNotNull(state)
        assertTrue(state.apps.isNotEmpty() || state.isInitializing)
    }
    
    @Test
    fun testUpdateAppCategory() = runTest {
        viewModel = AppListViewModel(repository, classifier)
        
        coEvery { repository.updateAppCategory(any(), any()) } returns Unit
        
        viewModel.updateAppCategory("com.test", "games")
        
        coVerify { repository.updateAppCategory("com.test", "games") }
    }
    
    @Test
    fun testSetSelectedCategory() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        viewModel.setSelectedCategory("games")
        
        assertEquals("games", viewModel.selectedCategory.value)
    }
    
    @Test
    fun testSetSearchQuery() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        viewModel.setSearchQuery("Face")
        
        assertEquals("Face", viewModel.searchQuery.value)
    }
    
    @Test
    fun testSortOption() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        viewModel.setSortOption(SortOption.NAME_DESC)
        
        assertEquals(SortOption.NAME_DESC, viewModel.sortOption.value)
    }
    
    @Test
    fun testToggleAppSelection() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        val packageName = "com.test"
        viewModel.toggleAppSelection(packageName)
        
        assertTrue(viewModel.selectedApps.value.contains(packageName))
        
        // Toggle again to deselect
        viewModel.toggleAppSelection(packageName)
        assertFalse(viewModel.selectedApps.value.contains(packageName))
    }
    
    @Test
    fun testSelectAllVisibleApps() = runTest {
        val mockApps = AppInfo.createSamples(10)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        Thread.sleep(100)
        viewModel.selectAllVisibleApps()
        
        assertTrue(viewModel.selectedApps.value.isNotEmpty())
    }
    
    @Test
    fun testClearSelection() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        viewModel.toggleAppSelection("com.test1")
        viewModel.toggleAppSelection("com.test2")
        
        assertTrue(viewModel.selectedApps.value.size > 0)
        
        viewModel.clearSelection()
        
        assertTrue(viewModel.selectedApps.value.isEmpty())
    }
    
    @Test
    fun testClearSearch() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        viewModel.setSearchQuery("test query")
        assertEquals("test query", viewModel.searchQuery.value)
        
        viewModel.clearSearch()
        assertEquals("", viewModel.searchQuery.value)
    }
    
    @Test
    fun testResetFilters() {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        // Change filters
        viewModel.setSelectedCategory("games")
        viewModel.setSearchQuery("test")
        viewModel.setSortOption(SortOption.NAME_DESC)
        viewModel.toggleShowSystemApps()
        viewModel.toggleAppSelection("com.test")
        
        // Reset
        viewModel.resetFilters()
        
        assertEquals("all", viewModel.selectedCategory.value)
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(SortOption.NAME_ASC, viewModel.sortOption.value)
        assertTrue(viewModel.showSystemApps.value)
        assertTrue(viewModel.selectedApps.value.isEmpty())
    }
    
    @Test
    fun testDeleteApp() = runTest {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        coEvery { repository.deleteApp(any()) } returns Unit
        
        viewModel = AppListViewModel(repository, classifier)
        
        viewModel.deleteApp("com.test")
        
        coVerify { repository.deleteApp("com.test") }
    }
    
    @Test
    fun testClassifyUnclassifiedApps() = runTest {
        val mockApps = listOf(
            AppInfo("com.app1", "App 1", "uncategorized"),
            AppInfo("com.app2", "App 2", "social")
        )
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        coEvery { repository.updateAppCategory(any(), any()) } returns Unit
        coEvery { classifier.classifyApp(any()) } returns "games"
        
        viewModel = AppListViewModel(repository, classifier)
        
        Thread.sleep(100)
        viewModel.classifyUnclassifiedApps()
        
        coVerify { classifier.classifyApp(any()) }
    }
    
    @Test
    fun testUpdateMultipleAppsCategory() = runTest {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        coEvery { repository.updateAppsCategory(any(), any()) } returns Unit
        
        viewModel = AppListViewModel(repository, classifier)
        
        val packages = listOf("com.test1", "com.test2")
        viewModel.updateAppsCategory(packages, "games")
        
        coVerify { repository.updateAppsCategory(packages, "games") }
    }
    
    @Test
    fun testSyncInstalledApps() = runTest {
        val mockApps = AppInfo.createSamples(5)
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        coEvery { repository.syncInstalledApps(any()) } returns Unit
        
        viewModel = AppListViewModel(repository, classifier)
        
        val newApps = AppInfo.createSamples(3)
        viewModel.syncInstalledApps(newApps)
        
        coVerify { repository.syncInstalledApps(newApps) }
    }
    
    @Test
    fun testScreenStateFiltering() = runTest {
        val mockApps = listOf(
            AppInfo("com.facebook.katana", "Facebook", "social"),
            AppInfo("com.instagram.android", "Instagram", "social"),
            AppInfo("com.minecraft", "Minecraft", "games", isSystemApp = true),
            AppInfo("com.docs", "Google Docs", "productivity", isSystemApp = true)
        )
        coEvery { repository.getAllAppsFlow() } returns flowOf(mockApps)
        
        viewModel = AppListViewModel(repository, classifier)
        
        Thread.sleep(100)
        
        // Filter by category
        viewModel.setSelectedCategory("social")
        var state = viewModel.screenState.value
        assertTrue(state.filteredApps.all { it.categoryId == "social" })
        
        // Filter by system apps
        viewModel.toggleShowSystemApps()
        state = viewModel.screenState.value
        assertTrue(state.filteredApps.all { !it.isSystemApp })
    }
}
