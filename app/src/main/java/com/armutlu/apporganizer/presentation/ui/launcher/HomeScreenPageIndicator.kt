package com.armutlu.apporganizer.presentation.ui.launcher

import android.animation.ValueAnimator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import kotlinx.coroutines.launch

/**
 * HorizontalPager sayfa gösterge noktaları — HomeScreen'den extract edildi.
 *
 * Döngü P14 — artık [pages] (`List<HomePageSpec>`) alır, sadece `pageCount` değil: bu sayede
 * Dashboard sayfası klasör sayfalarından görsel olarak ayırt edilebilir (ev ikonu vs. nokta).
 * `dashboardEnabledForPager=false` iken (bugünkü varsayılan, P24 bekliyor) `pages` listesinde
 * hiçbir [HomePageSpec.Dashboard] ELEMANI OLMAZ (`HomePagePlanner.buildPages` bunu garanti eder)
 * — bu durumda tüm item'lar `isDashboard=false` döner ve görünüm bugünküyle BİREBİR aynı kalır
 * (yalnız standart noktalar, ev ikonu hiç render edilmez). Roadmap kabul kriteri budur.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P14 (satır 1115-1171).
 */
@Composable
internal fun HomePageIndicator(
    pages: List<HomePageSpec>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    if (pages.size <= 1) return

    val items = remember(pages, pagerState.currentPage) {
        buildHomePageIndicatorItems(pages, pagerState.currentPage)
    }
    val reduceMotionEnabled = remember { !ValueAnimator.areAnimatorsEnabled() }
    val scope = rememberCoroutineScope()

    val dashboardSelectedDesc = stringResource(R.string.home_page_indicator_dashboard_selected)
    val dashboardUnselectedDesc = stringResource(R.string.home_page_indicator_dashboard_unselected)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics {
                role = Role.Tab
                contentDescription = "Sayfa ${pagerState.currentPage + 1} / ${pages.size}"
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        items.forEach { item ->
            val itemDesc = if (item.isDashboard) {
                if (item.isSelected) dashboardSelectedDesc else dashboardUnselectedDesc
            } else {
                stringResource(
                    R.string.home_page_indicator_folder_page,
                    item.folderNumber ?: (item.pageIndex + 1),
                    item.folderPageCount.coerceAtLeast(1)
                )
            }

            // Minimum 48dp dikey dokunma hedefi (roadmap madde 4) — yatayda dar tutulur, aksi
            // halde çok sayfalı planlarda (örn. 10+ klasör sayfası) indicator satırı taşabilir.
            // Görsel nokta/ikon küçük kalır, sadece dokunma alanı büyütülür.
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 48.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { scope.launch { pagerState.animateScrollToPage(item.pageIndex) } }
                    )
                    .semantics {
                        contentDescription = itemDesc
                    },
                contentAlignment = Alignment.Center
            ) {
                if (item.isDashboard) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        tint = if (item.isSelected) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(if (item.isSelected) 16.dp else 13.dp)
                    )
                } else {
                    val dotSize = if (reduceMotionEnabled) {
                        if (item.isSelected) 9.dp else 5.dp
                    } else {
                        animateDpAsState(
                            targetValue = if (item.isSelected) 9.dp else 5.dp,
                            animationSpec = tween(180),
                            label = "home_page_dot_${item.pageIndex}"
                        ).value
                    }
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .background(
                                if (item.isSelected) Color.White.copy(alpha = 0.9f)
                                else Color.White.copy(alpha = 0.3f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}
