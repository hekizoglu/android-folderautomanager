package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * HorizontalPager sayfa gösterge noktaları — HomeScreen'den extract edildi.
 */
@Composable
internal fun HomePageIndicator(
    pageCount: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics {
                role = Role.Tab
                contentDescription = "Sayfa ${pagerState.currentPage + 1} / $pageCount"
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { idx ->
            val dotSize = animateDpAsState(
                targetValue = if (pagerState.currentPage == idx) 9.dp else 5.dp,
                animationSpec = tween(180),
                label = "home_page_dot_$idx"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(dotSize.value)
                    .background(
                        if (pagerState.currentPage == idx) Color.White.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.3f),
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
