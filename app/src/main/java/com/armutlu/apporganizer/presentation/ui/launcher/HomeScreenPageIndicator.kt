package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
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
            .semantics { contentDescription = "Sayfa ${pagerState.currentPage + 1} / $pageCount" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { idx ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (pagerState.currentPage == idx) 8.dp else 5.dp)
                    .background(
                        if (pagerState.currentPage == idx) Color.White.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.3f),
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
