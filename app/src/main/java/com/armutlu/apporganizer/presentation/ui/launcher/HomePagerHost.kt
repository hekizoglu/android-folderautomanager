package com.armutlu.apporganizer.presentation.ui.launcher

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import kotlin.math.absoluteValue

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

    HorizontalPager(
        state = pagerState,
        key = { pages[it].stableKey },
        pageSpacing = 8.dp,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        // Dashboard pahalı state hesaplayabilir (P06+) — komşu sayfa önceden compose edilmesin.
        beyondViewportPageCount = 0,
        modifier = modifier.fillMaxSize(),
    ) { pageIndex ->
        val signedOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
        val pageOffset = signedOffset.absoluteValue.coerceIn(0f, 1f)

        val pageModifier = if (reduceMotionEnabled) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .graphicsLayer {
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
