package com.armutlu.apporganizer.presentation.ui.launcher

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * Saf yardımcı — geçerli pager sayfa index'ini [HomePageAnchor]'a çevirir (yazma yönü).
 * `HomePageAnchorResolver.resolve` bunun tersini yapar (anchor -> index, okuma yönü).
 * `HomeScreen.kt`'deki `snapshotFlow { pagerState.currentPage }` toplayıcısı bu fonksiyonu
 * kullanır — Compose/Android bağımlılığı yoktur, doğrudan birim testlerinden çağrılabilir.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P05.
 */
internal fun anchorForCurrentPage(pages: List<HomePageSpec>, rawPage: Int): HomePageAnchor {
    val safePage = rawPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    return when (val spec = pages.getOrNull(safePage)) {
        is HomePageSpec.Dashboard -> HomePageAnchor.Dashboard
        is HomePageSpec.FolderPage -> spec.firstFolderCategoryId
            ?.let { HomePageAnchor.Folder(it) }
            ?: HomePageAnchor.PageIndex(safePage)
        null -> HomePageAnchor.PageIndex(safePage)
    }
}

/**
 * Saf yardımcı — sayfa listesi değiştiğinde (klasör eklendi/silindi, Dashboard aç/kapat)
 * mevcut current page'in yeni listede hâlâ geçerli olup olmadığını kontrol eder; değilse
 * güvenli sınıra (`0..lastIndex`) çeker. `PagerState.pageCount` zaten clamp eder, bu yardımcı
 * ek olarak "güvenli sınır" davranışını saf/test edilebilir biçimde belgeler.
 */
internal fun clampPageToSafeBounds(pages: List<HomePageSpec>, currentPage: Int): Int {
    if (pages.isEmpty()) return 0
    return currentPage.coerceIn(0, pages.lastIndex)
}

/**
 * Döngü P13 — sayfa planı (klasör reorder/silme, Dashboard aç/kapat, page size değişimi)
 * değiştiğinde current page'in yeni planda SEMANTİK olarak yeniden çözülmesini sağlar.
 *
 * Ham index persistence kullanılmaz (roadmap P13 kabul kriteri): eski plandaki current page
 * önce [anchorForCurrentPage] ile anchor'a çevrilir (`previousPages[previousPageIndex]`),
 * sonra bu anchor yeni planda [HomePageAnchorResolver.resolve] ile tekrar index'e çözülür.
 * Böylece "3. sayfadaydım" değil "İş klasöründeydim" bilgisi taşınır — reorder sonrası
 * kullanıcı rastgele başka sayfaya fırlamaz (aynı klasör yeni planda başka index'e taşınmışsa
 * oraya takip eder; klasör silinmişse `HomePageAnchorResolver` kuralları uygulanır).
 *
 * `previousPages` boşsa (ilk composition) veya iki plan da aynı stableKey dizisine sahipse
 * (gerçek bir değişiklik yoksa) gereksiz re-resolve yapılmaz — çağıran taraf zaten
 * `pages` identity/stableKey listesini `LaunchedEffect` key'i olarak kullanmalı.
 */
internal fun resolvePageAfterPlanChange(
    previousPages: List<HomePageSpec>,
    previousPageIndex: Int,
    newPages: List<HomePageSpec>,
): Int {
    if (newPages.isEmpty()) return 0
    if (previousPages.isEmpty()) return previousPageIndex.coerceIn(0, newPages.lastIndex)

    val safePreviousIndex = clampPageToSafeBounds(previousPages, previousPageIndex)
    val anchor = anchorForCurrentPage(previousPages, safePreviousIndex)
    return HomePageAnchorResolver.resolve(newPages, anchor).coerceIn(0, newPages.lastIndex)
}

/**
 * Döngü P14 — indicator item modeli. `List<HomePageSpec>`'i (Dashboard/FolderPage karışık)
 * saf biçimde indicator'ın çizeceği dot listesine çevirir; Compose/Android bağımlılığı yoktur,
 * `UnifiedHomePageIndicator` composable'ı bu modeli tüketir.
 *
 * `folderNumber`: yalnız [HomePageSpec.FolderPage] item'larında dolu — "Klasör sayfası N/M"
 * açıklamasındaki N değeri (Dashboard hariç, 1'den başlayan klasör-sırası).
 * `folderPageCount`: plandaki TOPLAM klasör sayfası sayısı (M) — Dashboard hariç.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P14 (satır 1115-1171).
 */
internal data class HomePageIndicatorItem(
    val pageIndex: Int,
    val isDashboard: Boolean,
    val isSelected: Boolean,
    val folderNumber: Int?,
    val folderPageCount: Int,
)

/**
 * [pages]'i indicator dot modeline çevirir. `selectedPageIndex` sınır dışıysa güvenli sınıra
 * çekilir (aynı [clampPageToSafeBounds] deseni) — indicator hiçbir zaman "seçili yok" durumuna
 * düşmez.
 */
internal fun buildHomePageIndicatorItems(
    pages: List<HomePageSpec>,
    selectedPageIndex: Int,
): List<HomePageIndicatorItem> {
    if (pages.isEmpty()) return emptyList()
    val safeSelected = clampPageToSafeBounds(pages, selectedPageIndex)
    val folderPageCount = pages.count { it is HomePageSpec.FolderPage }
    var folderCounter = 0
    return pages.mapIndexed { index, spec ->
        val isDashboard = spec is HomePageSpec.Dashboard
        if (!isDashboard) folderCounter++
        HomePageIndicatorItem(
            pageIndex = index,
            isDashboard = isDashboard,
            isSelected = index == safeSelected,
            folderNumber = if (isDashboard) null else folderCounter,
            folderPageCount = folderPageCount,
        )
    }
}

/**
 * Döngü P19 — saf yardımcı: geçerli sayfa için TalkBack'in okuyacağı "sayfa başlığı" metnini
 * üretir ("Akıllı Ana Ekran" veya "Klasör sayfası N / M", indicator dot'larındaki
 * `home_page_indicator_folder_page` deseniyle BİREBİR aynı biçim — roadmap madde 5 iki yerde
 * de aynı cümleyi ister). Compose bağımlılığı yoktur; `HomePagerHost` bu metni kök pager
 * `contentDescription`'ına yazar (roadmap madde 1: "current page title").
 */
internal fun homePagerCurrentPageDescription(
    pages: List<HomePageSpec>,
    currentPage: Int,
    dashboardLabel: String,
    folderPageLabelFormat: (folderNumber: Int, folderPageCount: Int) -> String,
): String {
    if (pages.isEmpty()) return dashboardLabel
    val items = buildHomePageIndicatorItems(pages, currentPage)
    val current = items.firstOrNull { it.isSelected } ?: return dashboardLabel
    return if (current.isDashboard) {
        dashboardLabel
    } else {
        folderPageLabelFormat(
            current.folderNumber ?: (current.pageIndex + 1),
            current.folderPageCount.coerceAtLeast(1),
        )
    }
}

/**
 * Döngü P05 — tek yatay ana ekran pager'ı. `HomePageSpec` listesindeki sayfaları (Dashboard
 * veya klasör sayfası) tek `HorizontalPager` içinde render eder; iç içe `HorizontalPager` YOKTUR
 * (eskiden `FolderPager`in kendi pager'ı vardı, P05'te söküldü — bkz. HomeScreenFolderPager.kt).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P05 (satır 611-670).
 *
 * Sayfa geçiş efekti (hafif fade/scale, eskiden `FolderPager` içindeydi) burada tek yerden
 * uygulanır — hem Dashboard hem klasör sayfaları için ortak. "Azaltılmış hareket" sistem
 * ayarı açıkken (`ValueAnimator.areAnimatorsEnabled() == false`, bkz. FolderScreen.kt'deki
 * aynı desen) efekt kapatılır, sayfa tam opak/ölçek 1f render edilir.
 *
 * `pages` boş olamaz (çağıran taraf `HomePagePlanner.buildPages` üzerinden en az bir sayfa
 * garanti eder) — yine de savunma amaçlı `pages.isEmpty()` durumunda hiçbir şey çizilmez.
 *
 * Döngü P19 — erişilebilirlik (roadmap madde 1): kök pager `contentDescription`'ı geçerli
 * sayfa başlığını + toplam sayfa sayısını taşır (`homePagerCurrentPageDescription` +
 * "Sayfa N / M" eki), `customActions` ile TalkBack kullanıcısı yatay swipe yapmadan sonraki/
 * önceki sayfaya geçebilir (`animateScrollToPage`, reduce motion'da `scrollToPage` — aynı
 * kural `HomeScreen.kt`'nin `GoToStartPage` komutunda da uygulanır, bkz. dosya başı yorum).
 */
@Composable
internal fun HomePagerHost(
    pages: List<HomePageSpec>,
    pagerState: PagerState,
    userScrollEnabled: Boolean,
    dashboardContent: @Composable () -> Unit,
    folderPageContent: @Composable (HomePageSpec.FolderPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return

    val reduceMotionEnabled = remember { !ValueAnimator.areAnimatorsEnabled() }
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1)
    )
    val scope = rememberCoroutineScope()

    val dashboardLabel = stringResource(R.string.home_page_indicator_dashboard_unselected)
    val folderPageLabelTemplate = stringResource(R.string.home_page_indicator_folder_page)
    val pagerPositionSuffix = stringResource(
        R.string.home_pager_position_suffix,
        pagerState.currentPage + 1,
        pages.size,
    )
    val currentPageTitle = homePagerCurrentPageDescription(
        pages = pages,
        currentPage = pagerState.currentPage,
        dashboardLabel = dashboardLabel,
        folderPageLabelFormat = { n, m -> String.format(folderPageLabelTemplate, n, m) },
    )
    val nextPageActionLabel = stringResource(R.string.home_pager_next_page_action)
    val previousPageActionLabel = stringResource(R.string.home_pager_previous_page_action)

    fun goToPage(index: Int) {
        val target = index.coerceIn(0, pages.lastIndex)
        if (target == pagerState.currentPage) return
        scope.launch {
            if (reduceMotionEnabled) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
        }
    }

    HorizontalPager(
        state = pagerState,
        key = { pages[it].stableKey },
        pageSpacing = 8.dp,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        // Fix EX03/FAZ A-1 (devam) — eskiden 0'dı: görünür alandan çıkan sayfa AYNI ANDA
        // deactivate ediliyordu. Rotasyon (BoxWithConstraints yeniden ölçüm) + hızlı art arda
        // swipe kombinasyonunda, deactivate edilen sayfanın LazyVerticalGrid'i üzerinde HALA
        // bekleyen bir snapshot-observed remeasure isteği varsa Compose 1.7.x'te
        // "measure is called on a deactivated node" ile çöküyordu (canlı doğrulama: Samsung
        // tablet, 03:16, gerçek crash). 1 komşu sayfa tamponu, o sayfanın node'unun deactivate
        // edilmesini bir sonraki page geçişine erteler — Dashboard'un pahalı state hesaplaması
        // (P06+ yorumu) hâlâ geçerli ama artık YALNIZ 1 komşu (sağ VEYA sol, mevcut sayfaya
        // bağlı) önceden compose ediliyor; performans etkisi ölçülebilir düzeyde değil.
        beyondViewportPageCount = 1,
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "$currentPageTitle, $pagerPositionSuffix"
                customActions = buildList {
                    if (pagerState.currentPage < pages.lastIndex) {
                        add(CustomAccessibilityAction(nextPageActionLabel) { goToPage(pagerState.currentPage + 1); true })
                    }
                    if (pagerState.currentPage > 0) {
                        add(CustomAccessibilityAction(previousPageActionLabel) { goToPage(pagerState.currentPage - 1); true })
                    }
                }
            },
    ) { pageIndex ->
        // Fix EX03/FAZ A-1 — "measure is called on a deactivated node" (LazyVerticalGrid içinde
        // rotasyon + hızlı swipe kombinasyonunda çöküyordu). Kök neden: `signedOffset`/`pageOffset`
        // eskiden BURADA (page content composable scope'unda, graphicsLayer lambda'sının DIŞINDA)
        // `pagerState.currentPage`/`currentPageOffsetFraction` okuyordu — bu, page içeriğinin
        // (FolderGridPage -> LazyVerticalGrid dahil) HER frame'de yeniden COMPOSE edilmesine
        // sebep oluyordu (sadece re-layer değil). `beyondViewportPageCount = 0` ile pager sayfa
        // görünür alandan çıkar çıkmaz deactivate ediyor; rotasyon `BoxWithConstraints` ölçümünü
        // tetikleyip `pagerState`i içeren `remember` zincirini yeniden kurarken TAM O ANDA hızlı
        // swipe page content'i recompose ediyorsa, LazyVerticalGrid alt node'u deactivate
        // edilirken üstünde bekleyen bir remeasure isteği devam edip crash atıyordu.
        // Çözüm: `pagerState` okumaları `graphicsLayer { }` içine taşındı — Compose'un resmi
        // Pager kılavuzundaki "deferred read" deseni. graphicsLayer lambda'sı GraphicsLayerScope
        // içinde çalışır ve sadece layer'ı invalidate eder, page İÇERİĞİNİ recompose ETMEZ.
        val pageModifier = if (reduceMotionEnabled) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val signedOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    val pageOffset = signedOffset.absoluteValue.coerceIn(0f, 1f)
                    alpha = 1f - (pageOffset * 0.18f)
                    scaleX = 1f - (pageOffset * 0.055f)
                    scaleY = 1f - (pageOffset * 0.055f)
                    rotationY = signedOffset.coerceIn(-1f, 1f) * -4f
                    cameraDistance = 18f * density
                }
        }

        androidx.compose.foundation.layout.Box(modifier = pageModifier) {
            when (val page = pages[pageIndex]) {
                is HomePageSpec.Dashboard -> dashboardContent()
                is HomePageSpec.FolderPage -> folderPageContent(page)
            }
        }
    }
}
