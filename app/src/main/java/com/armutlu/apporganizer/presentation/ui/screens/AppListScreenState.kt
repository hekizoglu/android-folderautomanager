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
    val showSystemApps: Boolean = false,
    
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
            
            // Search: önce prefix/contains, sonra fuzzy
            if (searchQuery.isNotBlank()) {
                result = fuzzySearch(result, searchQuery)
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
 * Fuzzy search: önce tam eşleşme/prefix, sonra her harfin sırayla bulunması (fuzzy).
 * Sonuçlar alaka puanına göre sıralanır.
 *
 *  Puan 100 → tam eşleşme
 *  Puan  80 → baştan başlıyor (prefix)
 *  Puan  60 → içinde geçiyor (contains)
 *  Puan 1-59 → fuzzy (tüm harfler sırayla bulunuyor, mesafeye göre puan)
 */
private fun fuzzySearch(apps: List<AppInfo>, query: String): List<AppInfo> {
    val q = query.lowercase().trim()
    if (q.isEmpty()) return apps

    data class Scored(val app: AppInfo, val score: Int)

    val scored = apps.mapNotNull { app ->
        val name = app.appName.lowercase()
        val pkg  = app.packageName.lowercase()

        val score = when {
            name == q || pkg == q                    -> 100
            name.startsWith(q) || pkg.startsWith(q) -> 80
            name.contains(q) || pkg.contains(q)     -> 60
            else -> {
                // Fuzzy: her harf sırayla name içinde var mı?
                val fuzzyScore = fuzzyScore(name, q).takeIf { it > 0 }
                    ?: fuzzyScore(pkg, q).takeIf { it > 0 }
                fuzzyScore
            }
        }
        score?.let { Scored(app, it) }
    }

    return scored.sortedByDescending { it.score }.map { it.app }
}

/** Levenshtein benzeri olmayan, sıralı harf varlığı skoru (0 = eşleşme yok). */
private fun fuzzyScore(text: String, query: String): Int {
    var queryIdx = 0
    var lastMatchPos = -1
    var totalGap = 0

    for (i in text.indices) {
        if (queryIdx < query.length && text[i] == query[queryIdx]) {
            if (lastMatchPos >= 0) totalGap += (i - lastMatchPos - 1)
            lastMatchPos = i
            queryIdx++
        }
    }

    if (queryIdx < query.length) return 0  // tüm harfler bulunamadı

    // Az boşluk = yüksek puan (max 59, min 1)
    val score = (59 - totalGap.coerceAtMost(58)).coerceAtLeast(1)
    return score
}

/**
 * Sort options for app list
 */
enum class SortOption(val label: String) {
    NAME_ASC("İsim A→Z"),
    NAME_DESC("İsim Z→A"),
    INSTALL_DATE_NEWEST("En yeni kurulum"),
    INSTALL_DATE_OLDEST("En eski kurulum"),
    CATEGORY("Kategoriye göre")
}
