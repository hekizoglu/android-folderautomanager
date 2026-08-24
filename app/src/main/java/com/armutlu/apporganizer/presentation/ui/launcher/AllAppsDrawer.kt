package com.armutlu.apporganizer.presentation.ui.launcher


import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import com.armutlu.apporganizer.presentation.ui.common.diamondShine
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import com.armutlu.apporganizer.utils.SystemSettingsCatalog
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

private const val SWIPE_DOWN_THRESHOLD = 90f

// ── Arama + filtre bölümü ─────────────────────────────────────────────────────
@Composable
private fun DrawerSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    searchFocusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    totalCount: Int,
    filteredCount: Int,
    quickFilter: Int,
    onQuickFilterChange: (Int) -> Unit,
    quickFilterCounts: IntArray,
    sortMode: AllAppsSortMode,
    onSortModeChange: (AllAppsSortMode) -> Unit,
    onOpenDrawerSettings: () -> Unit,
    context: android.content.Context,
    pixelLookEnabled: Boolean = false,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val searchBg = onSurface.copy(alpha = 0.10f)
    val dragHandle = onSurface.copy(alpha = 0.20f)

    val quickFilterLabels = listOf("Tümü", "Kullanıcı", "Sistem", "Son 7 gün")

    // S3 — çekmece sadeleştirme: varsayılan kapalı (sade mod), açıkken eski iki chip satırı korunur.
    val chipRowsEnabled by rememberBooleanPreferenceState(
        context = context,
        key = AppPrefs.KEY_DRAWER_CHIP_ROWS_ENABLED,
        read = { AppPrefs.isDrawerChipRowsEnabled(context) },
    )

    // Drag handle
    Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(dragHandle))
    }
    Spacer(Modifier.height(10.dp))

    // Başlık
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Uygulamalar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        val countText = if (searchQuery.isNotBlank() || quickFilter != 0) {
            "$filteredCount / $totalCount"
        } else {
            "$totalCount uygulama"
        }
        Text(countText, fontSize = 12.sp, color = textSecondary)
    }

    // Arama + kapat — elmas parlaması dikkat çeker
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        DrawerSearchBox(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            searchFocusRequester = searchFocusRequester,
            pixelLookEnabled = pixelLookEnabled,
            primary = primary,
            onSurface = onSurface,
            textSecondary = textSecondary,
            searchBg = searchBg,
        )
        if (!chipRowsEnabled) {
            DrawerFilterSortMenu(
                quickFilter = quickFilter,
                quickFilterLabels = quickFilterLabels,
                onQuickFilterChange = onQuickFilterChange,
                sortMode = sortMode,
                onSortModeChange = onSortModeChange,
                haptic = haptic,
                context = context,
                tint = textSecondary,
                secondary = secondary,
            )
        }
        IconButton(onClick = onOpenDrawerSettings, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.drawer_settings_content_description), tint = textSecondary)
        }
        IconButton(
            onClick = {
                keyboardController?.hide()
                onClose()
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.Close, "Kapat", tint = textSecondary, modifier = Modifier.size(20.dp))
        }
    }

    Spacer(Modifier.height(8.dp))

    if (chipRowsEnabled) {
        DrawerChipRows(
            quickFilterLabels = quickFilterLabels,
            quickFilter = quickFilter,
            onQuickFilterChange = onQuickFilterChange,
            quickFilterCounts = quickFilterCounts,
            sortMode = sortMode,
            onSortModeChange = onSortModeChange,
            context = context,
        )
    }
    Spacer(Modifier.height(4.dp))
}

// ── Kaynak grubu başlık chip'i ────────────────────────────────────────────────
@Composable
private fun SourceGroupHeader(label: String, count: Int) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(onSurface.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$label  $count",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.65f),
            )
        }
        Box(Modifier.weight(1f).height(1.dp).background(onSurface.copy(alpha = 0.08f)))
    }
}

@Composable
private fun SearchDocumentRow(
    document: SearchDocument,
    badge: String,
    onClick: () -> Unit,
    showContactActions: Boolean = false,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val context = LocalContext.current
    // Rehber sonucunda telefon numarasi document.subtitle'da tutulur (ContactsIndexer.loadPrimaryPhone).
    // Numara yoksa hizli aksiyonlar gizlenir.
    val phone = document.subtitle.trim()
    val showActions = showContactActions && phone.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(onSurface.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = onSurface.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(
                document.title,
                color = onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = document.subtitle.substringBefore(" | ").ifBlank { document.sourceId }
            Text(subtitle, color = textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showActions) {
            // P1.3: contactId (numara DEGIL) ContactActionPrefs'e - saat bazli oneri altyapisi
            val contactId = document.sourceId.removePrefix("contact:")
            ContactQuickActions(context = context, phone = phone, contactId = contactId)
        }
    }
}

/**
 * Kisi hizli aksiyonlari - Ara / WhatsApp / SMS. CALL_PHONE izni GEREKMEZ
 * (ACTION_DIAL kullanilir, cagriyi kullanici dialer'da baslatir).
 * Her aksiyon SearchStatsPrefs.logAction ile anonim sayaca isaretlenir ve
 * P1.3: AppPrefs.isContactSuggestionsEnabled ise ContactActionPrefs'e (contactId, aksiyon,
 * zaman) olarak loglanir - saat bazli kisi onerisi altyapisi icin. Numara ASLA saklanmaz.
 */
@Composable
private fun ContactQuickActions(context: Context, phone: String, contactId: String = "") {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val encodedPhone = Uri.encode(phone)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = {
                SearchStatsPrefs.logAction(context, "CALL")
                logContactAction(context, contactId, com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.CALL)
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedPhone"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onFailure { Timber.w(it, "ACTION_DIAL baslatilamadi") }
            },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Ara",
                tint = onSurface.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(
            onClick = {
                val normalized = phone.filter { it.isDigit() || it == '+' }
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    SearchStatsPrefs.logAction(context, "WHATSAPP")
                    logContactAction(context, contactId, com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.WHATSAPP)
                }.onFailure { Timber.w(it, "WhatsApp acilamadi, yuklu olmayabilir") }
            },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = "WhatsApp",
                tint = onSurface.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(
            onClick = {
                SearchStatsPrefs.logAction(context, "SMS")
                logContactAction(context, contactId, com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType.SMS)
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$encodedPhone"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                    .onFailure { Timber.w(it, "SMS baslatilamadi") }
            },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Message,
                contentDescription = "SMS",
                tint = onSurface.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * P1.3: contactId bossa (izin sinirlamasi/parse hatasi) sessizce atlar.
 * Ayar kapaliysa (KEY_CONTACT_SUGGESTIONS_ENABLED=false) hicbir kayit yazilmaz.
 */
private fun logContactAction(
    context: Context,
    contactId: String,
    action: com.armutlu.apporganizer.utils.ContactActionPrefs.ActionType,
) {
    if (contactId.isBlank()) return
    if (!AppPrefs.isContactSuggestionsEnabled(context)) return
    com.armutlu.apporganizer.utils.ContactActionPrefs.logAction(context, contactId, action)
}

/**
 * Sıfır sonuçta gösterilen iki fallback satırı — Web'de ara / Play Store'da ara.
 * ACTION_WEB_SEARCH / market:// başarısız olursa https:// ACTION_VIEW'a düşer.
 * HomeAppSearchBar.SearchFallbackRows ile aynı davranış (HomeScreenComponents.kt).
 */
@Composable
private fun DrawerSearchFallbackRows(context: Context, query: String) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SearchStatsPrefs.logAction(context, "WEB_FALLBACK")
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
                runCatching { context.startActivity(webIntent) }.onFailure {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(fallback) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🌐", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_google, query),
            color = onSurface.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
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
                        Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(query)),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(fallback) }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("▶️", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_play_store, query),
            color = onSurface.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun openSearchDocument(context: Context, document: SearchDocument) {
    val intent = when (document.sourceType) {
        SourceType.CONTACT.key -> {
            val contactId = document.sourceId.removePrefix("contact:")
            Intent(
                Intent.ACTION_VIEW,
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId),
            )
        }
        SourceType.FILE.key -> {
            Intent(Intent.ACTION_VIEW, Uri.parse(document.sourceId))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        SourceType.SETTING.key -> {
            SystemSettingsCatalog.open(context, document)
            return
        }
        else -> return
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching { context.startActivity(intent) }
        .onFailure { Timber.w(it, "Search document could not be opened: ${document.sourceType}") }
}

// ── Liste bölümü ──────────────────────────────────────────────────────────────
@Composable
private fun DrawerAppList(
    state: DrawerState,
    listState: LazyListState,
    searchQuery: String,
    iconSize: Dp,
    quickAccess: DrawerQuickAccessConfig,
    callbacks: DrawerCallbacks,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    categories: List<Category> = emptyList(),
    searchResults: Map<SourceType, List<SearchDocument>> = emptyMap(),
    recentNotificationCounts: Map<String, Int> = emptyMap(),
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState =
        com.armutlu.apporganizer.domain.models.FileIndexState.Disabled,
) {
    // Callback grubundan yerel takma adlar (tur 29).
    val onFavoriteAppClick = callbacks.onFavoriteAppClick
    val onRecentAppClick = callbacks.onRecentAppClick
    val onAppClick = callbacks.onAppClick
    val onAppLongClick = callbacks.onAppLongClick
    val onEnableFilesSource = callbacks.onEnableFilesSource
    val onCategoryClick = callbacks.onCategoryClick
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val trLocale = java.util.Locale("tr")
    val context = LocalContext.current
    // P0.3: dosya kaynağı açık ama izin yoksa "0 sonuç" yerine izin kısayolu göster
    val showFilesPermissionHint = searchQuery.isNotBlank() &&
        filesIndexState is com.armutlu.apporganizer.domain.models.FileIndexState.PermissionRequired
    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource()
        }
    }

    // Arama modundayken kategori eşleşmelerini grupla
    val categoryMatches = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.lowercase(trLocale)
            categories.filter { it.categoryName.lowercase(trLocale).contains(q) }
        }
    }
    val contactMatches = searchResults[SourceType.CONTACT].orEmpty()
    val settingMatches = searchResults[SourceType.SETTING].orEmpty()
    val fileMatches = searchResults[SourceType.FILE].orEmpty()
    val hasSearchGroups = searchQuery.isNotBlank() &&
        (
            state.sortedApps.isNotEmpty() || categoryMatches.isNotEmpty() || settingMatches.isNotEmpty() ||
                contactMatches.isNotEmpty() || fileMatches.isNotEmpty() || showFilesPermissionHint
            )
    // Web/Play Store fallback — filtrelenmiş liste + SearchDocument sonuçları boşsa gösterilir (Ayarlar > Arama)
    val webFallbackEnabled = rememberWebFallbackEnabled(context)
    val showWebFallback = shouldShowWebFallback(webFallbackEnabled, searchQuery.trim(), hasSearchGroups)

    if (state.grouped.isNotEmpty()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            drawerQuickAccessSections(
                keySuffix = "",
                searchQuery = searchQuery,
                config = quickAccess,
                iconPackPkg = state.iconPackPkg,
                recentNotificationCounts = recentNotificationCounts,
                callbacks = callbacks,
            )
            state.grouped.forEach { (letter, letterApps) ->
                item(key = "header_$letter") {
                    Box(Modifier.semantics { heading() }) {
                        NiagaraLetterHeader(letter = letter)
                    }
                }
                items(items = letterApps, key = { it.packageName }) { app ->
                    NiagaraAppRow(
                        app = app, iconSize = iconSize, isActive = false,
                        sortMode = state.sortMode,
                        notifTextEnabled = state.notifTextEnabled,
                        recentNotificationCount = recentNotificationCounts[app.packageName] ?: 0,
                        unusedGreyDays = state.unusedGreyDays,
                        iconPackPkg = state.iconPackPkg,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            AppAnalytics.appLaunched("all_apps")
                            onAppClick(app.packageName)
                        },
                        onLongClick = { onAppLongClick?.invoke(app) },
                    )
                }
            }
        }
    } else {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            // D242-DENETIM FINDING-001 fix: bu iki blok eskiden yalnızca gruplu (A-Z) dalda vardı —
            // kullanıcı sıralamayı Akıllı/Kullanım/Boyut/Yükleme'ye değiştirince (düz dal devreye
            // girince) hızlı erişim bölümleri ayar kapatılmış gibi kayboluyordu.
            drawerQuickAccessSections(
                keySuffix = "_flat",
                searchQuery = searchQuery,
                config = quickAccess,
                iconPackPkg = state.iconPackPkg,
                recentNotificationCounts = recentNotificationCounts,
                callbacks = callbacks,
            )
            drawerSearchResultSections(
                context = context,
                onSurface = onSurface,
                textSecondary = textSecondary,
                searchQuery = searchQuery,
                hasSearchGroups = hasSearchGroups,
                sortedApps = state.sortedApps,
                categoryMatches = categoryMatches,
                settingMatches = settingMatches,
                contactMatches = contactMatches,
                fileMatches = fileMatches,
                showFilesPermissionHint = showFilesPermissionHint,
                filesPermLaunch = { filesPermLauncher.launch(it) },
                showWebFallback = showWebFallback,
                iconSize = iconSize,
                sortMode = state.sortMode,
                notifTextEnabled = state.notifTextEnabled,
                unusedGreyDays = state.unusedGreyDays,
                iconPackPkg = state.iconPackPkg,
                haptic = haptic,
                recentNotificationCounts = recentNotificationCounts,
                callbacks = callbacks,
            )

        }
    }
}

// ── Recent + Favorites combined section ──────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRecentFavSection(
    recentApps: List<AppInfo>,
    favoriteApps: List<AppInfo>,
    iconPackPkg: String,
    onRecentAppClick: (String) -> Unit,
    onFavoriteAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(modifier = Modifier.fillMaxWidth()) {
        if (recentApps.isNotEmpty()) {
            NiagaraLetterHeader(letter = '★', label = stringResource(R.string.recent_apps))
            RecentFavRows(
                apps = recentApps,
                iconPackPkg = iconPackPkg,
                onSurface = onSurface,
                onClick = onRecentAppClick,
                onAppLongClick = onAppLongClick,
            )
        }
        if (favoriteApps.isNotEmpty()) {
            NiagaraLetterHeader(letter = '♥', label = "Favoriler")
            RecentFavRows(
                apps = favoriteApps,
                iconPackPkg = iconPackPkg,
                onSurface = onSurface,
                onClick = onFavoriteAppClick,
                onAppLongClick = onAppLongClick,
            )
        }
    }
}

/** Son kullanılanlar/favoriler 4'lü satırları (tur 24: DrawerRecentFavSection'dan çıkarıldı). */
@Composable
private fun RecentFavRows(
    apps: List<AppInfo>,
    iconPackPkg: String,
    onSurface: Color,
    onClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)?,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    apps.chunked(4).forEach { rowApps ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top,
        ) {
            rowApps.forEach { app ->
                RecentFavAppCell(
                    app = app,
                    iconPackPkg = iconPackPkg,
                    onSurface = onSurface,
                    context = context,
                    onClick = { onClick(app.packageName) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAppLongClick?.invoke(app)
                    },
                )
            }
            repeat(4 - rowApps.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Tek uygulama hücresi: async ikon (LruCache) + tek satır etiket (tur 24). */
@Composable
private fun RowScope.RecentFavAppCell(
    app: AppInfo,
    iconPackPkg: String,
    onSurface: Color,
    context: android.content.Context,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        val cacheKey = remember(app.packageName, app.lastUpdatedTime, iconPackPkg) {
            if (iconPackPkg.isNotEmpty()) {
                "${app.packageName}_48_${app.lastUpdatedTime}_$iconPackPkg"
            } else {
                "${app.packageName}_48_${app.lastUpdatedTime}"
            }
        }
        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, cacheKey) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val cached = iconCacheInternal[cacheKey]
                if (cached != null) {
                    cached
                } else {
                    val bmp = runCatching { com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 96)?.asImageBitmap() }.getOrNull()
                    if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                    bmp
                }
            }
        }
        bitmap?.let {
            androidx.compose.foundation.Image(
                bitmap = it, contentDescription = app.appName,
                modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
            )
        } ?: Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)))
        Spacer(Modifier.height(3.dp))
        Text(
            app.appName,
            color = onSurface,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Sidebar ───────────────────────────────────────────────────────────────────
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DrawerRecentNotificationSection(
    apps: List<AppInfo>,
    notificationCounts: Map<String, Int> = emptyMap(),
    iconPackPkg: String,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    DrawerAppIconRowSection(
        apps = apps,
        notificationCounts = notificationCounts,
        iconPackPkg = iconPackPkg,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
        letter = '!',
        label = stringResource(R.string.recent_notifications_row_title),
    )
}

/**
 * EX01 — "Bugün Yüklenenler": bugün firstInstalledTime'a düşen uygulamalar. Aynı ikon-satırı
 * görünümünü (DrawerRecentNotificationSection ile paylaşılan) kullanır, dokununca sıralama
 * modu Yükleme'ye geçirilir (HomeScreen/AllAppsDrawer çağrı yerinde onAppClick zaten launch eder;
 * burada sadece görünüm — sıralama HomeScreen'in giriş kartından zaten INSTALL_DATE'e ayarlanır).
 */
@Composable
private fun DrawerTodayInstalledSection(
    apps: List<AppInfo>,
    iconPackPkg: String,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    DrawerAppIconRowSection(
        apps = apps,
        iconPackPkg = iconPackPkg,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
        letter = '+',
        label = stringResource(R.string.recent_installs_drawer_section_title),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppIconRowSection(
    apps: List<AppInfo>,
    notificationCounts: Map<String, Int> = emptyMap(),
    iconPackPkg: String,
    onAppClick: (String) -> Unit,
    letter: Char,
    label: String,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    if (apps.isEmpty()) return
    val context = LocalContext.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val rowHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        NiagaraLetterHeader(letter = letter, label = label)
        apps.chunked(4).forEach { rowApps ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top,
            ) {
                rowApps.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = { onAppClick(app.packageName) },
                                onLongClick = {
                                    rowHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAppLongClick?.invoke(app)
                                },
                            ),
                    ) {
                        val cacheKey = remember(app.packageName, app.lastUpdatedTime, iconPackPkg) {
                            if (iconPackPkg.isNotEmpty()) {
                                "${app.packageName}_48_${app.lastUpdatedTime}_$iconPackPkg"
                            } else {
                                "${app.packageName}_48_${app.lastUpdatedTime}"
                            }
                        }
                        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, cacheKey) {
                            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val cached = iconCacheInternal[cacheKey]
                                if (cached != null) {
                                    cached
                                } else {
                                    val bmp = runCatching { com.armutlu.apporganizer.utils.loadAppIcon(context, app.packageName, 96)?.asImageBitmap() }.getOrNull()
                                    if (bmp != null) iconCacheInternal.put(cacheKey, bmp)
                                    bmp
                                }
                            }
                        }
                        Box(contentAlignment = Alignment.TopEnd) {
                            bitmap?.let {
                                androidx.compose.foundation.Image(
                                    bitmap = it,
                                    contentDescription = app.appName,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
                                )
                            } ?: Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)))
                            val notificationCount = notificationCounts[app.packageName] ?: 0
                            if (notificationCount > 0) {
                                Badge {
                                    Text(if (notificationCount > 99) "99+" else notificationCount.toString())
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            app.appName,
                            color = onSurface,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                repeat(4 - rowApps.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DrawerSidebar(
    sidebarEntries: List<SidebarEntry>,
    activeSidebarIdx: Int,
    onActivate: (Int) -> Unit,
    onDeactivate: () -> Unit,
    listState: LazyListState,
    scope: kotlinx.coroutines.CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val sidebarPaddingDp = 56.dp
    var boxHeightPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp)
            .padding(vertical = sidebarPaddingDp)
            .onSizeChanged { boxHeightPx = it.height.toFloat() }
            .pointerInput(sidebarEntries) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val n = sidebarEntries.size
                        if (n > 0 && boxHeightPx > 0f) {
                            val idx = (offset.y / (boxHeightPx / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                            onActivate(idx)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                        }
                    },
                    onDragEnd = { onDeactivate() },
                    onDragCancel = { onDeactivate() },
                    onDrag = { change, _ ->
                        change.consume()
                        val n = sidebarEntries.size
                        if (n > 0 && boxHeightPx > 0f) {
                            val idx = (change.position.y / (boxHeightPx / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                            if (activeSidebarIdx != idx) {
                                onActivate(idx)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                            }
                        }
                    },
                )
            }
            .pointerInput(sidebarEntries) {
                detectTapGestures { offset ->
                    val n = sidebarEntries.size
                    if (n > 0 && boxHeightPx > 0f) {
                        val idx = (offset.y / (boxHeightPx / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                        onActivate(idx)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            sidebarEntries.forEachIndexed { idx, entry ->
                val isActive = activeSidebarIdx == idx
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.5f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "sidebar_scale_$idx",
                )
                Text(
                    text = entry.label,
                    fontSize = if (isActive) 16.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) onSurface else secondary,
                    modifier = Modifier.scale(scale).padding(horizontal = 2.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Ana Drawer ────────────────────────────────────────────────────────────────
@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    searchQuery: String = "",
    callbacks: DrawerCallbacks = DrawerCallbacks(),
    iconSize: Dp = 40.dp,
    quickAccess: DrawerQuickAccessConfig = DrawerQuickAccessConfig(),
    focusSearchOnOpen: Boolean = false,
    categories: List<Category> = emptyList(),
    searchResults: Map<SourceType, List<SearchDocument>> = emptyMap(),
    recentNotificationCounts: Map<String, Int> = emptyMap(),
    // P0.3: dosya kaynağı izin/indeks durumu — DrawerAppList "izin gerekli" satırı için kullanır
    filesIndexState: com.armutlu.apporganizer.domain.models.FileIndexState =
        com.armutlu.apporganizer.domain.models.FileIndexState.Disabled,
) {
    // Callback grubundan yerel takma adlar — gövde içi değişiklik minimumda kalır (tur 29).
    val onSearchQueryChange = callbacks.onSearchQueryChange
    val onClose = callbacks.onClose
    val onAppClick = callbacks.onAppClick
    val onAppLongClick = callbacks.onAppLongClick
    val onFavoriteAppClick = callbacks.onFavoriteAppClick
    val onRecentAppClick = callbacks.onRecentAppClick
    val onFocusSearchConsumed = callbacks.onFocusSearchConsumed
    val onOpenDrawerSettings = callbacks.onOpenDrawerSettings
    val onEnableFilesSource = callbacks.onEnableFilesSource
    val onCategoryClick = callbacks.onCategoryClick
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Çift tıkla arama: focusSearchOnOpen=true ise açılınca klavyeyi göster
    LaunchedEffect(focusSearchOnOpen) {
        if (focusSearchOnOpen) {
            runCatching { searchFocusRequester.requestFocus() }
            keyboardController?.show()
            onFocusSearchConsumed()
        }
    }

    // Klavye otomatik açılmasın — kullanıcı arama kutusuna tıklayınca açılır

    var sortMode by remember {
        val saved = AppPrefs.getAllAppsSortMode(context)
        mutableStateOf(parseAllAppsSortMode(saved))
    }
    var activeSidebarIdx by remember { mutableIntStateOf(-1) }
    var quickFilter by remember { mutableStateOf(AppPrefs.getAllAppsQuickFilter(context)) }

    // Görünüm tercihleri ayrı composable'da (tur 21): bu metot küçülür, dinleyici izole olur.
    val appearance = rememberDrawerAppearancePrefs(context)

    // Ağır hesaplamalar ayrı composable'da — DEX VerifyError'u önler (çok fazla register)
    val drawerData = rememberDrawerData(apps, searchQuery, quickFilter, sortMode)
    val sortedApps = drawerData.sortedApps
    val grouped = drawerData.grouped
    val sidebarEntries = drawerData.sidebarEntries
    val quickFilterCounts = drawerData.quickFilterCounts

    // F6: her tus vurusunda event YOK — sorgu 600ms duraksayinca TEK event; sourceMix de
    // sabit APPS_ONLY yerine gercek kaynak karisimindan hesaplanir (kategori/ayar/kisi/dosya
    // eslesmesi varsa MIXED, yalniz dosya varsa FILES_ONLY, hic sonuc yoksa OTHER).
    LaunchedEffect(searchQuery, sortedApps.size, searchResults, categories) {
        val q = searchQuery.trim()
        if (q.length < 2) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        val trLocale = java.util.Locale("tr")
        val lowerQ = q.lowercase(trLocale)
        val categoryHits = categories.count { it.categoryName.lowercase(trLocale).contains(lowerQ) }
        val nonAppHits = categoryHits + searchResults.values.sumOf { it.size }
        val appHits = sortedApps.size
        AppAnalytics.searchPerformed(
            resultCount = searchResultBucket(appHits + nonAppHits),
            latency = com.armutlu.apporganizer.telemetry.TelemetryEvent.LatencyBucket.UNKNOWN,
            sourceMix = searchSourceMix(appHits, categoryHits, nonAppHits),
        )
    }

    val drawerState = DrawerState(
        sortedApps = sortedApps,
        grouped = grouped,
        sidebarEntries = sidebarEntries,
        bgAlpha = appearance.bgAlpha,
        notifTextEnabled = appearance.notifTextEnabled,
        unusedGreyDays = appearance.unusedGreyDays,
        iconPackPkg = appearance.iconPackPkg,
        sortMode = sortMode,
    )

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    val swipeDownThreshold = with(density) { SWIPE_DOWN_THRESHOLD.dp.toPx() }
                    if (dragOffset > swipeDownThreshold) {
                        keyboardController?.hide()
                        onClose()
                    }
                    dragOffset = 0f
                },
                onDragCancel = { dragOffset = 0f },
                onVerticalDrag = { _, delta -> if (delta > 0) dragOffset += delta else dragOffset = 0f },
            )
        },
    ) {
        DrawerBackground(pixelLookEnabled = appearance.pixelLookEnabled, bgAlpha = appearance.bgAlpha)
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DrawerSearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onClose = onClose,
                        searchFocusRequester = searchFocusRequester,
                        keyboardController = keyboardController,
                        haptic = haptic,
                        totalCount = apps.size,
                        filteredCount = sortedApps.size,
                        quickFilter = quickFilter,
                        onQuickFilterChange = {
                            quickFilter = it
                            AppPrefs.setAllAppsQuickFilter(context, it)
                        },
                        quickFilterCounts = quickFilterCounts,
                        sortMode = sortMode,
                        onSortModeChange = { sortMode = it },
                        onOpenDrawerSettings = onOpenDrawerSettings,
                        context = context,
                        pixelLookEnabled = appearance.pixelLookEnabled,
                    )
                    DrawerAppList(
                        state = drawerState,
                        listState = listState,
                        searchQuery = searchQuery,
                        iconSize = iconSize,
                        quickAccess = quickAccess,
                        callbacks = callbacks,
                        haptic = haptic,
                        categories = categories,
                        searchResults = searchResults,
                        recentNotificationCounts = recentNotificationCounts,
                        filesIndexState = filesIndexState,
                    )
                }
                if (sidebarEntries.isNotEmpty()) {
                    DrawerSidebar(
                        sidebarEntries = sidebarEntries,
                        activeSidebarIdx = activeSidebarIdx,
                        onActivate = { activeSidebarIdx = it },
                        onDeactivate = { activeSidebarIdx = -1 },
                        listState = listState,
                        scope = scope,
                        haptic = haptic,
                    )
                }
            }
        }
    }
}

// ── Tur 21: AllAppsDrawer'dan çıkarılan yapı taşları ─────────────────────────

/** Çekmece görünüm tercihleri — reaktif SharedPreferences dinleyicisiyle tek yerde. */
private data class DrawerAppearancePrefs(
    val bgAlpha: Float,
    val notifTextEnabled: Boolean,
    val unusedGreyDays: Int,
    val iconPackPkg: String,
    val pixelLookEnabled: Boolean,
)

@Composable
private fun rememberDrawerAppearancePrefs(context: android.content.Context): DrawerAppearancePrefs {
    var bgAlpha by remember { mutableFloatStateOf(com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)) }
    var notifTextEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    var unusedGreyDays by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context)) }
    var iconPackPkg by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)) }
    // Android (Pixel) Görünümü — çekmecede blur yerine düz yüksek-opasite yüzey + hap arama kutusu
    var pixelLookEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isPixelLookEnabled(context)) }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                com.armutlu.apporganizer.utils.AppPrefs.KEY_ALLAPPS_BG_ALPHA ->
                    bgAlpha = com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_NOTIFICATION_TEXT_ENABLED ->
                    notifTextEnabled = com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_UNUSED_GREY_DAYS ->
                    unusedGreyDays = com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_ICON_PACK ->
                    iconPackPkg = com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)
                com.armutlu.apporganizer.utils.AppPrefs.KEY_PIXEL_LOOK_ENABLED ->
                    pixelLookEnabled = com.armutlu.apporganizer.utils.AppPrefs.isPixelLookEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return DrawerAppearancePrefs(bgAlpha, notifTextEnabled, unusedGreyDays, iconPackPkg, pixelLookEnabled)
}

/** Çekmece arka planı: Pixel modunda düz yüksek-opasite yüzey, aksi halde siyah saydam katman. */
@Composable
private fun DrawerBackground(pixelLookEnabled: Boolean, bgAlpha: Float) {
    if (pixelLookEnabled) {
        // Stok Android çekmecesi: blur yerine düz, yüksek opasiteli nötr yüzey
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = com.armutlu.apporganizer.presentation.ui.theme.PixelLookPolicy.DRAWER_SURFACE_ALPHA,
                    ),
                ),
        )
    } else {
        // blur(20.dp) kaldırıldı: boş bir Box'ın kendi (yok denecek) içeriğini bulanıklaştırıyordu,
        // arkasındaki gerçek launcher ağacını değil — görsel etkisi yoktu, sadece GPU maliyeti vardı.
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgAlpha)))
    }
}


/** Filtre + sıralama dropdown menüsü (tur 21: DrawerSearchBar'dan çıkarıldı). */
@Composable
private fun DrawerFilterSortMenu(
    quickFilter: Int,
    quickFilterLabels: List<String>,
    onQuickFilterChange: (Int) -> Unit,
    sortMode: AllAppsSortMode,
    onSortModeChange: (AllAppsSortMode) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: android.content.Context,
    tint: Color,
    secondary: Color,
) {
    var filterMenuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { filterMenuOpen = true },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Default.Tune,
                stringResource(R.string.drawer_filter_menu_content_description),
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
            Text(
                stringResource(R.string.drawer_filter_menu_section_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            quickFilterLabels.forEachIndexed { idx, label ->
                val active = quickFilter == idx
                DropdownMenuItem(
                    text = { Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = if (active) {
                        { Icon(Icons.Default.Check, null, tint = secondary, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickFilterChange(idx)
                        AppPrefs.setAllAppsQuickFilter(context, idx)
                        filterMenuOpen = false
                    },
                )
            }
            HorizontalDivider()
            Text(
                stringResource(R.string.drawer_sort_menu_section_title),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            listOf(AllAppsSortMode.ALPHA, AllAppsSortMode.USAGE, AllAppsSortMode.SIZE_DESC, AllAppsSortMode.INSTALL_DATE)
                .forEach { baseMode ->
                    val active = sortMode == baseMode || sortMode == baseMode.opposite()
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (sortMode == baseMode.opposite()) baseMode.opposite().label else baseMode.label,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = if (active) {
                            { Icon(Icons.Default.Check, null, tint = secondary, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            val newMode = if (active) sortMode.opposite() else baseMode
                            onSortModeChange(newMode)
                            AppPrefs.setAllAppsSortMode(context, newMode.name)
                            filterMenuOpen = false
                        },
                    )
                }
        }
    }
}


/** Çekmece arama kutusu: odak parlaması, elmas parlama, Pixel pill şekli (tur 24). */
@Composable
private fun RowScope.DrawerSearchBox(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    pixelLookEnabled: Boolean,
    primary: Color,
    onSurface: Color,
    textSecondary: Color,
    searchBg: Color,
) {
    val context = LocalContext.current
    val shineEnabled by rememberBooleanPreferenceState(
        context = context,
        key = AppPrefs.KEY_SEARCH_SHINE_ENABLED,
        read = { AppPrefs.isSearchShineEnabled(context) },
    )
    var searchFocused by remember { mutableStateOf(false) }
    val focusGlowAlpha by animateFloatAsState(
        targetValue = if (searchFocused) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "all_apps_search_focus_glow",
    )
    val focusColor = Color(0xFFB6FF4D)
    // Pixel modunda tam yuvarlatılmış hap (pill) — stok Android arama kutusu hissi.
    val searchBoxShape = if (pixelLookEnabled) RoundedCornerShape(percent = 50) else RoundedCornerShape(22.dp)
    val searchBoxOuterShape = if (pixelLookEnabled) RoundedCornerShape(percent = 50) else RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier.weight(1f).height(44.dp)
            .border(
                width = 3.dp,
                color = focusColor.copy(alpha = 0.18f * focusGlowAlpha),
                shape = searchBoxOuterShape,
            )
            .padding(2.dp)
            .border(
                width = 1.5.dp,
                color = focusColor.copy(alpha = 0.82f * focusGlowAlpha),
                shape = searchBoxShape,
            )
            .clip(searchBoxShape).background(
                if (pixelLookEnabled) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (searchFocused) 0.9f else 0.75f)
                } else if (searchFocused) searchBg.copy(alpha = 0.18f) else searchBg,
            )
            .diamondShine(shineEnabled, searchBoxShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, "Ara", tint = textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) Text("Uygulama ara...", color = textSecondary, fontSize = 14.sp)
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(primary),
                    textStyle = TextStyle(color = onSurface, fontSize = 14.sp),
                    modifier = Modifier
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged { searchFocused = it.isFocused },
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Temizle", tint = textSecondary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

/** Hızlı filtre + sıralama chip satırları (chipRowsEnabled açıkken; tur 24). */
@Composable
private fun DrawerChipRows(
    quickFilterLabels: List<String>,
    quickFilter: Int,
    onQuickFilterChange: (Int) -> Unit,
    quickFilterCounts: IntArray,
    sortMode: AllAppsSortMode,
    onSortModeChange: (AllAppsSortMode) -> Unit,
    context: android.content.Context,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val haptic = LocalHapticFeedback.current
    // Hızlı filtre chip'leri
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(quickFilterLabels) { idx, label ->
            val active = quickFilter == idx
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (active) secondary.copy(alpha = 0.8f) else onSurface.copy(alpha = 0.08f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickFilterChange(idx)
                    }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                val countLabel = if (idx < quickFilterCounts.size) " (${quickFilterCounts[idx]})" else ""
                Text(
                    label + if (active) countLabel else "",
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSecondary else Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
    // Sıralama chip'leri — 4 temel kategori, aynı butona basınca yön değişir
    val baseSortChips = listOf(
        AllAppsSortMode.ALPHA,
        AllAppsSortMode.USAGE,
        AllAppsSortMode.SIZE_DESC,
        AllAppsSortMode.INSTALL_DATE,
    )
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        itemsIndexed(baseSortChips) { _, baseMode ->
            val isActive = sortMode == baseMode || sortMode == baseMode.opposite()
            val displayLabel = if (sortMode == baseMode.opposite()) baseMode.opposite().label else baseMode.label
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (isActive) primary else onSurface.copy(alpha = 0.12f))
                    .clickable {
                        val newMode = if (isActive) sortMode.opposite() else baseMode
                        onSortModeChange(newMode)
                        AppPrefs.setAllAppsSortMode(context, newMode.name)
                    }
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Text(
                    displayLabel,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}


/** Web/Play fallback tercihi — reaktif SharedPreferences dinleyicisiyle (tur 24). */
@Composable
private fun rememberWebFallbackEnabled(context: android.content.Context): Boolean {
    var enabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED) {
                enabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}

/** Hızlı erişim bölümleri: son kullanılanlar + favoriler, bildirim alanlar, bugün yüklenenler.
 *  Gruplu ve düz listelerde aynı içerik farklı key ile render edilir (keySuffix). */
private fun LazyListScope.drawerQuickAccessSections(
    keySuffix: String,
    searchQuery: String,
    config: DrawerQuickAccessConfig,
    iconPackPkg: String,
    recentNotificationCounts: Map<String, Int>,
    callbacks: DrawerCallbacks,
) {
    if (searchQuery.isNotEmpty() || !config.hasAnySection) return
    val onRecentAppClick = callbacks.onRecentAppClick
    val onFavoriteAppClick = callbacks.onFavoriteAppClick
    val onAppClick = callbacks.onAppClick
    val onAppLongClick = callbacks.onAppLongClick
    val max = config.maxShownAppsCount
    if (config.recentAppsEnabled && config.recentApps.isNotEmpty() || config.favoritesEnabled && config.favoriteApps.isNotEmpty()) {
        item(key = "recent_fav_section$keySuffix") {
            DrawerRecentFavSection(
                recentApps = if (config.recentAppsEnabled) config.recentApps.take(max) else emptyList(),
                favoriteApps = if (config.favoritesEnabled) config.favoriteApps.take(max) else emptyList(),
                iconPackPkg = iconPackPkg,
                onRecentAppClick = onRecentAppClick,
                onFavoriteAppClick = onFavoriteAppClick,
                onAppLongClick = onAppLongClick,
            )
        }
    }
    if (config.recentNotificationAppsEnabled && config.recentNotificationApps.isNotEmpty()) {
        item(key = "recent_notification_apps_section$keySuffix") {
            DrawerRecentNotificationSection(
                apps = config.recentNotificationApps.take(max),
                notificationCounts = recentNotificationCounts,
                iconPackPkg = iconPackPkg,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
            )
        }
    }
    if (config.todayInstalledAppsEnabled && config.todayInstalledApps.isNotEmpty()) {
        item(key = "today_installed_apps_section$keySuffix") {
            DrawerTodayInstalledSection(
                apps = config.todayInstalledApps.take(max),
                iconPackPkg = iconPackPkg,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
            )
        }
    }
}

/** Arama modu sonuç bölümleri: boş durum + web fallback + kaynak grupları (tur 24). */
private fun LazyListScope.drawerSearchResultSections(
    context: android.content.Context,
    onSurface: Color,
    textSecondary: Color,
    searchQuery: String,
    hasSearchGroups: Boolean,
    sortedApps: List<AppInfo>,
    categoryMatches: List<Category>,
    settingMatches: List<SearchDocument>,
    contactMatches: List<SearchDocument>,
    fileMatches: List<SearchDocument>,
    showFilesPermissionHint: Boolean,
    filesPermLaunch: (Array<String>) -> Unit,
    showWebFallback: Boolean,
    iconSize: Dp,
    sortMode: AllAppsSortMode,
    notifTextEnabled: Boolean,
    unusedGreyDays: Int,
    iconPackPkg: String,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    recentNotificationCounts: Map<String, Int>,
    callbacks: DrawerCallbacks,
) {
    val onCategoryClick = callbacks.onCategoryClick
    val onAppClick = callbacks.onAppClick
    val onAppLongClick = callbacks.onAppLongClick
    if (sortedApps.isEmpty() && categoryMatches.isEmpty() && settingMatches.isEmpty() &&
        contactMatches.isEmpty() && fileMatches.isEmpty() && !showFilesPermissionHint
    ) {
        item {
            Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_results), color = textSecondary, fontSize = 14.sp)
            }
        }
        if (showWebFallback) {
            item(key = "web_fallback_rows") {
                Column(Modifier.padding(top = 12.dp)) {
                    DrawerSearchFallbackRows(context = context, query = searchQuery.trim())
                }
            }
        }
    } else {
        // Arama modunda kaynak bazlı gruplama
        if (hasSearchGroups && sortedApps.isNotEmpty()) {
            item(key = "source_header_apps") {
                SourceGroupHeader(label = "Uygulamalar", count = sortedApps.size)
            }
        }
        items(items = sortedApps, key = { it.packageName }) { app ->
            NiagaraAppRow(
                app = app, iconSize = iconSize, isActive = false,
                sortMode = sortMode,
                notifTextEnabled = notifTextEnabled,
                recentNotificationCount = recentNotificationCounts[app.packageName] ?: 0,
                unusedGreyDays = unusedGreyDays,
                iconPackPkg = iconPackPkg,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    AppAnalytics.appLaunched("all_apps")
                    onAppClick(app.packageName)
                },
                onLongClick = { onAppLongClick?.invoke(app) },
            )
        }
        // Kategori eşleşmeleri
        if (hasSearchGroups && categoryMatches.isNotEmpty()) {
            item(key = "source_header_categories") {
                SourceGroupHeader(label = "Kategoriler", count = categoryMatches.size)
            }
            items(items = categoryMatches, key = { "cat_${it.categoryId}" }) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(cat.categoryId) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(onSurface.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(cat.iconEmoji.ifBlank { "📁" }, fontSize = 18.sp)
                    }
                    Column {
                        Text(cat.categoryName, color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Kategori", color = textSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        if (hasSearchGroups && settingMatches.isNotEmpty()) {
            item(key = "source_header_settings") {
                SourceGroupHeader(label = "Ayarlar", count = settingMatches.size)
            }
            itemsIndexed(items = settingMatches, key = { _, doc -> "setting_${doc.sourceId}" }) { index, document ->
                SearchDocumentRow(
                    document = document,
                    badge = "A",
                    onClick = {
                        SearchStatsPrefs.logClick(context, SourceType.SETTING.key, index)
                        openSearchDocument(context, document)
                    },
                )
            }
        }
        if (hasSearchGroups && contactMatches.isNotEmpty()) {
            item(key = "source_header_contacts") {
                SourceGroupHeader(label = "Kisiler", count = contactMatches.size)
            }
            itemsIndexed(items = contactMatches, key = { _, doc -> "contact_${doc.sourceId}" }) { index, document ->
                SearchDocumentRow(
                    document = document,
                    badge = "K",
                    onClick = {
                        SearchStatsPrefs.logClick(context, SourceType.CONTACT.key, index)
                        openSearchDocument(context, document)
                    },
                    showContactActions = true,
                )
            }
        }
        // P0.3: dosya kaynağı açık ama izin yoksa "0 sonuç" yerine izin kısayolu
        if (hasSearchGroups && showFilesPermissionHint) {
            item(key = "source_files_permission_hint") {
                SourceGroupHeader(label = "Dosyalar", count = 0)
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
                            filesPermLaunch(permissions)
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.home_search_files_permission_required),
                        color = onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (hasSearchGroups && fileMatches.isNotEmpty()) {
            item(key = "source_header_files") {
                SourceGroupHeader(label = "Dosyalar", count = fileMatches.size)
            }
            itemsIndexed(items = fileMatches, key = { _, doc -> "file_${doc.sourceId}" }) { index, document ->
                SearchDocumentRow(
                    document = document,
                    badge = "D",
                    onClick = {
                        SearchStatsPrefs.logClick(context, SourceType.FILE.key, index)
                        openSearchDocument(context, document)
                    },
                )
            }
        }
    }
}
