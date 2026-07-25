package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.provider.Settings
import com.armutlu.apporganizer.presentation.ui.MainActivity
import com.armutlu.apporganizer.presentation.navigation.Routes
import com.armutlu.apporganizer.presentation.ui.screens.isNotificationListenerGranted
import com.armutlu.apporganizer.utils.UsageStatsHelper
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import com.armutlu.apporganizer.presentation.ui.common.diamondShine
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.ContactActionPrefs
import com.armutlu.apporganizer.utils.SearchCache
import com.armutlu.apporganizer.utils.SearchHistoryPrefs
import com.armutlu.apporganizer.utils.SearchOverlayDecisions
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import com.armutlu.apporganizer.utils.SystemSettingsCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import java.util.Locale

/**
 * P1.3: contactId bossa (numara yoksa/parse hatasi) sessizce atlar.
 * Ayar kapaliysa (KEY_CONTACT_SUGGESTIONS_ENABLED=false) hicbir kayit yazilmaz.
 */
private fun homeLogContactAction(
    context: Context,
    contactId: String,
    action: com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType
) {
    if (contactId.isBlank()) return
    if (!AppPrefs.isContactSuggestionsEnabled(context)) return
    com.armutlu.apporganizer.utils.ContactActionPrefs.logAction(context, contactId, action)
}

@Composable
internal fun SwipeHint(context: Context, visible: Boolean) {
    // swipeHintEnabled — SharedPrefs'ten reaktif okuma; incrementSwipeHintCount sonrası state güncellenir
    var hintAllowed by remember { mutableStateOf(AppPrefs.shouldShowSwipeHint(context)) }
    val showSwipeHint = visible && hintAllowed
    val infiniteTransition = rememberInfiniteTransition(label = "swipe_hint")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe_y"
    )
    LaunchedEffect(showSwipeHint) {
        if (showSwipeHint) {
            AppPrefs.incrementSwipeHintCount(context)
            hintAllowed = AppPrefs.shouldShowSwipeHint(context)
        }
    }
    if (showSwipeHint && visible) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Yukarı kaydırarak tüm uygulamaları aç"
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Yukarı kaydırma ipucu",
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp).offset(y = offsetY.dp)
            )
            Text(
                text = "Tüm uygulamalar",
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Ana ekran birleşik arama çubuğu (S1) — tek sorguda uygulama + klasör + kişi + dosya
 * sonuçları kaynak gruplarıyla gösterilir (AllAppsDrawer'daki SourceGroupHeader pattern'i).
 * "Uygulama / Klasör" sekmesi kaldırıldı; klasör eşleşmeleri "Klasörler" sonuç grubudur.
 * Kişi araması izin verilmişse varsayılan etkindir; izin yoksa "Kişiler" grubunda
 * "izin ver" kısayolu görünür (S2).
 * Long-press (300ms) → drag handle görünür + scale(1.04f); bırakınca snap noktasına oturur.
 */
@Composable
internal fun HomeAppSearchBar(
    allApps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPositionSnap: ((String) -> Unit)? = null,
    folders: List<AppFolder> = emptyList(),
    folderCustomNames: Map<String, String> = emptyMap(),
    folderCustomEmojis: Map<String, String> = emptyMap(),
    onFolderClick: (AppFolder) -> Unit = {},
    searchResults: Map<SourceType, List<SearchDocument>> = emptyMap(),
    onQueryChange: (String) -> Unit = {},
    onEnableContactsSource: () -> Unit = {},
    onEnableFilesSource: () -> Unit = {},
    // P0.3: dosya kaynağı izin/indeks durumu — PermissionRequired iken "izin gerekli" satırı gösterilir,
    // sahte "0 sonuç" izlenimi verilmez.
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState =
        com.armutlu.apporganizer.domain.models.FileIndexState.Disabled,
    fullScreenEnabled: Boolean = false,
    onOpenFullScreen: () -> Unit = {},
    homeResumeTrigger: Int = 0,
    // Çubuk alttayken sonuçlar ÜSTTE (yukarı doğru) açılır — sayfa kaymaz (D258)
    resultsAbove: Boolean = false
) {
    val context = LocalContext.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    if (fullScreenEnabled) {
        var isDragging by remember { mutableStateOf(false) }
        var dragOffsetY by remember { mutableStateOf(0f) }
        var showGhostZones by remember { mutableStateOf(false) }
        val shineEnabled = AppPrefs.isSearchShineEnabled(context)
        val barScale by animateFloatAsState(
            targetValue = if (isDragging) 1.04f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "fullscreen_search_bar_scale"
        )

        Box(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = if (isTablet) HomeContentWidthTokens.tabletMaxContentWidthDp else HomeContentWidthTokens.maxContentWidthDp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            if (showGhostZones) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (dragOffsetY < 0) Color.White.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.07f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↑ Üst", color = Color.White.copy(alpha = if (dragOffsetY < 0) 0.80f else 0.30f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(barScale)
                    .diamondShine(shineEnabled, RoundedCornerShape(28.dp), trigger = homeResumeTrigger)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (!isDragging) onOpenFullScreen() },
                            onLongPress = {
                                isDragging = true
                                showGhostZones = true
                            }
                        )
                    }
                    .then(
                        if (isDragging) Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    val snapPos = if (dragOffsetY < 0) AppPrefs.SEARCH_BAR_POS_TOP else AppPrefs.SEARCH_BAR_POS_BOTTOM
                                    AppPrefs.setSearchBarPosition(context, snapPos)
                                    onPositionSnap?.invoke(snapPos)
                                    isDragging = false
                                    showGhostZones = false
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    showGhostZones = false
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                }
                            )
                        } else Modifier
                    ),
                cornerRadius = 28.dp,
                backgroundAlpha = if (isDragging) 0.22f else 0.12f,
                borderAlpha = if (isDragging) 0.45f else 0.25f,
                borderColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.search_overlay_title),
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (showGhostZones) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (dragOffsetY > 0) Color.White.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.07f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↓ Alt", color = Color.White.copy(alpha = if (dragOffsetY > 0) 0.80f else 0.30f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            }
        }
        return
    }

    var query by rememberSaveable { mutableStateOf("") }

    // Arama ayarları — Ayarlar ekranından dönünce canlı güncellensin (Reaktif AppPrefs pattern'i, LEARNINGS)
    var fuzzy         by remember { mutableStateOf(AppPrefs.isSearchFuzzyEnabled(context)) }
    var phonetic      by remember { mutableStateOf(AppPrefs.isSearchPhoneticEnabled(context)) }
    var sortByUsage   by remember { mutableStateOf(AppPrefs.isSearchSortByUsage(context)) }
    var maxResults    by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var showIcons     by remember { mutableStateOf(AppPrefs.isSearchShowIcons(context)) }
    var showAvatar    by remember { mutableStateOf(AppPrefs.isSearchShowContactAvatar(context)) }
    var shineEnabled  by remember { mutableStateOf(AppPrefs.isSearchShineEnabled(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_FUZZY -> fuzzy = AppPrefs.isSearchFuzzyEnabled(context)
                AppPrefs.KEY_SEARCH_PHONETIC -> phonetic = AppPrefs.isSearchPhoneticEnabled(context)
                AppPrefs.KEY_SEARCH_SORT_BY_USAGE -> sortByUsage = AppPrefs.isSearchSortByUsage(context)
                AppPrefs.KEY_SEARCH_MAX_RESULTS -> maxResults = AppPrefs.getSearchMaxResults(context)
                AppPrefs.KEY_SEARCH_SHOW_ICONS -> showIcons = AppPrefs.isSearchShowIcons(context)
                AppPrefs.KEY_SEARCH_SHOW_CONTACT_AVATAR -> showAvatar = AppPrefs.isSearchShowContactAvatar(context)
                AppPrefs.KEY_SEARCH_SHINE_ENABLED -> shineEnabled = AppPrefs.isSearchShineEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Kişi kaynağı — Settings'ten dönünce güncellensin (Reaktif AppPrefs pattern'i, LEARNINGS)
    var contactsOn by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesOn by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    // Kullanıcı Ayarlar'dan kişi/dosya kaynağını BİLİNÇLİ kapattıysa "izin ver" kısayolu da gizlenir
    var contactsOptedOut by remember {
        mutableStateOf(
            AppPrefs.hasSearchSourceContactsPreference(context) &&
                !AppPrefs.isSearchSourceContactsEnabled(context)
        )
    }
    var filesOptedOut by remember {
        mutableStateOf(
            AppPrefs.hasSearchSourceFilesPreference(context) &&
                !AppPrefs.isSearchSourceFilesEnabled(context)
        )
    }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_SOURCE_CONTACTS -> {
                    contactsOn = AppPrefs.isSearchSourceContactsEnabled(context)
                    contactsOptedOut = AppPrefs.hasSearchSourceContactsPreference(context) && !contactsOn
                }
                AppPrefs.KEY_SEARCH_SOURCE_FILES -> {
                    filesOn = AppPrefs.isSearchSourceFilesEnabled(context)
                    filesOptedOut = AppPrefs.hasSearchSourceFilesPreference(context) && !filesOn
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // READ_CONTACTS izin durumu + istek launcher'ı (S2: sonuç grubunda "izin ver" kısayolu)
    var contactsPermGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    // Spec §5: kullanıcı bir kez reddederse o oturumda tekrar sorulmaz
    var contactsPermDeniedSession by remember { mutableStateOf(false) }
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsPermGranted = true
            AppPrefs.setSearchSourceContactsEnabled(context, true)
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
            onEnableContactsSource() // FTS indeksi (ContactsIndexer) arka planda başlar
        } else {
            contactsPermDeniedSession = true
        }
    }

    // P0.3: dosya izinleri (READ_MEDIA_* / READ_EXTERNAL_STORAGE) + istek launcher'ı —
    // "Dosya araması için izin gerekli" satırına dokununca doğrudan sistem izin diyaloğu açılır.
    var filesPermDeniedSession by remember { mutableStateOf(false) }
    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource() // FilesIndexer.indexAll() arka planda başlar (SearchRepository.enableFilesSource)
        } else {
            filesPermDeniedSession = true
        }
    }

    // Cache'i allApps değişince güncelle
    LaunchedEffect(allApps) {
        withContext(Dispatchers.IO) { SearchCache.warmApps(allApps) }
    }

    // Kişi cache'ini başlat (kaynak açık + izin varsa)
    LaunchedEffect(contactsOn, contactsPermGranted) {
        if (contactsOn && contactsPermGranted) {
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
        }
    }

    // Sonuç grupları: uygulama + klasör + kişi + dosya (S1 — birleşik arama)
    val appResults = remember(query, allApps, fuzzy, phonetic, sortByUsage, maxResults) {
        if (query.isBlank()) emptyList()
        else SearchCache.searchApps(query, maxResults, phonetic, fuzzy, sortByUsage)
    }
    // Klasörler — eski "Klasör" sekmesinin yerine sonuç grubu; özel klasör adı varsa onunla eşleşir
    val folderResults = remember(query, folders, folderCustomNames) {
        if (query.isBlank() || folders.isEmpty()) emptyList()
        else {
            val q = query.trim().lowercase(Locale("tr"))
            folders.filter { folder ->
                val displayName = folderCustomNames[folder.category.categoryId]
                    ?: folder.category.categoryName
                displayName.lowercase(Locale("tr")).contains(q)
            }.take(4)
        }
    }
    fun SearchDocument.matchesCurrentQuery(): Boolean {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.isBlank()) return false
        return title.lowercase(Locale("tr")).contains(q) ||
            subtitle.lowercase(Locale("tr")).contains(q) ||
            sourceId.lowercase(Locale("tr")).contains(q)
    }
    val contactResults = remember(query, contactsOn, contactsPermGranted) {
        if (!contactsOn || !contactsPermGranted || query.isBlank()) emptyList()
        else SearchCache.searchContacts(query, 3, phonetic = true, fuzzy = true)
    }
    // Dosya adları — SearchRepository FTS5 indeksinden (LauncherViewModel.searchResults akışı)
    val fileResults = if (query.isBlank()) emptyList()
        else searchResults[SourceType.FILE].orEmpty().filter { it.matchesCurrentQuery() }.take(4)
    val settingResults = if (query.isBlank()) emptyList()
        else searchResults[SourceType.SETTING].orEmpty().filter { it.matchesCurrentQuery() }.take(4)
    val searchHintRes = if ((contactsOn && contactsPermGranted) || filesOn) {
        R.string.home_search_hint_full
    } else {
        R.string.home_search_hint_basic
    }
    // İzin yoksa "Kişiler" grubunda izin kısayolu göster (kullanıcı kaynağı kapatmadıysa)
    val showContactsPermissionHint = query.isNotBlank() && !contactsPermGranted &&
        !contactsOptedOut && !contactsPermDeniedSession
    // P0.3: dosya kaynağı açık ama izin yoksa "izin gerekli" göster — Ready değilken
    // sahte "0 sonuç" izlenimi verilmez (spec madde 4). Aksiyon Ayarlar > Arama'ya yönlendirir
    // (izin isteği zaten SearchSettingsScreen'deki ContextualPermissionDialog akışında var).
    val showFilesPermissionHint = query.isNotBlank() && filesOn && !filesPermDeniedSession &&
        filesIndexState is com.armutlu.apporganizer.domain.models.FileIndexState.PermissionRequired
    // Görev 2 kök neden: dosya kaynağı hiç açılmadıysa (filesOn=false) hiçbir satır
    // gösterilmiyordu — kullanıcı kaynağı etkinleştiremiyordu. Kaynak kapalı + bilinçli
    // kapatılmamışsa (Ayarlar'dan "kapat" seçilmediyse) "Dosyalarda da ara" kısayolu gösterilir.
    val showFilesEnableHint = query.isNotBlank() && !filesOn && !filesOptedOut && !filesPermDeniedSession

    // Web/Play Store fallback — sorgu >= 2 karakter, tüm kaynaklar sıfır sonuç verince gösterilir (Ayarlar > Arama'dan kapatılabilir)
    var webFallbackEnabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED) {
                webFallbackEnabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val showWebFallback = webFallbackEnabled && query.trim().length >= 2 &&
        appResults.isEmpty() && folderResults.isEmpty() && contactResults.isEmpty() &&
        settingResults.isEmpty() && fileResults.isEmpty() &&
        !showContactsPermissionHint && !showFilesPermissionHint

    // Sorguyu ViewModel'e ilet — FTS5 çok-kaynak araması (dosyalar) debounce ile orada çalışır
    LaunchedEffect(query) { onQueryChange(query) }

    // İzin ipucu (E10) — Kullanım Erişimi veya Bildirim Erişimi eksikse arama çubuğu altında göster.
    // ON_RESUME'da yeniden kontrol: Ayarlar'dan izin verilince ipucu anında kaybolur.
    var usageGranted by remember { mutableStateOf(UsageStatsHelper.hasPermission(context)) }
    var notifListenerGranted by remember { mutableStateOf(isNotificationListenerGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = UsageStatsHelper.hasPermission(context)
                notifListenerGranted = isNotificationListenerGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val permMissing = !usageGranted || !notifListenerGranted
    // Bu oturumda X ile kapatıldı mı (aktif ipucu için geçici gizleme)
    var permHintSessionDismissed by remember { mutableStateOf(false) }
    // Kaç açılışta gösterildi — MAX'a ulaşınca rahatsız etmeyen pasif linke döner
    var permHintCount by remember { mutableStateOf(AppPrefs.getSearchPermHintCount(context)) }
    val permHintPassiveMode = permHintCount >= AppPrefs.SEARCH_PERM_HINT_MAX
    // Pasif link kalıcı kapatıldıysa bir daha hiç gösterme
    var permHintPermDismissed by remember { mutableStateOf(AppPrefs.isSearchPermHintDismissed(context)) }
    val showPermHint = permMissing && !permHintSessionDismissed && !permHintPermDismissed
    // Aktif modda ilk gösterimde sayaç artır — birkaç açılış sonra pasif moda geçsin
    LaunchedEffect(showPermHint, permHintPassiveMode) {
        if (showPermHint && !permHintPassiveMode) {
            AppPrefs.incrementSearchPermHintCount(context)
            permHintCount = AppPrefs.getSearchPermHintCount(context)
        }
    }

    // Drag handle state
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    // Focus state — arama alanı seçilince kart belirginleşir (E9)
    var isFocused by remember { mutableStateOf(false) }
    var showGhostZones by remember { mutableStateOf(false) }
    // Döngü P19 madde 3 — arama alanı contentDescription'ı, ortak string kaynağı.
    val searchFieldRoleDescription = stringResource(R.string.search_field_role_description)
    val barScale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "search_bar_scale"
    )

    // Sonuc bolumu tek yerde tanimlanir; resultsAbove=true iken cubugun USTUNDE render
    // edilir (alttan yukari acilir, sayfa kaymaz — D258), aksi halde eski gibi altta.
    val searchResultsSection: @Composable () -> Unit = {
        // Sonuç listesi — kaynak grupları: Uygulamalar / Klasörler / Kişiler / Dosyalar (S1)
        val hasAnyResult = appResults.isNotEmpty() || folderResults.isNotEmpty() ||
            settingResults.isNotEmpty() || contactResults.isNotEmpty() || fileResults.isNotEmpty() ||
            showContactsPermissionHint || showFilesPermissionHint ||
            showWebFallback
        if (hasAnyResult && !isDragging) {
            // Tek grup varsa başlık gereksiz kalabalık — yalnızca çoklu grupta göster
            val multiGroup = listOf(
                appResults.isNotEmpty(),
                folderResults.isNotEmpty(),
                settingResults.isNotEmpty(),
                contactResults.isNotEmpty() || showContactsPermissionHint,
                fileResults.isNotEmpty() || showFilesPermissionHint
            ).count { it } > 1
            Spacer(Modifier.height(4.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.18f) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .then(
                            // Yukari acilirken ekrani kaplamasin — sinirli yukseklik + ic scroll
                            if (resultsAbove) Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                            else Modifier
                        )
                ) {

                    // App sonuçları
                    if (appResults.isNotEmpty() && multiGroup) {
                        HomeSearchGroupHeader(label = "Uygulamalar", icon = Icons.Default.Search)
                    }
                    appResults.forEachIndexed { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SearchStatsPrefs.logClick(context, SourceType.APP.key, index)
                                    query = ""
                                    onAppClick(app.packageName)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (showIcons) {
                                val cacheKey = "${app.packageName}_32_${app.lastUpdatedTime}"
                                val icon by produceState<ImageBitmap?>(null, cacheKey) {
                                    value = withContext(Dispatchers.IO) {
                                        iconCacheInternal[cacheKey] ?: run {
                                            val bmp = runCatching {
                                                com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 64)?.asImageBitmap()
                                            }.getOrNull()
                                            if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                            bmp
                                        }
                                    }
                                }
                                if (icon != null) {
                                    Image(bitmap = icon!!, contentDescription = null,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                                } else {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f)))
                                }
                            }
                            Text(app.appName, color = Color.White.copy(alpha = 0.90f),
                                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                        }
                    }

                    // Klasör sonuçları — eski "Klasör" sekmesinin yerini alan sonuç grubu (S1)
                    if (folderResults.isNotEmpty()) {
                        if (appResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Klasörler", icon = Icons.Default.Folder)
                        folderResults.forEachIndexed { index, folder ->
                            val displayName = folderCustomNames[folder.category.categoryId]
                                ?: folder.category.categoryName
                            val emoji = (folderCustomEmojis[folder.category.categoryId]
                                ?: folder.category.iconEmoji).ifBlank { "📁" }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.CATEGORY.key, index)
                                        query = ""
                                        onFolderClick(folder)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayName, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${folder.apps.size} uygulama",
                                        color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Android ayarları sonuçları — Wi-Fi, bildirim erişimi, kullanım erişimi vb.
                    if (settingResults.isNotEmpty()) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Ayarlar", icon = Icons.Default.Search)
                        settingResults.forEachIndexed { index, document ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.SETTING.key, index)
                                        query = ""
                                        SystemSettingsCatalog.open(context, document)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(document.title, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(document.subtitle.substringBefore(" | "), color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Kişi sonuçları — ayraç
                    if (contactResults.isNotEmpty()) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() || settingResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Kişiler", icon = Icons.Default.Person)
                        contactResults.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.CONTACT.key, index)
                                        query = ""
                                        // Kişi tıklaması: arama ekranına veya telefon dialer'a
                                        val dialIntent = android.content.Intent(
                                            android.content.Intent.ACTION_DIAL,
                                            android.net.Uri.parse("tel:${android.net.Uri.encode(contact.phone)}")
                                        ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                        runCatching { context.startActivity(dialIntent) }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (showAvatar) {
                                    val avatarBitmap by produceState<ImageBitmap?>(null, contact.photoUri) {
                                        value = withContext(Dispatchers.IO) {
                                            if (contact.photoUri != null) {
                                                runCatching {
                                                    val uri = android.net.Uri.parse(contact.photoUri)
                                                    context.contentResolver.openInputStream(uri)?.use {
                                                        android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                                                    }
                                                }.getOrNull()
                                            } else null
                                        }
                                    }
                                    if (avatarBitmap != null) {
                                        Image(bitmap = avatarBitmap!!, contentDescription = null,
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop)
                                    } else {
                                        Box(
                                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                                .background(Color.White.copy(alpha = 0.20f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                contact.displayName.take(1).uppercase(),
                                                color = Color.White.copy(alpha = 0.80f),
                                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.displayName, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (contact.phone.isNotBlank()) {
                                        Text(contact.phone, color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                // Hizli aksiyonlar - numara varsa gosterilir; satirin kendisi ayri
                                // olarak dialer'i acmaya devam eder (mevcut davranis korunur)
                                if (contact.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            SearchStatsPrefs.logAction(context, "CALL")
                                            homeLogContactAction(context, contact.id.toString(),
                                                com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.CALL)
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:${android.net.Uri.encode(contact.phone)}")
                                            ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Ara",
                                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val normalized = contact.phone.filter { it.isDigit() || it == '+' }
                                            runCatching {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://wa.me/$normalized")
                                                ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                                context.startActivity(intent)
                                                SearchStatsPrefs.logAction(context, "WHATSAPP")
                                                homeLogContactAction(context, contact.id.toString(),
                                                    com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.WHATSAPP)
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp",
                                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            SearchStatsPrefs.logAction(context, "SMS")
                                            homeLogContactAction(context, contact.id.toString(),
                                                com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.SMS)
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_SENDTO,
                                                android.net.Uri.parse("smsto:${android.net.Uri.encode(contact.phone)}")
                                            ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                                            runCatching { context.startActivity(intent) }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS",
                                            tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }

                    // İzin kısayolu — kişi araması etkin ama READ_CONTACTS yok (S2)
                    if (showContactsPermissionHint) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() || settingResults.isNotEmpty()) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Kişiler", icon = Icons.Default.Person)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    contactsPermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_search_contacts_enable_title), color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp)
                                Text(stringResource(R.string.home_search_contacts_enable_desc),
                                    color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                            }
                        }
                    }

                    // P0.3: dosya kaynağı açık ama Android izni yok — "0 sonuç" yerine izin kısayolu
                    if (showFilesPermissionHint) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() ||
                            settingResults.isNotEmpty() || contactResults.isNotEmpty() || showContactsPermissionHint
                        ) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Dosyalar", icon = Icons.Default.Description)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES,
                                            android.Manifest.permission.READ_MEDIA_VIDEO,
                                            android.Manifest.permission.READ_MEDIA_AUDIO,
                                        )
                                    } else {
                                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    filesPermLauncher.launch(permissions)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_search_files_permission_required), color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp)
                                Text(stringResource(R.string.home_search_files_permission_required_desc),
                                    color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                            }
                        }
                    }

                    // Görev 2: dosya kaynağı hiç açılmamışsa etkinleştirme kısayolu (contactsOptedOut ile aynı desen)
                    if (showFilesEnableHint) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() ||
                            settingResults.isNotEmpty() || contactResults.isNotEmpty() || showContactsPermissionHint
                        ) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Dosyalar", icon = Icons.Default.Description)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES,
                                            android.Manifest.permission.READ_MEDIA_VIDEO,
                                            android.Manifest.permission.READ_MEDIA_AUDIO,
                                        )
                                    } else {
                                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    filesPermLauncher.launch(permissions)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.home_search_files_enable_title), color = Color.White.copy(alpha = 0.90f),
                                    fontSize = 14.sp)
                                Text(stringResource(R.string.home_search_files_enable_desc),
                                    color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                            }
                        }
                    }

                    // Dosya sonuçları — FTS5 indeksinden (kaynak Ayarlar > Arama'dan kapatılabilir)
                    if (fileResults.isNotEmpty()) {
                        if (appResults.isNotEmpty() || folderResults.isNotEmpty() ||
                            settingResults.isNotEmpty() || contactResults.isNotEmpty() ||
                            showContactsPermissionHint || showFilesPermissionHint
                        ) {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            )
                        }
                        HomeSearchGroupHeader(label = "Dosyalar", icon = Icons.Default.Description)
                        fileResults.forEachIndexed { index, document ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SearchStatsPrefs.logClick(context, SourceType.FILE.key, index)
                                        query = ""
                                        // AllAppsDrawer.openSearchDocument ile aynı pattern
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(document.sourceId))
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        runCatching { context.startActivity(intent) }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(document.title, color = Color.White.copy(alpha = 0.90f),
                                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val subtitle = document.subtitle.ifBlank { document.sourceId }
                                    Text(subtitle, color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    // Web/Play Store fallback — sıfır sonuçta arama devam ettirilebilsin (Ayarlar > Arama)
                    if (showWebFallback) {
                        SearchFallbackRows(context = context, query = query.trim())
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .widthIn(max = if (isTablet) HomeContentWidthTokens.tabletMaxContentWidthDp else HomeContentWidthTokens.maxContentWidthDp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (resultsAbove) searchResultsSection()

        // Ghost zones — TOP / BOTTOM snap hedefleri
        if (showGhostZones) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dragOffsetY < 0) Color.White.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑ Üst",
                    color = Color.White.copy(alpha = if (dragOffsetY < 0) 0.80f else 0.30f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        // Arama alanı — glass kart stilinde + drag handle + elmas parlaması
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .scale(barScale)
                .diamondShine(shineEnabled, RoundedCornerShape(28.dp), trigger = homeResumeTrigger)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            isDragging = true
                            showGhostZones = true
                        }
                    )
                }
                .then(
                    if (isDragging) Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                val snapPos = if (dragOffsetY < 0)
                                    AppPrefs.SEARCH_BAR_POS_TOP
                                else
                                    AppPrefs.SEARCH_BAR_POS_BOTTOM
                                AppPrefs.setSearchBarPosition(context, snapPos)
                                onPositionSnap?.invoke(snapPos)
                                isDragging = false
                                showGhostZones = false
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                showGhostZones = false
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                            }
                        )
                    } else Modifier
                ),
            cornerRadius = 28.dp,
            // Focus'ta da belirginleş — seçili olduğu net görünsün (E9)
            backgroundAlpha = if (isDragging || isFocused) 0.22f else 0.12f,
            borderAlpha = if (isDragging) 0.45f else if (isFocused) 0.70f else 0.25f,
            borderColor = if (isFocused) Color(0xFF26C6DA) else Color.White
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // "Uygulama / Klasör" sekmesi kaldırıldı (S1) — klasörler artık sonuç grubu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Ara",
                        tint = if (isFocused) Color.White else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp))
                    // Döngü P19 madde 3 — global arama TalkBack'e `heading` DEĞİL, ayrı bir
                    // "arama alanı" contentDescription'ı ile yansır (bu Compose UI sürümünde
                    // `Role.Search` yok — mevcut Role enum'u Button/Checkbox/Switch/RadioButton/
                    // Tab/Image/DropdownList ile sınırlı, bkz. görev raporu) — `home_page_indicator_...`
                    // gibi sayfa başlıklarıyla karıştırılmaması için ayrı string kaynağı kullanılır.
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused }
                            .semantics {
                                contentDescription = searchFieldRoleDescription
                            },
                        decorationBox = { inner ->
                            Box(Modifier.weight(1f)) {
                                // Spec §5: placeholder kalabalıklaşmasın — kişi/dosya eklenmez
                                if (query.isEmpty()) Text(
                                    stringResource(searchHintRes),
                                    color = Color.White.copy(alpha = 0.40f), fontSize = 14.sp
                                )
                                inner()
                            }
                        }
                    )
                    if (query.isNotEmpty()) {
                        Icon(Icons.Default.Close, contentDescription = "Aramayı temizle",
                            tint = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(18.dp).clickable { query = "" })
                    }
                }
            }
        }

        // İzin ipucu satırı (E10) — arama çubuğunun hemen altında; sürükleme sırasında gizli
        if (showPermHint && !isDragging) {
            Spacer(Modifier.height(4.dp))
            val permHintText = when {
                permHintPassiveMode -> "İzinler ayarlardan yönetilebilir →"
                !usageGranted -> "🔍 Daha iyi arama ve öneriler için kullanım erişimi gerekli — Ver"
                else -> "🔔 Bildirim rozetleri için erişim gerekli — Ver"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable {
                        if (permHintPassiveMode) {
                            // Rahatsız etmeden İzinler rehberine yönlendir (MainActivity → PERMISSIONS_GUIDE)
                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra(MainActivity.EXTRA_OPEN_ROUTE, Routes.PERMISSIONS_GUIDE)
                                addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                            }
                            runCatching { context.startActivity(intent) }
                        } else {
                            // Eksik olan izni doğrudan sistem ayarında aç
                            if (!usageGranted) {
                                UsageStatsHelper.openPermissionSettings(context)
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    permHintText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "İpucunu kapat",
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            permHintSessionDismissed = true
                            if (permHintPassiveMode) {
                                // Pasif link kapatıldıysa kalıcı gizle — bir daha rahatsız etme
                                AppPrefs.setSearchPermHintDismissed(context, true)
                                permHintPermDismissed = true
                            } else {
                                // Aktif ipucu kapatıldıysa sayacı ilerlet (pasif moda daha çabuk geçsin)
                                AppPrefs.incrementSearchPermHintCount(context)
                                permHintCount = AppPrefs.getSearchPermHintCount(context)
                            }
                        }
                )
            }
        }

        // Ghost zone — BOTTOM
        if (showGhostZones) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (dragOffsetY > 0) Color.White.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↓ Alt",
                    color = Color.White.copy(alpha = if (dragOffsetY > 0) 0.80f else 0.30f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (!resultsAbove) searchResultsSection()
        }
    }
}

@Composable
internal fun FullScreenSearchOverlayV2(
    allApps: List<AppInfo>,
    folders: List<AppFolder>,
    folderCustomNames: Map<String, String>,
    searchResults: Map<SourceType, List<SearchDocument>>,
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState,
    suggestedContacts: List<SearchCache.ContactEntry>,
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    onFolderClick: (AppFolder) -> Unit,
    onEnableContactsSource: () -> Unit,
    onEnableFilesSource: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query by rememberSaveable { mutableStateOf("") }
    var fuzzy by remember { mutableStateOf(AppPrefs.isSearchFuzzyEnabled(context)) }
    var phonetic by remember { mutableStateOf(AppPrefs.isSearchPhoneticEnabled(context)) }
    var sortByUsage by remember { mutableStateOf(AppPrefs.isSearchSortByUsage(context)) }
    var maxResults by remember { mutableStateOf(AppPrefs.getSearchMaxResults(context)) }
    var showIcons by remember { mutableStateOf(AppPrefs.isSearchShowIcons(context)) }
    var showAvatar by remember { mutableStateOf(AppPrefs.isSearchShowContactAvatar(context)) }
    var contactsOn by remember { mutableStateOf(AppPrefs.isSearchSourceContactsEnabled(context)) }
    var filesOn by remember { mutableStateOf(AppPrefs.isSearchSourceFilesEnabled(context)) }
    var webFallbackEnabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }

    fun closeOverlay(clearQuery: Boolean = true) {
        if (clearQuery) {
            query = ""
            onQueryChange("")
            keyboardController?.hide()
        }
        onClose()
    }

    BackHandler(enabled = true) { closeOverlay() }

    // Arama acilir acilmaz alan odaklanir ve klavye gosterilir (Huseyin bildirimi — requestFocus eksikti).
    // D284: Staging optimize — Frame 1: render, 50ms, Frame 2: requestFocus, 50ms, Frame 3: show
    // IME padding frame conflict çözüldü (HomeShell Column'dan imePadding kaldırıldı)
    LaunchedEffect(Unit) {
        delay(50)
        runCatching { focusRequester.requestFocus() }
        delay(50)
        keyboardController?.show()
    }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_SEARCH_FUZZY -> fuzzy = AppPrefs.isSearchFuzzyEnabled(context)
                AppPrefs.KEY_SEARCH_PHONETIC -> phonetic = AppPrefs.isSearchPhoneticEnabled(context)
                AppPrefs.KEY_SEARCH_SORT_BY_USAGE -> sortByUsage = AppPrefs.isSearchSortByUsage(context)
                AppPrefs.KEY_SEARCH_MAX_RESULTS -> maxResults = AppPrefs.getSearchMaxResults(context)
                AppPrefs.KEY_SEARCH_SHOW_ICONS -> showIcons = AppPrefs.isSearchShowIcons(context)
                AppPrefs.KEY_SEARCH_SHOW_CONTACT_AVATAR -> showAvatar = AppPrefs.isSearchShowContactAvatar(context)
                AppPrefs.KEY_SEARCH_SOURCE_CONTACTS -> contactsOn = AppPrefs.isSearchSourceContactsEnabled(context)
                AppPrefs.KEY_SEARCH_SOURCE_FILES -> filesOn = AppPrefs.isSearchSourceFilesEnabled(context)
                AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED -> webFallbackEnabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var contactsPermGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsPermGranted = true
            AppPrefs.setSearchSourceContactsEnabled(context, true)
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
            onEnableContactsSource()
        }
    }
    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource()
        }
    }

    LaunchedEffect(Unit) {
        onQueryChange("")
    }
    LaunchedEffect(allApps) {
        withContext(Dispatchers.IO) { SearchCache.warmApps(allApps) }
    }
    LaunchedEffect(contactsOn, contactsPermGranted) {
        if (contactsOn && contactsPermGranted) {
            SearchCache.loadContacts(context)
            SearchCache.observeContacts(context)
        }
    }
    LaunchedEffect(query) { onQueryChange(query) }

    fun SearchDocument.matchesCurrentQuery(): Boolean {
        val q = query.trim().lowercase(Locale("tr"))
        if (q.isBlank()) return false
        return title.lowercase(Locale("tr")).contains(q) ||
            subtitle.lowercase(Locale("tr")).contains(q) ||
            sourceId.lowercase(Locale("tr")).contains(q)
    }

    val appResults = remember(query, allApps, fuzzy, phonetic, sortByUsage, maxResults) {
        if (query.isBlank()) emptyList()
        else SearchCache.searchApps(query, maxResults.coerceAtLeast(8), phonetic, fuzzy, sortByUsage)
    }
    val folderResults = remember(query, folders, folderCustomNames) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase(Locale("tr"))
            folders.filter { folder ->
                val displayName = folderCustomNames[folder.category.categoryId] ?: folder.category.categoryName
                displayName.lowercase(Locale("tr")).contains(q)
            }.take(8)
        }
    }
    val contactResults = remember(query, contactsOn, contactsPermGranted) {
        if (!contactsOn || !contactsPermGranted || query.isBlank()) emptyList()
        else SearchCache.searchContacts(query, 5, phonetic = true, fuzzy = true)
    }
    val fileResults = if (query.isBlank()) emptyList()
    else searchResults[SourceType.FILE].orEmpty().filter { it.matchesCurrentQuery() }.take(8)
    val settingResults = if (query.isBlank()) emptyList()
    else searchResults[SourceType.SETTING].orEmpty().filter { it.matchesCurrentQuery() }.take(8)

    val showFilesPermissionHint = SearchOverlayDecisions.shouldShowFilesPermissionHint(
        query = query,
        filesOn = filesOn,
        filesIndexState = filesIndexState,
    )
    val showWebFallback = SearchOverlayDecisions.shouldShowWebFallback(
        query = query,
        webFallbackEnabled = webFallbackEnabled,
        appCount = appResults.size,
        folderCount = folderResults.size,
        contactCount = contactResults.size,
        settingCount = settingResults.size,
        fileCount = fileResults.size,
        showFilesPermissionHint = showFilesPermissionHint,
    )

    val zeroStateApps = remember(allApps) {
        val visibleByPkg = allApps.filterNot { it.isHidden }.associateBy { it.packageName }
        UsageStatsHelper.getCurrentSlotTopApps(context, days = 28)
            .mapNotNull { visibleByPkg[it] }
            .take(5)
    }
    val historyItems = remember(query) {
        if (query.isBlank()) SearchHistoryPrefs.getAll(context) else emptyList()
    }

    fun recordSearch(queryText: String, title: String, sourceType: SourceType, sourceId: String) {
        SearchHistoryPrefs.record(context, queryText, title, sourceType, sourceId)
    }

    fun openHistoryItem(item: SearchHistoryPrefs.SearchHistoryItem) {
        when (item.sourceType) {
            SourceType.APP.key -> {
                closeOverlay(clearQuery = false)
                onAppClick(item.sourceId)
            }
            SourceType.CATEGORY.key -> folders.firstOrNull { it.category.categoryId == item.sourceId }?.let {
                closeOverlay(clearQuery = false)
                onFolderClick(it)
            }
            SourceType.FILE.key -> {
                closeOverlay(clearQuery = false)
                openSearchDocument(context, item.sourceId)
            }
            SourceType.SETTING.key -> searchResults[SourceType.SETTING].orEmpty()
                .firstOrNull { it.sourceId == item.sourceId }
                ?.let {
                    closeOverlay(clearQuery = false)
                    SystemSettingsCatalog.open(context, it)
                }
            SourceType.CONTACT.key -> SearchCache.getContactList()
                .firstOrNull { it.id.toString() == item.sourceId }
                ?.let {
                    closeOverlay(clearQuery = false)
                    launchDial(context, it.phone)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .semantics { isTraversalGroup = true }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { traversalIndex = 0f },
                    cornerRadius = 24.dp,
                    backgroundAlpha = 0.16f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = { closeOverlay() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_overlay_close),
                                tint = Color.White
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .semantics {
                                    traversalIndex = 1f
                                    contentDescription = context.getString(R.string.search_overlay_field_content_desc)
                                },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Search,
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.search_overlay_title),
                                            color = Color.White.copy(alpha = 0.48f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.search_overlay_clear_query),
                                    tint = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
            }

            if (query.isBlank()) {
                if (zeroStateApps.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_apps_title), Icons.Default.Search) }
                    items(zeroStateApps) { app ->
                        SearchAppRow(app = app, showIcons = showIcons) {
                            recordSearch(app.appName, app.appName, SourceType.APP, app.packageName)
                            closeOverlay(clearQuery = false)
                            onAppClick(app.packageName)
                        }
                    }
                }
                if (suggestedContacts.take(3).isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_contacts_title), Icons.Default.Person) }
                    items(suggestedContacts.take(3)) { contact ->
                        SearchContactRow(context = context, contact = contact, showAvatar = showAvatar, query = contact.displayName)
                    }
                }
                if (historyItems.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_zero_state_history_title), Icons.Default.Search) }
                    items(historyItems) { item ->
                        SearchHistoryRow(item = item) { openHistoryItem(item) }
                    }
                }
                if (zeroStateApps.isEmpty() && suggestedContacts.isEmpty() && historyItems.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.search_zero_state_empty),
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            } else {
                if (appResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_apps), Icons.Default.Search) }
                    items(appResults) { app ->
                        SearchAppRow(app = app, showIcons = showIcons) {
                            recordSearch(query, app.appName, SourceType.APP, app.packageName)
                            SearchStatsPrefs.logClick(context, SourceType.APP.key, 0)
                            closeOverlay(clearQuery = false)
                            onAppClick(app.packageName)
                        }
                    }
                }
                if (folderResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_folders), Icons.Default.Folder) }
                    items(folderResults) { folder ->
                        SearchSimpleRow(
                            leading = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            title = folderCustomNames[folder.category.categoryId] ?: folder.category.categoryName,
                            subtitle = stringResource(R.string.search_folder_result_count, folder.apps.size)
                        ) {
                            recordSearch(query, folder.category.categoryName, SourceType.CATEGORY, folder.category.categoryId)
                            closeOverlay(clearQuery = false)
                            onFolderClick(folder)
                        }
                    }
                }
                if (settingResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_settings), Icons.Default.Search) }
                    items(settingResults) { document ->
                        SearchDocumentRow(document = document, icon = Icons.Default.Search) {
                            recordSearch(query, document.title, SourceType.SETTING, document.sourceId)
                            closeOverlay(clearQuery = false)
                            SystemSettingsCatalog.open(context, document)
                        }
                    }
                }
                if (contactResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_contacts), Icons.Default.Person) }
                    items(contactResults) { contact ->
                        SearchContactRow(context = context, contact = contact, showAvatar = showAvatar, query = query)
                    }
                }
                if (showFilesPermissionHint) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Description, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.home_search_files_permission_required),
                            subtitle = stringResource(R.string.home_search_files_permission_required_desc)
                        ) {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_IMAGES,
                                    android.Manifest.permission.READ_MEDIA_VIDEO,
                                    android.Manifest.permission.READ_MEDIA_AUDIO,
                                )
                            } else {
                                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            filesPermLauncher.launch(permissions)
                        }
                    }
                }
                if (fileResults.isNotEmpty()) {
                    item { HomeSearchGroupHeader(stringResource(R.string.search_group_files), Icons.Default.Description) }
                    items(fileResults) { document ->
                        SearchDocumentRow(document = document, icon = Icons.Default.Description) {
                            recordSearch(query, document.title, SourceType.FILE, document.sourceId)
                            closeOverlay(clearQuery = false)
                            openSearchDocument(context, document.sourceId)
                        }
                    }
                }
                // Görev 2 kök neden: dosya kaynağı hiç açılmadıysa (filesOn=false) hiçbir satır
                // gösterilmiyordu — kullanıcı kaynağı hiç etkinleştiremiyordu.
                if (!filesOn) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Description, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.home_search_files_enable_title),
                            subtitle = stringResource(R.string.home_search_files_enable_desc)
                        ) {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_IMAGES,
                                    android.Manifest.permission.READ_MEDIA_VIDEO,
                                    android.Manifest.permission.READ_MEDIA_AUDIO,
                                )
                            } else arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            filesPermLauncher.launch(permissions)
                        }
                    }
                }
                if (!contactsPermGranted && contactsOn) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.search_contacts_permission_required_title),
                            subtitle = stringResource(R.string.search_contacts_permission_required_desc)
                        ) {
                            contactsPermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    }
                }
                // Görev 2 kök neden: kişi kaynağı hiç açılmadıysa hiçbir satır gösterilmiyordu.
                if (!contactsOn) {
                    item {
                        SearchSimpleRow(
                            leading = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f)) },
                            title = stringResource(R.string.home_search_contacts_enable_title),
                            subtitle = stringResource(R.string.home_search_contacts_enable_desc)
                        ) {
                            contactsPermLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    }
                }
                if (showWebFallback) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.14f) {
                            Column { SearchFallbackRows(context = context, query = query.trim()) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAppRow(
    app: AppInfo,
    showIcons: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    SearchSimpleRow(
        leading = {
            if (showIcons) {
                val cacheKey = "${app.packageName}_32_${app.lastUpdatedTime}"
                val icon by produceState<ImageBitmap?>(null, cacheKey) {
                    value = withContext(Dispatchers.IO) {
                        iconCacheInternal[cacheKey] ?: run {
                            val bmp = runCatching {
                                com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 64)?.asImageBitmap()
                            }.getOrNull()
                            if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                            bmp
                        }
                    }
                }
                if (icon != null) {
                    Image(bitmap = icon!!, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.16f)))
                }
            } else {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
            }
        },
        title = app.appName,
        subtitle = app.packageName,
        onClick = onClick
    )
}

@Composable
private fun SearchDocumentRow(
    document: SearchDocument,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    SearchSimpleRow(
        leading = { Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.72f)) },
        title = document.title,
        subtitle = document.subtitle.ifBlank { document.sourceId }.substringBefore(" | "),
        onClick = onClick
    )
}

@Composable
private fun SearchContactRow(
    context: Context,
    contact: SearchCache.ContactEntry,
    showAvatar: Boolean,
    query: String,
) {
    SearchSimpleRow(
        leading = {
            if (showAvatar && contact.photoUri != null) {
                val avatarBitmap by produceState<ImageBitmap?>(null, contact.photoUri) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(Uri.parse(contact.photoUri))?.use {
                                android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                }
                if (avatarBitmap != null) {
                    Image(bitmap = avatarBitmap!!, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f))
                    }
                }
            } else {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.70f))
                }
            }
        },
        title = contact.displayName,
        subtitle = contact.phone,
    ) {
        SearchHistoryPrefs.record(context, query, contact.displayName, SourceType.CONTACT, contact.id.toString())
        SearchStatsPrefs.logClick(context, SourceType.CONTACT.key, 0)
        ContactActionPrefs.logAction(context, contact.id.toString(), ContactActionPrefs.ActionType.CALL)
        launchDial(context, contact.phone)
    }
}

@Composable
private fun SearchHistoryRow(
    item: SearchHistoryPrefs.SearchHistoryItem,
    onClick: () -> Unit,
) {
    val subtitle = if (item.query.isBlank()) {
        stringResource(R.string.search_history_recent_result)
    } else {
        stringResource(R.string.search_history_query_prefix, item.query)
    }
    SearchSimpleRow(
        leading = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f)) },
        title = item.title,
        subtitle = subtitle,
        onClick = onClick
    )
}

@Composable
private fun SearchSimpleRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, backgroundAlpha = 0.12f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { leading() }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color.White.copy(alpha = 0.52f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun launchDial(context: Context, phone: String) {
    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(dialIntent) }
}

private fun openSearchDocument(context: Context, sourceId: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sourceId))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/**
 * Sıfır sonuçta gösterilen iki fallback satırı — Web'de ara / Play Store'da ara.
 * ACTION_WEB_SEARCH / market:// başarısız olursa https:// ACTION_VIEW'a düşer.
 */
@Composable
private fun SearchFallbackRows(context: Context, query: String) {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = Color.White.copy(alpha = 0.10f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SearchStatsPrefs.logAction(context, "WEB_FALLBACK")
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
                runCatching { context.startActivity(webIntent) }.onFailure {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(fallback) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🌐", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_google, query),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SearchStatsPrefs.logAction(context, "PLAY_FALLBACK")
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(query)))
                runCatching { context.startActivity(marketIntent) }.onFailure {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(query))
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(fallback) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("▶️", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_play_store, query),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Ana ekran arama sonuç grubu başlığı — glass stilinde küçük kaynak etiketi
 * (AllAppsDrawer.SourceGroupHeader'ın home karşılığı).
 */
@Composable
private fun HomeSearchGroupHeader(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null,
                tint = Color.White.copy(alpha = 0.40f), modifier = Modifier.size(11.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.40f), fontSize = 11.sp)
    }
}

/**
 * Ana ekran klasör arama çubuğu.
 * [query] boş değilken aktif — 30s hareketsizlikte [onClear] tetiklenir.
 * [countdown] dışarıdan yönetilir (HomeScreen LaunchedEffect).
 */
@Composable
internal fun FolderSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    countdown: Int,
    modifier: Modifier = Modifier
) {
    val active = query.isNotEmpty() || countdown < 30
    val searchFieldRoleDescription = stringResource(R.string.search_field_role_description)
    Row(
        modifier = modifier
            .height(44.dp)
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(22.dp)
            )
            .background(
                color = Color.White.copy(alpha = if (active) 0.18f else 0.10f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Klasör ara",
            tint = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
        // Döngü P19 madde 3 — HomeAppSearchBar ile aynı desen: ayrı "arama alanı" contentDescription'ı.
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
            decorationBox = { inner ->
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Klasör ara...",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = searchFieldRoleDescription
                }
        )
        if (active) {
            Text(
                text = "${countdown}s",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.wrapContentWidth()
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Aramayı temizle",
                tint = Color.White.copy(alpha = 0.70f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onClear() }
            )
        }
    }
}
