package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.domain.home.GridBounds
import com.armutlu.apporganizer.domain.home.GridPosition
import com.armutlu.apporganizer.domain.home.findFirstFreeCell
import com.armutlu.apporganizer.domain.home.isValidPlacement
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlin.math.roundToInt

/**
 * Faz S2 — Klasör içi serbest 2D grid yerleşimi. `AppPrefs.KEY_FOLDER_FREE_GRID_ENABLED`
 * varsayılan KAPALI olduğu için bu composable sadece kullanıcı Ayarlar'dan açtığında devreye
 * girer; FolderScreen'deki mevcut LazyVerticalGrid yolunu hiç etkilemez.
 *
 * Konum kalıcılığı: [FolderFreeGridViewModel] üzerinden [com.armutlu.apporganizer.data.local.HomeGridItemDao]
 * kullanılır. Her klasör kendi "sanal ekranı" olarak ele alınır — screenIndex, categoryId'den
 * [folderScreenIndex] ile stabil bir Int üretilerek elde edilir (klasörler arası çakışma olmaz,
 * aynı klasör her zaman aynı screenIndex'e haritalanır).
 */

/** Bir klasörün categoryId'sinden stabil bir sanal screenIndex üretir (Room sorgusu için). */
fun folderScreenIndex(categoryId: String): Int = categoryId.hashCode()

/** Bir klasördeki uygulamanın Room'daki benzersiz itemId'si. */
fun folderGridItemId(categoryId: String, packageName: String): String =
    "folder_${categoryId}_$packageName"

/**
 * Verilen kullanılabilir genişliğe göre kaç sütun sığdığını hesaplar — mevcut
 * `GridCells.Adaptive(minSize = 76.dp)` davranışına yakın: hücre en az [minCellSize] genişliğinde
 * olacak şekilde en fazla sütun sayısını döner (en az 1).
 */
fun computeFreeGridColumns(availableWidth: Dp, minCellSize: Dp = 76.dp): Int {
    if (availableWidth <= 0.dp || minCellSize <= 0.dp) return 1
    val columns = (availableWidth / minCellSize).toInt()
    return columns.coerceAtLeast(1)
}

@Composable
fun FolderFreeGrid(
    apps: List<AppInfo>,
    categoryId: String,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    iconSize: Dp = 56.dp,
    viewModel: FolderFreeGridViewModel = hiltViewModel(),
) {
    val screenIndex = remember(categoryId) { folderScreenIndex(categoryId) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val positions by viewModel.observePositions(screenIndex).collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellSizeDp = iconSize + 20.dp
        val columns = remember(maxWidth, cellSizeDp) { computeFreeGridColumns(maxWidth, cellSizeDp) }
        val cellSizePx = with(density) { cellSizeDp.toPx() }
        val rows = remember(apps.size, columns) {
            ((apps.size + columns - 1) / columns).coerceAtLeast(1) + 2
        }
        val bounds = remember(columns, rows) { GridBounds(columns = columns, rows = rows) }

        // İlk kullanımda kayıtlı konumu olmayan uygulamalara sırayla boş hücre ata (lazy-init) —
        // kullanıcı hiç sürüklemeden önce bile grid mevcut LazyVerticalGrid sırasına yakın dolu görünür.
        LaunchedEffect(apps.map { it.packageName }, positions.keys, bounds) {
            val occupied = positions.values.toMutableList()
            val missing = apps.filter { app ->
                positions[folderGridItemId(categoryId, app.packageName)] == null
            }
            if (missing.isNotEmpty()) {
                val newlyAssigned = mutableMapOf<String, GridPosition>()
                missing.forEach { app ->
                    val cell = findFirstFreeCell(occupied, bounds)
                    if (cell != null) {
                        occupied.add(cell)
                        newlyAssigned[folderGridItemId(categoryId, app.packageName)] = cell
                    }
                }
                if (newlyAssigned.isNotEmpty()) {
                    viewModel.persistPositions(screenIndex, newlyAssigned)
                }
            }
        }

        var draggingItemId by remember { mutableStateOf<String?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        Layout(
            modifier = Modifier.height(cellSizeDp * rows),
            content = {
                apps.forEach { app ->
                    val itemId = folderGridItemId(categoryId, app.packageName)
                    val position = positions[itemId]
                    val isDragging = draggingItemId == itemId
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    alpha = 0.85f
                                }
                            }
                            .pointerInput(itemId) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingItemId = itemId
                                        dragOffset = Offset.Zero
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        val current = position
                                        if (current != null) {
                                            val deltaCellX = (dragOffset.x / cellSizePx).roundToInt()
                                            val deltaCellY = (dragOffset.y / cellSizePx).roundToInt()
                                            val candidate = GridPosition(
                                                cellX = (current.cellX + deltaCellX).coerceAtLeast(0),
                                                cellY = (current.cellY + deltaCellY).coerceAtLeast(0),
                                            )
                                            val otherOccupied = positions.filterKeys { it != itemId }.values.toList()
                                            val target = if (isValidPlacement(candidate, otherOccupied, bounds)) {
                                                candidate
                                            } else {
                                                current
                                            }
                                            viewModel.persistPositions(screenIndex, mapOf(itemId to target))
                                        }
                                        draggingItemId = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingItemId = null
                                        dragOffset = Offset.Zero
                                    },
                                )
                            }
                    ) {
                        AppIconView(
                            app = app,
                            onClick = { onClick(app) },
                            onLongClick = { onLongClick(app) },
                            iconSize = iconSize,
                            showLabel = true,
                        )
                    }
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, (cellSizePx * rows).roundToInt()) {
                placeables.forEachIndexed { index, placeable ->
                    val app = apps.getOrNull(index) ?: return@forEachIndexed
                    val itemId = folderGridItemId(categoryId, app.packageName)
                    val pos = positions[itemId] ?: GridPosition(cellX = index % columns, cellY = index / columns)
                    placeable.place(
                        x = (pos.cellX * cellSizePx).roundToInt(),
                        y = (pos.cellY * cellSizePx).roundToInt(),
                    )
                }
            }
        }
    }
}
