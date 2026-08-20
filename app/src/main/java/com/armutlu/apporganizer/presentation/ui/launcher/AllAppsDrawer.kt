# Kod İncelemesi - Bulunan Hatalar ve Düzeltmeler

Kodu detaylıca inceledim. Aşağıdaki **gerçek hatalar/buglar** tespit edildi:

## 🔴 Kritik Hatalar

1. **Arama modunda sonuçlar gizleniyor**: `DrawerAppList` içinde `state.grouped` her zaman `sortedApps`'ten (arama filtresi dahil) hesaplanıyor. Bu yüzden `if (state.grouped.isNotEmpty())` kontrolü arama sırasında da `true` dönüyor ve **Kategoriler/Kişiler/Ayarlar/Dosyalar** sonuçları hiç gösterilmiyordu. Düzeltme: `searchQuery.isBlank() && state.grouped.isNotEmpty()`.

2. **Sidebar (A-Z hızlı kaydırma) arama sırasında yanlış çalışıyor**: `sidebarEntries`, harf-gruplu (browse) düzenine göre index hesaplıyor ama arama modunda liste tamamen farklı (düz + bölüm başlıklı) render ediliyor. Sidebar arama sırasında da gösterildiği için sürüklemede **yanlış konuma scroll** ediyordu. Düzeltme: sidebar sadece `searchQuery.isBlank()` iken gösteriliyor.

## 🟠 Derleme Hataları (Eksik import/tanım)

3. `Image` composable'ı kullanılıyor ama import edilmemiş.
4. `LazyRow` kullanılıyor ama import edilmemiş.
5. `Modifier.semantics { heading() }` kullanılıyor ama `semantics`/`heading` import edilmemiş.
6. `AllAppsSortMode` enum'ı hiçbir yerde tanımlı değil (projede başka dosyada varsa aşağıdaki bloğu silin).

## 🟡 Mantık/Tutarlılık Hataları

7. `quickFilterCounts: IntArray` parametresi sona kadar taşınıyor ama **hiç kullanılmıyordu** (dead code) — artık chip ve dropdown'da sayaç olarak gösteriliyor.
8. Dropdown menüde hızlı filtre seçilince `AppPrefs.setAllAppsQuickFilter` **iki kez** çağrılıyordu (hem `onQuickFilterChange` içinde hem doğrudan) — tekrar kaldırıldı.
9. `ContactQuickActions`: CALL ve SMS, intent **başarısız olsa bile** loglanıyordu; WhatsApp ise sadece başarılı başlatmada logluyordu. Tutarsızlık giderildi — hepsi `onSuccess` içinde loglanıyor.
10. `NiagaraAppRow` placeholder'ında uygulama adı **iki kez** gösteriliyordu (AppIconRow zaten altında gösteriyor, ayrıca sağda tekrar Text vardı) — düzeltildi.
11. `SortChips`/dropdown'daki etiket seçme mantığı gereksiz karmaşıktı, sadeleştirildi.
12. Küçük yazım hatası: `"Kisiler"` → `"Kişiler"`.
13. Performans: `Int`/`Float` state'ler için `mutableStateOf` yerine `mutableIntStateOf`/`mutableFloatStateOf` kullanıldı (autoboxing önlenir).

---

Aşağıda **tam düzeltilmiş kod** yer almaktadır:

```kotlin
package com.armutlu.apporganizer.presentation.ui.launcher

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType
import com.armutlu.apporganizer.presentation.ui.common.diamondShine
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.SystemSettingsCatalog
import com.armutlu.apporganizer.utils.ContactActionPrefs
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.utils.loadAppIcon
import com.armutlu.apporganizer.presentation.ui.theme.PixelLookPolicy
import com.armutlu.apporganizer.domain.models.FileIndexState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

// ── Sabitler ──────────────────────────────────────────────────────────────────
private const val SWIPE_DOWN_THRESHOLD = 90f
private const val MAX_RECENT_FAV_COUNT = 4
private const val ICON_CACHE_SIZE = 48
private const val ICON_LOAD_SIZE = 96
private const val SEARCH_DEBOUNCE_MS = 600L
private const val ICON_CACHE_MAX_SIZE = 50
private val TR_LOCALE = Locale("tr")

// ── Dosya seviyesinde sabit veriler ──────────────────────────────────────────
private val QUICK_FILTER_LABELS = listOf("Tümü", "Kullanıcı", "Sistem", "Son 7 gün")
private val BASE_SORT_CHIPS = listOf(
    AllAppsSortMode.ALPHA,
    AllAppsSortMode.USAGE,
    AllAppsSortMode.SIZE_DESC,
    AllAppsSortMode.INSTALL_DATE
)

// ── Bellek dostu LruCache (50 ikon limitiyle) ────────────────────────────────
private val iconCacheInternal = LruCache<String, ImageBitmap>(ICON_CACHE_MAX_SIZE)

/**
 * NOT: `AllAppsSortMode` enum'ı proje içinde başka bir dosyada (örn. AllAppsSortMode.kt)
 * zaten tanımlıysa AŞAĞIDAKİ BLOĞU SİLİN. Verilen kaynak dosyada bu tip hiçbir yerde
 * tanımlanmadığı/import edilmediği için derleme hatası vermemesi adına buraya
 * minimal bir tanım eklenmiştir.
 */
enum class AllAppsSortMode(val label: String) {
    ALPHA("A-Z"),
    ALPHA_DESC("Z-A"),
    USAGE("Son kullanım"),
    USAGE_ASC("Eski kullanım"),
    SIZE_DESC("Büyük boyut"),
    SIZE_ASC("Küçük boyut"),
    INSTALL_DATE("Yeni yüklenen"),
    INSTALL_DATE_ASC("Eski yüklenen")
}

// ── Yardımcı Fonksiyonlar ────────────────────────────────────────────────────

/**
 * Güvenli intent başlatma - tüm hata yönetimini tek yerde toplar
 */
private fun Context.safeStartActivity(intent: Intent, errorTag: String, onSuccess: () -> Unit = {}) {
    try {
        startActivity(intent)
        onSuccess()
    } catch (e: SecurityException) {
        Timber.w(e, "$errorTag SecurityException: ${e.message}")
    } catch (e: Exception) {
        Timber.w(e, "$errorTag başlatılamadı: ${e.message}")
    }
}

/**
 * İkon yükleme - LruCache ile bellek dostu
 */
@Composable
private fun rememberAppIcon(
    context: Context,
    packageName: String,
    lastUpdatedTime: Long,
    iconPackPkg: String,
    size: Int = ICON_CACHE_SIZE
): ImageBitmap? {
    val cacheKey = remember(packageName, lastUpdatedTime, iconPackPkg) {
        if (iconPackPkg.isNotEmpty())
            "${packageName}_${size}_${lastUpdatedTime}_$iconPackPkg"
        else
            "${packageName}_${size}_${lastUpdatedTime}"
    }

    return produceState<ImageBitmap?>(null, cacheKey) {
        value = withContext(Dispatchers.IO) {
            iconCacheInternal.get(cacheKey) ?: runCatching {
                loadAppIcon(context, packageName, ICON_LOAD_SIZE)?.asImageBitmap()
            }.getOrNull()?.also { loaded ->
                iconCacheInternal.put(cacheKey, loaded)
            }
        }
    }.value
}

// ── AllAppsSortMode Extension ────────────────────────────────────────────────
private fun AllAppsSortMode.opposite(): AllAppsSortMode {
    return when (this) {
        AllAppsSortMode.ALPHA -> AllAppsSortMode.ALPHA_DESC
        AllAppsSortMode.ALPHA_DESC -> AllAppsSortMode.ALPHA
        AllAppsSortMode.USAGE -> AllAppsSortMode.USAGE_ASC
        AllAppsSortMode.USAGE_ASC -> AllAppsSortMode.USAGE
        AllAppsSortMode.SIZE_DESC -> AllAppsSortMode.SIZE_ASC
        AllAppsSortMode.SIZE_ASC -> AllAppsSortMode.SIZE_DESC
        AllAppsSortMode.INSTALL_DATE -> AllAppsSortMode.INSTALL_DATE_ASC
        AllAppsSortMode.INSTALL_DATE_ASC -> AllAppsSortMode.INSTALL_DATE
    }
}

// ── AppIconRow - Ortak ikon satırı bileşeni ─────────────────────────────────
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppIconRow(
    app: AppInfo,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    notificationCount: Int = 0,
    showBadge: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val onSurface = MaterialTheme.colorScheme.onSurface
    val iconPackPkg = AppPrefs.getIconPack(context)

    val bitmap = rememberAppIcon(
        context = context,
        packageName = app.packageName,
        lastUpdatedTime = app.lastUpdatedTime,
        iconPackPkg = iconPackPkg
    )

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    } else {
        Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = clickModifier
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
            if (showBadge && notificationCount > 0) {
                Badge {
                    Text(if (notificationCount > 99) "99+" else notificationCount.toString())
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            app.appName,
            color = onSurface,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Quick Filter Chips ────────────────────────────────────────────────────────
@Composable
private fun QuickFilterChips(
    quickFilterLabels: List<String>,
    quickFilter: Int,
    onQuickFilterChange: (Int) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    counts: IntArray = IntArray(0)
) {
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(quickFilterLabels) { idx, label ->
            val active = quickFilter == idx
            val count = counts.getOrNull(idx)
            val displayText = if (count != null) "$label ($count)" else label
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (active) secondary.copy(alpha = 0.8f) else onSurface.copy(alpha = 0.08f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickFilterChange(idx)
                    }
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    displayText,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onSecondary else Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ── Sort Chips ────────────────────────────────────────────────────────────────
@Composable
private fun SortChips(
    sortMode: AllAppsSortMode,
    onSortModeChange: (AllAppsSortMode) -> Unit,
    onSortModePersist: (AllAppsSortMode) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(BASE_SORT_CHIPS) { _, baseMode ->
            val isActive = sortMode == baseMode || sortMode == baseMode.opposite()
            // Basitleştirildi: aktifse mevcut sortMode'un etiketi, değilse temel modun etiketi
            val displayLabel = if (isActive) sortMode.label else baseMode.label
            Box(
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    .background(if (isActive) primary else onSurface.copy(alpha = 0.12f))
                    .clickable {
                        val newMode = if (isActive) sortMode.opposite() else baseMode
                        onSortModeChange(newMode)
                        onSortModePersist(newMode)
                    }
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    displayLabel,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ── Arama Çubuğu ─────────────────────────────────────────────────────────────
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
    context: Context,
    pixelLookEnabled: Boolean = false,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val searchBg = onSurface.copy(alpha = 0.10f)
    val dragHandle = onSurface.copy(alpha = 0.20f)

    val chipRowsEnabled by rememberBooleanPreferenceState(
        context = context,
        key = AppPrefs.KEY_DRAWER_CHIP_ROWS_ENABLED,
        read = { AppPrefs.isDrawerChipRowsEnabled(context) }
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Uygulamalar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onSurface)
        val countText = if (searchQuery.isNotBlank() || quickFilter != 0)
            "$filteredCount / $totalCount" else "$totalCount uygulama"
        Text(countText, fontSize = 12.sp, color = textSecondary)
    }

    // Arama kutusu - shape'ler remember edildi
    val shineEnabled by rememberBooleanPreferenceState(
        context = context,
        key = AppPrefs.KEY_SEARCH_SHINE_ENABLED,
        read = { AppPrefs.isSearchShineEnabled(context) }
    )
    var searchFocused by remember { mutableStateOf(false) }
    val focusGlowAlpha by animateFloatAsState(
        targetValue = if (searchFocused) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "all_apps_search_focus_glow",
    )
    val focusColor = Color(0xFFB6FF4D)

    val searchBoxShape = remember(pixelLookEnabled) {
        if (pixelLookEnabled) RoundedCornerShape(percent = 50) else RoundedCornerShape(22.dp)
    }
    val searchBoxOuterShape = remember(pixelLookEnabled) {
        if (pixelLookEnabled) RoundedCornerShape(percent = 50) else RoundedCornerShape(24.dp)
    }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    } else if (searchFocused) searchBg.copy(alpha = 0.18f) else searchBg
                )
                .diamondShine(shineEnabled, searchBoxShape)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
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
                            .onFocusChanged { searchFocused = it.isFocused }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Temizle", tint = textSecondary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // Filtre menüsü
        if (!chipRowsEnabled) {
            var filterMenuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { filterMenuOpen = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        stringResource(R.string.drawer_filter_menu_content_description),
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = filterMenuOpen, onDismissRequest = { filterMenuOpen = false }) {
                    Text(
                        stringResource(R.string.drawer_filter_menu_section_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    QUICK_FILTER_LABELS.forEachIndexed { idx, label ->
                        val active = quickFilter == idx
                        val count = quickFilterCounts.getOrNull(idx)
                        val text = if (count != null) "$label ($count)" else label
                        DropdownMenuItem(
                            text = { Text(text, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (active) {
                                { Icon(Icons.Default.Check, null, tint = secondary, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // NOT: Persist işlemi zaten onQuickFilterChange callback'i içinde
                                // (AllAppsDrawer tarafında) yapılıyor; burada tekrar çağırmaya gerek yok.
                                onQuickFilterChange(idx)
                                filterMenuOpen = false
                            }
                        )
                    }
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.drawer_sort_menu_section_title),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    BASE_SORT_CHIPS.forEach { baseMode ->
                        val active = sortMode == baseMode || sortMode == baseMode.opposite()
                        val label = if (active) sortMode.label else baseMode.label
                        DropdownMenuItem(
                            text = { Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (active) {
                                { Icon(Icons.Default.Check, null, tint = secondary, modifier = Modifier.size(18.dp)) }
                            } else null,
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

        IconButton(onClick = onOpenDrawerSettings, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.drawer_settings_content_description), tint = textSecondary)
        }
        IconButton(
            onClick = { keyboardController?.hide(); onClose() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.Close, "Kapat", tint = textSecondary, modifier = Modifier.size(20.dp))
        }
    }

    Spacer(Modifier.height(8.dp))

    if (chipRowsEnabled) {
        QuickFilterChips(
            quickFilterLabels = QUICK_FILTER_LABELS,
            quickFilter = quickFilter,
            onQuickFilterChange = onQuickFilterChange,
            haptic = haptic,
            counts = quickFilterCounts
        )
        SortChips(
            sortMode = sortMode,
            onSortModeChange = onSortModeChange,
            onSortModePersist = { mode -> AppPrefs.setAllAppsSortMode(context, mode.name) }
        )
    }
    Spacer(Modifier.height(4.dp))
}

// ── Kaynak Grubu Başlığı ──────────────────────────────────────────────────────
@Composable
private fun SourceGroupHeader(label: String, count: Int) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(onSurface.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$label  $count",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = onSurface.copy(alpha = 0.65f)
            )
        }
        Box(Modifier.weight(1f).height(1.dp).background(onSurface.copy(alpha = 0.08f)))
    }
}

// ── SearchDocumentRow ─────────────────────────────────────────────────────────
@Composable
private fun SearchDocumentRow(
    document: SearchDocument,
    badge: String,
    onClick: () -> Unit,
    showContactActions: Boolean = false
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val context = LocalContext.current
    val phone = document.subtitle?.trim().orEmpty()
    val showActions = showContactActions && phone.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(onSurface.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(badge, color = onSurface.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(document.title.orEmpty(), color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = document.subtitle?.substringBefore(" | ")?.ifBlank { document.sourceId } ?: document.sourceId
            Text(subtitle, color = textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showActions) {
            val contactId = document.sourceId.removePrefix("contact:")
            ContactQuickActions(context = context, phone = phone, contactId = contactId)
        }
    }
}

// ── ContactQuickActions ───────────────────────────────────────────────────────
@Composable
private fun ContactQuickActions(context: Context, phone: String, contactId: String = "") {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val encodedPhone = Uri.encode(phone)

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedPhone"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.safeStartActivity(intent, "ACTION_DIAL") {
                    // Sadece intent başarıyla başlatıldıysa logla (WhatsApp ile tutarlı)
                    SearchStatsPrefs.logAction(context, "CALL")
                    logContactAction(context, contactId, ContactActionPrefs.ActionType.CALL)
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Call, contentDescription = "Ara",
                tint = onSurface.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
        }
        IconButton(
            onClick = {
                val normalized = phone.filter { it.isDigit() || it == '+' }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.safeStartActivity(intent, "WhatsApp") {
                    SearchStatsPrefs.logAction(context, "WHATSAPP")
                    logContactAction(context, contactId, ContactActionPrefs.ActionType.WHATSAPP)
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp",
                tint = onSurface.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
        }
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$encodedPhone"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.safeStartActivity(intent, "SMS") {
                    SearchStatsPrefs.logAction(context, "SMS")
                    logContactAction(context, contactId, ContactActionPrefs.ActionType.SMS)
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS",
                tint = onSurface.copy(alpha = 0.70f), modifier = Modifier.size(16.dp))
        }
    }
}

// ── logContactAction ─────────────────────────────────────────────────────────
private fun logContactAction(
    context: Context,
    contactId: String,
    action: ContactActionPrefs.ActionType
) {
    if (contactId.isBlank() || !AppPrefs.isContactSuggestionsEnabled(context)) return
    ContactActionPrefs.logAction(context, contactId, action)
}

// ── Fallback Rows ────────────────────────────────────────────────────────────
@Composable
private fun DrawerSearchFallbackRows(context: Context, query: String) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SearchStatsPrefs.logAction(context, "WEB_FALLBACK")
                val webIntent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
                context.safeStartActivity(webIntent, "WEB_SEARCH") {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.safeStartActivity(fallback, "WEB_SEARCH_FALLBACK")
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🌐", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_google, query),
            color = onSurface.copy(alpha = 0.85f),
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
                context.safeStartActivity(marketIntent, "MARKET_SEARCH") {
                    val fallback = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(query))
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.safeStartActivity(fallback, "PLAY_FALLBACK")
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("▶️", fontSize = 16.sp)
        Text(
            stringResource(R.string.search_fallback_play_store, query),
            color = onSurface.copy(alpha = 0.85f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── openSearchDocument ───────────────────────────────────────────────────────
private fun openSearchDocument(context: Context, document: SearchDocument) {
    val intent = when (document.sourceType) {
        SourceType.CONTACT.key -> {
            val contactId = document.sourceId.removePrefix("contact:")
            Intent(
                Intent.ACTION_VIEW,
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
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

    context.safeStartActivity(intent, "SearchDocument")
}

// ── DrawerAppList ─────────────────────────────────────────────────────────────
@Composable
private fun DrawerAppList(
    state: DrawerState,
    listState: LazyListState,
    searchQuery: String,
    iconSize: Dp,
    favoritesEnabled: Boolean,
    favoriteApps: List<AppInfo>,
    onFavoriteAppClick: (String) -> Unit,
    recentAppsEnabled: Boolean,
    recentApps: List<AppInfo>,
    onRecentAppClick: (String) -> Unit,
    recentNotificationAppsEnabled: Boolean = false,
    recentNotificationApps: List<AppInfo> = emptyList(),
    todayInstalledAppsEnabled: Boolean = false,
    todayInstalledApps: List<AppInfo> = emptyList(),
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)?,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    categories: List<Category> = emptyList(),
    searchResults: Map<SourceType, List<SearchDocument>> = emptyMap(),
    recentNotificationCounts: Map<String, Int> = emptyMap(),
    filesIndexState: FileIndexState = FileIndexState.Disabled,
    onEnableFilesSource: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    maxShownAppsCount: Int = MAX_RECENT_FAV_COUNT,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val context = LocalContext.current

    val showFilesPermissionHint = searchQuery.isNotBlank() &&
        filesIndexState is FileIndexState.PermissionRequired

    val filesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            AppPrefs.setSearchSourceFilesEnabled(context, true)
            onEnableFilesSource()
        }
    }

    // Arama modunda kategori eşleşmeleri - memoized
    val categoryMatches = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.lowercase(TR_LOCALE)
            categories.filter { it.categoryName.lowercase(TR_LOCALE).contains(q) }
        }
    }

    val contactMatches = searchResults[SourceType.CONTACT].orEmpty()
    val settingMatches = searchResults[SourceType.SETTING].orEmpty()
    val fileMatches = searchResults[SourceType.FILE].orEmpty()

    val hasSearchGroups = remember(
        searchQuery, state.sortedApps.size, categoryMatches.size,
        settingMatches.size, contactMatches.size, fileMatches.size, showFilesPermissionHint
    ) {
        searchQuery.isNotBlank() &&
            (state.sortedApps.isNotEmpty() || categoryMatches.isNotEmpty() || settingMatches.isNotEmpty() ||
                contactMatches.isNotEmpty() || fileMatches.isNotEmpty() || showFilesPermissionHint)
    }

    // Web fallback ayarı - DisposableEffect(Unit)
    var webFallbackEnabled by remember { mutableStateOf(AppPrefs.isSearchWebFallbackEnabled(context)) }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPrefs.KEY_SEARCH_WEB_FALLBACK_ENABLED) {
                webFallbackEnabled = AppPrefs.isSearchWebFallbackEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val showWebFallback = webFallbackEnabled &&
        searchQuery.trim().length >= 2 &&
        !hasSearchGroups &&
        state.sortedApps.isEmpty() &&
        categoryMatches.isEmpty() &&
        settingMatches.isEmpty() &&
        contactMatches.isEmpty() &&
        fileMatches.isEmpty() &&
        !showFilesPermissionHint

    // ── DÜZELTME (KRİTİK): Bu bayrak, harf-gruplu (A-Z) görünümün SADECE
    // arama yapılmadığı (göz atma) modunda kullanılmasını sağlar. Eskiden
    // `state.grouped` her zaman `sortedApps`'ten (arama filtresi dahil)
    // hesaplandığı için arama sırasında Kategoriler/Kişiler/Ayarlar/Dosyalar
    // sonuçları tamamen gizleniyordu.
    val useLetterGroupedView = searchQuery.isBlank() && state.grouped.isNotEmpty()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Hızlı erişim bölümleri
        if (searchQuery.isEmpty()) {
            if (recentAppsEnabled && recentApps.isNotEmpty() ||
                favoritesEnabled && favoriteApps.isNotEmpty()
            ) {
                item(key = "recent_fav_section") {
                    DrawerRecentFavSection(
                        recentApps = if (recentAppsEnabled) recentApps.take(maxShownAppsCount) else emptyList(),
                        favoriteApps = if (favoritesEnabled) favoriteApps.take(maxShownAppsCount) else emptyList(),
                        onRecentAppClick = onRecentAppClick,
                        onFavoriteAppClick = onFavoriteAppClick,
                        onAppLongClick = onAppLongClick,
                    )
                }
            }
            if (recentNotificationAppsEnabled && recentNotificationApps.isNotEmpty()) {
                item(key = "recent_notification_apps_section") {
                    DrawerRecentNotificationSection(
                        apps = recentNotificationApps.take(maxShownAppsCount),
                        notificationCounts = recentNotificationCounts,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                    )
                }
            }
            if (todayInstalledAppsEnabled && todayInstalledApps.isNotEmpty()) {
                item(key = "today_installed_apps_section") {
                    DrawerTodayInstalledSection(
                        apps = todayInstalledApps.take(maxShownAppsCount),
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                    )
                }
            }
        }

        // Gruplu veya düz liste
        if (useLetterGroupedView) {
            state.grouped.forEach { (letter, letterApps) ->
                item(key = "header_$letter") {
                    Box(Modifier.semantics { heading() }) {
                        NiagaraLetterHeader(letter = letter)
                    }
                }
                items(items = letterApps, key = { it.packageName }) { app ->
                    NiagaraAppRow(
                        app = app,
                        iconSize = iconSize,
                        isActive = false,
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
                        onLongClick = { onAppLongClick?.invoke(app) }
                    )
                }
            }
        } else {
            // Düz liste (arama modu ya da filtre sonucu boş görünüm)
            val noResults = state.sortedApps.isEmpty() &&
                categoryMatches.isEmpty() &&
                settingMatches.isEmpty() &&
                contactMatches.isEmpty() &&
                fileMatches.isEmpty() &&
                !showFilesPermissionHint

            if (noResults) {
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
                // Arama sonuçları
                if (hasSearchGroups && state.sortedApps.isNotEmpty()) {
                    item(key = "source_header_apps") {
                        SourceGroupHeader(label = "Uygulamalar", count = state.sortedApps.size)
                    }
                }
                items(items = state.sortedApps, key = { it.packageName }) { app ->
                    NiagaraAppRow(
                        app = app,
                        iconSize = iconSize,
                        isActive = false,
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
                        onLongClick = { onAppLongClick?.invoke(app) }
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(onSurface.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
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

                // Ayarlar sonuçları
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
                            }
                        )
                    }
                }

                // Kişiler sonuçları
                if (hasSearchGroups && contactMatches.isNotEmpty()) {
                    item(key = "source_header_contacts") {
                        SourceGroupHeader(label = "Kişiler", count = contactMatches.size)
                    }
                    itemsIndexed(items = contactMatches, key = { _, doc -> "contact_${doc.sourceId}" }) { index, document ->
                        SearchDocumentRow(
                            document = document,
                            badge = "K",
                            onClick = {
                                SearchStatsPrefs.logClick(context, SourceType.CONTACT.key, index)
                                openSearchDocument(context, document)
                            },
                            showContactActions = true
                        )
                    }
                }

                // Dosya izin uyarısı
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
                                    filesPermLauncher.launch(permissions)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                stringResource(R.string.home_search_files_permission_required),
                                color = onSurface,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Dosya sonuçları
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
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Recent+Favorites Bölümü ──────────────────────────────────────────────────
@Composable
private fun DrawerRecentFavSection(
    recentApps: List<AppInfo>,
    favoriteApps: List<AppInfo>,
    onRecentAppClick: (String) -> Unit,
    onFavoriteAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (recentApps.isNotEmpty()) {
            NiagaraLetterHeader(letter = '★', label = stringResource(R.string.recent_apps))
            recentApps.chunked(4).forEach { rowApps ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top
                ) {
                    rowApps.forEach { app ->
                        AppIconRow(
                            app = app,
                            iconSize = 44.dp,
                            onClick = { onRecentAppClick(app.packageName) },
                            onLongClick = { onAppLongClick?.invoke(app) }
                        )
                    }
                    repeat(4 - rowApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (favoriteApps.isNotEmpty()) {
            NiagaraLetterHeader(letter = '♥', label = "Favoriler")
            favoriteApps.chunked(4).forEach { rowApps ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top
                ) {
                    rowApps.forEach { app ->
                        AppIconRow(
                            app = app,
                            iconSize = 44.dp,
                            onClick = { onFavoriteAppClick(app.packageName) },
                            onLongClick = { onAppLongClick?.invoke(app) }
                        )
                    }
                    repeat(4 - rowApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Bildirim Bölümü ───────────────────────────────────────────────────────────
@Composable
private fun DrawerRecentNotificationSection(
    apps: List<AppInfo>,
    notificationCounts: Map<String, Int> = emptyMap(),
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    DrawerAppIconRowSection(
        apps = apps,
        notificationCounts = notificationCounts,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
        letter = '!',
        label = stringResource(R.string.recent_notifications_row_title),
    )
}

// ── Bugün Yüklenenler Bölümü ─────────────────────────────────────────────────
@Composable
private fun DrawerTodayInstalledSection(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    DrawerAppIconRowSection(
        apps = apps,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
        letter = '+',
        label = stringResource(R.string.recent_installs_drawer_section_title),
    )
}

// ── Ortak İkon Satırı Bölümü ─────────────────────────────────────────────────
@Composable
private fun DrawerAppIconRowSection(
    apps: List<AppInfo>,
    notificationCounts: Map<String, Int> = emptyMap(),
    onAppClick: (String) -> Unit,
    letter: Char,
    label: String,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
) {
    if (apps.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        NiagaraLetterHeader(letter = letter, label = label)
        apps.chunked(4).forEach { rowApps ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                rowApps.forEach { app ->
                    AppIconRow(
                        app = app,
                        iconSize = 44.dp,
                        onClick = { onAppClick(app.packageName) },
                        onLongClick = { onAppLongClick?.invoke(app) },
                        notificationCount = notificationCounts[app.packageName] ?: 0,
                        showBadge = notificationCounts[app.packageName] != null
                    )
                }
                repeat(4 - rowApps.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ── DrawerSidebar ─────────────────────────────────────────────────────────────
@Composable
private fun DrawerSidebar(
    sidebarEntries: List<SidebarEntry>,
    activeSidebarIdx: Int,
    onActivate: (Int) -> Unit,
    onDeactivate: () -> Unit,
    listState: LazyListState,
    scope: CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val sidebarPaddingDp = 56.dp

    // mutableFloatStateOf ile primitive state - GC dostu
    val boxHeightPx = remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp)
            .padding(vertical = sidebarPaddingDp)
            .onSizeChanged {
                boxHeightPx.floatValue = it.height.toFloat()
            }
            .pointerInput(sidebarEntries) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val n = sidebarEntries.size
                        val height = boxHeightPx.floatValue // güncel değer
                        if (n > 0 && height > 0f) {
                            val idx = (offset.y / (height / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
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
                        val height = boxHeightPx.floatValue // güncel değer
                        if (n > 0 && height > 0f) {
                            val idx = (change.position.y / (height / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                            if (activeSidebarIdx != idx) {
                                onActivate(idx)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                            }
                        }
                    }
                )
            }
            .pointerInput(sidebarEntries) {
                detectTapGestures { offset ->
                    val n = sidebarEntries.size
                    val height = boxHeightPx.floatValue // güncel değer
                    if (n > 0 && height > 0f) {
                        val idx = (offset.y / (height / n)).toInt().coerceIn(0, sidebarEntries.lastIndex)
                        onActivate(idx)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch { listState.scrollToItem(sidebarEntries[idx].scrollIndex) }
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sidebarEntries.forEachIndexed { idx, entry ->
                val isActive = activeSidebarIdx == idx
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.5f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "sidebar_scale_$idx"
                )
                Text(
                    text = entry.label,
                    fontSize = if (isActive) 16.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) onSurface else secondary,
                    modifier = Modifier.scale(scale).padding(horizontal = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

// ── Data Classes ──────────────────────────────────────────────────────────────
data class DrawerState(
    val sortedApps: List<AppInfo>,
    val grouped: Map<Char, List<AppInfo>>,
    val sidebarEntries: List<SidebarEntry>,
    val bgAlpha: Float,
    val notifTextEnabled: Boolean,
    val unusedGreyDays: Int,
    val iconPackPkg: String,
    val sortMode: AllAppsSortMode
)

data class DrawerData(
    val sortedApps: List<AppInfo>,
    val grouped: Map<Char, List<AppInfo>>,
    val sidebarEntries: List<SidebarEntry>,
    val quickFilterCounts: IntArray
)

data class SidebarEntry(
    val label: String,
    val scrollIndex: Int
)

// ── rememberDrawerData ────────────────────────────────────────────────────────
@Composable
private fun rememberDrawerData(
    apps: List<AppInfo>,
    searchQuery: String,
    quickFilter: Int,
    sortMode: AllAppsSortMode
): DrawerData {
    // Filtreleme ve sıralama - memoized
    val sortedApps = remember(apps, searchQuery, quickFilter, sortMode) {
        var result = apps

        // Arama filtresi
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase(TR_LOCALE)
            result = result.filter { it.appName.lowercase(TR_LOCALE).contains(query) }
        }

        // Hızlı filtre
        when (quickFilter) {
            1 -> result = result.filter { !it.isSystemApp }
            2 -> result = result.filter { it.isSystemApp }
            3 -> {
                val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                result = result.filter { it.lastOpenedTime >= weekAgo }
            }
        }

        // Sıralama
        when (sortMode) {
            AllAppsSortMode.ALPHA -> result.sortedBy { it.appName.lowercase(TR_LOCALE) }
            AllAppsSortMode.ALPHA_DESC -> result.sortedByDescending { it.appName.lowercase(TR_LOCALE) }
            AllAppsSortMode.USAGE -> result.sortedByDescending { it.lastOpenedTime }
            AllAppsSortMode.USAGE_ASC -> result.sortedBy { it.lastOpenedTime }
            AllAppsSortMode.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
            AllAppsSortMode.SIZE_ASC -> result.sortedBy { it.sizeBytes }
            AllAppsSortMode.INSTALL_DATE -> result.sortedByDescending { it.firstInstalledTime }
            AllAppsSortMode.INSTALL_DATE_ASC -> result.sortedBy { it.firstInstalledTime }
        }
    }

    // Gruplama (sadece göz atma modunda kullanılacak - bkz. DrawerAppList)
    val grouped = remember(sortedApps) {
        val groupedMap = sortedApps.groupBy {
            it.appName.firstOrNull()?.uppercaseChar() ?: '#'
        }
        groupedMap.toSortedMap()
    }

    // Sidebar girişleri
    val sidebarEntries = remember(grouped) {
        val entries = mutableListOf<SidebarEntry>()
        var index = 0
        grouped.keys.forEach { letter ->
            entries.add(SidebarEntry(letter.toString(), index))
            index += grouped[letter]?.size ?: 0
        }
        entries
    }

    // Quick filter counts - sadece apps değiştiğinde hesapla
    val quickFilterCounts = remember(apps) {
        val userCount = apps.count { !it.isSystemApp }
        val systemCount = apps.count { it.isSystemApp }
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recent7Count = apps.count { it.firstInstalledTime >= weekAgo }
        intArrayOf(apps.size, userCount, systemCount, recent7Count)
    }

    return DrawerData(
        sortedApps = sortedApps,
        grouped = grouped,
        sidebarEntries = sidebarEntries,
        quickFilterCounts = quickFilterCounts
    )
}

// ── NiagaraLetterHeader (Eksik import için placeholder) ──────────────────────
@Composable
private fun NiagaraLetterHeader(letter: Char, label: String? = null) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label ?: letter.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface.copy(alpha = 0.45f)
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(onSurface.copy(alpha = 0.08f)))
    }
}

// ── NiagaraAppRow (Eksik import için placeholder) ────────────────────────────
// NOT: Bu fonksiyonun gerçek/tam implementasyonu projenin başka bir dosyasında
// bulunmalıdır. Aşağıdaki versiyon, DÜZELTİLMİŞ bir yer tutucudur:
// eskiden uygulama adı hem AppIconRow içinde (ikon altında) hem de yanda
// tekrar gösteriliyordu (duplicate). Şimdi tek bir satır düzeninde gösteriliyor.
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NiagaraAppRow(
    app: AppInfo,
    iconSize: Dp,
    isActive: Boolean,
    sortMode: AllAppsSortMode,
    notifTextEnabled: Boolean,
    recentNotificationCount: Int,
    unusedGreyDays: Int,
    iconPackPkg: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val context = LocalContext.current
    val onSurface = MaterialTheme.colorScheme.onSurface

    val bitmap = rememberAppIcon(
        context = context,
        packageName = app.packageName,
        lastUpdatedTime = app.lastUpdatedTime,
        iconPackPkg = iconPackPkg
    )

    val rowClickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowClickModifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.appName,
                    modifier = Modifier.size(iconSize).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
            if (notifTextEnabled && recentNotificationCount > 0) {
                Badge {
                    Text(if (recentNotificationCount > 99) "99+" else recentNotificationCount.toString())
                }
            }
        }
        Text(
            text = app.appName,
            color = onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Ana Drawer ────────────────────────────────────────────────────────────────
@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
    iconSize: Dp = 40.dp,
    favoriteApps: List<AppInfo> = emptyList(),
    favoritesEnabled: Boolean = false,
    onFavoriteAppClick: (String) -> Unit = {},
    recentApps: List<AppInfo> = emptyList(),
    recentAppsEnabled: Boolean = false,
    onRecentAppClick: (String) -> Unit = {},
    recentNotificationAppsEnabled: Boolean = false,
    recentNotificationApps: List<AppInfo> = emptyList(),
    todayInstalledAppsEnabled: Boolean = false,
    todayInstalledApps: List<AppInfo> = emptyList(),
    focusSearchOnOpen: Boolean = false,
    onFocusSearchConsumed: () -> Unit = {},
    categories: List<Category> = emptyList(),
    searchResults: Map<SourceType, List<SearchDocument>> = emptyMap(),
    recentNotificationCounts: Map<String, Int> = emptyMap(),
    onOpenDrawerSettings: () -> Unit = {},
    filesIndexState: FileIndexState = FileIndexState.Disabled,
    onEnableFilesSource: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    maxShownAppsCount: Int = MAX_RECENT_FAV_COUNT,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // mutableFloatStateOf ile primitive state - GC dostu
    val dragOffset = remember { mutableFloatStateOf(0f) }

    // Çift tıkla arama
    LaunchedEffect(focusSearchOnOpen) {
        if (focusSearchOnOpen) {
            runCatching {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }
            onFocusSearchConsumed()
        }
    }

    var sortMode by remember {
        val saved = AppPrefs.getAllAppsSortMode(context)
        mutableStateOf(parseAllAppsSortMode(saved))
    }
    var activeSidebarIdx by remember { mutableIntStateOf(-1) }
    var quickFilter by remember { mutableIntStateOf(AppPrefs.getAllAppsQuickFilter(context)) }

    var bgAlpha by remember { mutableFloatStateOf(AppPrefs.getAllAppsBgAlpha(context)) }
    var notifTextEnabled by remember { mutableStateOf(AppPrefs.isNotificationTextEnabled(context)) }
    var unusedGreyDays by remember { mutableIntStateOf(AppPrefs.getUnusedGreyDays(context)) }
    var iconPackPkg by remember { mutableStateOf(AppPrefs.getIconPack(context)) }
    var pixelLookEnabled by remember { mutableStateOf(AppPrefs.isPixelLookEnabled(context)) }

    // Pref değişikliklerini dinle - DisposableEffect(Unit)
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_ALLAPPS_BG_ALPHA -> bgAlpha = AppPrefs.getAllAppsBgAlpha(context)
                AppPrefs.KEY_NOTIFICATION_TEXT_ENABLED -> notifTextEnabled = AppPrefs.isNotificationTextEnabled(context)
                AppPrefs.KEY_UNUSED_GREY_DAYS -> unusedGreyDays = AppPrefs.getUnusedGreyDays(context)
                AppPrefs.KEY_ICON_PACK -> iconPackPkg = AppPrefs.getIconPack(context)
                AppPrefs.KEY_PIXEL_LOOK_ENABLED -> pixelLookEnabled = AppPrefs.isPixelLookEnabled(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Ağır hesaplamalar - memoized
    val drawerData = rememberDrawerData(apps, searchQuery, quickFilter, sortMode)
    val sortedApps = drawerData.sortedApps
    val grouped = drawerData.grouped
    val sidebarEntries = drawerData.sidebarEntries
    val quickFilterCounts = drawerData.quickFilterCounts

    // Analytics - debounce edilmiş
    LaunchedEffect(searchQuery, sortedApps.size, searchResults, categories) {
        val q = searchQuery.trim()
        if (q.length < 2) return@LaunchedEffect

        delay(SEARCH_DEBOUNCE_MS)

        val lowerQ = q.lowercase(TR_LOCALE)
        val categoryHits = categories.count { it.categoryName.lowercase(TR_LOCALE).contains(lowerQ) }
        val nonAppHits = categoryHits + searchResults.values.sumOf { it.size }
        val appHits = sortedApps.size

        AppAnalytics.searchPerformed(
            resultCount = when (appHits + nonAppHits) {
                0 -> TelemetryEvent.ResultBucket.ZERO
                in 1..5 -> TelemetryEvent.ResultBucket.ONE_TO_FIVE
                in 6..20 -> TelemetryEvent.ResultBucket.SIX_TO_TWENTY
                else -> TelemetryEvent.ResultBucket.TWENTY_ONE_PLUS
            },
            latency = TelemetryEvent.LatencyBucket.UNKNOWN,
            sourceMix = when {
                appHits > 0 && nonAppHits == 0 -> TelemetryEvent.SearchSourceMix.APPS_ONLY
                appHits > 0 || categoryHits > 0 -> TelemetryEvent.SearchSourceMix.MIXED
                nonAppHits > 0 -> TelemetryEvent.SearchSourceMix.FILES_ONLY
                else -> TelemetryEvent.SearchSourceMix.OTHER
            }
        )
    }

    val drawerState = DrawerState(
        sortedApps = sortedApps,
        grouped = grouped,
        sidebarEntries = sidebarEntries,
        bgAlpha = bgAlpha,
        notifTextEnabled = notifTextEnabled,
        unusedGreyDays = unusedGreyDays,
        iconPackPkg = iconPackPkg,
        sortMode = sortMode
    )

    Box(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    val swipeDownThreshold = with(density) { SWIPE_DOWN_THRESHOLD.dp.toPx() }
                    if (dragOffset.floatValue > swipeDownThreshold) {
                        keyboardController?.hide()
                        onClose()
                    }
                    dragOffset.floatValue = 0f
                },
                onDragCancel = { dragOffset.floatValue = 0f },
                onVerticalDrag = { _, delta ->
                    if (delta > 0) dragOffset.floatValue += delta else dragOffset.floatValue = 0f
                }
            )
        }
    ) {
        // Arka plan
        if (pixelLookEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = PixelLookPolicy.DRAWER_SURFACE_ALPHA
                        )
                    )
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgAlpha)))
        }

        // İçerik
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
                        pixelLookEnabled = pixelLookEnabled
                    )

                    DrawerAppList(
                        state = drawerState,
                        listState = listState,
                        searchQuery = searchQuery,
                        iconSize = iconSize,
                        favoritesEnabled = favoritesEnabled,
                        favoriteApps = favoriteApps,
                        onFavoriteAppClick = onFavoriteAppClick,
                        recentAppsEnabled = recentAppsEnabled,
                        recentApps = recentApps,
                        onRecentAppClick = onRecentAppClick,
                        recentNotificationAppsEnabled = recentNotificationAppsEnabled,
                        recentNotificationApps = recentNotificationApps,
                        todayInstalledAppsEnabled = todayInstalledAppsEnabled,
                        todayInstalledApps = todayInstalledApps,
                        onAppClick = onAppClick,
                        onAppLongClick = onAppLongClick,
                        haptic = haptic,
                        categories = categories,
                        searchResults = searchResults,
                        recentNotificationCounts = recentNotificationCounts,
                        filesIndexState = filesIndexState,
                        onEnableFilesSource = onEnableFilesSource,
                        onCategoryClick = onCategoryClick,
                        maxShownAppsCount = maxShownAppsCount
                    )
                }

                // DÜZELTME: Sidebar (A-Z hızlı kaydırma) sadece arama yapılmadığı
                // (göz atma/harf-gruplu) modda anlamlıdır. Arama sırasında
                // scrollIndex değerleri gerçek liste sırasıyla uyuşmadığı için
                // gizleniyor.
                if (searchQuery.isBlank() && sidebarEntries.isNotEmpty()) {
                    DrawerSidebar(
                        sidebarEntries = sidebarEntries,
                        activeSidebarIdx = activeSidebarIdx,
                        onActivate = { activeSidebarIdx = it },
                        onDeactivate = { activeSidebarIdx = -1 },
                        listState = listState,
                        scope = scope,
                        haptic = haptic
                    )
                }
            }
        }
    }
}
```

## Özet Tablo

| # | Hata | Etki | Çözüm |
|---|------|------|-------|
| 1 | Arama modunda `grouped` her zaman doluyordu | Kişi/Ayar/Dosya/Kategori sonuçları hiç görünmüyordu | `searchQuery.isBlank()` koşulu eklendi |
| 2 | Sidebar arama sırasında da gösteriliyordu | Yanlış scroll pozisyonu | `searchQuery.isBlank()` koşuluyla gizlendi |
| 3 | Eksik importlar (`Image`, `LazyRow`, `semantics`, `heading`) | Derleme hatası | Importlar eklendi |
| 4 | `AllAppsSortMode` tanımsız | Derleme hatası | Minimal enum eklendi (proje içinde varsa silinmeli) |
| 5 | `quickFilterCounts` kullanılmıyordu | Ölü kod / eksik özellik | Chip ve dropdown'da sayaç gösterimi eklendi |
| 6 | Çift `AppPrefs.setAllAppsQuickFilter` çağrısı | Gereksiz tekrar | Tek çağrıya indirildi |
| 7 | CALL/SMS başarısız olsa da loglanıyordu | Yanlış analytics verisi | Sadece başarılı `onSuccess` içinde loglanıyor |
| 8 | Uygulama adı iki kez basılıyordu | UI görsel bug | Tek satırlı temiz layout |
| 9 | Karmaşık etiket seçim mantığı | Okunabilirlik | Sadeleştirildi |
| 10 | `mutableStateOf<Int/Float>` | Gereksiz autoboxing | `mutableIntStateOf`/`mutableFloatStateOf` |
| 11 | "Kisiler" yazım hatası | Lokalizasyon | "Kişiler" olarak düzeltildi |