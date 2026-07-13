package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import java.util.Locale

/**
 * UI State for AppListScreen
 * Represents the complete state of the app list UI
 */
data class AppListScreenState(
    val apps: List<AppInfo> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isInitializing: Boolean = true,
    val error: String? = null,
    val selectedCategory: String = "all",
    val searchQuery: String = "",
    val showSystemApps: Boolean = false,
    val showUncertainOnly: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedApps: Set<String> = emptySet(),
    val sortBy: SortOption = SortOption.NAME_ASC,
    val filteredApps: List<AppInfo> = emptyList(),
    val visibleCategories: List<Category> = emptyList(),
    val categoryStats: Map<String, Int> = emptyMap(),
    val showCategoryDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val selectedAppForEdit: AppInfo? = null
) {
    fun countAppsByCategory(categoryId: String): Int = categoryStats[categoryId] ?: 0

    val hasSelectedApps: Boolean
        get() = selectedApps.isNotEmpty()

    val totalAppsCount: Int
        get() = apps.size

    val filteredAppsCount: Int
        get() = filteredApps.size

    val isProcessing: Boolean
        get() = isLoading || isInitializing || isRefreshing

    companion object {
        fun loading(): AppListScreenState {
            return AppListScreenState(isLoading = true, isInitializing = true)
        }

        fun error(message: String): AppListScreenState {
            return AppListScreenState(
                isLoading = false,
                isInitializing = false,
                error = message
            )
        }
    }
}

internal fun computeFilteredApps(
    apps: List<AppInfo>,
    selectedCategory: String,
    searchQuery: String,
    showSystemApps: Boolean,
    sortBy: SortOption
): List<AppInfo> {
    var result = apps

    if (selectedCategory != "all") {
        result = result.filter { it.categoryId == selectedCategory }
    }

    if (!showSystemApps) {
        result = result.filter { !it.isSystemApp }
    }

    if (searchQuery.isNotBlank()) {
        result = fuzzySearch(result, searchQuery)
    }

    return when (sortBy) {
        SortOption.NAME_ASC -> result.sortedBy { it.appName }
        SortOption.NAME_DESC -> result.sortedByDescending { it.appName }
        SortOption.INSTALL_DATE_NEWEST -> result.sortedByDescending { it.installTime }
        SortOption.INSTALL_DATE_OLDEST -> result.sortedBy { it.installTime }
        SortOption.CATEGORY -> result.sortedWith(compareBy({ it.categoryId }, { it.appName }))
    }
}

internal fun computeCategoryStats(
    apps: List<AppInfo>,
    categories: List<Category>
): Map<String, Int> {
    val counts = apps.groupingBy { it.categoryId }.eachCount()
    return categories.associate { category -> category.categoryId to (counts[category.categoryId] ?: 0) }
}

internal fun computeVisibleCategories(
    categories: List<Category>,
    categoryStats: Map<String, Int>
): List<Category> {
    return categories
        .asSequence()
        .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        .filter { (categoryStats[it.categoryId] ?: 0) > 0 }
        .sortedBy { it.categoryName.lowercase(Locale("tr")) }
        .toList()
}

private fun fuzzySearch(apps: List<AppInfo>, query: String): List<AppInfo> {
    val locale = Locale("tr")
    val q = query.lowercase(locale).trim()
    if (q.isEmpty()) return apps

    data class Scored(val app: AppInfo, val score: Int)

    val scored = apps.mapNotNull { app ->
        val name = app.appName.lowercase(locale)
        val pkg = app.packageName.lowercase(locale)

        val score = when {
            name == q || pkg == q -> 100
            name.startsWith(q) || pkg.startsWith(q) -> 80
            name.contains(q) || pkg.contains(q) -> 60
            else -> {
                val fuzzyScore = fuzzyScore(name, q).takeIf { it > 0 }
                    ?: fuzzyScore(pkg, q).takeIf { it > 0 }
                fuzzyScore
            }
        }
        score?.let { Scored(app, it) }
    }

    return scored.sortedByDescending { it.score }.map { it.app }
}

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

    if (queryIdx < query.length) return 0

    return (59 - totalGap.coerceAtMost(58)).coerceAtLeast(1)
}

enum class SortOption(val label: String) {
    NAME_ASC("İsim A→Z"),
    NAME_DESC("İsim Z→A"),
    INSTALL_DATE_NEWEST("En yeni kurulum"),
    INSTALL_DATE_OLDEST("En eski kurulum"),
    CATEGORY("Kategoriye göre")
}
