package com.armutlu.apporganizer.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.presentation.ui.screens.OrganizeState
import com.armutlu.apporganizer.service.LauncherAccessibilityService
import com.armutlu.apporganizer.utils.LauncherOrganizer
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
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
    private val classifier: AppClassifier,
    private val appDatabaseService: com.armutlu.apporganizer.data.remote.AppDatabaseService
) : AndroidViewModel(application) {

    // ── Log sistemi — MUTLAKA ilk sırada olmalı (init'ten önce hazır) ─────────
    private val _liveDebugLogs = MutableStateFlow<List<String>>(emptyList())
    val liveDebugLogs: StateFlow<List<String>> = _liveDebugLogs.asStateFlow()

    // Launcher organize state
    private val _organizeState = MutableStateFlow<OrganizeState>(OrganizeState.Idle)
    val organizeState: StateFlow<OrganizeState> = _organizeState.asStateFlow()

    // Accessibility service bağlantı durumu
    private val _isA11yConnected = MutableStateFlow(false)
    val isA11yConnected: StateFlow<Boolean> = _isA11yConnected.asStateFlow()

    // Private state flows
    private val _screenState = MutableStateFlow(AppListScreenState.loading())
    
    private val _selectedCategory = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    private val _showSystemApps = MutableStateFlow(false)
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
            // Uygulama veritabanını arka planda indir
            launch {
                appendDebugLog("AppDatabase indirme başlıyor...")
                val result = appDatabaseService.fetchAndCache()
                when (result) {
                    is com.armutlu.apporganizer.data.remote.FetchResult.Success ->
                        appendDebugLog("✅ AppDatabase: ${result.count} uygulama indirildi (v${result.version})")
                    is com.armutlu.apporganizer.data.remote.FetchResult.FromCache ->
                        appendDebugLog("AppDatabase: cache'den ${result.count} uygulama yüklendi")
                    is com.armutlu.apporganizer.data.remote.FetchResult.Error ->
                        appendDebugLog("⚠️ AppDatabase indirilemedi: ${result.message}")
                    com.armutlu.apporganizer.data.remote.FetchResult.NoCache ->
                        appendDebugLog("⚠️ AppDatabase: cache yok, internet bağlantısını kontrol edin")
                }
            }
        }
        viewModelScope.launch {
            try {
                Timber.d("Initializing screen...")

                // Get categories
                val categories = Category.getDefaultCategories()
                
                // Get apps from repository — tüm filtre flow'larını combine et
                combine(
                    repository.getAllAppsFlow(),
                    _selectedCategory,
                    _searchQuery,
                    _sortOption,
                    _showSystemApps
                ) { apps, category, query, sort, showSystem ->
                    createScreenState(apps, categories, category, query, sort, showSystem)
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
    private fun createScreenState(
        apps: List<AppInfo>,
        categories: List<Category>,
        category: String,
        query: String = _searchQuery.value,
        sort: SortOption = _sortOption.value,
        showSystem: Boolean = _showSystemApps.value
    ): AppListScreenState {
        return AppListScreenState(
            apps = apps,
            categories = categories,
            selectedCategory = category,
            searchQuery = query,
            showSystemApps = showSystem,
            sortBy = sort,
            selectedApps = _selectedApps.value,
            isLoading = false,
            isInitializing = false
        )
    }
    
    /**
     * Sync installed apps from device
     */
    fun syncInstalledApps(installedApps: List<AppInfo>) {
        appendDebugLog("syncInstalledApps: ${installedApps.size} uygulama tarandı")
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

                if (unclassifiedApps.isEmpty()) {
                    appendDebugLog("ℹ️ Tüm uygulamalar zaten sınıflandırılmış. 'Menü → Kategorileri Sıfırla' ile baştan başlayabilirsiniz.")
                    _screenState.value = _screenState.value.copy(isRefreshing = false)
                    return@launch
                }

                var classified = 0
                unclassifiedApps.forEach { app ->
                    val category = classifier.classifyApp(app)
                    if (category != "uncategorized") {
                        repository.updateAppCategory(app.packageName, category)
                        classified++
                    }
                }

                _screenState.value = _screenState.value.copy(isRefreshing = false)
                appendDebugLog("✅ AI sınıflandırma: $classified/${unclassifiedApps.size} uygulama kategorilendi")
            } catch (e: Exception) {
                Timber.e(e, "Error classifying apps")
                _screenState.value = _screenState.value.copy(isRefreshing = false)
            }
        }
    }
    
    fun organizeOnLauncher(useAccessibility: Boolean) {
        _organizeState.value = OrganizeState.Done(true, "Launcher otomatik kategorileme aktif.")
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

    fun resetOrganizeState() {
        _organizeState.value = OrganizeState.Idle
    }

    fun resetAndReclassifyAllApps() {
        viewModelScope.launch {
            try {
                _screenState.value = _screenState.value.copy(isRefreshing = true)
                appendDebugLog("Tüm kategoriler sıfırlanıyor...")
                repository.resetAllCategories()
                appendDebugLog("Kategoriler sıfırlandı — yeniden sınıflandırılıyor...")
                val apps = _screenState.value.apps
                apps.forEach { app ->
                    val category = classifier.classifyApp(app)
                    if (category != "uncategorized") {
                        repository.updateAppCategory(app.packageName, category)
                    }
                }
                appendDebugLog("✅ ${apps.size} uygulama yeniden sınıflandırıldı")
                _screenState.value = _screenState.value.copy(isRefreshing = false)
            } catch (e: Exception) {
                Timber.e(e, "Error resetting and reclassifying")
                _screenState.value = _screenState.value.copy(isRefreshing = false)
            }
        }
    }

    fun resetFilters() {
        _selectedCategory.value = "all"
        _searchQuery.value = ""
        _sortOption.value = SortOption.NAME_ASC
        _showSystemApps.value = false
        clearSelection()
    }


    fun appendDebugLog(line: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val entry = "[$timestamp] $line"
        val current = _liveDebugLogs.value.toMutableList()
        current.add(entry)
        if (current.size > 200) current.removeAt(0)
        _liveDebugLogs.value = current
        Timber.d(line)
    }

    fun clearDebugLogs() {
        _liveDebugLogs.value = emptyList()
    }

    fun isAccessibilityServiceEnabledInSystem(): Boolean {
        return try {
            val am = getApplication<Application>()
                .getSystemService(AccessibilityManager::class.java) ?: return false
            val pkg = getApplication<Application>().packageName
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.packageName == pkg }
        } catch (e: Exception) {
            false
        }
    }

    val hiddenApps: kotlinx.coroutines.flow.StateFlow<List<com.armutlu.apporganizer.domain.models.AppInfo>> =
        repository.getHiddenApps()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            repository.updateAppHidden(packageName, false)
        }
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
            _liveDebugLogs.value.forEach { appendLine(it) }
        }
    }
}
