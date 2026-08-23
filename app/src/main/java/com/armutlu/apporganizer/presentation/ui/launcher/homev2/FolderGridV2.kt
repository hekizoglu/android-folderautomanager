package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlin.math.ceil

/** Satır yüksekliği sabittir — sürükle-sırala hit-test matematiği bunu gerektirir. */
internal val FOLDER_ROW_HEIGHT = 88.dp
internal val FOLDER_CELL_HEIGHT = FOLDER_ROW_HEIGHT
internal const val FOLDER_GRID_SPACING_DP = 8f
internal const val FOLDER_GRID_PADDING_DP = 12f

/**
 * Tek klasör sayfası — her klasör tam genişlik YATAY SATIR olarak render edilir.
 * Sayfa başına [pageSize] satır sığar; fazlası yatay pager ile sayfalara bölünür.
 *
 * Jestler (satır başına TEK pointerInput, çakışma yok):
 *  - Dokun → klasörü aç
 *  - Basılı tut + dikey sürükle → SIRA TAŞI (hedef satır vurgulanır, bırakınca kalıcı sıralama)
 *  - Hızlı yukarı kaydır → hızlı başlat (klasörün en sık açılan uygulaması)
 * onReorder GLOBAL indekslerle çağrılır (sayfa ofseti içeride eklenir).
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
    textAlpha: Float = 1f,
    pageSize: Int = 8,
    modifier: Modifier = Modifier,
) {
    // NOT: Klasör sayfaları HomeV2Screen'deki DIŞ pager tarafından chunk'lanır; bu
    // fonksiyon tek bir sayfanın satırlarını render eder (iç pager YOK, çift
    // sayfalama olmaz). onReorder sayfa-içi indekslerle çağrılır; global ofseti
    // çağıran (HomeV2Screen) ekler.
    val density = LocalDensity.current
    val rowHeightPx = with(density) { FOLDER_ROW_HEIGHT.toPx() }
    val spacingPx = with(density) { FOLDER_GRID_SPACING_DP.dp.toPx() }

    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dropTarget by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FOLDER_GRID_PADDING_DP.dp, vertical = 4.dp),
    ) {
        tiles.forEachIndexed { rowIndex, tile ->
            val isDragged = dragIndex == rowIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FOLDER_ROW_HEIGHT)
                    .graphicsLayer {
                        if (isDragged) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                        }
                    }
                    .pointerInput(rowIndex, tiles.size) {
                        folderRowGestures(
                            rowHeightPx = rowHeightPx,
                            onQuickLaunch = { tile.quickLaunchPackage?.let(onQuickLaunch) },
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragIndex = rowIndex
                                dragOffset = Offset.Zero
                                dropTarget = null
                            },
                            onDrag = { delta ->
                                dragOffset += delta
                                dropTarget = hitTestRowIndex(
                                    fromIndex = rowIndex,
                                    offsetY = dragOffset.y,
                                    rowCount = tiles.size,
                                    rowHeightPx = rowHeightPx,
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
                FolderRowV2(
                    tile = tile,
                    previewApps = tile.previewPackages.mapNotNull { appsByPackage[it] },
                    onOpen = { onOpenFolder(tile) },
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    textAlpha = textAlpha,
                    lifted = isDragged,
                    dropHighlight = dropTarget == rowIndex && dragIndex != null && dragIndex != rowIndex,
                    interactionsEnabled = dragIndex == null,
                )
            }
            if (rowIndex < tiles.lastIndex) {
                Spacer(Modifier.height(FOLDER_GRID_SPACING_DP.dp))
            }
        }
    }
}

/** Tek satır için jestler: hızlı yukarı = hızlı başlat, uzun bas + sürükle = sıra taşı. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.folderRowGestures(
    rowHeightPx: Float,
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
                if (mode == GESTURE_REORDER) onDragCancel()
                break
            }
            if (!change.pressed) {
                when (mode) {
                    GESTURE_REORDER -> onDragEnd()
                    GESTURE_QUICK -> if (totalUp > rowHeightPx * 0.35f) onQuickLaunch()
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

/** Dikey sürüklemede hedef satır indeksi (tek sütun). */
internal fun hitTestRowIndex(
    fromIndex: Int,
    offsetY: Float,
    rowCount: Int,
    rowHeightPx: Float,
    spacingPx: Float,
): Int? {
    if (rowCount <= 0) return null
    val centerY = fromIndex * (rowHeightPx + spacingPx) + rowHeightPx / 2f + offsetY
    val row = (centerY / (rowHeightPx + spacingPx)).toInt().coerceIn(0, rowCount - 1)
    return row
}

internal const val GESTURE_NONE = 0
internal const val GESTURE_REORDER = 1
internal const val GESTURE_QUICK = 2

/** Sayfa başına klasör sayfası adedini sütun sayısına göre yuvarlar (test uyumu için korunur). */
internal fun folderChunks(folders: List<FolderTileState>, pageSize: Int, columns: Int): List<List<FolderTileState>> {
    if (folders.isEmpty() || pageSize <= 0 || columns <= 0) return emptyList()
    val rows = ceil(pageSize.toDouble() / columns).toInt().coerceAtLeast(1)
    return folders.chunked(rows * columns)
}

/** Liste öğesini from'dan to'ya taşır (saf fonksiyon, birim testli). */
internal fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from == to || from !in list.indices || to !in list.indices) return list
    val mutable = list.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to, item)
    return mutable
}
