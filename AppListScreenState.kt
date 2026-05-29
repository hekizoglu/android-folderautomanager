package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category

/**
 * UI State for AppListScreen
 * Represents the complete state of the app list UI
 */
data class AppListScreenState(
    // Core data
    val apps: List<AppInfo> = emptyList(),
    val categories: List<Category> = emptyList(),
    
    // UI state
    val isLoading: Boolean = false,
    val isInitializing: Boolean = true,
    val error: String? = null,
    
    // Filters & Search
    val selectedCategory: String = "all",
    val searchQuery: String = "",
    val showSystemApps: Boolean = true,
    
    // UI behavior
    val isRefreshing: Boolean = false,
    val selectedApps: Set<String> = emptySet(),
    val sortBy: SortOption = SortOption.NAME_ASC,
    
    // Dialog states
    val showCategoryDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val selectedAppForEdit: AppInfo? = null
) {
    
    /**
     * Get filtered and sorted apps based on current UI state
     */
    val filteredApps: List<AppInfo>
        get() {
            var result = apps
            
            // Filter by category
            if (selectedCategory != "all") {
                result = result.filter { it.categoryId == selectedCategory }
            }
            
            // Filter by system apps
            if (!showSystemApps) {
                result = result.filter { !it.isSystemApp }
            }
            
            // Filter by search query
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase()
                result = result.filter { 
                    it.appName.lowercase().contains(query) ||
                    it.packageName.lowercase().contains(query)
                }
            }
            
            // Sort
            result = when (sortBy) {
                SortOption.NAME_ASC -> result.sortedBy { it.appName }
                SortOption.NAME_DESC -> result.sortedByDescending { it.appName }
                SortOption.INSTALL_DATE_NEWEST -> result.sortedByDescending { it.installTime }
                SortOption.INSTALL_DATE_OLDEST -> result.sortedBy { it.installTime }
                SortOption.CATEGORY -> result.sortedWith(compareBy({ it.categoryId }, { it.appName }))
            }
            
            return result
        }
    
    /**
     * Count apps by category
     */
    fun countAppsByCategory(categoryId: String): Int {
        return apps.count { it.categoryId == categoryId }
    }
    
    /**
     * Get category stats
     */
    fun getCategoryStats(): Map<String, Int> {
        return categories.associate { category ->
            category.categoryId to countAppsByCategory(category.categoryId)
        }
    }
    
    /**
     * Check if any apps are selected
     */
    val hasSelectedApps: Boolean
        get() = selectedApps.isNotEmpty()
    
    /**
     * Total apps count
     */
    val totalAppsCount: Int
        get() = apps.size
    
    /**
     * Filtered apps count
     */
    val filteredAppsCount: Int
        get() = filteredApps.size
    
    /**
     * Is state loading or initializing
     */
    val isProcessing: Boolean
        get() = isLoading || isInitializing || isRefreshing
    
    companion object {
        /**
         * Create a loading state
         */
        fun loading(): AppListScreenState {
            return AppListScreenState(isLoading = true, isInitializing = true)
        }
        
        /**
         * Create an error state
         */
        fun error(message: String): AppListScreenState {
            return AppListScreenState(
                isLoading = false,
                isInitializing = false,
                error = message
            )
        }
    }
}

/**
 * Sort options for app list
 */
enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    INSTALL_DATE_NEWEST,
    INSTALL_DATE_OLDEST,
    CATEGORY
}
