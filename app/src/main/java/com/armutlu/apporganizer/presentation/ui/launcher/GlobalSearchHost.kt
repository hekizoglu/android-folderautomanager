package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.FileIndexState
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType

/**
 * Döngü P08 — `GlobalSearchHost` (roadmap satır 805-866).
 *
 * Her Şeyi Ara'yı sayfa bağımsız, TEK instance haline getiren global katman. Eskiden
 * `HomeScreen.kt` içindeki `searchBarSection` local lambda'sıydı (HEADER/FOOTER konumuna göre
 * `HomeShell.topSearch`/`bottomSearch` slotlarından biri çağrılıyordu) — bu döngüde tek bir
 * composable'a çıkarıldı ki hangi sayfada (Dashboard, klasör pager sayfası, ...) olunursa
 * olunsun aynı arama çubuğu/aynı state kullanılsın (roadmap madde 5: "Aktif sayfa
 * GlobalSearchHost'a business parametresi olarak verilmez").
 *
 * State sahipliği: query/sonuçlar hâlâ [LauncherViewModel] akışlarında (searchQuery,
 * searchResults, filesIndexState) — çift state YARATILMADI (roadmap madde 2). Bu composable
 * sadece o akışlardan `computeGlobalSearchUiState` ile TÜRETİLEN [GlobalSearchUiState]'i okur;
 * kendi sahip olduğu tek state fullscreen arama ekranının açık/kapalı bayrağıdır
 * ([fullScreenSearchOpen]) — bu da eskiden `HomeScreen.kt`de zaten local `remember` idi, davranış
 * DEĞİŞMEDİ, sadece taşındı.
 *
 * NOT — overlay yerleşimi: `FullScreenSearchOverlayV2` kendi kökünde `Modifier.fillMaxSize()`
 * uygulayan opak bir `Box` (bkz. HomeScreenComponents.kt ~2483) — bu yüzden Column tabanlı
 * `topSearch`/`bottomSearch` slotu içine gömülemez (Column'da yalnız kalan alanı kaplar, dock/pager
 * ile üst üste binmesi gereken davranışı bozar). Bu yüzden bu host overlay'i KENDİSİ render ETMEZ;
 * bunun yerine [onFullScreenSearchOpenChanged] ile açık/kapalı durumunu dışarı (HomeScreen) bildirir,
 * HomeScreen bunu eskiden olduğu gibi `HomeShell`'in `overlays` (Box) slotunda render eder — mutlak
 * konumlandırma/z-order BİREBİR korunur, sadece state kaynağı bu host'a taşındı.
 *
 * Davranış BİREBİR korunur:
 * - `homeAppSearchEnabled` açıkken birleşik arama çubuğu ([HomeAppSearchBar]) render edilir.
 * - Kapalı ama `homeSearchEnabled` açıkken sadece klasör filtresi ([FolderSearchBar]) render edilir.
 * - İkisi de kapalıyken hiçbir şey render edilmez (eski `searchBarSection` ile aynı if/else if).
 * - Tıklanınca tam ekran arama açılması ([onOpenFullScreen] → state true) davranışı aynen sürüyor.
 *
 * NOT: Klasör içi arama ([FolderScreen] yerel araması) bu host'un KAPSAMI DIŞINDA — roadmap
 * madde 3, sadece ana ekran global araması taşınır.
 */
@Composable
fun GlobalSearchHost(
    homeAppSearchEnabled: Boolean,
    homeSearchEnabled: Boolean,
    fullscreenSearchEnabled: Boolean,
    allApps: List<AppInfo>,
    folders: List<AppFolder>,
    folderCustomNames: Map<String, String>,
    folderCustomEmojis: Map<String, String>,
    searchQuery: String,
    searchResults: Map<SourceType, List<SearchDocument>>,
    filesIndexState: FileIndexState,
    homeResumeTrigger: Int,
    resultsAbove: Boolean,
    onAppClick: (String) -> Unit,
    onNavigateToFolder: (AppFolder) -> Unit,
    onQueryChange: (String) -> Unit,
    onEnableContactsSource: () -> Unit,
    onEnableFilesSource: () -> Unit,
    // Folder-only aramada (homeAppSearchEnabled=false) local filtre state'i dışarıdan yönetilir —
    // eskiden HomeScreen'in kendi `folderSearchQuery`/`folderSearchCountdown` remember'ıydı,
    // 30s otomatik sıfırlama LaunchedEffect'i hâlâ çağıran tarafta (HomeScreen) kalır çünkü o,
    // arama sonucu değil "klasör grid filtresi" — GlobalSearchHost sadece render eder.
    folderSearchQuery: String,
    onFolderSearchQueryChange: (String) -> Unit,
    onFolderSearchClear: () -> Unit,
    folderSearchCountdown: Int,
    // Fullscreen arama ekranının açık/kapalı hedef durumu dışarıdan (BackHandler, overlay onClose)
    // da değiştirilebilsin diye host, bu değeri hem okur hem `onFullScreenSearchOpenChanged` ile
    // kendi iç kararını (arama çubuğuna tıklanınca true) dışarı yayınlar — tek gerçek kaynak
    // HomeScreen'deki `fullScreenSearchOpen` state'i, host sadece onu okuyup günceller.
    fullScreenSearchOpen: Boolean,
    onFullScreenSearchOpenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val uiState = remember(searchQuery, fullScreenSearchOpen, fullscreenSearchEnabled, searchResults, filesIndexState) {
        computeGlobalSearchUiState(
            query = searchQuery,
            fullscreenOpen = fullScreenSearchOpen,
            fullscreenEnabled = fullscreenSearchEnabled,
            resultGroups = searchResults,
            filesIndexState = filesIndexState,
        )
    }

    if (homeAppSearchEnabled) {
        HomeAppSearchBar(
            allApps = allApps,
            onAppClick = onAppClick,
            // Klasör grubu "Klasor ve Kategori Aramasi" toggle'ına bağlı kalır
            folders = if (homeSearchEnabled) folders else emptyList(),
            folderCustomNames = folderCustomNames,
            folderCustomEmojis = folderCustomEmojis,
            onFolderClick = { folder ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigateToFolder(folder)
            },
            searchResults = uiState.resultGroups,
            onQueryChange = onQueryChange,
            onEnableContactsSource = onEnableContactsSource,
            onEnableFilesSource = onEnableFilesSource,
            filesIndexState = uiState.filesIndexState,
            fullScreenEnabled = fullscreenSearchEnabled,
            onOpenFullScreen = { onFullScreenSearchOpenChanged(true) },
            homeResumeTrigger = homeResumeTrigger,
            // Çubuk alttayken sonuçlar yukarı doğru açılır — sayfa kaymaz (D258)
            resultsAbove = resultsAbove,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
    } else if (homeSearchEnabled) {
        // Uygulama araması kapalı ama klasör araması açık — sadece klasör filtresi
        FolderSearchBar(
            query = folderSearchQuery,
            onQueryChange = onFolderSearchQueryChange,
            onClear = onFolderSearchClear,
            countdown = folderSearchCountdown,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}
