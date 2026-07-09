package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.data.repository.SearchRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import com.armutlu.apporganizer.service.AppNotificationListenerService
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.DockPrefs
import com.armutlu.apporganizer.utils.DominantColorExtractor
import com.armutlu.apporganizer.utils.WidgetSuggestionEngine
import com.armutlu.apporganizer.utils.InsightCard
import com.armutlu.apporganizer.utils.InsightEngine
import com.armutlu.apporganizer.utils.PackageManagerHelper
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WidgetHostManager
import com.armutlu.apporganizer.utils.WidgetPrefs
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
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
internal fun buildFolders(apps: List<AppInfo>, categories: List<Category>): List<AppFolder> =
    categories
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
    private val repository: AppRepository,
    private val searchRepository: SearchRepository,
    private val packageManagerHelper: PackageManagerHelper,
    private val classifier: com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
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

    // launchApp'ta başlatılan son paket — onResume'da timestamp güncellenir (startActivity process'i askıya aldığında coroutine tamamlanamıyor)
    @Volatile private var lastLaunchedPkg: String? = null
    @Volatile private var lastLaunchedTs: Long = 0L

    // Favori paket seti — toggleFavorite() ile güncellenir, allApps ile combine edilir
    private val _favoritePkgs = MutableStateFlow<Set<String>>(emptySet())

    private val _allAppsOpen = MutableStateFlow(false)
    val allAppsOpen: StateFlow<Boolean> = _allAppsOpen.asStateFlow()

    private val _focusSearchOnOpen = MutableStateFlow(false)
    val focusSearchOnOpen: StateFlow<Boolean> = _focusSearchOnOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<Map<SourceType, List<SearchDocument>>> = _searchQuery
        .debounce(250)
        .map { query ->
            val trimmed = query.trim()
            if (trimmed.length < 2) {
                emptyMap()
            } else {
                runCatching { searchRepository.search(trimmed, limit = 24) }
                    .onFailure { Timber.w(it, "Launcher search results failed") }
                    .getOrDefault(emptyMap())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categories: StateFlow<List<Category>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Category.getDefaultCategories())

    // Eagerly: launcher her zaman arka planda çalışır — akış hiç durmamalı.
    // WhileSubscribed(5s) ile 5+ saniye sonra dönüşte kısa "yükleniyor" flaşı oluyordu.
    val folders: StateFlow<List<AppFolder>> = combine(
        repository.getAllAppsFlow(),
        categories,
        _folderOrder
    ) { apps, categoryList, order ->
        val built = buildFolders(apps, categoryList)
        if (order.isEmpty()) built
        else {
            val orderMap = order.mapIndexed { i, id -> id to i }.toMap()
            built.sortedBy { orderMap[it.category.categoryId] ?: Int.MAX_VALUE }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _openFolderId = MutableStateFlow<String?>(null)
    // folders flow'undan türetilir — kategori değişince FolderScreen anlık güncellenir
    val openFolder: StateFlow<AppFolder?> = combine(
        _openFolderId,
        folders
    ) { id, folderList ->
        if (id == null) null else folderList.firstOrNull { it.category.categoryId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { buildAllApps(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Room'dan ilk emisyon geldi mi — cold resume'da yanlis "yukleniyor" flasini onler (Fix 3):
    // process yeniden yaratildiginda folders/allApps baslangicta emptyList() ile basliyor,
    // ilk Room emit'ine kadar HomeScreen "Uygulamalar yukleniyor..." flasi gosteriyordu.
    val initialLoadDone: StateFlow<Boolean> = repository.getAllAppsFlow()
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @OptIn(FlowPreview::class)
    val filteredAllApps: StateFlow<List<AppInfo>> = combine(
        repository.getAllAppsFlow(),
        _searchQuery.debounce(300)
    ) { apps, q ->
        val query = q.trim().lowercase(java.util.Locale("tr"))
        if (query.isEmpty()) buildAllApps(apps)
        else buildAllApps(apps).filter {
            it.appName.lowercase(java.util.Locale("tr")).contains(query) ||
            it.packageName.lowercase().contains(query)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultCategories()
        }
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

        // Klasör Rengi Otomatik — renk atanmamış klasörler için ikonlardan dominant renk çıkar
        folders
            .onEach { folderList ->
                val ctx = getApplication<Application>()
                if (!AppPrefs.isAutoFolderColorEnabled(ctx)) return@onEach
                val existing = AppPrefs.getFolderCustomColors(ctx)
                folderList.filter { folder ->
                    folder.apps.isNotEmpty() && existing[folder.category.categoryId].isNullOrEmpty()
                }.forEach { folder ->
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching {
                            val pkgs = folder.apps.map { it.packageName }
                            val hex = DominantColorExtractor.extractFromPackages(ctx, pkgs)
                            if (hex != null) AppPrefs.setFolderCustomColor(ctx, folder.category.categoryId, hex)
                        }.onFailure { Timber.w(it, "auto-color hatası: ${folder.category.categoryId}") }
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
            runCatching {
                val pm = context.packageManager
                val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val installedCount = pm.queryIntentActivities(launchIntent, 0)
                    .mapTo(mutableSetOf()) { it.activityInfo.packageName }
                    .count { !PackageManagerHelper.shouldHide(it) }
                val dbCount = repository.countApps()
                if (installedCount != dbCount) {
                    Timber.d("reconcileIfNeeded: cihaz=$installedCount DB=$dbCount — tam reconcile başlatılıyor")
                    loadAppsIfEmpty()
                }
            }.onFailure { Timber.e(it, "reconcileIfNeeded hatası") }
        }
    }

    /** İlk açılışta DB boşsa tarar; her açılışta DB ↔ cihaz farkını temizler. */
    fun loadAppsIfEmpty() {
        if (!isLoadingApps.compareAndSet(false, true)) return  // atomik: zaten çalışıyorsa dön
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val installed = packageManagerHelper.getInstalledApps(includeSystem = true, onlyLaunchable = true)
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
        _focusSearchOnOpen.value = false
        _allAppsOpen.value = true
    }

    fun openAllAppsWithSearch() {
        _focusSearchOnOpen.value = true
        _allAppsOpen.value = true
    }

    fun resetFocusSearchOnOpen() {
        _focusSearchOnOpen.value = false
    }

    fun closeAllApps() {
        _allAppsOpen.value = false
        _focusSearchOnOpen.value = false
        _searchQuery.value = ""
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    /**
     * S2: Ana ekran aramasında rehber izni verilince kişi kaynağının FTS indeksini
     * arka planda başlatır (ContactsIndexer.indexAll + ContentObserver kaydı).
     * AppPrefs.setSearchSourceContactsEnabled(true) çağrı yerinde (HomeAppSearchBar) yapılır.
     */
    fun enableContactsSearchSource() {
        viewModelScope.launch {
            runCatching { searchRepository.enableContactsSource() }
                .onFailure { Timber.w(it, "Kişi arama kaynağı etkinleştirilemedi") }
        }
    }

    /** Paketi launcher üzerinden başlatır ve kullanım sayacını artırır. */
    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: run {
                Timber.w("launchApp: getLaunchIntent null for $packageName")
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val ts = System.currentTimeMillis()
            lastLaunchedPkg = packageName
            lastLaunchedTs = ts
            context.startActivity(intent)
            viewModelScope.launch(Dispatchers.IO) {
                repository.incrementUsageCount(packageName)
                repository.updateLastUsedTimestamp(packageName, ts)
            }
        } catch (e: Exception) {
            Timber.e(e, "launchApp failed: $packageName")
        }
    }

    /** onResume'da çağrılır — son başlatılan uygulamanın timestamp'ini garantiler (startActivity süreci askıya aldığında coroutine tamamlanamayabilir). */
    fun refreshLastLaunched() {
        val pkg = lastLaunchedPkg ?: return
        val ts = lastLaunchedTs
        lastLaunchedPkg = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLastUsedTimestamp(pkg, ts)
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
        val removed = DockPrefs.removeFromDock(context, packageName)
        if (removed) {
            _dockPackages.value = _dockPackages.value - packageName
            _toastMessage.tryEmit("Dock'tan kaldirildi")
        } else {
            _toastMessage.tryEmit("Dock'ta bu uygulama bulunamadi")
        }
    }

    fun updateAppCategory(packageName: String, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppCategory(packageName, categoryId)
            AppPrefs.setManualCategoryOverride(getApplication(), packageName, categoryId)
            // Room Flow'un emit etmesi için küçük gecikme — stale UI race condition önlemi
            kotlinx.coroutines.delay(50)
            _folderOrder.update { it.toList() }
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
    fun onPackageAdded(@Suppress("UNUSED_PARAMETER") context: Context, packageName: String) {
        // Uygulama güncellemelerinde ikon değişmiş olabilir — cache'i temizle
        iconCacheInternal.snapshot().keys
            .filter { it.startsWith("${packageName}_") }
            .forEach { iconCacheInternal.remove(it) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                // Tam tarama yerine tek paket fetch: ~5x daha hızlı
                val app = packageManagerHelper.getAppInfo(packageName) ?: return@launch
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

    fun reorderWidgets(context: Context, newOrder: List<Int>) {
        WidgetPrefs.saveWidgetIds(context, newOrder)
        _widgetIds.value = newOrder
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

    /** Favori toggle — memory-first: StateFlow anlık güncellenir, SharedPrefs'e async persist. */
    fun toggleFavorite(context: Context, packageName: String) {
        val current = _favoritePkgs.value
        val updated = if (packageName in current) current - packageName else current + packageName
        _favoritePkgs.value = updated
        // SharedPrefs'e persist — async, race condition yok (memory zaten güncel)
        if (packageName in current) {
            com.armutlu.apporganizer.utils.AppPrefs.removeFavorite(context, packageName)
        } else {
            com.armutlu.apporganizer.utils.AppPrefs.addFavorite(context, packageName)
        }
    }

    // 30 dakikada bir tick — suggestedApps'ın time-slot skorunu yenilemek için
    private val _suggestionTick = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30 * 60 * 1000L)
        }
    }

    // Memory cache: SADECE pahalı UsageStats skorları 30 dakika cache'lenir.
    // Uygulama listesi değişimi (kurulum/güncelleme/silme) her emisyonda anında yansır
    // — stale liste bug'ı (öneriler yenilenmiyor) bu ayrımla çözüldü.
    @Volatile private var cachedScores: Map<String, Float>? = null
    @Volatile private var cacheTimestamp: Long = 0L
    private val CACHE_DURATION_MS = 30 * 60 * 1000L  // 30 dakika

    // Akıllı öneriler — UsageScore v2: recency+frequency+timeSlot + dock boost
    // Dock kararlılığı (Fix 1): giriş akışı sadece sıralamayı etkileyen alanlar değişince
    // yeniden emit eder — her bildirimde updateNotificationCount() DB'ye yazınca
    // getAllAppsFlow() yeniden emit oluyordu, bu da suggestedApps'i (ve dolayısıyla
    // contextualDockPackages'ı) her seferinde yeniden sıralayıp dock'u kararsız yapıyordu.
    val suggestedApps: StateFlow<List<AppInfo>> = combine(
        repository.getAllAppsFlow()
            .distinctUntilChanged { old, new ->
                old.size == new.size && old.zip(new).all { (a, b) ->
                    a.packageName == b.packageName && a.usageCount == b.usageCount &&
                    a.lastUsedTimestamp == b.lastUsedTimestamp && a.isHidden == b.isHidden
                }
            },
        _suggestionTick
    ) { apps, _ ->
        val now = System.currentTimeMillis()
        val ctx = getApplication<Application>()
        val visible = apps.filter { !it.isHidden }
        // UsageScore v2 boost faktörleri (anlık snapshot)
        val dockPkgs = _dockPackages.value
        val favPkgs = _favoritePkgs.value
        if (UsageStatsHelper.hasPermission(ctx)) {
            val baseScores = cachedScores.takeIf { it != null && now - cacheTimestamp <= CACHE_DURATION_MS }
                ?: UsageStatsHelper.getWeightedScores(ctx, days = 28).also {
                    cachedScores = it
                    cacheTimestamp = now
                }
            // v2 boost: dock/favorite +0.15. Bildirim boost'u (+0.2) KALDIRILDI (Fix 1) —
            // dock kararlılığı bildirim tazeliğinden daha önemli; bildirim geldikçe
            // sıralama değişip contextualDockPackages'taki akıllı slotlar zıplıyordu.
            val boosted = baseScores.mapValues { (pkg, score) ->
                var s = score
                if (dockPkgs.contains(pkg) || favPkgs.contains(pkg)) s += 0.15f
                s
            }
            visible
                .filter { boosted.containsKey(it.packageName) }
                .sortedByDescending { boosted[it.packageName] ?: 0f }
                .take(4)
                .ifEmpty {
                    visible.filter { it.usageCount > 0 }
                        .sortedWith(compareByDescending<AppInfo> { it.lastUsedTimestamp }.thenByDescending { it.usageCount })
                        .take(4)
                }
        } else {
            visible.filter { it.usageCount > 0 }
                .sortedWith(compareByDescending<AppInfo> { it.lastUsedTimestamp }.thenByDescending { it.usageCount })
                .take(4)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Assistant Kartları — her refresh'te rastgele seçim, tekrar önleme
    private val _insightCards = MutableStateFlow<List<InsightCard>>(emptyList())
    val insightCards: StateFlow<List<InsightCard>> = _insightCards.asStateFlow()

    fun refreshInsights(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = repository.getAllApps().filter { !it.isHidden }
            _insightCards.value = InsightEngine.generate(
                apps = apps,
                categories = Category.getDefaultCategories(),
                badgeCounts = AppNotificationListenerService.badgeCounts.value,
                context = context,
            )
        }
    }

    // Dokunulan/görülen ticker haberleri bu oturum boyunca tekrar gösterilmez (D226) —
    // aksi halde her recomposition'da aynı en-büyük-5-klasör listesi sabit kalıyordu.
    private val _dismissedTickerKeys = MutableStateFlow<Set<String>>(emptySet())

    fun dismissTickerItem(key: String) {
        _dismissedTickerKeys.value = _dismissedTickerKeys.value + key
    }

    /** TickerSpec.routeKey -> Routes sabiti eslemesi (TickerComposer Routes'a bagimli degil). */
    private fun resolveTickerRoute(routeKey: String?): String? = when (routeKey) {
        "DASHBOARD" -> com.armutlu.apporganizer.presentation.navigation.Routes.DASHBOARD
        "NOTIFICATION_REPORT" -> com.armutlu.apporganizer.presentation.navigation.Routes.NOTIFICATION_REPORT
        "APP_LIST" -> com.armutlu.apporganizer.presentation.navigation.Routes.APP_LIST
        "SETTINGS" -> com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS
        else -> null
    }

    /** Haftalik Rapor (Wrapped) teaser haberi — hafta sonu ve pazartesi gorunur, dokununca rapor acilir. */
    private fun buildWrappedTicker(): List<TickerItem> = runCatching {
        val ctx = getApplication<Application>()
        if (!AppPrefs.isWrappedEnabled(ctx)) return@runCatching emptyList()
        val day = java.time.LocalDate.now().dayOfWeek
        val weekendOrMonday = day == java.time.DayOfWeek.SATURDAY ||
            day == java.time.DayOfWeek.SUNDAY || day == java.time.DayOfWeek.MONDAY
        if (!weekendOrMonday) return@runCatching emptyList()
        listOf(TickerItem(
            text = "Haftalık raporun hazır — skorunu ve rozetlerini gör",
            emoji = "🎁",
            route = com.armutlu.apporganizer.presentation.navigation.Routes.WRAPPED_REPORT
        ))
    }.getOrDefault(emptyList())

    /** Arama istatistigi haberi — SearchStatsPrefs anonim agregatlarindan uretilir; 5+ arama olunca gorunur. */
    private fun buildSearchStatsTicker(): List<TickerItem> = runCatching {
        val ctx = getApplication<Application>()
        if (!AppPrefs.isSearchStatsEnabled(ctx)) return@runCatching emptyList()
        val s = com.armutlu.apporganizer.utils.SearchStatsPrefs.getSummary(ctx)
        if (s.totalSearches < 5) return@runCatching emptyList()
        val text = if (s.totalClicks > 0) {
            val firstPct = s.firstResultClicks * 100 / s.totalClicks
            "${s.totalSearches} arama yaptın, %$firstPct ilk sonuçta buldu — detay için dokun"
        } else {
            "${s.totalSearches} arama yaptın — istatistikler için dokun"
        }
        listOf(TickerItem(
            text = text,
            emoji = "🔎",
            route = com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS_STATS
        ))
    }.getOrDefault(emptyList())

    private fun com.armutlu.apporganizer.utils.TickerSpec.toTickerItem() = TickerItem(
        text = text,
        emoji = emoji.ifBlank { "📰" },
        categoryId = categoryId,
        route = resolveTickerRoute(routeKey)
    )

    // Haber şeridi (ticker) — klasör istatistikleri + içgörüler + bildirim özeti + saat bazlı
    // selamlama + unutulan uygulama + günün şampiyonu + ipucu (TickerComposer, D227 çeşitlilik).
    // Dokunma hedefleri: klasör haberi → FolderScreen, bildirim haberi → Bildirim Raporu, içgörü → Dashboard.
    val tickerItems: StateFlow<List<TickerItem>> = combine(
        folders,
        insightCards,
        AppNotificationListenerService.badgeCounts,
        _dismissedTickerKeys
    ) { folderList, cards, badges, dismissed ->
        val folderSnapshots = folderList.map { f ->
            com.armutlu.apporganizer.utils.FolderSnapshot(
                categoryId = f.category.categoryId,
                categoryName = f.category.categoryName,
                emoji = f.category.iconEmoji,
                appCount = f.apps.size,
            )
        }
        val appSnapshots = folderList.flatMap { it.apps }.map { a ->
            com.armutlu.apporganizer.utils.AppSnapshot(
                packageName = a.packageName,
                appName = a.appName,
                usageCount = a.usageCount,
                lastUsedTimestamp = a.lastUsedTimestamp,
            )
        }
        val insightSnapshots = cards.map { card ->
            com.armutlu.apporganizer.utils.InsightSnapshot(
                id = card.id,
                message = card.message,
                categoryId = card.categoryId,
            )
        }
        // Dusuk guvenli otomatik kategorileme uyarisi (K3, Dongu 227, Fable danismanligi) —
        // getConfidence() mevcuttu ama hicbir UX'e baglanmamisti. Esik: 60 altinda "belirsiz" sayilir.
        val lowConfidenceCount = folderList.sumOf { f ->
            f.apps.count { classifier.getConfidence(it, f.category.categoryId) < 60 }
        }
        val totalNotif = badges.values.sum()

        val composed = com.armutlu.apporganizer.utils.TickerComposer.compose(
            folders = folderSnapshots,
            apps = appSnapshots,
            badgeTotal = totalNotif,
            insights = insightSnapshots,
            lowConfidenceCount = lowConfidenceCount,
            nowMillis = System.currentTimeMillis(),
        ).map { it.toTickerItem() } + buildSearchStatsTicker() + buildWrappedTicker()

        val visible = composed.filterNot { it.key in dismissed }
        // Hepsi dismiss edildiyse bu oturumda haberler tükendi demektir — sıfırla ki
        // ticker boş kalmasın (yeni klasör/içgörü verisi geldiğinde zaten otomatik güncellenir).
        if (visible.isEmpty()) composed else visible
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Contextual Dock — 2 sabit (kullanici) + 2 akilli öneri (saat/gun/kullanim)
    val contextualDockPackages: StateFlow<List<String>> = combine(
        dockPackages,
        suggestedApps
    ) { fixed, suggested ->
        val ctx = getApplication<Application>()
        if (!AppPrefs.isContextualDockEnabled(ctx)) return@combine fixed
        val fixedSlots = fixed.take(2)
        val smartSlots = suggested
            .map { it.packageName }
            .filter { it !in fixedSlots }
            .take(2)
        fixedSlots + smartSlots
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Son kullanilan 4 uygulama — RecentAppsRow icin, lastUsedTimestamp sirasinda
    val recentApps: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .map { apps ->
            apps.filter { !it.isHidden && it.lastUsedTimestamp > 0L }
                .sortedByDescending { it.lastUsedTimestamp }
                .take(4)
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
                // IfNewer: launchApp'ın anlık timestamp'ini daha eski UsageStats verisiyle ezme
                lastUsed.forEach { (pkg, ts) -> repository.updateLastUsedTimestampIfNewer(pkg, ts) }
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

    // Widget öneri listesi — en çok kullanılan ve widget'ı olan uygulamalar
    val widgetSuggestions = repository.getAllAppsFlow()
        .map { apps ->
            WidgetSuggestionEngine.getSuggestions(getApplication(), apps.filter { !it.isHidden })
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun dispatchGestureAction(context: Context, action: AppPrefs.GestureAction) {
        when (action) {
            AppPrefs.GestureAction.OPEN_DRAWER       -> openAllApps()
            AppPrefs.GestureAction.OPEN_SEARCH       -> openAllAppsWithSearch()
            AppPrefs.GestureAction.OPEN_APP_MANAGER  -> openManager(context)
            AppPrefs.GestureAction.LAUNCH_CAMERA     -> {
                runCatching {
                    val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            AppPrefs.GestureAction.DO_NOTHING        -> Unit
        }
    }
}
