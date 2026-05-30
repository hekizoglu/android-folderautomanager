package com.armutlu.apporganizer.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.presentation.ui.screens.OrganizeState
import com.armutlu.apporganizer.service.LauncherAccessibilityService
import com.armutlu.apporganizer.utils.LauncherOrganizer
import com.armutlu.apporganizer.utils.LauncherType
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.AppClassifier
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreenState
import com.armutlu.apporganizer.presentation.ui.screens.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for AppListScreen
 * Manages all state, user interactions, and business logic
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    application: Application,
    private val repository: AppRepository,
    private val classifier: AppClassifier
) : AndroidViewModel(application) {
    
    // Launcher organize state
    private val _organizeState = MutableStateFlow<OrganizeState>(OrganizeState.Idle)
    val organizeState: StateFlow<OrganizeState> = _organizeState.asStateFlow()

    val detectedLauncher: LauncherType by lazy {
        LauncherOrganizer(getApplication()).detectLauncher()
    }

    // Private state flows
    private val _screenState = MutableStateFlow(AppListScreenState.loading())
    
    private val _selectedCategory = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    private val _showSystemApps = MutableStateFlow(true)
    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    
    // Public state flows
    val screenState: StateFlow<AppListScreenState> = _screenState.asStateFlow()
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()
    
    // Initialization
    init {
        initializeScreen()
    }
    
    /**
     * Initialize screen with data from repository
     */
    private fun initializeScreen() {
        viewModelScope.launch {
            try {
                Timber.d("Initializing screen...")
                
                // Get categories
                val categories = Category.getDefaultCategories()
                
                // Get apps from repository
                repository.getAllAppsFlow()
                    .combine(selectedCategory) { apps, category ->
                        createScreenState(apps, categories, category)
                    }
                    .collect { state ->
                        _screenState.value = state
                    }
                
                Timber.d("Screen initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error initializing screen")
                _screenState.value = AppListScreenState.error("Failed to load apps: ${e.message}")
            }
        }
    }
    
    /**
     * Create screen state from data and filter options
     */
    private suspend fun createScreenState(
        apps: List<AppInfo>,
        categories: List<Category>,
        category: String
    ): AppListScreenState {
        return AppListScreenState(
            apps = apps,
            categories = categories,
            selectedCategory = category,
            searchQuery = searchQuery.value,
            showSystemApps = showSystemApps.value,
            sortBy = sortOption.value,
            selectedApps = selectedApps.value,
            isLoading = false,
            isInitializing = false
        )
    }
    
    /**
     * Sync installed apps from device
     */
    fun syncInstalledApps(installedApps: List<AppInfo>) {
        viewModelScope.launch {
            try {
                _screenState.value = _screenState.value.copy(isRefreshing = true)
                
                repository.syncInstalledApps(installedApps)
                
                Timber.d("Synced ${installedApps.size} apps")
                _screenState.value = _screenState.value.copy(isRefreshing = false)
            } catch (e: Exception) {
                Timber.e(e, "Error syncing apps")
                _screenState.value = _screenState.value.copy(
                    isRefreshing = false,
                    error = "Failed to sync apps"
                )
            }
        }
    }
    
    /**
     * Update app category
     */
    fun updateAppCategory(packageName: String, categoryId: String) {
        viewModelScope.launch {
            try {
                repository.updateAppCategory(packageName, categoryId)
                Timber.d("Updated $packageName to $categoryId")
            } catch (e: Exception) {
                Timber.e(e, "Error updating app category")
                _screenState.value = _screenState.value.copy(
                    error = "Failed to update category"
                )
            }
        }
    }
    
    /**
     * Update multiple apps category (batch)
     */
    fun updateAppsCategory(packageNames: List<String>, categoryId: String) {
        viewModelScope.launch {
            try {
                repository.updateAppsCategory(packageNames, categoryId)
                clearSelection()
                Timber.d("Updated ${packageNames.size} apps to $categoryId")
            } catch (e: Exception) {
                Timber.e(e, "Error updating apps category")
            }
        }
    }
    
    /**
     * Change selected category filter
     */
    fun setSelectedCategory(categoryId: String) {
        _selectedCategory.value = categoryId
        clearSelection()
    }
    
    /**
     * Change search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Change sort option
     */
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    /**
     * Toggle show system apps
     */
    fun toggleShowSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
    }
    
    /**
     * Toggle app selection
     */
    fun toggleAppSelection(packageName: String) {
        val current = _selectedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedApps.value = current
    }
    
    /**
     * Select all visible apps
     */
    fun selectAllVisibleApps() {
        _selectedApps.value = _screenState.value.filteredApps.map { it.packageName }.toSet()
    }
    
    /**
     * Clear all selections
     */
    fun clearSelection() {
        _selectedApps.value = emptySet()
    }
    
    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _screenState.value = _screenState.value.copy(error = null)
    }
    
    /**
     * Get category by ID
     */
    fun getCategoryById(categoryId: String): Category? {
        return _screenState.value.categories.find { it.categoryId == categoryId }
    }
    
    /**
     * Get app by package name
     */
    fun getAppByPackageName(packageName: String): AppInfo? {
        return _screenState.value.apps.find { it.packageName == packageName }
    }
    
    /**
     * Get app count by category
     */
    fun getAppCountByCategory(categoryId: String): Int {
        return _screenState.value.countAppsByCategory(categoryId)
    }
    
    /**
     * Get category statistics
     */
    fun getCategoryStats(): Map<String, Int> {
        return _screenState.value.getCategoryStats()
    }
    
    /**
     * Delete app
     */
    fun deleteApp(packageName: String) {
        viewModelScope.launch {
            try {
                repository.deleteApp(packageName)
                Timber.d("Deleted app: $packageName")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting app")
            }
        }
    }
    
    /**
     * Classify unclassified apps
     */
    fun classifyUnclassifiedApps() {
        viewModelScope.launch {
            try {
                _screenState.value = _screenState.value.copy(isRefreshing = true)
                
                val unclassifiedApps = _screenState.value.apps.filter { 
                    it.categoryId == "uncategorized" 
                }
                
                unclassifiedApps.forEach { app ->
                    val category = classifier.classifyApp(app)
                    if (category != "uncategorized") {
                        repository.updateAppCategory(app.packageName, category)
                    }
                }
                
                _screenState.value = _screenState.value.copy(isRefreshing = false)
                Timber.d("Classified ${unclassifiedApps.size} apps")
            } catch (e: Exception) {
                Timber.e(e, "Error classifying apps")
                _screenState.value = _screenState.value.copy(isRefreshing = false)
            }
        }
    }
    
    fun organizeOnLauncher(useAccessibility: Boolean) {
        viewModelScope.launch {
            _organizeState.value = OrganizeState.Running("Başlatılıyor...")
            try {
                val apps = _screenState.value.apps
                val categories = _screenState.value.categories
                val organizer = LauncherOrganizer(getApplication())

                if (useAccessibility && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val service = LauncherAccessibilityService.instance
                    if (service == null) {
                        _organizeState.value = OrganizeState.Done(
                            false,
                            "Accessibility Service aktif değil.\nAyarlardan etkinleştirin."
                        )
                        return@launch
                    }
                    // Kategori eşlemesi: packageName -> categoryId
                    val appMap = apps.map { it.packageName to it.categoryId }
                    service.startOrganize(appMap) { status ->
                        _organizeState.value = if (status.startsWith("✅"))
                            OrganizeState.Done(true, status)
                        else
                            OrganizeState.Running(status)
                    }
                } else {
                    // Shortcut yöntemi — tüm cihazlarda çalışır
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val result = organizer.organizeByCategories(apps, categories)
                        _organizeState.value = OrganizeState.Done(result.success, result.message)
                    } else {
                        _organizeState.value = OrganizeState.Done(
                            false, "Android 8.0+ gerekli (API 26+)"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "organizeOnLauncher failed")
                _organizeState.value = OrganizeState.Done(false, "Hata: ${e.message}")
            }
        }
    }

    fun launchIntent(intent: Intent) {
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "launchIntent failed")
        }
    }

    fun launchApp(packageName: String) {
        try {
            val ctx = getApplication<Application>()
            val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } else {
                Timber.w("No launch intent for $packageName")
                _screenState.value = _screenState.value.copy(error = "$packageName açılamadı")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching $packageName")
        }
    }

    fun resetFilters() {
        _selectedCategory.value = "all"
        _searchQuery.value = ""
        _sortOption.value = SortOption.NAME_ASC
        _showSystemApps.value = true
        clearSelection()
    }

    // In-memory debug log buffer (son 100 satır)
    private val debugLogs = ArrayDeque<String>(100)

    fun appendDebugLog(line: String) {
        if (debugLogs.size >= 100) debugLogs.removeFirst()
        debugLogs.addLast(line)
    }

    fun getDebugLogs(): String {
        val state = _screenState.value
        return buildString {
            appendLine("=== AppOrganizer Debug ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
            appendLine("Total apps in DB: ${state.apps.size}")
            appendLine("Categories: ${state.categories.size}")
            appendLine("Error state: ${state.error ?: "none"}")
            appendLine("isLoading: ${state.isLoading}, isInitializing: ${state.isInitializing}")
            appendLine("--- Recent Logs ---")
            debugLogs.forEach { appendLine(it) }
        }
    }
}
