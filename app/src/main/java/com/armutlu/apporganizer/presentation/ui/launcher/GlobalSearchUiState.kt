package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.FileIndexState
import com.armutlu.apporganizer.domain.models.SearchDocument
import com.armutlu.apporganizer.domain.models.SourceType

/**
 * Döngü P08 — `GlobalSearchHost` state sözleşmesi (roadmap satır 805-866).
 *
 * Aramanın sayfa bağımsız GLOBAL bir katmana taşınmasının ilk adımı: query/aktiflik/sonuç
 * akışının TEK sahibi bu state — hangi sayfada olursa olsun (Dashboard, klasör pager sayfası)
 * aynı arama deneyimi sunulur. State sahipliği hâlâ `LauncherViewModel`'de kalır (roadmap madde 2
 * — "mevcut LauncherViewModel akışlarını yeniden kullan, çift state yaratma"); bu dosya sadece
 * o akışlardan TÜRETİLEN salt-okunur anlık görüntüyü (snapshot) ve türetim mantığını taşır.
 *
 * P09 bu state'i tüketip sonuç overlay'ini yeniden tasarlayacak — bu döngüde overlay tasarımı
 * DEĞİŞMEZ, sadece state'in tek bir yerden hesaplanması sağlanır.
 */
data class GlobalSearchUiState(
    val query: String,
    val active: Boolean,
    val overlayVisible: Boolean,
    val fullscreenVisible: Boolean,
    val resultGroups: Map<SourceType, List<SearchDocument>>,
    val filesIndexState: FileIndexState,
) {
    companion object {
        /** Hiçbir arama etkileşimi olmadığı başlangıç durumu. */
        val Empty = GlobalSearchUiState(
            query = "",
            active = false,
            overlayVisible = false,
            fullscreenVisible = false,
            resultGroups = emptyMap(),
            filesIndexState = FileIndexState.Disabled,
        )
    }
}

/**
 * Query + fullscreen açık/kapalı bayrağından `GlobalSearchUiState.active`/`overlayVisible`
 * türetir — pure fonksiyon, ViewModel/Compose bağımlılığı yok (P08 testleri satır 851-857).
 *
 * - `active`: kullanıcı bir şey yazdı mı (boşluksuz uzunluk > 0). İki harften kısa query'de
 *   [LauncherViewModel.searchResults] zaten boş map döner (debounce sonrası `trimmed.length < 2`
 *   filtresi) — o davranışa burada DOKUNULMAZ, sadece "aktiflik" ayrı bir kavram: kullanıcı
 *   yazmaya başladığı an (1 harf bile olsa) arama çubuğu aktif görünür/inline sonuç alanı açılır.
 * - `overlayVisible`: inline sonuç listesi sadece aktifken VE fullscreen mod kapalıyken görünür
 *   (fullscreen açıkken inline liste HomeAppSearchBar içinde zaten render edilmiyordu — mevcut
 *   davranış, bkz. HomeScreenComponents.kt HomeAppSearchBar `fullScreenEnabled` dalı).
 */
internal fun computeGlobalSearchUiState(
    query: String,
    fullscreenOpen: Boolean,
    fullscreenEnabled: Boolean,
    resultGroups: Map<SourceType, List<SearchDocument>>,
    filesIndexState: FileIndexState,
): GlobalSearchUiState {
    val active = query.isNotEmpty()
    val fullscreenVisible = fullscreenOpen && fullscreenEnabled
    return GlobalSearchUiState(
        query = query,
        active = active,
        overlayVisible = active && !fullscreenVisible,
        fullscreenVisible = fullscreenVisible,
        resultGroups = resultGroups,
        filesIndexState = filesIndexState,
    )
}
