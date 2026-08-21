package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlin.math.ceil

/**
 * Home V2 klasör grid'i — sayfalanmış, adaptif sütunlu.
 *
 * Sütun sayısı ekran genişliğine göre ölçeklenir (zengin kartlar için min 168.dp kart
 * genişliği hedeflenir): telefon ~2, geniş ekran/tablet 3-4. Sayfa başına klasör sayısı
 * ayarlardaki `pageSize` ile belirlenir; sayfalar yatay pager ile geçilir.
 */
@Composable
internal fun FolderGridV2(
    folders: List<FolderTileState>,
    pageSize: Int,
    appsByPackage: Map<String, AppInfo>,
    onOpenFolder: (FolderTileState) -> Unit,
    onQuickLaunch: (String) -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    if (folders.isEmpty()) return

    BoxWithColumns(modifier) { columns ->
        val perPage = remember(pageSize, columns) {
            val rows = ceil(pageSize.toDouble() / columns).toInt().coerceAtLeast(1)
            rows * columns
        }
        val pages = remember(folders, perPage) { folders.chunked(perPage) }
        val pagerState = rememberPagerState(pageCount = { pages.size })

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { page ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false,
                ) {
                    items(pages[page], key = { it.categoryId }) { tile ->
                        FolderTileV2(
                            tile = tile,
                            previewApps = tile.previewPackages.mapNotNull { appsByPackage[it] },
                            onOpen = { onOpenFolder(tile) },
                            onQuickLaunch = onQuickLaunch,
                            onAppClick = onAppClick,
                        )
                    }
                }
            }
            if (pages.size > 1) {
                PageDotsV2(pageCount = pages.size, currentPage = pagerState.currentPage)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** Kart önizleme ikonlarına yer bırakacak şekilde sütun sayısını genişlikten türetir. */
@Composable
private fun BoxWithColumns(modifier: Modifier, content: @Composable (Int) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val columns = (maxWidth / 168.dp).toInt().coerceIn(1, 4)
        content(columns)
    }
}

@Composable
private fun PageDotsV2(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
            )
        }
    }
}
