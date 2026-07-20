package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.data.repository.SearchRepository
import com.armutlu.apporganizer.domain.common.valueOrNull
import com.armutlu.apporganizer.domain.home.HomeIntelligenceCoordinator
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.RefreshReason
import com.armutlu.apporganizer.domain.home.SmartTickerItem
import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.home.TickerActionRouter
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
import com.armutlu.apporganizer.utils.SharedPrefsSuggestionHistoryStore
import com.armutlu.apporganizer.utils.SuggestionCoordinator
import com.armutlu.apporganizer.utils.UsageStatsHelper
import com.armutlu.apporganizer.utils.WidgetHostManager
import com.armutlu.apporganizer.utils.WidgetPrefs
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
private const val RECENT_NOTIFICATIONS_WINDOW_MS = 24L * 60L * 60L * 1000L

internal val DOCK_MAX_SIZE: Int
    get() = DockPrefs.MAX_SLOTS

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

/**
 * LauncherViewModel.filteredAllApps ile senkron tutulmalı — searchQuery'e göre uygulama
 * adı/paket adı filtreler (Türkçe locale-aware lowercase, D0 kuralı: contains(ignoreCase)
 * Türkçe'de güvenilmez). Pure function — sayfa/pager state'inden tamamen bağımsızdır.
 */
internal fun filterAllAppsByQuery(sortedApps: List<AppInfo>, query: String): List<AppInfo> {
    val trimmed = query.trim().lowercase(java.util.Locale("tr"))
    if (trimmed.isEmpty()) return sortedApps
    return sortedApps.filter {
        it.appName.lowercase(java.util.Locale("tr")).contains(trimmed) ||
        it.packageName.lowercase().contains(trimmed)
    }
}

/**
 * EX01 — "Bugün Yüklenenler" filtresi. Pure function — Android bağımlılığı yok, birim
 * testlerinden doğrudan çağrılabilir. [dayStartInclusive]/[dayEndExclusive] çağıran taraf
 * PeriodBoundaryResolver.currentDay() ile üretir (yerel gün sınırı, DST-safe).
 */
internal fun filterTodayInstalledApps(
    apps: List<AppInfo>,
    dayStartInclusive: Long,
    dayEndExclusive: Long,
): List<AppInfo> =
    apps.filter { !it.isHidden && it.firstInstalledTime in dayStartInclusive until dayEndExclusive }
        .sortedByDescending { it.firstInstalledTime }

internal fun fillDockSuggestions(
    slotApps: List<AppInfo>,
    fallbackApps: List<AppInfo>,
    maxSize: Int = DOCK_MAX_SIZE
): List<AppInfo> {
    val slotPicks = slotApps.take(maxSize)
    if (slotPicks.size >= maxSize) return slotPicks
    val pickedPackages = slotPicks.map { it.packageName }.toSet()
    return (slotPicks + fallbackApps.filter { it.packageName !in pickedPackages }).take(maxSize)
}

internal fun buildContextualDockPackages(
    fixed: List<String>,
    suggested: List<String>,
    contextualEnabled: Boolean,
    maxSize: Int = DOCK_MAX_SIZE
): List<String> {
    val fixedSlots = fixed.take(maxSize)
    if (!contextualEnabled || fixedSlots.size >= maxSize) return fixedSlots
    val smartSlots = suggested
        .filter { it !in fixedSlots }
        .take(maxSize - fixedSlots.size)
    return fixedSlots + smartSlots
}

@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val repository: AppRepository,
    private val searchRepository: SearchRepository,
    private val notificationEventDao: NotificationEventDao,
    private val packageManagerHelper: PackageManagerHelper,
    private val classifier: com.armutlu.apporganizer.domain.usecase.classify.AppClassifier,
    private val homeIntelligenceCoordinator: HomeIntelligenceCoordinator,
) : AndroidViewModel(application) {

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Kullanıcı tarafından drag&drop ile değiştirilen klasör sırası (categoryId listesi)
    private val _folderOrder = MutableStateFlow<List<String>>(emptyList())

    private val _dockPackages = MutableStateFlow<List<String>>(emptyList())
    val dockPackages: StateFlow<List<String>> = _dockPackages.asStateFlow()

    // Klasör sırası ilk yüklemede okunur. Dock ise Settings/restore gibi ViewModel dışı
    // yazımları da yakalamak için her onResume'da DockPrefs ile uzlaştırılır.
    @Volatile private var dockLoaded = false
    @Volatile private var socialFolderDockChecked = false

    // Eş zamanlı çift loadAppsIfEmpty engelleyici — compareAndSet atomik check-then-set sağlar
    private val isLoadingApps = AtomicBoolean(false)

    // launchApp'ta başlatılan son paket — onResume'da timestamp güncellenir (startActivity process'i askıya aldığında coroutine tamamlanamıyor)
    @Volatile private var lastLaunchedPkg: String? = null
    @Volatile private var lastLaunchedTs: Long = 0L

    // Favori paket seti — toggleFavorite() ile güncellenir, allApps ile combine edilir
    private val _favoritePkgs = MutableStateFlow<Set<String>>(emptySet())

    private val _allAppsOpen = MutableStateFlow(false)
    val allAppsOpen: StateFlow<Boolean> = _allAppsOpen.asStateFlow()

    // Döngü P12 — Home tuşu ham sinyali. LauncherActivity.onNewIntent() All Apps zaten açıksa
    // (kapatıp) erken döner ve BU flow'a hiç emit ETMEZ (roadmap madde 1, HomeCommandPolicy.kt
    // dosya başı notu) — dolayısıyla bu flow'u toplayan taraf (HomeScreen) `allAppsOpen==false`
    // varsayımıyla HomeCommandPolicy.resolveHomeCommand() çağırabilir. Search/modal açık/kapalı
    // bilgisi HomeScreen'in kendi local state'inde yaşadığı için komut ÇÖZÜMÜ (resolveHomeCommand)
    // burada değil, HomeScreen'in bu flow'u topladığı LaunchedEffect içinde yapılır.
    private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homePressed: SharedFlow<Unit> = _homePressed.asSharedFlow()

    /** LauncherActivity.onNewIntent() tarafından çağrılır — All Apps kapalıyken Home basışı. */
    fun onHomePressed() {
        _homePressed.tryEmit(Unit)
    }

    private val _focusSearchOnOpen = MutableStateFlow(false)
    val focusSearchOnOpen: StateFlow<Boolean> = _focusSearchOnOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val allAppsSource: StateFlow<List<AppInfo>> = repository.getAllAppsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        allAppsSource,
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

    val allApps: StateFlow<List<AppInfo>> = allAppsSource
        .map { buildAllApps(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Room'dan ilk emisyon geldi mi — cold resume'da yanlis "yukleniyor" flasini onler (Fix 3):
    // process yeniden yaratildiginda folders/allApps baslangicta emptyList() ile basliyor,
    // ilk Room emit'ine kadar HomeScreen "Uygulamalar yukleniyor..." flasi gosteriyordu.
    val initialLoadDone: StateFlow<Boolean> = allAppsSource
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recentNotificationCounts: StateFlow<Map<String, Int>> = notificationEventDao
        .observeCountsSince(System.currentTimeMillis() - RECENT_NOTIFICATIONS_WINDOW_MS)
        .map { counts -> counts.associate { it.packageName to it.count } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val recentNotificationApps: StateFlow<List<AppInfo>> = combine(
        allAppsSource,
        recentNotificationCounts
    ) { apps, counts ->
        if (counts.isEmpty()) emptyList()
        else apps
            .filter { !it.isHidden && (counts[it.packageName] ?: 0) > 0 }
            .sortedByDescending { counts[it.packageName] ?: 0 }
            .take(4)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // P1.3: Saat bazli kisi onerileri altyapisi. Sadece ViewModel + engine + logging burada -
    // gorunur UI EKLENMEZ (P1.2 bu akisi tuketecek, cakismayi onlemek icin).
    // İzin yoksa veya ayar kapaliysa veya SearchCache'te kisi verisi yoksa BOS liste doner.
    private val _suggestedContactsRefresh = MutableStateFlow(0)
    val suggestedContacts: StateFlow<List<com.armutlu.apporganizer.utils.SearchCache.ContactEntry>> =
        _suggestedContactsRefresh.map {
            val context = getApplication<Application>()
            if (!AppPrefs.isContactSuggestionsEnabled(context)) return@map emptyList()
            val events = com.armutlu.apporganizer.utils.ContactActionPrefs.getAll(context)
            val suggestedIds = com.armutlu.apporganizer.domain.usecase.contacts.ContactSuggestionEngine
                .suggest(events)
            if (suggestedIds.isEmpty()) return@map emptyList()
            val contactsById = com.armutlu.apporganizer.utils.SearchCache.getContactList()
                .associateBy { it.id.toString() }
            suggestedIds.mapNotNull { contactsById[it] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Ayarlar/arama akisindan sonra oneri listesini yeniden hesaplatmak icin cagirilabilir. */
    fun refreshSuggestedContacts() {
        _suggestedContactsRefresh.update { it + 1 }
    }

    @OptIn(FlowPreview::class)
    val filteredAllApps: StateFlow<List<AppInfo>> = combine(
        allAppsSource,
        _searchQuery.debounce(300)
    ) { apps, q -> filterAllAppsByQuery(buildAllApps(apps), q) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultCategories()
        }
        // Dongu H02 — HomeIntelligenceCoordinator baglantisi: yalniz refresh tetikleme.
        // Mevcut ticker/skor akislarina (tickerItems, homePulseSummary, suggestedApps vb.)
        // DOKUNULMADI — bunlar D00/T dongulerinde bu koordinatore tasinacak.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { homeIntelligenceCoordinator.refresh(RefreshReason.APP_START) }
                .onFailure { Timber.w(it, "HomeIntelligenceCoordinator APP_START refresh hatası") }
        }
        // P0.5: NotificationListenerService'ten gelen AKTIF bildirim sayilarini ham haliyle
        // DB'ye yazmak yerine "okunmamis" modelinden gecir. "Aktif sistem bildirimi" ile
        // "kullanici henuz gormedi" ayni sey degil: kullanici uygulamayi actiktan sonra bile
        // sistem bildirimi launcher tarafindan iptal EDILMIYOR (bilincli — kullanicinin
        // sistem bildirimleri silinmemeli), bu yuzden badge'in "okundu" bilgisini ayrica
        // NotificationReadPrefs.lastReadAt (launchApp'ta yazilir) ile hesapliyoruz.
        // Tum bildirimler silindiginde counts bos map gelir — guard olmadan her durumda temizle.
        combine(
            AppNotificationListenerService.badgeCounts,
            AppNotificationListenerService.lastPostedAt,
        ) { active, posted -> active to posted }
            .onEach { (active, posted) ->
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        val ctx = getApplication<Application>()
                        val lastReadAt = com.armutlu.apporganizer.utils.NotificationReadPrefs.getAll(ctx)
                        val unread = com.armutlu.apporganizer.domain.usecase.notification.UnreadNotificationModel
                            .computeUnreadCounts(active, posted, lastReadAt)
                        repository.updateNotificationCounts(unread)
                        // active haritasindaki ama unread'de kalmayan (okunmus) paketler + tum
                        // bildirimleri silinen paketler icin DB'deki sayaci sifirla.
                        val knownUnreadPkgs = unread.keys
                        val toReset = repository.getAllApps()
                            .filter { it.notificationCount > 0 && it.packageName !in knownUnreadPkgs }
                        if (toReset.isNotEmpty()) {
                            repository.updateNotificationCounts(toReset.associate { it.packageName to 0 })
                        }
                    }.onFailure { Timber.e(it, "badgeCounts observer hatası") }
                }
            }
            .launchIn(viewModelScope)

        AppNotificationListenerService.latestTexts
            .onEach { texts ->
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        repository.updateNotificationTexts(texts)
                        val knownPkgs = texts.keys
                        val toClean = repository.getAllApps()
                            .filter { it.notificationText.isNotBlank() && it.packageName !in knownPkgs }
                        if (toClean.isNotEmpty()) {
                            repository.updateNotificationTexts(toClean.associate { it.packageName to "" })
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
     * Launcher acilisinda Room katalogunu birincil kaynak olarak kullanir.
     * Tam tarama sadece DB bos, katalog surumu eski veya dusuk frekansli fallback gerektiginde calisir.
     */
    fun reconcileIfNeeded(context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = repository.getAllApps()
                val needsRefresh = existing.isEmpty() || !AppPrefs.isAppCatalogSchemaCurrent(context)
                if (needsRefresh) {
                    Timber.d(
                        "reconcileIfNeeded: full catalog sync baslatiliyor " +
                            "(empty=${existing.isEmpty()}, schemaCurrent=${AppPrefs.isAppCatalogSchemaCurrent(context)})"
                    )
                    loadAppsIfEmpty()
                } else {
                    onSuccess()
                }
            }.onFailure { Timber.e(it, "reconcileIfNeeded hatası") }
        }
    }

    /** Tam katalog taramasi — sadece bootstrap, surum gecisi ve dusuk frekansli fallback icin. */
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
                    val installedByPkg = installed.associateBy { it.packageName }
                    existing.forEach { dbApp ->
                        val installedApp = installedByPkg[dbApp.packageName] ?: return@forEach
                        if (
                            dbApp.appName != installedApp.appName ||
                            dbApp.appFileName != installedApp.appFileName ||
                            dbApp.isSystemApp != installedApp.isSystemApp ||
                            dbApp.lastUpdatedTime != installedApp.lastUpdatedTime ||
                            dbApp.versionName != installedApp.versionName ||
                            dbApp.targetSdkVersion != installedApp.targetSdkVersion
                        ) {
                            repository.updateApp(
                                dbApp.copy(
                                    appName = installedApp.appName,
                                    appFileName = installedApp.appFileName,
                                    isSystemApp = installedApp.isSystemApp,
                                    lastUpdated = System.currentTimeMillis(),
                                    lastUpdatedTime = installedApp.lastUpdatedTime,
                                    versionName = installedApp.versionName,
                                    targetSdkVersion = installedApp.targetSdkVersion,
                                    iconUrl = installedApp.iconUrl,
                                )
                            )
                        }
                    }
                }
                AppPrefs.markAppCatalogSchemaCurrent(getApplication())
                AppPrefs.markReconciled(getApplication())
            } finally {
                isLoadingApps.set(false)
            }
        }
    }

    fun openFolder(folder: AppFolder) {
        _openFolderId.value = folder.category.categoryId
    }

    /**
     * F6: cekmece arama sonucundaki kategori satirindan klasor acma — categoryId ile.
     * openFolder StateFlow'u zaten id'yi folders listesinden cozer; klasor yoksa
     * (bos kategori) hicbir sey acilmaz, sessizce yok sayilir.
     */
    fun openFolderByCategoryId(categoryId: String) {
        _openFolderId.value = categoryId
    }

    fun openAdjacentFolder(next: Boolean): Boolean {
        val currentId = _openFolderId.value ?: return false
        val folderList = folders.value
        if (folderList.size < 2) return false
        val currentIndex = folderList.indexOfFirst { it.category.categoryId == currentId }
        if (currentIndex == -1) return false
        val targetIndex = if (next) {
            (currentIndex + 1) % folderList.size
        } else {
            (currentIndex - 1 + folderList.size) % folderList.size
        }
        val target = folderList[targetIndex]
        _openFolderId.value = target.category.categoryId
        return true
    }

    fun closeFolder() {
        _openFolderId.value = null
    }

    fun openAllApps() {
        _focusSearchOnOpen.value = false
        // Cekmece TEMIZ acilir — ana ekran global aramasi ayni _searchQuery'yi paylastigi
        // icin onceki oturumun sorgusu cekmecede filtre olarak kalabiliyordu (Huseyin bildirimi).
        _searchQuery.value = ""
        _allAppsOpen.value = true
    }

    fun openAllAppsWithSearch() {
        _focusSearchOnOpen.value = true
        _searchQuery.value = ""
        _allAppsOpen.value = true
    }

    fun resetFocusSearchOnOpen() {
        _focusSearchOnOpen.value = false
    }

    fun closeAllApps() {
        _allAppsOpen.value = false
        _focusSearchOnOpen.value = false
        // Döngü P11 (roadmap madde 4) — AllAppsDrawer ve GlobalSearchHost/HomeAppSearchBar AYNI
        // _searchQuery state'ini paylaşır ("Tercih" seçeneği: ayrı state yaratmak yerine, drawer
        // kapanınca query temizlenir). Kök gesture'lar zaten searchActive==true iken kilitlendiği
        // için (HomeGestureArbiter.SEARCH_ACTIVE_LOCKS_ROOT) global arama açıkken swipe-up ile
        // drawer açılamaz — iki arama arasında gerçek bir çakışma senaryosu oluşmaz.
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

    /**
     * P0.3: Ana ekran aramasında dosya izni verilince (HomeAppSearchBar "izin ver" kısayolu)
     * dosya kaynağının FTS indeksini arka planda başlatır.
     * AppPrefs.setSearchSourceFilesEnabled(true) çağrı yerinde (HomeAppSearchBar) yapılır.
     */
    fun enableFilesSearchSource() {
        viewModelScope.launch {
            runCatching { searchRepository.enableFilesSource() }
                .onFailure { Timber.w(it, "Dosya arama kaynağı etkinleştirilemedi") }
        }
    }

    /** P0.3: Dosya kaynağının izin/indeks durumu — HomeAppSearchBar/AllAppsDrawer bu duruma göre render eder. */
    val filesIndexState: StateFlow<com.armutlu.apporganizer.domain.models.FileIndexState> =
        searchRepository.filesIndexState

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
                repository.incrementLaunchCount(packageName)
                repository.updateLastUsedTimestamp(packageName, ts)
            }
            // P0.5: yalnizca yerel "okundu" zaman damgasini guncelle — badge bir sonraki
            // badgeCounts akisinda bu zamana gore sifirlanir. SISTEM BILDIRIMINI ILETMIYORUZ
            // (cancelNotification cagrisi YOK, bilincli) — kullanicinin bildirim panelindeki
            // gercek bildirimleri launcher tarafindan silinmemeli, yalnizca "gorulmedi" isareti
            // kalkiyor. (Kod tarandi: bu projede daha once de cancelNotification cagrisi yoktu.)
            com.armutlu.apporganizer.utils.NotificationReadPrefs.markRead(context, packageName, ts)
        } catch (e: Exception) {
            Timber.e(e, "launchApp failed: $packageName")
        }
    }

    /**
     * Dongu H02 — HomeScreen onResume'da cagrilabilir (opsiyonel, henuz UI tarafinda bagli degil).
     * Mevcut onResume akislarina (refreshLastLaunched vb.) DOKUNMAZ, sadece koordinatoru tetikler.
     */
    fun refreshHomeIntelligence(reason: RefreshReason = RefreshReason.HOME_RESUME) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { homeIntelligenceCoordinator.refresh(reason) }
                .onFailure { Timber.w(it, "HomeIntelligenceCoordinator $reason refresh hatası") }
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
        val persistedPackages = DockPrefs.getDockPackages(context)
        if (persistedPackages != _dockPackages.value) {
            _dockPackages.value = persistedPackages
        }

        // Klasör sırası yalnız ilk yüklemede okunur; dock senkronundan bağımsızdır.
        if (!dockLoaded) {
            dockLoaded = true
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_FOLDER_ORDER, null)
            if (!saved.isNullOrBlank()) {
                val newOrder = saved.split(",").filter { it.isNotBlank() }
                if (newOrder != _folderOrder.value) {
                    _folderOrder.value = newOrder
                }
            }
        }
        ensureSocialFolderInDock(context)
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

    fun addFolderToDock(context: Context, categoryId: String) {
        val item = DockPrefs.folderItem(categoryId)
        val current = _dockPackages.value
        when {
            current.contains(item) -> _toastMessage.tryEmit("Klasor zaten Dock'ta")
            current.size >= DOCK_MAX_SIZE -> _toastMessage.tryEmit("Dock dolu (max $DOCK_MAX_SIZE) - once bir oge cikar")
            else -> {
                val updated = current + item
                DockPrefs.saveDockPackages(context, updated)
                _dockPackages.value = updated
                _toastMessage.tryEmit("Klasor Dock'a eklendi")
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

    private fun ensureSocialFolderInDock(context: Context) {
        if (socialFolderDockChecked) return
        val hasSocialFolder = folders.value.any { folder ->
            folder.category.categoryId == Category.CAT_SOCIAL && folder.apps.isNotEmpty()
        }
        if (!hasSocialFolder) return

        socialFolderDockChecked = true
        if (DockPrefs.addSocialFolderIfRoom(context)) {
            _dockPackages.value = DockPrefs.getDockPackages(context)
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
                // Kök neden (EX01 bug): PACKAGE_ADDED bazen PackageManager paketi tam commit
                // etmeden tetiklenir — getAppInfo null donebilir. Kisa backoff ile 3 deneme.
                var app = packageManagerHelper.getAppInfo(packageName)
                var attempt = 0
                while (app == null && attempt < 2) {
                    delay(150L * (attempt + 1))
                    app = packageManagerHelper.getAppInfo(packageName)
                    attempt++
                }
                if (app == null) {
                    Timber.w("onPackageAdded: getAppInfo $attempt denemeden sonra hala null, $packageName atlandi")
                    return@launch
                }
                repository.insertApps(listOf(app))
                Timber.d("onPackageAdded: $packageName eklendi/güncellendi")
            }.onFailure { Timber.e(it, "onPackageAdded failed: $packageName") }
        }
    }

    // Widget ID listesi — SharedPrefs'ten yüklenir, ekleme/silmede güncellenir
    private val _widgetIds = MutableStateFlow<List<Int>>(emptyList())
    val widgetIds: StateFlow<List<Int>> = _widgetIds.asStateFlow()

    // Widget ID'leri cihaza/kuruluma ozeldir (AppWidgetManager tarafindan uretilir) — restore/cihaz
    // degisikligi sonrasi eski ID'ler artik gecerli olmayabilir. Yuklerken AppWidgetManager'a
    // karsi dogrulanir, gecersiz olanlar sessizce SharedPrefs'ten temizlenir (F21, D261 denetimi).
    fun loadWidgetIds(context: Context) {
        val stored = WidgetPrefs.getWidgetIds(context)
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val valid = stored.filter { id -> runCatching { manager.getAppWidgetInfo(id) }.getOrNull() != null }
        if (valid.size != stored.size) {
            WidgetPrefs.saveWidgetIds(context, valid)
            Timber.w("loadWidgetIds: ${stored.size - valid.size} gecersiz widget ID temizlendi")
        }
        _widgetIds.value = valid
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
    val favoriteApps: StateFlow<List<AppInfo>> = allAppsSource
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

    // "Bu saatte en çok" — saat dilimine bağlı cache (dilim değişince yenilenir)
    @Volatile private var cachedSlotApps: List<String>? = null
    @Volatile private var cachedSlotHour: Int = -1

    // Akıllı öneriler — UsageScore v2: recency+frequency+timeSlot + dock boost
    // Dock kararlılığı (Fix 1): giriş akışı sadece sıralamayı etkileyen alanlar değişince
    // yeniden emit eder — her bildirimde updateNotificationCount() DB'ye yazınca
    // getAllAppsFlow() yeniden emit oluyordu, bu da suggestedApps'i (ve dolayısıyla
    // contextualDockPackages'ı) her seferinde yeniden sıralayıp dock'u kararsız yapıyordu.
    val suggestedApps: StateFlow<List<AppInfo>> = combine(
        allAppsSource
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
        val dockPkgs = _dockPackages.value.filterNot { DockPrefs.isFolderItem(it) }
        val favPkgs = _favoritePkgs.value
        if (UsageStatsHelper.hasPermission(ctx)) {
            // "Bu saatte en çok kullandıkların" — birincil kaynak: şu anki saat dilimindeki
            // mutlak başlatma sıralaması. Dilim değişince cache yenilenir.
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val slotApps = cachedSlotApps.takeIf {
                it != null && cachedSlotHour >= 0 &&
                    UsageStatsHelper.slotOf(cachedSlotHour) == UsageStatsHelper.slotOf(currentHour)
            } ?: UsageStatsHelper.getCurrentSlotTopApps(ctx, days = 28).also {
                cachedSlotApps = it
                cachedSlotHour = currentHour
            }
            val visibleByPkg = visible.associateBy { it.packageName }
            val slotPicks = slotApps.mapNotNull { visibleByPkg[it] }

            // 4'ü doldurmak için ağırlıklı skorla tamamla (saat verisi yetersizse)
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
            val slotPkgs = slotPicks.map { it.packageName }.toSet()
            val fill = visible
                .filter { boosted.containsKey(it.packageName) && it.packageName !in slotPkgs }
                .sortedByDescending { boosted[it.packageName] ?: 0f }
            fillDockSuggestions(
                slotApps = slotPicks,
                fallbackApps = fill,
            )
                .ifEmpty {
                    visible.filter { it.usageCount > 0 }
                        .sortedWith(compareByDescending<AppInfo> { it.lastUsedTimestamp }.thenByDescending { it.usageCount })
                        .take(DOCK_MAX_SIZE)
                }
        } else {
            visible.filter { it.usageCount > 0 }
                .sortedWith(compareByDescending<AppInfo> { it.lastUsedTimestamp }.thenByDescending { it.usageCount })
                .take(DOCK_MAX_SIZE)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Assistant Kartları — her refresh'te rastgele seçim, tekrar önleme
    private val _insightCards = MutableStateFlow<List<InsightCard>>(emptyList())

    // Dijital Yaşam kartı — Döngü D02: eski "Skor NN" rozeti yerine açıklayıcı özet
    // (durum + trend + en büyük etki). Döngü D00'dan itibaren TickerComposer KENDİ skorunu
    // hesaplamıyor; tek kaynak DigitalPulseRepository'dir (HomeIntelligenceCoordinator üzerinden
    // okunur) — ana ekran kartı, Pulse Clock ve Wrapped raporu AYNI sayıyı gösterir.
    private val homePulseFreshnessResolver = com.armutlu.apporganizer.domain.common.DataFreshnessResolver(
        java.time.Clock.systemDefaultZone(),
    )
    val homePulseSummary: StateFlow<com.armutlu.apporganizer.domain.home.HomePulseSummary?> =
        homeIntelligenceCoordinator.state
            .map { state ->
                val snapshot = state.pulse.valueOrNull()?.snapshot
                val freshness = homePulseFreshnessResolver.resolve(snapshot?.computedAt)
                com.armutlu.apporganizer.domain.home.toHomePulseSummary(
                    snapshot = snapshot,
                    freshness = freshness,
                    nowMillis = System.currentTimeMillis(),
                )
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val insightCards: StateFlow<List<InsightCard>> = _insightCards.asStateFlow()

    // Dongu M07 — Ana ekran "Görevler" karti (HomeMissionCard) icin
    // HomeIntelligenceCoordinator.state.mission dilimi. Ready/Stale -> ozet dolu; Missing/Failed
    // -> null (kart bu durumda kendi bos/DATA_UNAVAILABLE gosterimini yapar, coordinator'in
    // ham hata kodunu HomeScreen'e sizdirmaya gerek yok — primaryStatus zaten DATA_UNAVAILABLE
    // tasiyabiliyor).
    val homeMissionSummary: StateFlow<HomeMissionSummary?> = homeIntelligenceCoordinator.state
        .map { it.mission.valueOrNull()?.summary }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
        SuggestionCoordinator.recordRejected(
            dedupeKey = key,
            store = SharedPrefsSuggestionHistoryStore(getApplication()),
            nowMillis = System.currentTimeMillis(),
        )
    }

    // İçerik bazlı bastırma ("Bu tür bilgileri gösterme", roadmap T04) — SmartTickerType.name
    // AppPrefs.KEY_TICKER_HIDDEN_TYPES setine yazılır, bu StateFlow reaktif olarak yansıtır.
    private val _hiddenTickerTypes = MutableStateFlow(AppPrefs.getTickerHiddenTypes(getApplication()))

    fun hideTickerType(type: SmartTickerType) {
        val ctx = getApplication<Application>()
        AppPrefs.addTickerHiddenType(ctx, type.name)
        _hiddenTickerTypes.value = AppPrefs.getTickerHiddenTypes(ctx)
    }

    // T05 (Akıllı Nabız ayarları) — SmartTickerSettingsScreen'deki toplu içerik-türü/hassas-bilgi
    // switch'leri doğrudan AppPrefs'e yazılır (SettingsSwitchRow + rememberBooleanPreferenceState);
    // bu ViewModel'in combine() zincirinde o değerleri OKUYAN filtre yeniden tetiklenmeli, aksi
    // halde Ayarlar'dan dönüşte şerit eski haliyle kalır (CLAUDE.md "Reaktif AppPrefs" tuzağı).
    // Basit sayaç: hangi anahtar değiştiği önemli değil, herhangi bir T05 anahtarı değişince
    // tickerItems combine'ı yeniden hesaplanır.
    private val _smartTickerPrefsRevision = MutableStateFlow(0)
    private val smartTickerPrefKeys = setOf(
        AppPrefs.KEY_SMART_TICKER_ACTIONS,
        AppPrefs.KEY_SMART_TICKER_MISSIONS,
        AppPrefs.KEY_SMART_TICKER_PULSE,
        AppPrefs.KEY_SMART_TICKER_REPORTS,
        AppPrefs.KEY_SMART_TICKER_CONTEXTUAL,
        AppPrefs.KEY_SMART_TICKER_DISCOVERY,
        AppPrefs.KEY_SMART_TICKER_HEALTH,
        AppPrefs.KEY_TICKER_SENSITIVE_VISIBLE,
    )
    private val smartTickerPrefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in smartTickerPrefKeys) {
            _smartTickerPrefsRevision.value = _smartTickerPrefsRevision.value + 1
        }
    }.also { listener ->
        getApplication<Application>()
            .getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * [TickerActionRouter] route stringi -> `presentation.navigation.Routes` sabiti eslemesi.
     * Router domain katmaninda kendi string sabitlerini tutar (navigation modulune bagimli
     * olmamak icin); burada TEK yerde gercek Routes sabitine cevrilir (D04 PulseActionRouter
     * ile ayni desen).
     */
    private fun resolveTickerRoute(routeString: String?): String? = when (routeString) {
        TickerActionRouter.ROUTE_DASHBOARD -> com.armutlu.apporganizer.presentation.navigation.Routes.DASHBOARD
        TickerActionRouter.ROUTE_NOTIFICATION_REPORT -> com.armutlu.apporganizer.presentation.navigation.Routes.NOTIFICATION_REPORT
        TickerActionRouter.ROUTE_APP_LIST_UNCERTAIN -> com.armutlu.apporganizer.presentation.navigation.Routes.APP_LIST_UNCERTAIN
        TickerActionRouter.ROUTE_APP_LIST -> com.armutlu.apporganizer.presentation.navigation.Routes.APP_LIST
        TickerActionRouter.ROUTE_SETTINGS -> com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS
        TickerActionRouter.ROUTE_SETTINGS_LAUNCHER -> com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS_LAUNCHER
        TickerActionRouter.ROUTE_SETTINGS_NOTIFICATIONS -> com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS_NOTIFICATIONS
        TickerActionRouter.ROUTE_SETTINGS_APPEARANCE -> com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS_APPEARANCE
        TickerActionRouter.ROUTE_SETTINGS_STATS -> com.armutlu.apporganizer.presentation.navigation.Routes.SETTINGS_STATS
        TickerActionRouter.ROUTE_SEARCH_SETTINGS -> com.armutlu.apporganizer.presentation.navigation.Routes.SEARCH_SETTINGS
        TickerActionRouter.ROUTE_REPORTS_CENTER -> com.armutlu.apporganizer.presentation.navigation.Routes.REPORTS_CENTER
        TickerActionRouter.ROUTE_USAGE_REPORT -> com.armutlu.apporganizer.presentation.navigation.Routes.USAGE_REPORT
        TickerActionRouter.ROUTE_WRAPPED_REPORT -> com.armutlu.apporganizer.presentation.navigation.Routes.WRAPPED_REPORT
        else -> null
    }

    /**
     * [SmartTickerItem.action] -> navigasyon hedefi çözümü (Döngü T04, T01 köprüsünün yerini
     * alır). HomeScreen bu sonucu tüketip klasör/route/paket açılışını UI katmanında kurar
     * (M05/D04 pattern'i — Intent UI'da kurulur, ViewModel yalnız hedefi çözer).
     */
    fun resolveTickerTarget(item: SmartTickerItem): TickerActionRouter.RouteTarget {
        val target = TickerActionRouter.resolve(item.action)
        val screen = target as? TickerActionRouter.RouteTarget.Screen ?: return TickerActionRouter.RouteTarget.None
        return screen.copy(route = resolveTickerRoute(screen.route) ?: screen.route)
    }

    // Haber şeridi (ticker) — klasör istatistikleri + içgörüler + bildirim özeti + ipucu.
    // Döngü U01: snapshot inşası, TickerComposer/TickerRanker çağrısı, tür/hassasiyet/dismiss
    // filtreleri ve ekstra üreticiler (arama istatistiği/Wrapped) [HomeTickerComposer] use-case'ine
    // taşındı — ViewModel burada yalnızca kaynak flow'ları combine edip ona devreder.
    // MissionPulseTickerFactory (T03) bu akışa henüz bağlanmadı — RealSmartTickerSource/
    // HomeIntelligenceCoordinator üzerinden ayrı çalışıyor (bkz. MissionPulseTickerFactory.kt
    // dosya başı notu, döngüsel bağımlılık nedeniyle).
    // Döngü T04: SmartTickerItem doğrudan UI'ya taşınır (T01 köprüsü kaldırıldı) — dokunma hedefi
    // [resolveTickerTarget] ile, dismiss/hideType [dismissTickerItem]/[hideTickerType] ile çözülür.
    // kotlinx.coroutines combine() en fazla 5 flow'u tipli overload'la destekler; T05'in
    // yeni _smartTickerPrefsRevision sinyalini eklemek için _hiddenTickerTypes ile önce ikili
    // combine edilip Pair olarak taşınır (6. flow ekleme ihtiyacını Pair'e sığdırma deseni).
    private val hiddenTypesAndPrefsRevision: Flow<Pair<Set<String>, Int>> =
        combine(_hiddenTickerTypes, _smartTickerPrefsRevision) { hidden, revision -> hidden to revision }

    val tickerItems: StateFlow<List<SmartTickerItem>> = combine(
        folders,
        insightCards,
        AppNotificationListenerService.badgeCounts,
        _dismissedTickerKeys,
        hiddenTypesAndPrefsRevision,
    ) { folderList, cards, badges, dismissed, hiddenTypesAndRevision ->
        val (hiddenTypes, _) = hiddenTypesAndRevision
        com.armutlu.apporganizer.domain.home.HomeTickerComposer.compose(
            context = getApplication(),
            classifier = classifier,
            input = com.armutlu.apporganizer.domain.home.HomeTickerComposer.Input(
                folders = folderList,
                insightCards = cards,
                badgeCounts = badges,
                dismissedKeys = dismissed,
                hiddenTickerTypes = hiddenTypes,
            ),
        )
        // WhileSubscribed: ticker yalnizca HomeScreen gorunurken hesaplanir; initial emptyList
        // HomeTickerRow'da erken-donus ile guvenli — cold start yuku azaltildi (D234).
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // Contextual Dock — kullanicinin sectigi TUM slotlar korunur; akilli oneriler
    // (saat/gun/kullanim) SADECE bos kalan slotlari doldurur (dock kapasitesi kadar).
    // Eski davranis (ilk 2 sabit + son 2 akilli) kullanicinin sectigi dock'u sessizce
    // eziyordu — "sectigim dock'ta kamera var ama ekranda WhatsApp cikiyor" bug'i (D257).
    val contextualDockPackages: StateFlow<List<String>> = combine(
        dockPackages,
        suggestedApps
    ) { fixed, suggested ->
        val ctx = getApplication<Application>()
        buildContextualDockPackages(
            fixed = fixed,
            suggested = suggested.map { it.packageName },
            contextualEnabled = AppPrefs.isContextualDockEnabled(ctx),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Son kullanilan 4 uygulama — RecentAppsRow icin, lastUsedTimestamp sirasinda
    val recentApps: StateFlow<List<AppInfo>> = allAppsSource
        .map { apps ->
            apps.filter { !it.isHidden && it.lastUsedTimestamp > 0L }
                .sortedByDescending { it.lastUsedTimestamp }
                .take(4)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // EX01 — "Bugun Yuklenenler": bugun (yerel gun siniri, PeriodBoundaryResolver) yuklenen
    // uygulamalar. Ana ekran kompakt giris + cekmece bolumu bu akistan beslenir.
    // KEY_RECENT_INSTALLS_ENABLED kapaliyken bos liste doner — UI tamamen gizlenir.
    private val periodBoundaryResolver = com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver(
        java.time.Clock.systemDefaultZone(),
        java.time.ZoneId.systemDefault(),
    )
    // Ayar toggle'i degisince akisi yeniden tetiklemek icin — allAppsSource yeni emisyon
    // yapana kadar UI eski degerde takili kalmasin (refreshTodayInstalled() HomeScreen'den cagrilir).
    private val _todayInstalledRefresh = MutableStateFlow(0)
    val todayInstalledApps: StateFlow<List<AppInfo>> = combine(
        allAppsSource,
        _todayInstalledRefresh
    ) { apps, _ ->
        val ctx = getApplication<Application>()
        if (!AppPrefs.isRecentInstallsEnabled(ctx)) return@combine emptyList()
        val today = periodBoundaryResolver.currentDay()
        filterTodayInstalledApps(apps, today.startInclusive, today.endExclusive)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** KEY_RECENT_INSTALLS_ENABLED degisince HomeScreen'den cagrilir — todayInstalledApps'i yeniden hesaplatir. */
    fun refreshTodayInstalled() {
        _todayInstalledRefresh.update { it + 1 }
    }

    /** UsageStatsManager'dan kullanım verilerini Room DB'ye senkronize eder. */
    fun syncUsageStats(context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!UsageStatsHelper.hasPermission(context)) return@launch
            runCatching {
                val counts = UsageStatsHelper.getUsageCounts(context, days = 30)
                counts.forEach { (pkg, ms) -> repository.updateUsageTimeMs(pkg, ms) }
                val lastUsed = UsageStatsHelper.getLastUsedTimes(context, days = 90)
                // IfNewer: launchApp'ın anlık timestamp'ini daha eski UsageStats verisiyle ezme
                lastUsed.forEach { (pkg, ts) -> repository.updateLastUsedTimestampIfNewer(pkg, ts) }
                Timber.d("UsageStats synced: ${counts.size} apps, ${lastUsed.size} lastUsed")
                onSuccess()
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

    // Widget öneri listesi — en çok kullanılan ve widget'ı olan uygulamalar.
    // WhileSubscribed: sadece widget seçici açıkken hesaplanır — cold start yükü azaltıldı (D234).
    val widgetSuggestions = allAppsSource
        .map { apps ->
            WidgetSuggestionEngine.getSuggestions(getApplication(), apps.filter { !it.isHidden })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

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
