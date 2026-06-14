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
import com.armutlu.apporganizer.utils.WidgetHostManager
import com.armutlu.apporganizer.utils.WidgetPrefs
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
import java.util.concurrent.atomic.AtomicBoolean
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

    // Klasör sırası ve dock paketleri ilk yüklemede SharedPrefs'ten okunur;
    // sonraki resume'larda _dockPackages ViewModel metotlarıyla güncel tutulur — yeniden okuma gerekmez.
    @Volatile private var dockLoaded = false

    // Eş zamanlı çift loadAppsIfEmpty engelleyici — compareAndSet atomik check-then-set sağlar
    private val isLoadingApps = AtomicBoolean(false)

    // Favori paket seti — toggleFavorite() ile güncellenir, allApps ile combine edilir
    private val _favoritePkgs = MutableStateFlow<Set<String>>(emptySet())

    private val _allAppsOpen = MutableStateFlow(false)
    val allAppsOpen: StateFlow<Boolean> = _allAppsOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Eagerly: launcher her zaman arka planda çalışır — akış hiç durmamalı.
    // WhileSubscribed(5s) ile 5+ saniye sonra dönüşte kısa "yükleniyor" flaşı oluyordu.
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _openFolderId = MutableStateFlow<String?>(null)
    // folders flow'undan türetilir — kategori değişince FolderSheet anlık güncellenir
    val openFolder: StateFlow<AppFolder?> = combine(
        _openFolderId,
        folders
    ) { id, folderList ->
        if (id == null) null else folderList.firstOrNull { it.category.categoryId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { buildAllApps(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // NotificationListenerService'ten gelen badge sayilarini DB'ye yaz.
        // Tum bildirimler silindiginde counts bos map gelir — guard olmadan her durumda temizle.
        AppNotificationListenerService.badgeCounts
            .onEach { counts ->
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        counts.forEach { (pkg, count) ->
                            repository.updateNotificationCount(pkg, count)
                        }
                        val knownPkgs = counts.keys
                        val toReset = repository.getAllApps()
                            .filter { it.notificationCount > 0 && it.packageName !in knownPkgs }
                        if (toReset.isNotEmpty()) {
                            toReset.forEach { repository.updateNotificationCount(it.packageName, 0) }
                        }
                    }.onFailure { Timber.e(it, "badgeCounts observer hatası") }
                }
            }
            .launchIn(viewModelScope)

        AppNotificationListenerService.latestTexts
            .onEach { texts ->
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        texts.forEach { (pkg, text) ->
                            repository.updateNotificationText(pkg, text)
                        }
                        val knownPkgs = texts.keys
                        val toClean = repository.getAllApps()
                            .filter { it.notificationText.isNotBlank() && it.packageName !in knownPkgs }
                        if (toClean.isNotEmpty()) {
                            toClean.forEach { repository.updateNotificationText(it.packageName, "") }
                        }
                    }.onFailure { Timber.e(it, "latestTexts observer hatası") }
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
            runCatching {
                val pm = context.packageManager
                val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val installedCount = pm.queryIntentActivities(launchIntent, 0)
                    .mapTo(mutableSetOf()) { it.activityInfo.packageName }
                    .count { !PackageManagerHelper.shouldHide(it) }
                val dbCount = repository.countApps()
                if (installedCount != dbCount) {
                    Timber.d("reconcileIfNeeded: cihaz=$installedCount DB=$dbCount — tam reconcile başlatılıyor")
                    loadAppsIfEmpty(context)
                }
            }.onFailure { Timber.e(it, "reconcileIfNeeded hatası") }
        }
    }

    /** İlk açılışta DB boşsa tarar; her açılışta DB ↔ cihaz farkını temizler. */
    fun loadAppsIfEmpty(context: Context) {
        if (!isLoadingApps.compareAndSet(false, true)) return  // atomik: zaten çalışıyorsa dön
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
            } finally {
                isLoadingApps.set(false)
            }
        }
    }

    fun openFolder(folder: AppFolder) {
        _openFolderId.value = folder.category.categoryId
    }

    fun closeFolder() {
        _openFolderId.value = null
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
        // İlk yüklemede hem dock paketleri hem klasör sırası SharedPrefs'ten okunur.
        // Sonraki resume'larda _dockPackages, saveDockPackages/addToDock/removeFromDock ile
        // her zaman güncel — tekrar SharedPrefs okumaya gerek yok.
        if (!dockLoaded) {
            dockLoaded = true
            val newPackages = DockPrefs.getDockPackages(context)
            if (newPackages != _dockPackages.value) {
                _dockPackages.value = newPackages
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_FOLDER_ORDER, null)
            if (!saved.isNullOrBlank()) {
                val newOrder = saved.split(",").filter { it.isNotBlank() }
                if (newOrder != _folderOrder.value) {
                    _folderOrder.value = newOrder
                }
            }
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
        // SharedPrefs okumak yerine bellekteki listeyi kullan — dockLoaded sonrasi her zaman güncel
        val current = _dockPackages.value
        when {
            current.contains(packageName) -> _toastMessage.tryEmit("Uygulama zaten Dock'ta")
            current.size >= DOCK_MAX_SIZE -> _toastMessage.tryEmit("Dock dolu (max $DOCK_MAX_SIZE) — önce bir uygulama çıkar")
            else -> {
                val updated = current + packageName
                DockPrefs.saveDockPackages(context, updated)
                _dockPackages.value = updated
                _toastMessage.tryEmit("Dock'a eklendi")
            }
        }
    }

    fun removeFromDock(context: Context, packageName: String) {
        val updated = _dockPackages.value - packageName
        DockPrefs.saveDockPackages(context, updated)
        _dockPackages.value = updated
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
        // Icon cache'ten bu pakete ait tüm boyut varyantlarını temizle
        iconCacheInternal.snapshot().keys
            .filter { it.startsWith("${packageName}_") }
            .forEach { iconCacheInternal.remove(it) }
        // Silinen uygulama dock'taysa hemen kaldır — geri dönüşte kırık ikon görünmez
        val current = _dockPackages.value
        if (packageName in current) {
            _dockPackages.value = current - packageName
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteApp(packageName)
        }
    }

    /** Yeni kurulan veya güncellenen uygulamayı DB'ye ekler/günceller. */
    fun onPackageAdded(context: Context, packageName: String) {
        // Uygulama güncellemelerinde ikon değişmiş olabilir — cache'i temizle
        iconCacheInternal.snapshot().keys
            .filter { it.startsWith("${packageName}_") }
            .forEach { iconCacheInternal.remove(it) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Tam tarama yerine tek paket fetch: ~5x daha hızlı
                val helper = PackageManagerHelper(context)
                val app = helper.getAppInfo(packageName) ?: return@launch
                repository.insertApps(listOf(app))
                Timber.d("onPackageAdded: $packageName eklendi/güncellendi")
            }.onFailure { Timber.e(it, "onPackageAdded failed: $packageName") }
        }
    }

    // Widget ID listesi — SharedPrefs'ten yüklenir, ekleme/silmede güncellenir
    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    val widgetIds: StateFlow<List<Int>> = _widgetIds.asStateFlow()

    fun loadWidgetIds(context: Context) {
        _widgetIds.value = WidgetPrefs.getWidgetIds(context)
    }

    fun addWidgetId(context: Context, id: Int) {
        WidgetPrefs.addWidgetId(context, id)
        _widgetIds.value = WidgetPrefs.getWidgetIds(context)
    }

    fun removeWidgetId(context: Context, id: Int) {
        WidgetHostManager.deleteId(context, id)
        WidgetPrefs.removeWidgetId(context, id)
        _widgetIds.value = WidgetPrefs.getWidgetIds(context)
    }

    val hiddenApps: StateFlow<List<AppInfo>> = repository.getHiddenApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // Favori uygulamalar — _favoritePkgs + allApps combine ile reaktif StateFlow
    val favoriteApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .combine(_favoritePkgs) { apps, favPkgs ->
            apps.filter { it.packageName in favPkgs && !it.isHidden }
                .sortedBy { it.appName.lowercase(java.util.Locale("tr")) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** LauncherActivity.onCreate'de bir kez çağrılır; SharedPrefs'ten favori seti yüklenir. */
    fun initFavorites(context: Context) {
        _favoritePkgs.value = com.armutlu.apporganizer.utils.AppPrefs.getFavorites(context)
    }

    /** Favori toggle — SharedPrefs + reaktif StateFlow birlikte güncellenir. */
    fun toggleFavorite(context: Context, packageName: String) {
        val isFav = com.armutlu.apporganizer.utils.AppPrefs.isFavorite(context, packageName)
        if (isFav) {
            com.armutlu.apporganizer.utils.AppPrefs.removeFavorite(context, packageName)
        } else {
            com.armutlu.apporganizer.utils.AppPrefs.addFavorite(context, packageName)
        }
        _favoritePkgs.value = com.armutlu.apporganizer.utils.AppPrefs.getFavorites(context)
    }

    // En son kullanilan 4 uygulama — lastUsedTimestamp oncelikli, esitlerde usageCount ile sirala
    val suggestedApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { apps ->
            apps.filter { !it.isHidden && it.usageCount > 0 }
                .sortedWith(compareByDescending<AppInfo> { it.lastUsedTimestamp }.thenByDescending { it.usageCount })
                .take(4)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Son kullanilan 8 uygulama — RecentAppsRow icin, lastUsedTimestamp sirasinda
    val recentApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { apps ->
            apps.filter { !it.isHidden && it.lastUsedTimestamp > 0L }
                .sortedByDescending { it.lastUsedTimestamp }
                .take(8)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** UsageStatsManager'dan kullanım verilerini Room DB'ye senkronize eder. */
    fun syncUsageStats(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!UsageStatsHelper.hasPermission(context)) return@launch
            runCatching {
                val counts = UsageStatsHelper.getUsageCounts(context, days = 30)
                counts.forEach { (pkg, ms) -> repository.updateUsageCount(pkg, ms) }
                val lastUsed = UsageStatsHelper.getLastUsedTimes(context, days = 90)
                lastUsed.forEach { (pkg, ts) -> repository.updateLastUsedTimestamp(pkg, ts) }
                Timber.d("UsageStats synced: ${counts.size} apps, ${lastUsed.size} lastUsed")
            }.onFailure { Timber.e(it, "syncUsageStats hatası") }
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

    /** Bir uygulamaya kişisel not ekler/günceller. */
    fun saveAppNote(packageName: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomNotes(packageName, note.trim())
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
