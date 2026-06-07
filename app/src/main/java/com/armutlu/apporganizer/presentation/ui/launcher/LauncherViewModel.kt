package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

data class AppFolder(
    val category: Category,
    val apps: List<AppInfo>
)

/**
 * Pure function — Android bağımlılığı yok, birim testlerinden doğrudan çağrılabilir.
 * Her kategori için bir klasör oluşturur. Boş kategoriler dahil edilmez.
 */
internal fun buildFolders(apps: List<AppInfo>): List<AppFolder> =
    Category.getDefaultCategories()
        .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
        .sortedBy { it.displayOrder }
        .map { cat ->
            AppFolder(
                category = cat,
                apps = apps
                    .filter { it.categoryId == cat.categoryId }
                    .sortedBy { it.appName }
            )
        }
        .filter { it.apps.isNotEmpty() }

/** Tüm uygulamaları ada göre sıralı döndürür. */
internal fun buildAllApps(apps: List<AppInfo>): List<AppInfo> =
    apps.sortedBy { it.appName }

@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val _openFolder = MutableStateFlow<AppFolder?>(null)
    val openFolder: StateFlow<AppFolder?> = _openFolder.asStateFlow()

    private val _allAppsOpen = MutableStateFlow(false)
    val allAppsOpen: StateFlow<Boolean> = _allAppsOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val folders: StateFlow<List<AppFolder>> = repository.getAllAppsFlow()
        .map { buildFolders(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val allApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { buildAllApps(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val filteredAllApps: StateFlow<List<AppInfo>> = combine(
        repository.getAllAppsFlow(),
        _searchQuery
    ) { apps, q ->
        val query = q.trim().lowercase()
        if (query.isEmpty()) buildAllApps(apps)
        else buildAllApps(apps).filter {
            it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun openFolder(folder: AppFolder) {
        _openFolder.value = folder
    }

    fun closeFolder() {
        _openFolder.value = null
    }

    fun openAllApps() {
        _allAppsOpen.value = true
    }

    fun closeAllApps() {
        _allAppsOpen.value = false
        _searchQuery.value = ""
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    /** Paketi launcher üzerinden başlatır. */
    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: run {
                Timber.w("launchApp: getLaunchIntent null for $packageName")
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "launchApp failed: $packageName")
        }
    }

    /** Yönetim ekranını (MainActivity) açar. */
    fun openManager(context: Context) {
        try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(context.packageName) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "openManager failed")
        }
    }
}
