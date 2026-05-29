package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import org.junit.Test
import org.junit.Assert.*

class AppListScreenStateTest {
    
    @Test
    fun testStateCreation() {
        val state = AppListScreenState()
        
        assertNotNull(state)
        assertTrue(state.apps.isEmpty())
        assertEquals("all", state.selectedCategory)
        assertEquals("", state.searchQuery)
    }
    
    @Test
    fun testLoadingState() {
        val state = AppListScreenState.loading()
        
        assertTrue(state.isLoading)
        assertTrue(state.isInitializing)
    }
    
    @Test
    fun testErrorState() {
        val errorMsg = "Test error message"
        val state = AppListScreenState.error(errorMsg)
        
        assertEquals(errorMsg, state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isInitializing)
    }
    
    @Test
    fun testFilteredAppsByCategoryAll() {
        val apps = listOf(
            AppInfo("com.app1", "App 1", "social"),
            AppInfo("com.app2", "App 2", "games"),
            AppInfo("com.app3", "App 3", "productivity")
        )
        
        val state = AppListScreenState(
            apps = apps,
            selectedCategory = "all"
        )
        
        assertEquals(3, state.filteredApps.size)
    }
    
    @Test
    fun testFilteredAppsByCategorySpecific() {
        val apps = listOf(
            AppInfo("com.app1", "App 1", "social"),
            AppInfo("com.app2", "App 2", "social"),
            AppInfo("com.app3", "App 3", "games")
        )
        
        val state = AppListScreenState(
            apps = apps,
            selectedCategory = "social"
        )
        
        val filtered = state.filteredApps
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.categoryId == "social" })
    }
    
    @Test
    fun testFilteredAppsBySearch() {
        val apps = listOf(
            AppInfo("com.facebook.katana", "Facebook"),
            AppInfo("com.instagram.android", "Instagram"),
            AppInfo("com.minecraft", "Minecraft")
        )
        
        val state = AppListScreenState(
            apps = apps,
            searchQuery = "face"
        )
        
        val filtered = state.filteredApps
        assertEquals(1, filtered.size)
        assertEquals("Facebook", filtered[0].appName)
    }
    
    @Test
    fun testFilteredAppsSystemApps() {
        val apps = listOf(
            AppInfo("com.app1", "App 1", isSystemApp = true),
            AppInfo("com.app2", "App 2", isSystemApp = false),
            AppInfo("com.app3", "App 3", isSystemApp = true)
        )
        
        val state = AppListScreenState(
            apps = apps,
            showSystemApps = false
        )
        
        val filtered = state.filteredApps
        assertEquals(1, filtered.size)
        assertFalse(filtered[0].isSystemApp)
    }
    
    @Test
    fun testSortByNameAscending() {
        val apps = listOf(
            AppInfo("com.app3", "Zebra"),
            AppInfo("com.app1", "Apple"),
            AppInfo("com.app2", "Banana")
        )
        
        val state = AppListScreenState(
            apps = apps,
            sortBy = SortOption.NAME_ASC
        )
        
        val sorted = state.filteredApps
        assertEquals("Apple", sorted[0].appName)
        assertEquals("Banana", sorted[1].appName)
        assertEquals("Zebra", sorted[2].appName)
    }
    
    @Test
    fun testSortByNameDescending() {
        val apps = listOf(
            AppInfo("com.app1", "Apple"),
            AppInfo("com.app2", "Banana"),
            AppInfo("com.app3", "Zebra")
        )
        
        val state = AppListScreenState(
            apps = apps,
            sortBy = SortOption.NAME_DESC
        )
        
        val sorted = state.filteredApps
        assertEquals("Zebra", sorted[0].appName)
        assertEquals("Banana", sorted[1].appName)
        assertEquals("Apple", sorted[2].appName)
    }
    
    @Test
    fun testSortByCategory() {
        val apps = listOf(
            AppInfo("com.app1", "App 1", "games"),
            AppInfo("com.app2", "App 2", "social"),
            AppInfo("com.app3", "App 3", "games")
        )
        
        val state = AppListScreenState(
            apps = apps,
            sortBy = SortOption.CATEGORY
        )
        
        val sorted = state.filteredApps
        assertEquals("games", sorted[0].categoryId)
        assertEquals("games", sorted[1].categoryId)
        assertEquals("social", sorted[2].categoryId)
    }
    
    @Test
    fun testCountAppsByCategory() {
        val apps = listOf(
            AppInfo("com.app1", "App 1", "social"),
            AppInfo("com.app2", "App 2", "social"),
            AppInfo("com.app3", "App 3", "games")
        )
        
        val state = AppListScreenState(apps = apps)
        
        assertEquals(2, state.countAppsByCategory("social"))
        assertEquals(1, state.countAppsByCategory("games"))
        assertEquals(0, state.countAppsByCategory("unknown"))
    }
    
    @Test
    fun testGetCategoryStats() {
        val apps = AppInfo.createSamples(10)
        val categories = Category.getDefaultCategories()
        
        val state = AppListScreenState(
            apps = apps,
            categories = categories
        )
        
        val stats = state.getCategoryStats()
        assertNotNull(stats)
        assertTrue(stats.values.sum() == 10)
    }
    
    @Test
    fun testHasSelectedApps() {
        val state1 = AppListScreenState(selectedApps = emptySet())
        assertFalse(state1.hasSelectedApps)
        
        val state2 = AppListScreenState(selectedApps = setOf("com.app1"))
        assertTrue(state2.hasSelectedApps)
    }
    
    @Test
    fun testTotalAppsCount() {
        val apps = AppInfo.createSamples(15)
        val state = AppListScreenState(apps = apps)
        
        assertEquals(15, state.totalAppsCount)
    }
    
    @Test
    fun testFilteredAppsCount() {
        val apps = listOf(
            AppInfo("com.app1", "App 1", "social"),
            AppInfo("com.app2", "App 2", "games"),
            AppInfo("com.app3", "App 3", "games")
        )
        
        val state = AppListScreenState(
            apps = apps,
            selectedCategory = "games"
        )
        
        assertEquals(3, state.totalAppsCount)
        assertEquals(2, state.filteredAppsCount)
    }
    
    @Test
    fun testIsProcessing() {
        val state1 = AppListScreenState(isLoading = true)
        assertTrue(state1.isProcessing)
        
        val state2 = AppListScreenState(isInitializing = true)
        assertTrue(state2.isProcessing)
        
        val state3 = AppListScreenState(isRefreshing = true)
        assertTrue(state3.isProcessing)
        
        val state4 = AppListScreenState()
        assertFalse(state4.isProcessing)
    }
    
    @Test
    fun testComplexFiltering() {
        val apps = listOf(
            AppInfo("com.facebook.katana", "Facebook", "social"),
            AppInfo("com.instagram.android", "Instagram", "social"),
            AppInfo("com.minecraft", "Minecraft", "games", isSystemApp = true),
            AppInfo("com.docs", "Google Docs", "productivity", isSystemApp = true),
            AppInfo("com.twitter", "Twitter", "social")
        )
        
        // Filter: Category=social + no system apps + search="Insta" + sort by name
        val state = AppListScreenState(
            apps = apps,
            selectedCategory = "social",
            showSystemApps = false,
            searchQuery = "Insta",
            sortBy = SortOption.NAME_ASC
        )
        
        val filtered = state.filteredApps
        assertEquals(1, filtered.size)
        assertEquals("Instagram", filtered[0].appName)
    }
}
