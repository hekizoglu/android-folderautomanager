package com.armutlu.apporganizer.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.WeeklyGoalDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.data.repository.SearchRepository
import com.armutlu.apporganizer.presentation.ui.screens.OrganizeState
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.TaskScoreManager
import com.armutlu.apporganizer.utils.WidgetSuggestion
import com.armutlu.apporganizer.utils.WidgetSuggestionEngine
import com.armutlu.apporganizer.utils.WeekUtils
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoal
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.classify.CategoryLLMFallback
import com.armutlu.apporganizer.domain.usecase.classify.CLASSIFICATION_ENGINE_VERSION
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationDecision
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationReason
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationReviewPolicy
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationReviewState
import com.armutlu.apporganizer.domain.usecase.classify.ClassificationSource
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestion
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionEngine
import com.armutlu.apporganizer.presentation.ui.screens.AppListScreenState
import com.armutlu.apporganizer.presentation.ui.screens.SortOption
import com.armutlu.apporganizer.presentation.ui.screens.computeCategoryStats
import com.armutlu.apporganizer.presentation.ui.screens.computeFilteredApps
import com.armutlu.apporganizer.presentation.ui.screens.computeVisibleCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import java.util.Locale

/**
 * ViewModel for AppListScreen
 * Manages all state, user interactions, and business logic
 */
@HiltViewModel
class AppListViewModel @Inject constructor(
    application: Application,
    private val repository: AppRepository,
    private val searchRepository: SearchRepository,
    private val classifier: AppClassifier,
    private val llmFallback: CategoryLLMFallback,
    private val weeklyGoalDao: WeeklyGoalDao,
    private val appDatabaseService: com.armutlu.apporganizer.data.remote.AppDatabaseService
) : AndroidViewModel(application) {

    // â"€â"€ Log sistemi - MUTLAKA ilk sırada olmalı (init'ten önce hazır) â"€â"€â"€â"€â"€â"€â"€â"€â"€
    private val _liveDebugLogs = MutableStateFlow<List<String>>(emptyList())
    val liveDebugLogs: StateFlow<List<String>> = _liveDebugLogs.asStateFlow()

    private fun applyLowConfidenceReviewPreference(decision: ClassificationDecision): ClassificationDecision {
        val reviewEnabled = AppPrefs.isLowConfidenceReviewEnabled(getApplication())
        if (reviewEnabled || !decision.requiresReview) return decision
        return decision.copy(
            requiresReview = false,
            reviewState = ClassificationReviewState.NOT_REQUIRED,
        )
    }

    // Launcher organize state
    private val _organizeState = MutableStateFlow<OrganizeState>(OrganizeState.Idle)
    val organizeState: StateFlow<OrganizeState> = _organizeState.asStateFlow()

    // Private state flows
    private val _screenState = MutableStateFlow(AppListScreenState.loading())
    
    private val _selectedCategory = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    private val _showSystemApps = MutableStateFlow(
        com.armutlu.apporganizer.utils.AppPrefs.isShowSystemApps(application)
    )
    private val _showUncertainOnly = MutableStateFlow(false)
    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    
    // Public state flows
    val screenState: StateFlow<AppListScreenState> = _screenState.asStateFlow()
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()
    val showUncertainOnly: StateFlow<Boolean> = _showUncertainOnly.asStateFlow()
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()
    val weeklyGoals: StateFlow<List<WeeklyGoal>> =
        weeklyGoalDao.observeGoals(WeekUtils.currentWeekStartEpochDay())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Widget öneri listesi - en çok kullanılan ve widget'ı olan uygulamalar
    val widgetSuggestions: StateFlow<List<WidgetSuggestion>> = repository.getAllAppsFlow()
        .map { apps -> WidgetSuggestionEngine.getSuggestions(getApplication(), apps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // LLM kategorize durumu - DeepSeek fallback
    private val _llmCategorizing = MutableStateFlow(false)
    val llmCategorizing: StateFlow<Boolean> = _llmCategorizing.asStateFlow()
    private val _llmProgress = MutableStateFlow("")
    val llmProgress: StateFlow<String> = _llmProgress.asStateFlow()

    // Sınıflandırılmamışları sınıflandır yükleme durumu (layout jump â†' mail bug önlemi)
    private val _classifyLoading = MutableStateFlow(false)
    val classifyLoading: StateFlow<Boolean> = _classifyLoading.asStateFlow()
    private val _classifyResult = MutableStateFlow("")
    val classifyResult: StateFlow<String> = _classifyResult.asStateFlow()

    private val _suggestedSimilarApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val suggestedSimilarApps: StateFlow<List<AppInfo>> = _suggestedSimilarApps.asStateFlow()
    private val _suggestedSimilarCategoryId = MutableStateFlow<String?>(null)
    val suggestedSimilarCategoryId: StateFlow<String?> = _suggestedSimilarCategoryId.asStateFlow()
    private val _folderSuggestionRefresh = MutableStateFlow(0)

    // Tek dogruluk kaynagi: ClassificationAttentionPolicy. Sayac (SettingsAppsSection,
    // Dashboard) ve liste (ClassificationReviewScreen) ayni flow'dan besleniyor —
    // artik sayac dolu iken liste bos kalamaz (P0.2).
    val classificationAttentionApps: StateFlow<List<AppInfo>> =
        repository.getAllAppsFlow()
            .map { apps -> com.armutlu.apporganizer.domain.usecase.classify.ClassificationAttentionPolicy.attentionList(apps) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val folderSuggestions: StateFlow<List<FolderSuggestion>> =
        combine(
            repository.getAllAppsFlow(),
            repository.getAllCategoriesFlow(),
            _folderSuggestionRefresh,
        ) { apps, categories, _ ->
            val context = getApplication<Application>()
            if (!AppPrefs.isFolderSuggestionsEnabled(context)) return@combine emptyList()
            val snoozed = AppPrefs.getSnoozedFolderSuggestions(context)
                .mapValues { (_, value) -> value.toLongOrNull() ?: 0L }
            FolderSuggestionEngine.generate(
                apps = apps,
                categories = categories,
                dismissedIds = AppPrefs.getDismissedFolderSuggestions(context),
                snoozedUntilById = snoozed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
    private val _folderSuggestionsInfoDismissed = MutableStateFlow(
        AppPrefs.isFolderSuggestionsInfoDismissed(application)
    )
    val folderSuggestionsInfoDismissed: StateFlow<Boolean> = _folderSuggestionsInfoDismissed.asStateFlow()
    
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
                        appendDebugLog("âœ… AppDatabase: ${result.count} uygulama indirildi (v${result.version})")
                    is com.armutlu.apporganizer.data.remote.FetchResult.FromCache ->
                        appendDebugLog("AppDatabase: cache'den ${result.count} uygulama yüklendi")
                    is com.armutlu.apporganizer.data.remote.FetchResult.Error ->
                        appendDebugLog("âš ï¸ AppDatabase indirilemedi: ${result.message}")
                    com.armutlu.apporganizer.data.remote.FetchResult.NoCache ->
                        appendDebugLog("âš ï¸ AppDatabase: cache yok, internet bağlantısını kontrol edin")
                }
            }
        }
        viewModelScope.launch {
            try {
                Timber.d("Initializing screen...")
                repository.ensureDefaultCategories()
                migrateManualOverridesIfNeeded()
                
                // Get apps from repository - tüm filtre flow'larını combine et
                combine(
                    repository.getAllAppsFlow(),
                    repository.getAllCategoriesFlow(),
                    _selectedCategory,
                    _searchQuery,
                    _sortOption,
                    _showSystemApps,
                    _showUncertainOnly,
                    _selectedApps
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    createScreenState(
                        apps = values[0] as List<AppInfo>,
                        categories = values[1] as List<Category>,
                        category = values[2] as String,
                        query = values[3] as String,
                        sort = values[4] as SortOption,
                        showSystem = values[5] as Boolean,
                        showUncertainOnly = values[6] as Boolean,
                        selectedApps = values[7] as Set<String>
                    )
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

    private suspend fun migrateManualOverridesIfNeeded() {
        val context = getApplication<Application>()
        if (AppPrefs.isManualOverridesRoomMigrated(context)) return
        val migrated = repository.migrateManualOverrides(AppPrefs.getManualCategoryOverrides(context))
        AppPrefs.setManualOverridesRoomMigrated(context, true)
        if (migrated > 0) {
            appendDebugLog("Manuel kategori kararları Room metadata'ya taşındı: $migrated")
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
        showSystem: Boolean = _showSystemApps.value,
        showUncertainOnly: Boolean = _showUncertainOnly.value,
        selectedApps: Set<String> = _selectedApps.value
    ): AppListScreenState {
        val visibleApps = if (showSystem) apps else apps.filter { !it.isSystemApp }
        val categoryStats = computeCategoryStats(visibleApps, categories)
        val filteredApps = computeFilteredApps(apps, category, query, showSystem, sort)
            .let { list ->
                if (showUncertainOnly) {
                    list.filter { app -> classifier.isLowConfidence(app, app.categoryId) }
                } else {
                    list
                }
            }
        return AppListScreenState(
            apps = apps,
            categories = categories,
            selectedCategory = category,
            searchQuery = query,
            showSystemApps = showSystem,
            showUncertainOnly = showUncertainOnly,
            sortBy = sort,
            selectedApps = selectedApps,
            filteredApps = filteredApps,
            visibleCategories = computeVisibleCategories(categories, categoryStats),
            categoryStats = categoryStats,
            isLoading = false,
            isInitializing = false
        )
    }
    
    /**
     * Sync installed apps from device
     */
    fun syncInstalledApps(installedApps: List<AppInfo>) {
        appendDebugLog("syncInstalledApps: ${installedApps.size} uygulama tarandı")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _screenState.value = _screenState.value.copy(isRefreshing = true)
                
                repository.syncInstalledApps(installedApps)
                searchRepository.bootstrapIndex()
                
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
    fun updateAppCategory(
        packageName: String,
        categoryId: String,
        scoreEvent: TaskScoreManager.EventType? = null,
    ) {
        viewModelScope.launch {
            try {
                val oldCategoryId = repository.getAppByPackageName(packageName)?.categoryId
                repository.updateAppCategory(packageName, categoryId)
                AppPrefs.setManualCategoryOverride(getApplication(), packageName, categoryId)
                repository.getAppByPackageName(packageName)?.let { searchRepository.indexApp(it) }
                scoreEvent?.let { TaskScoreManager.record(getApplication(), it) }
                if (oldCategoryId != null) {
                    prepareSimilarCategorySuggestions(packageName, oldCategoryId, categoryId)
                }
                Timber.d("Updated $packageName to $categoryId")
            } catch (e: Exception) {
                Timber.e(e, "Error updating app category")
                _screenState.value = _screenState.value.copy(
                    error = "Failed to update category"
                )
            }
        }
    }

    fun confirmPendingClassification(packageName: String) {
        viewModelScope.launch {
            repository.confirmClassification(packageName)
            repository.getAppByPackageName(packageName)?.let { searchRepository.indexApp(it) }
            TaskScoreManager.record(getApplication(), TaskScoreManager.EventType.ClassificationApproved)
        }
    }

    fun correctPendingClassification(packageName: String, categoryId: String) {
        updateAppCategory(
            packageName = packageName,
            categoryId = categoryId,
            scoreEvent = TaskScoreManager.EventType.ClassificationCorrected,
        )
    }

    fun skipPendingClassification(packageName: String) {
        viewModelScope.launch {
            repository.skipClassificationReview(packageName, days = 7)
            TaskScoreManager.record(getApplication(), TaskScoreManager.EventType.ClassificationSnoozed)
        }
    }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                _screenState.value = _screenState.value.copy(error = "Kategori adı boş olamaz")
                return@launch
            }
            if (repository.findCategoryByName(trimmedName) != null) {
                _screenState.value = _screenState.value.copy(error = "Bu isimde bir kategori zaten var")
                return@launch
            }
            runCatching {
                val categoryId = buildCategoryId(trimmedName)
                val order = repository.getNextCategoryDisplayOrder()
                val category = Category(
                    categoryId = categoryId,
                    categoryName = trimmedName,
                    iconEmoji = emoji,
                    colorHex = "#00897B",
                    isSystemCategory = false,
                    displayOrder = order
                )
                repository.addCategory(category)
                searchRepository.reindexCategory(null, category)
            }.onFailure {
                Timber.e(it, "Error adding category")
                _screenState.value = _screenState.value.copy(error = "Kategori eklenemedi")
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            val trimmedName = category.categoryName.trim()
            if (trimmedName.isEmpty()) {
                _screenState.value = _screenState.value.copy(error = "Kategori adı boş olamaz")
                return@launch
            }
            val duplicate = repository.findCategoryByName(trimmedName)
            if (duplicate != null && duplicate.categoryId != category.categoryId) {
                _screenState.value = _screenState.value.copy(error = "Bu isimde bir kategori zaten var")
                return@launch
            }
            val oldCategory = repository.getCategoryById(category.categoryId)
            runCatching {
                val updatedCategory = category.copy(categoryName = trimmedName)
                repository.updateCategory(updatedCategory)
                searchRepository.reindexCategory(oldCategory, updatedCategory)
            }.onFailure {
                Timber.e(it, "Error updating category")
                _screenState.value = _screenState.value.copy(error = "Kategori güncellenemedi")
            }
        }
    }

    fun deleteCategory(category: Category) {
        if (category.isSystemCategory) {
            _screenState.value = _screenState.value.copy(error = "Sistem kategorileri silinemez")
            return
        }
        viewModelScope.launch {
            runCatching {
                val affectedPackages = repository.getAllApps()
                    .filter { it.categoryId == category.categoryId }
                    .map { it.packageName }
                if (affectedPackages.isNotEmpty()) {
                    repository.updateAppsCategoryAutomatically(affectedPackages, Category.CAT_UNCATEGORIZED)
                    affectedPackages.forEach { AppPrefs.clearManualCategoryOverride(getApplication(), it) }
                }
                repository.deleteCategory(category.categoryId)
                searchRepository.removeCategory(category.categoryId)
            }.onFailure {
                Timber.e(it, "Error deleting category")
                _screenState.value = _screenState.value.copy(error = "Kategori silinemedi")
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
                packageNames.forEach { packageName ->
                    AppPrefs.setManualCategoryOverride(getApplication(), packageName, categoryId)
                    repository.getAppByPackageName(packageName)?.let { searchRepository.indexApp(it) }
                }
                clearSelection()
                Timber.d("Updated ${packageNames.size} apps to $categoryId")
            } catch (e: Exception) {
                Timber.e(e, "Error updating apps category")
            }
        }
    }

    /**
     * Seçilen benzer uygulamaları öneri kategorisine taşır. Kullanıcı satır bazlı
     * checkbox ile seçim yapar - [selectedPackageNames] boşsa hiçbir şey yapılmaz.
     */
    fun acceptSimilarCategorySuggestions(selectedPackageNames: Set<String> = _suggestedSimilarApps.value.map { it.packageName }.toSet()) {
        val apps = _suggestedSimilarApps.value.filter { it.packageName in selectedPackageNames }
        val categoryId = _suggestedSimilarCategoryId.value
        if (apps.isEmpty() || categoryId == null) {
            clearSimilarCategorySuggestions()
            return
        }
        viewModelScope.launch {
            val packageNames = apps.map { it.packageName }
            repository.updateAppsCategory(packageNames, categoryId)
            packageNames.forEach { packageName ->
                AppPrefs.setManualCategoryOverride(getApplication(), packageName, categoryId)
                repository.getAppByPackageName(packageName)?.let { searchRepository.indexApp(it) }
            }
            AppPrefs.addAcceptedOverridePattern(getApplication(), categoryId, packageNames)
            TaskScoreManager.record(
                context = getApplication(),
                eventType = TaskScoreManager.EventType.SimilarAppsAccepted,
                weight = packageNames.size,
            )
            clearSimilarCategorySuggestions()
        }
    }

    fun clearSimilarCategorySuggestions() {
        _suggestedSimilarApps.value = emptyList()
        _suggestedSimilarCategoryId.value = null
    }

    fun acceptFolderSuggestion(suggestionId: String) {
        val suggestion = folderSuggestions.value.firstOrNull { it.id == suggestionId } ?: return
        viewModelScope.launch {
            repository.updateAppsCategory(suggestion.packageNames, suggestion.targetCategoryId)
            suggestion.packageNames.forEach { packageName ->
                AppPrefs.setManualCategoryOverride(getApplication(), packageName, suggestion.targetCategoryId)
                repository.getAppByPackageName(packageName)?.let { searchRepository.indexApp(it) }
            }
            AppPrefs.dismissFolderSuggestion(getApplication(), suggestionId)
            TaskScoreManager.record(
                context = getApplication(),
                eventType = TaskScoreManager.EventType.FolderSuggestionAccepted,
                weight = suggestion.packageNames.size.coerceAtLeast(1),
            )
            _folderSuggestionRefresh.value += 1
        }
    }

    fun dismissFolderSuggestion(suggestionId: String) {
        viewModelScope.launch {
            AppPrefs.dismissFolderSuggestion(getApplication(), suggestionId)
            TaskScoreManager.record(getApplication(), TaskScoreManager.EventType.FolderSuggestionDismissed)
            _folderSuggestionRefresh.value += 1
        }
    }

    fun snoozeFolderSuggestion(suggestionId: String) {
        val until = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
        viewModelScope.launch {
            AppPrefs.snoozeFolderSuggestion(getApplication(), suggestionId, until)
            TaskScoreManager.record(getApplication(), TaskScoreManager.EventType.FolderSuggestionSnoozed)
            _folderSuggestionRefresh.value += 1
        }
    }

    fun dismissFolderSuggestionsInfo() {
        AppPrefs.setFolderSuggestionsInfoDismissed(getApplication(), true)
        _folderSuggestionsInfoDismissed.value = true
    }

    fun setWeeklyGoal(categoryId: String, targetMinutes: Int) {
        if (categoryId.isBlank()) return
        viewModelScope.launch {
            weeklyGoalDao.upsert(
                WeeklyGoal(
                    categoryId = categoryId,
                    targetMinutes = targetMinutes.coerceIn(1, 7 * 24 * 60),
                    weekStartEpochDay = WeekUtils.currentWeekStartEpochDay(),
                )
            )
        }
    }

    fun deleteWeeklyGoal(categoryId: String) {
        viewModelScope.launch {
            weeklyGoalDao.delete(categoryId, WeekUtils.currentWeekStartEpochDay())
        }
    }
    
    /**
     * Change selected category filter
     */
    fun setSelectedCategory(categoryId: String) {
        _selectedCategory.value = categoryId
        clearSelection()
    }

    fun setUncertainFilterEnabled(enabled: Boolean) {
        if (_showUncertainOnly.value == enabled) return
        _showUncertainOnly.value = enabled
        if (enabled) {
            _selectedCategory.value = "all"
            _searchQuery.value = ""
        }
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
        val newVal = !_showSystemApps.value
        _showSystemApps.value = newVal
        com.armutlu.apporganizer.utils.AppPrefs.setShowSystemApps(getApplication(), newVal)
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
        return _screenState.value.categoryStats
    }

    private suspend fun prepareSimilarCategorySuggestions(packageName: String, oldCategoryId: String, newCategoryId: String) {
        val context = getApplication<Application>()
        if (!AppPrefs.isOverrideSuggestionsEnabled(context)) {
            clearSimilarCategorySuggestions()
            return
        }
        val allApps = repository.getAllApps()
        val changedApp = allApps.firstOrNull { it.packageName == packageName }
        if (changedApp == null) {
            clearSimilarCategorySuggestions()
            return
        }
        val manualOverrides = AppPrefs.getManualCategoryOverrides(context)
        val suggestions = classifier.findSimilarUnclassifiedApps(
            changedApp = changedApp,
            oldCategoryId = oldCategoryId,
            newCategoryId = newCategoryId,
            allApps = allApps,
            manualOverrides = manualOverrides
        )
        _suggestedSimilarApps.value = suggestions
        _suggestedSimilarCategoryId.value = newCategoryId.takeIf { suggestions.isNotEmpty() }
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
        if (_classifyLoading.value) return
        viewModelScope.launch {
            try {
                _classifyLoading.value = true
                _classifyResult.value = ""
                val ctx = getApplication<Application>()
                val mode = com.armutlu.apporganizer.utils.AppPrefs.getClassificationMode(ctx)
                _screenState.value = _screenState.value.copy(isRefreshing = true)

                val unclassifiedApps = _screenState.value.apps.filter {
                    it.categoryId == "uncategorized"
                }

                if (unclassifiedApps.isEmpty()) {
                    _classifyResult.value = "Tüm uygulamalar zaten sınıflandırılmış."
                    appendDebugLog("Tum uygulamalar zaten siniflandirilmis.")
                    _screenState.value = _screenState.value.copy(isRefreshing = false)
                    _classifyLoading.value = false
                    return@launch
                }

                if (mode == com.armutlu.apporganizer.utils.AppPrefs.ClassificationMode.MANUAL_REVIEW_ONLY) {
                    _classifyResult.value = "Manuel inceleme modu aktif — otomatik sınıflandırma yapılmadı."
                    appendDebugLog("Manuel inceleme modu: otomatik siniflandirma atlandi.")
                    _screenState.value = _screenState.value.copy(isRefreshing = false)
                    _classifyLoading.value = false
                    return@launch
                }

                var classified = 0
                unclassifiedApps.forEach { app ->
                    val decision = applyLowConfidenceReviewPreference(
                        classifier.classifyAppDecision(app, mode)
                    )
                    if (decision.categoryId != "uncategorized") {
                        repository.updateAppCategoryAutomatically(app.packageName, decision)
                        classified++
                    }
                }

                _screenState.value = _screenState.value.copy(isRefreshing = false)
                _classifyResult.value = "$classified / ${unclassifiedApps.size} uygulama kategorilendi."
                appendDebugLog("âœ… AI sınıflandırma: $classified/${unclassifiedApps.size} uygulama kategorilendi")
            } catch (e: Exception) {
                Timber.e(e, "Error classifying apps")
                _screenState.value = _screenState.value.copy(isRefreshing = false)
                _classifyResult.value = "Hata: ${e.message}"
            } finally {
                _classifyLoading.value = false
            }
        }
    }
    
    /**
     * "Diger" klasorundeki uygulamalari DeepSeek LLM ile kategorize eder.
     */
    fun categorizeDigerWithLLM(apiKey: String) {
        viewModelScope.launch {
            if (_llmCategorizing.value) return@launch
            val ctx = getApplication<Application>()
            val mode = com.armutlu.apporganizer.utils.AppPrefs.getClassificationMode(ctx)
            if (mode != com.armutlu.apporganizer.utils.AppPrefs.ClassificationMode.LOCAL_WITH_LLM_FALLBACK) {
                _llmProgress.value = "LLM fallback yalnizca \"Yerel + LLM\" modunda calisir — Ayarlar'dan modu degistirin."
                appendDebugLog("LLM kategorize atlandi: mod=$mode")
                return@launch
            }
            _llmCategorizing.value = true
            try {
                val otherAppsList = repository.getAppsByCategory(com.armutlu.apporganizer.domain.models.Category.CAT_OTHER).firstOrNull() ?: emptyList()
                if (otherAppsList.isEmpty()) {
                    _llmProgress.value = "Diger klasoru bos - kategorize edilecek uygulama yok."
                    appendDebugLog("LLM: Diger klasoru bos.")
                    return@launch
                }
                _llmProgress.value = "Hazirlaniyor (${otherAppsList.size} uygulama)..."
                appendDebugLog("LLM kategorize basliyor: ${otherAppsList.size} uygulama")
                val packageNames = otherAppsList.map { it.packageName }
                // Batch'lere bölerek ilerleme raporla
                val results = mutableMapOf<String, String>()
                packageNames.chunked(15).forEachIndexed { idx, batch ->
                    _llmProgress.value = "Kategorize ediliyor: ${(idx * 15).coerceAtMost(packageNames.size)}/${packageNames.size}"
                    val batchResult = llmFallback.classifyBatch(batch, apiKey)
                    results.putAll(batchResult)
                }
                var updated = 0
                results.forEach { (pkg, catId) ->
                    if (catId != Category.CAT_OTHER) {
                        val (requiresReview, reviewState) = ClassificationReviewPolicy.resolve(
                            categoryId = catId,
                            confidence = 65,
                            source = ClassificationSource.LLM_LEGACY,
                        )
                        val decision = applyLowConfidenceReviewPreference(
                            ClassificationDecision(
                                categoryId = catId,
                                confidence = 65,
                                source = ClassificationSource.LLM_LEGACY,
                                reasonCode = ClassificationReason.LEGACY_AI_RESULT,
                                requiresReview = requiresReview,
                                reviewState = reviewState,
                                engineVersion = CLASSIFICATION_ENGINE_VERSION,
                            )
                        )
                        repository.updateAppCategoryAutomatically(pkg, decision)
                        updated++
                    }
                }
                val msg = "LLM kategorize tamamlandi: $updated/${otherAppsList.size} uygulama yeniden kategorilendirildi"
                _llmProgress.value = msg
                appendDebugLog("LLM: $msg")
            } catch (e: Exception) {
                val err = "LLM kategorize hatasi: ${e.message}"
                _llmProgress.value = err
                appendDebugLog(err)
                Timber.e(e, "categorizeDigerWithLLM error")
            } finally {
                _llmCategorizing.value = false
            }
        }
    }

    fun organizeOnLauncher() {
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
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val intent = ctx.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    val now = System.currentTimeMillis()
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                    repository.incrementLaunchCount(packageName)
                    repository.updateLastUsedTimestamp(packageName, now)
                } else {
                    Timber.w("No launch intent for $packageName")
                    _screenState.value = _screenState.value.copy(error = "$packageName açılamadı")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error launching $packageName")
            }
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
                appendDebugLog("Kategoriler sıfırlandı - yeniden sınıflandırılıyor...")
                val apps = _screenState.value.apps
                val mode = com.armutlu.apporganizer.utils.AppPrefs
                    .getClassificationMode(getApplication())
                apps.forEach { app ->
                    val decision = applyLowConfidenceReviewPreference(
                        classifier.classifyAppDecision(app, mode)
                    )
                    if (decision.categoryId != "uncategorized") {
                        repository.updateAppCategoryAutomatically(app.packageName, decision)
                    }
                }
                appendDebugLog("âœ… ${apps.size} uygulama yeniden sınıflandırıldı")
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
        AppPrefs.setShowSystemApps(getApplication(), false)
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

    fun resetAllPrivacyData(context: android.content.Context) {
        viewModelScope.launch {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    repository.resetAllCategories()
                    repository.getAllApps().forEach { app ->
                        repository.updateUsageTimeMs(app.packageName, 0)
                        repository.updateLaunchCount(app.packageName, 0)
                        repository.updateLastUsedTimestamp(app.packageName, 0L)
                        repository.updateNotificationCount(app.packageName, 0)
                        repository.updateCustomNotes(app.packageName, "")
                    }
                    repository.clearAllNotificationTexts()
                    repository.clearAllNotificationEvents()
                    context.getSharedPreferences(com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .edit().remove(com.armutlu.apporganizer.utils.AppPrefs.KEY_FAVORITES_SET).apply()
                    Timber.d("Privacy reset: tüm kullanım verisi temizlendi")
                }.onFailure { Timber.e(it, "resetAllPrivacyData hatası") }
            }
        }
    }

    // P0.4: İstatistik sıfırlama sihirbazı — kapsam seçimli. Sonuç [_statsResetResult] üzerinden
    // UI'a bildirilir (snackbar/dialog); sessiz bitirme yok.
    private val _statsResetResult =
        kotlinx.coroutines.flow.MutableStateFlow<List<com.armutlu.apporganizer.domain.usecase.stats.StatsResetService.ScopeResult>?>(null)
    val statsResetResult: kotlinx.coroutines.flow.StateFlow<List<com.armutlu.apporganizer.domain.usecase.stats.StatsResetService.ScopeResult>?> =
        _statsResetResult

    fun resetStatsScoped(
        context: android.content.Context,
        scopes: Set<com.armutlu.apporganizer.domain.usecase.stats.StatsResetService.Scope>
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val results = com.armutlu.apporganizer.domain.usecase.stats.StatsResetService.reset(context, repository, scopes)
            Timber.d("StatsResetService: ${results.count { it.success }}/${results.size} kapsam sıfırlandı")
            _statsResetResult.value = results
        }
    }

    fun consumeStatsResetResult() {
        _statsResetResult.value = null
    }

    val hiddenApps: kotlinx.coroutines.flow.StateFlow<List<com.armutlu.apporganizer.domain.models.AppInfo>> =
        repository.getHiddenApps()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000L), emptyList())

    // P0.2: "Diger Klasoru" sayaci artik ClassificationAttentionPolicy'den besleniyor.
    // Onceden sadece categoryId == CAT_OTHER bakiyordu (review state/confidence/isSystemApp
    // yok sayiliyordu) — sayac (SettingsAppsSection basligi) ile liste (kartlar) farkli
    // kumeler dondurebiliyordu. Simdi ikisi de ayni attentionList() cikisindan
    // OTHER_WITHOUT_CONFIDENCE nedenine filtrelenerek turetiliyor.
    val otherApps: kotlinx.coroutines.flow.StateFlow<List<com.armutlu.apporganizer.domain.models.AppInfo>> =
        classificationAttentionApps
            .map { apps ->
                apps.filter {
                    com.armutlu.apporganizer.domain.usecase.classify.ClassificationAttentionPolicy.evaluate(it) ==
                        com.armutlu.apporganizer.domain.usecase.classify.AttentionReason.OTHER_WITHOUT_CONFIDENCE
                }
            }
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            repository.updateAppHidden(packageName, false)
        }
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            repository.updateAppHidden(packageName, hidden)
        }
    }

    suspend fun exportBackup(context: android.content.Context): android.content.Intent? =
        com.armutlu.apporganizer.utils.BackupManager.exportAndShare(context, repository)

    suspend fun importBackup(
        context: android.content.Context,
        json: String
    ): com.armutlu.apporganizer.utils.BackupManager.ImportResult =
        com.armutlu.apporganizer.utils.BackupManager.importFromJson(context, json, repository, searchRepository)

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

    private fun buildCategoryId(name: String): String {
        val normalized = name
            .trim()
            .lowercase(Locale("tr"))
            .replace('ı', 'i')
            .replace('ğ', 'g')
            .replace('ü', 'u')
            .replace('ş', 's')
            .replace('ö', 'o')
            .replace('ç', 'c')
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
        return "custom_${normalized.ifBlank { System.currentTimeMillis().toString() }}"
    }
}
