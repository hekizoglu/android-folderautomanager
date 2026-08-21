package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlin.math.ceil

/**
 * Tek klasör sayfası (pager'sız) — üst düzey pager [HomeV2Screen]'de yaşar; böylece
 * widget sayfası ve klasör sayfaları tek yatay pager'da birleşir (iç içe yatay pager yok).
 *
 * Sütun sayısı ekran genişliğinden türetilir (zengin kartlar için ~168.dp hedef kart
 * genişliği): telefon ~2, geniş ekran/tablet 3-4.
 */
@Composable
internal fun FolderPageV2(
    tiles: List<FolderTileState>,
    appsByPackage: Map<String, AppInfo>,
    onOpenFolder: (FolderTileState) -> Unit,
    onQuickLaunch: (String) -> Unit,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = (maxWidth / 168.dp).toInt().coerceIn(1, 4)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false,
        ) {
            items(tiles, key = { it.categoryId }) { tile ->
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
}

/** Sayfa başına klasör sayfası adedini sütun sayısına göre yuvarlar (test edilebilir saf yardımcı). */
internal fun folderChunks(folders: List<FolderTileState>, pageSize: Int, columns: Int): List<List<FolderTileState>> {
    if (folders.isEmpty() || pageSize <= 0 || columns <= 0) return emptyList()
    val rows = ceil(pageSize.toDouble() / columns).toInt().coerceAtLeast(1)
    return folders.chunked(rows * columns)
}
