package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlin.math.ceil

/** Hücre yüksekliği sabittir — sürükle-sırala hit-test matematiği bunu gerektirir. */
internal val FOLDER_CELL_HEIGHT = 124.dp
internal const val FOLDER_GRID_SPACING_DP = 12f
internal const val FOLDER_GRID_PADDING_DP = 16f

/**
 * Tek klasör sayfası — sabit hücreli manuel grid.
 *
 * Jestler (hücre başına TEK pointerInput, çakışma yok):
 *  - Dokun → klasörü aç
 *  - Basılı tut + sürükle → SIRA TAŞI (hedef kart vurgulanır, bırakınca kalıcı sıralama)
 *  - Hızlı yukarı kaydır → hızlı başlat (klasörün en sık açılan uygulaması)
 */
@Composable
internal fun FolderPageV2(
    tiles: List<FolderTileState>,
    appsByPackage: Map<String, AppInfo>,
    onOpenFolder: (FolderTileState) -> Unit,
    onQuickLaunch: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onAppLongClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = (maxWidth / 168.dp).toInt().coerceIn(1, 4)
        val density = LocalDensity.current
        val spacingPx = with(density) { FOLDER_GRID_SPACING_DP.dp.toPx() }
        val paddingPx = with(density) { FOLDER_GRID_PADDING_DP.dp.toPx() }
        val cellHeightPx = with(density) { FOLDER_CELL_HEIGHT.toPx() }
        val gridWidthPx = with(density) { maxWidth.toPx() }
        val cellWidthPx = (gridWidthPx - 2 * paddingPx - (columns - 1) * spacingPx) / columns

        var dragIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var dropTarget by remember { mutableStateOf<Int?>(null) }
        val haptic = LocalHapticFeedback.current

        val rows = tiles.chunked(columns)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FOLDER_GRID_PADDING_DP.dp),
        ) {
            rows.forEachIndexed { rowIndex, rowTiles ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowTiles.forEachIndexed { colIndex, tile ->
                        val index = rowIndex * columns + colIndex
                        val isDragged = dragIndex == index
                        Box(
                            modifier = Modifier
                                .width(with(density) { cellWidthPx.toDp() })
                                .height(FOLDER_CELL_HEIGHT)
                                .graphicsLayer {
                                    if (isDragged) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                    }
                                }
                                .pointerInput(index, tiles.size, columns) {
                                    folderCellGestures(
                                        cellHeightPx = cellHeightPx,
                                        onQuickLaunch = { tiles.getOrNull(index)?.quickLaunchPackage?.let(onQuickLaunch) },
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dragIndex = index
                                            dragOffset = Offset.Zero
                                            dropTarget = null
                                        },
                                        onDrag = { delta ->
                                            dragOffset += delta
                                            dropTarget = hitTestFolderIndex(
                                                fromIndex = index,
                                                offset = dragOffset,
                                                tileCount = tiles.size,
                                                columns = columns,
                                                cellWidthPx = cellWidthPx,
                                                cellHeightPx = cellHeightPx,
                                                spacingPx = spacingPx,
                                            )
                                        },
                                        onDragEnd = {
                                            val from = dragIndex
                                            val to = dropTarget
                                            dragIndex = null
                                            dragOffset = Offset.Zero
                                            dropTarget = null
                                            if (from != null && to != null && from != to) onReorder(from, to)
                                        },
                                        onDragCancel = {
                                            dragIndex = null
                                            dragOffset = Offset.Zero
                                            dropTarget = null
                                        },
                                    )
                                },
                        ) {
                            FolderTileV2(
                                tile = tile,
                                previewApps = tile.previewPackages.mapNotNull { appsByPackage[it] },
                                onOpen = { onOpenFolder(tile) },
                                onAppClick = onAppClick,
                                onAppLongClick = onAppLongClick,
                                lifted = isDragged,
                                dropHighlight = dropTarget == index && dragIndex != null && dragIndex != index,
                                interactionsEnabled = dragIndex == null,
                            )
                        }
                        if (colIndex < columns - 1) Spacer(Modifier.width(FOLDER_GRID_SPACING_DP.dp))
                    }
                }
                if (rowIndex < rows.lastIndex) Spacer(Modifier.height(FOLDER_GRID_SPACING_DP.dp))
            }
        }
    }
}

/**
 * Hücre jestlerini TEK elde toplayan coroutine:
 * 1) Parmak slop'u AŞMADAN uzun basış süresi dolarsa → SIRA TAŞIMA modu (hareket tüketilir).
 * 2) Uzun basış dolmadan hareket slop'u aşarsa → HIZLI BAŞLAT modu (yalnız yukarı mesafe
 *    ölçülür; eşik aşılırsa bırakışta hızlı başlat tetiklenir).
 * Kısa dokunmalar tüketilmez → Card.onClick klasörü açar.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.folderCellGestures(
    cellHeightPx: Float,
    onQuickLaunch: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var mode = GESTURE_NONE
        var longPressFired = false
        var total = Offset.Zero
        var totalUp = 0f

        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null) {
                // Parmak izi kayboldu — iptal
                if (mode == GESTURE_REORDER) onDragCancel()
                break
            }
            if (!change.pressed) {
                when (mode) {
                    GESTURE_REORDER -> onDragEnd()
                    GESTURE_QUICK -> if (totalUp > cellHeightPx * 0.35f) onQuickLaunch()
                }
                break
            }
            val delta = change.positionChange()
            total += delta
            if (delta.y < 0) totalUp += -delta.y

            when (mode) {
                GESTURE_NONE -> {
                    val heldLongEnough = change.uptimeMillis - down.uptimeMillis >= longPressTimeout
                    if (!longPressFired && heldLongEnough && total.getDistance() < slop) {
                        longPressFired = true
                        onDragStart()
                    }
                    if (total.getDistance() > slop) {
                        if (longPressFired) {
                            mode = GESTURE_REORDER
                            change.consume()
                            onDrag(delta)
                        } else {
                            mode = GESTURE_QUICK
                            change.consume()
                        }
                    }
                }
                GESTURE_REORDER -> {
                    change.consume()
                    onDrag(delta)
                }
                GESTURE_QUICK -> change.consume()
            }
        }
    }
}

private const val GESTURE_NONE = 0
private const val GESTURE_REORDER = 1
private const val GESTURE_QUICK = 2

/**
 * Sürükleme sırasındaki hedef kart indeksini hesaplar — SAF fonksiyon, birim testli.
 * Sürüklenen kartın MERKEZİ + ofset hangi hücreye denk geliyorsa hedef odur.
 */
internal fun hitTestFolderIndex(
    fromIndex: Int,
    offset: Offset,
    tileCount: Int,
    columns: Int,
    cellWidthPx: Float,
    cellHeightPx: Float,
    spacingPx: Float,
): Int? {
    if (tileCount <= 0 || columns <= 0) return null
    val fromCol = fromIndex % columns
    val fromRow = fromIndex / columns
    val centerX = fromCol * (cellWidthPx + spacingPx) + cellWidthPx / 2f + offset.x
    val centerY = fromRow * (cellHeightPx + spacingPx) + cellHeightPx / 2f + offset.y
    val col = ((centerX) / (cellWidthPx + spacingPx)).toInt().coerceIn(0, columns - 1)
    val rowCount = ceil(tileCount.toFloat() / columns).toInt()
    val row = ((centerY) / (cellHeightPx + spacingPx)).toInt().coerceIn(0, rowCount - 1)
    val target = (row * columns + col).coerceIn(0, tileCount - 1)
    return target
}

/**
 * Sayfa başına klasör sayfası adedini sütun sayısına göre yuvarlar (test edilebilir saf yardımcı).
 */
internal fun folderChunks(folders: List<FolderTileState>, pageSize: Int, columns: Int): List<List<FolderTileState>> {
    if (folders.isEmpty() || pageSize <= 0 || columns <= 0) return emptyList()
    val rows = ceil(pageSize.toDouble() / columns).toInt().coerceAtLeast(1)
    return folders.chunked(rows * columns)
}

/**
 * Bir öğeyi listede `from` konumundan `to` konumuna taşır — SAF fonksiyon, birim testli.
 * Geçersiz indekslerde veya from==to'da liste DEĞİŞMEDEN döner (sürükle-bırak no-op güvenliği).
 */
internal fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from == to || from !in list.indices || to !in list.indices) return list
    val mutable = list.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    return mutable
}
