package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.service.AppNotificationListenerService
import com.armutlu.apporganizer.utils.DockPrefs
import com.armutlu.apporganizer.utils.PackageManagerHelper
import com.armutlu.apporganizer.utils.UsageStatsHelper
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val PREFS_NAME = "launcher_prefs"
private const val KEY_DOCK_PACKAGES = "dock_packages"
private const val KEY_FOLDER_ORDER = "folder_order"
private const val DOCK_MAX_SIZE = 4

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

/** Tüm uygulamaları ada göre sıralı döndürür. Gizli uygulamalar hariç. */
internal fun buildAllApps(apps: List<AppInfo>): List<AppInfo> =
    apps.filter { !it.isHidden }.sortedBy { it.appName }

@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Kullanıcı tarafından drag&drop ile değiştirilen klasör sırası (categoryId listesi)
    private val _folderOrder = MutableStateFlow<List<String>>(emptyList())

    private val _dockPackages = MutableStateFlow<List<String>>(emptyList())
    val dockPackages: StateFlow<List<String>> = _dockPackages.asStateFlow()

    private val _openFolder = MutableStateFlow<AppFolder?>(null)
    val openFolder: StateFlow<AppFolder?> = _openFolder.asStateFlow()

    private val _allAppsOpen = MutableStateFlow(false)
    val allAppsOpen: StateFlow<Boolean> = _allAppsOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val folders: StateFlow<List<AppFolder>> = combine(
        repository.getAllAppsFlow(),
        _folderOrder
    ) { apps, order ->
        val built = buildFolders(apps)
        if (order.isEmpty()) built
        else {
            val orderMap = order.mapIndexed { i, id -> id to i }.toMap()
            built.sortedBy { orderMap[it.category.categoryId] ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val allApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { buildAllApps(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    @OptIn(FlowPreview::class)
    val filteredAllApps: StateFlow<List<AppInfo>> = combine(
        repository.getAllAppsFlow(),
        _searchQuery.debounce(300)
    ) { apps, q ->
        val query = q.trim().lowercase()
        if (query.isEmpty()) buildAllApps(apps)
        else buildAllApps(apps).filter {
            it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    init {
        // NotificationListenerService'ten gelen badge sayılarını DB'ye yaz
        AppNotificationListenerService.badgeCounts
            .onEach { counts ->
                if (counts.isNotEmpty()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        counts.forEach { (pkg, count) ->
                            repository.updateNotificationCount(pkg, count)
                        }
                        // Servis bilgisi olmayan uygulamaların sayısını sıfırla
                        val knownPkgs = counts.keys
                        repository.getAllApps()
                            .filter { it.notificationCount > 0 && it.packageName !in knownPkgs }
                            .forEach { repository.updateNotificationCount(it.packageName, 0) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * onResume'da çağrılır: yüklü paket sayısı DB ile uyuşmuyorsa tam reconcile tetikler.
     * Paket sayısı eşitse sıfır IO — launcher hızı korunur.
     */
    fun reconcileIfNeeded(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installedCount = pm.getInstalledPackages(0)
                .count { it.applicationInfo != null &&
                    pm.getLaunchIntentForPackage(it.packageName) != null }
            val dbCount = repository.countApps()
            if (installedCount != dbCount) {
                Timber.d("reconcileIfNeeded: cihaz=$installedCount DB=$dbCount — tam reconcile başlatılıyor")
                loadAppsIfEmpty(context)
            }
        }
    }

    /** İlk açılışta DB boşsa tarar; her açılışta DB ↔ cihaz farkını temizler. */
    fun loadAppsIfEmpty(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val helper = PackageManagerHelper(context)
            val installed = helper.getInstalledApps(includeSystem = true, onlyLaunchable = true)
            val existing = repository.getAllApps()
            if (existing.isEmpty()) {
                Timber.d("DB boş — ${installed.size} uygulama yazılıyor")
                repository.insertApps(installed)
            } else {
                // Cihazda olmayan ama DB'de kalan uygulamaları temizle
                val installedPkgs = installed.map { it.packageName }.toSet()
                val stale = existing.filter { it.packageName !in installedPkgs }
                if (stale.isNotEmpty()) {
                    stale.forEach { repository.deleteApp(it.packageName) }
                    Timber.d("Reconcile: ${stale.size} eski uygulama silindi")
                }
                // DB'de olmayan yeni uygulamaları ekle
                val existingPkgs = existing.map { it.packageName }.toSet()
                val newApps = installed.filter { it.packageName !in existingPkgs }
                if (newApps.isNotEmpty()) {
                    repository.insertApps(newApps)
                    Timber.d("Reconcile: ${newApps.size} yeni uygulama eklendi")
                }
            }
        }
    }

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

    /** Paketi launcher üzerinden başlatır ve kullanım sayacını artırır. */
    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: run {
                Timber.w("launchApp: getLaunchIntent null for $packageName")
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            viewModelScope.launch(Dispatchers.IO) {
                repository.incrementUsageCount(packageName)
            }
        } catch (e: Exception) {
            Timber.e(e, "launchApp failed: $packageName")
        }
    }

    fun loadDockPackages(context: Context) {
        _dockPackages.value = DockPrefs.getDockPackages(context)
        // Kayıtlı klasör sırasını da yükle
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FOLDER_ORDER, null)
        if (!saved.isNullOrBlank()) {
            _folderOrder.value = saved.split(",").filter { it.isNotBlank() }
        }
    }

    fun reorderFolders(context: Context, newOrder: List<AppFolder>) {
        val ids = newOrder.map { it.category.categoryId }
        _folderOrder.value = ids
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_FOLDER_ORDER, ids.joinToString(",")).apply()
    }

    fun saveDockPackages(context: Context, packages: List<String>) {
        DockPrefs.saveDockPackages(context, packages)
        _dockPackages.value = packages
    }

    fun addToDock(context: Context, packageName: String) {
        val current = DockPrefs.getDockPackages(context)
        when {
            current.contains(packageName) -> _toastMessage.tryEmit("Uygulama zaten Dock'ta")
            current.size >= 4 -> _toastMessage.tryEmit("Dock dolu (max 4) — önce bir uygulama çıkar")
            else -> {
                DockPrefs.addToDock(context, packageName)
                _dockPackages.value = DockPrefs.getDockPackages(context)
                _toastMessage.tryEmit("Dock'a eklendi")
            }
        }
    }

    fun removeFromDock(context: Context, packageName: String) {
        DockPrefs.removeFromDock(context, packageName)
        _dockPackages.value = DockPrefs.getDockPackages(context)
    }

    fun updateAppCategory(packageName: String, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppCategory(packageName, categoryId)
        }
    }

    fun setAppHidden(packageName: String, hidden: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppHidden(packageName, hidden)
        }
    }

    fun onPackageRemoved(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteApp(packageName)
        }
    }

    /** Yeni kurulan veya güncellenen uygulamayı DB'ye ekler/günceller. */
    fun onPackageAdded(context: Context, packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val helper = PackageManagerHelper(context)
                val apps = helper.getInstalledApps(includeSystem = true, onlyLaunchable = true)
                val app = apps.firstOrNull { it.packageName == packageName } ?: return@launch
                repository.insertApps(listOf(app))
                Timber.d("onPackageAdded: $packageName eklendi/güncellendi")
            }.onFailure { Timber.e(it, "onPackageAdded failed: $packageName") }
        }
    }

    val hiddenApps: StateFlow<List<AppInfo>> = repository.getHiddenApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    /** UsageStatsManager'dan kullanım verilerini Room DB'ye senkronize eder. */
    fun syncUsageStats(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!UsageStatsHelper.hasPermission(context)) return@launch
            val counts = UsageStatsHelper.getUsageCounts(context, days = 30)
            counts.forEach { (pkg, ms) -> repository.updateUsageCount(pkg, ms) }
            val lastUsed = UsageStatsHelper.getLastUsedTimes(context, days = 90)
            lastUsed.forEach { (pkg, ts) -> repository.updateLastUsedTimestamp(pkg, ts) }
            Timber.d("UsageStats synced: ${counts.size} apps, ${lastUsed.size} lastUsed")
        }
    }

    /** Yüklü uygulamaların APK boyutlarını DB'ye senkronize eder (arka planda). */
    fun syncAppSizes(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            repository.getAllApps()
                .filter { it.appSizeBytes == 0L }
                .forEach { app ->
                    runCatching {
                        val info = pm.getApplicationInfo(app.packageName, 0)
                        val size = File(info.sourceDir).length()
                        if (size > 0L) repository.updateAppSize(app.packageName, size)
                    }
                }
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
