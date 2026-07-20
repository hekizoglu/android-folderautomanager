package com.armutlu.apporganizer.presentation.ui.launcher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.domain.home.EdgeScrollDirection
import com.armutlu.apporganizer.domain.home.GridBounds
import com.armutlu.apporganizer.domain.home.GridPosition
import com.armutlu.apporganizer.domain.home.detectEdgeScroll
import com.armutlu.apporganizer.domain.home.findFirstFreeCell
import com.armutlu.apporganizer.domain.home.isValidPlacement
import com.armutlu.apporganizer.utils.WidgetHostManager
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Faz S3 — Dashboard widget alanının serbest 2D grid yerleşimi. `AppPrefs.KEY_WIDGET_FREE_GRID_ENABLED`
 * varsayılan KAPALI olduğu için bu composable sadece kullanıcı Ayarlar'dan açtığında devreye
 * girer; [WidgetArea]'nın mevcut 1D dikey `Column` yolunu hiç etkilemez — dış API'si (parametre
 * listesi) çağıran tarafta minimal değişiklik gerektirecek şekilde [WidgetArea] ile aynı tutuldu.
 *
 * Konum kalıcılığı: [WidgetFreeGridViewModel] üzerinden [com.armutlu.apporganizer.data.local.HomeGridItemDao]
 * kullanılır. [FolderFreeGrid]'in aynı deseni: lazy-init (kayıtlı konumu olmayan widget'lara ilk
 * boş hücre ata), detectDragGesturesAfterLongPress ile sürükle, sürükleme bitince
 * isValidPlacement/findFirstFreeCell ile snap ve Room'a persist.
 *
 * Farkı: widget'ların boyutu (spanX/spanY) sabit değil — her widget'ın
 * `AppWidgetProviderInfo.minWidth/minHeight` değerinden grid hücre boyutuna bölünerek hesaplanır
 * (en az 1x1). Ayrıca gerçek widget view'ı [WidgetHostManager.createView] ile oluşturulur
 * ([WidgetArea] içindeki `WidgetCard`'daki `AndroidView` kullanımıyla aynı desen).
 */

/** Dashboard'un sanal screenIndex'i (Room sorgusu için) — klasörlerin `categoryId.hashCode()`
 * ile üretilen screenIndex'leriyle çakışmayı önlemek için ayrı, sabit bir değer kullanılır. */
const val WIDGET_GRID_SCREEN_INDEX = -1_000_000

/** Bir widget'ın Room'daki benzersiz itemId'si. */
fun widgetGridItemId(widgetId: Int): String = "widget_$widgetId"

/** Bir widget'ın min genişlik/yükseklik (dp) değerinden grid hücre span'ini hesaplar (en az 1x1). */
fun computeWidgetSpan(minWidthDp: Int, minHeightDp: Int, cellSizeDp: Int): GridPosition {
    if (cellSizeDp <= 0) return GridPosition(cellX = 0, cellY = 0, spanX = 1, spanY = 1)
    val spanX = (minWidthDp + cellSizeDp - 1) / cellSizeDp
    val spanY = (minHeightDp + cellSizeDp - 1) / cellSizeDp
    return GridPosition(cellX = 0, cellY = 0, spanX = spanX.coerceAtLeast(1), spanY = spanY.coerceAtLeast(1))
}

@Composable
fun WidgetFreeGrid(
    widgetIds: List<Int>,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgets: ((List<Int>) -> Unit)? = null,
    editMode: Boolean = false,
    autoResize: Boolean = false,
    screenHeightDp: Int = 800,
    modifier: Modifier = Modifier,
    onDragActiveChange: (Boolean) -> Unit = {},
    // Faz S4 — opsiyonel: verilirse sürükleme sırasında ekran kenarına yaklaşınca sayfa
    // otomatik kaydırılır. null (varsayılan) = eski davranış, hiçbir şey değişmez.
    pagerState: PagerState? = null,
    viewModel: WidgetFreeGridViewModel = hiltViewModel(),
) {
    if (widgetIds.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val positions by viewModel.observePositions(WIDGET_GRID_SCREEN_INDEX).collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Her widget'ın minWidth/minHeight (dp) bilgisini AppWidgetManager'dan tek seferlik okur.
    var widgetSizes by remember { mutableStateOf<Map<Int, Pair<Int, Int>>>(emptyMap()) }
    LaunchedEffect(widgetIds) {
        val manager = AppWidgetManager.getInstance(context)
        val dpDensity = context.resources.displayMetrics.density
        val sizes = widgetIds.associateWith { id ->
            val info = manager.getAppWidgetInfo(id)
            val w = ((info?.minWidth ?: 0) / dpDensity).toInt().coerceAtLeast(56)
            val h = ((info?.minHeight ?: 0) / dpDensity).toInt().coerceAtLeast(56)
            w to h
        }
        widgetSizes = sizes
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cellSizeDp = 56.dp
        val cellSizeDpInt = 56
        val columns = remember(maxWidth) { computeFreeGridColumns(maxWidth, cellSizeDp) }
        val cellSizePx = with(density) { cellSizeDp.toPx() }
        // Faz S4 — kenar auto-scroll için composable genişliği px cinsinden.
        val widthPx = with(density) { maxWidth.toPx() }

        val spans = remember(widgetSizes, columns) {
            widgetIds.associateWith { id ->
                val (w, h) = widgetSizes[id] ?: (cellSizeDpInt to cellSizeDpInt)
                val span = computeWidgetSpan(w, h, cellSizeDpInt)
                GridPosition(
                    cellX = 0,
                    cellY = 0,
                    spanX = span.spanX.coerceAtMost(columns.coerceAtLeast(1)),
                    spanY = span.spanY,
                )
            }
        }

        val rows = remember(widgetIds.size, columns, spans) {
            val totalSpanY = widgetIds.sumOf { spans[it]?.spanY ?: 1 }
            (totalSpanY).coerceAtLeast(1) + 2
        }
        val bounds = remember(columns, rows) { GridBounds(columns = columns, rows = rows) }

        // İlk kullanımda kayıtlı konumu olmayan widget'lara sırayla boş hücre ata (lazy-init).
        LaunchedEffect(widgetIds, positions.keys, bounds, spans) {
            val occupied = positions.values.toMutableList()
            val missing = widgetIds.filter { id -> positions[widgetGridItemId(id)] == null }
            if (missing.isNotEmpty()) {
                val newlyAssigned = mutableMapOf<String, GridPosition>()
                missing.forEach { id ->
                    val span = spans[id] ?: GridPosition(cellX = 0, cellY = 0)
                    val cell = findFirstFreeCell(occupied, bounds, spanX = span.spanX, spanY = span.spanY)
                    if (cell != null) {
                        occupied.add(cell)
                        newlyAssigned[widgetGridItemId(id)] = cell
                    }
                }
                if (newlyAssigned.isNotEmpty()) {
                    viewModel.persistPositions(WIDGET_GRID_SCREEN_INDEX, newlyAssigned)
                }
            }
        }

        var draggingItemId by remember { mutableStateOf<String?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        // Faz S4 — son kenar-scroll tetiklemesinin zamanı (debounce, 700ms).
        var lastEdgeScrollAt by remember { mutableStateOf(0L) }

        LaunchedEffect(draggingItemId) {
            onDragActiveChange(draggingItemId != null)
        }

        Layout(
            modifier = Modifier.height(cellSizeDp * rows),
            content = {
                widgetIds.forEach { widgetId ->
                    val itemId = widgetGridItemId(widgetId)
                    val position = positions[itemId]
                    val isDragging = draggingItemId == itemId
                    var showRemoveButton by remember(widgetId) { mutableStateOf(false) }

                    var hostView by remember(widgetId) { mutableStateOf<AppWidgetHostView?>(null) }
                    LaunchedEffect(widgetId) {
                        hostView = WidgetHostManager.createView(context, widgetId)
                    }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    alpha = 0.85f
                                }
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(2.dp)
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

                                        // Faz S4 — parmak ekranın sol/sağ %12 bandına girip 700ms
                                        // kalırsa pager'ı bir sayfa kaydır (sürükleme devam eder,
                                        // widget'ın screenIndex'i DEĞİŞMEZ — sadece görünen sayfa).
                                        if (pagerState != null) {
                                            val direction = detectEdgeScroll(change.position.x, widthPx)
                                            val now = System.currentTimeMillis()
                                            if (direction != EdgeScrollDirection.NONE &&
                                                now - lastEdgeScrollAt >= 700L
                                            ) {
                                                lastEdgeScrollAt = now
                                                val targetPage = (pagerState.currentPage +
                                                    if (direction == EdgeScrollDirection.NEXT) 1 else -1)
                                                    .coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(targetPage)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val current = position
                                        if (current != null) {
                                            val deltaCellX = (dragOffset.x / cellSizePx).roundToInt()
                                            val deltaCellY = (dragOffset.y / cellSizePx).roundToInt()
                                            val candidate = GridPosition(
                                                cellX = (current.cellX + deltaCellX).coerceAtLeast(0),
                                                cellY = (current.cellY + deltaCellY).coerceAtLeast(0),
                                                spanX = current.spanX,
                                                spanY = current.spanY,
                                            )
                                            val otherOccupied = positions.filterKeys { it != itemId }.values.toList()
                                            val target = if (isValidPlacement(candidate, otherOccupied, bounds)) {
                                                candidate
                                            } else {
                                                current
                                            }
                                            viewModel.persistPositions(WIDGET_GRID_SCREEN_INDEX, mapOf(itemId to target))
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
                            // Widget'lar kendi dokunuşlarını tüketir (bkz. WidgetArea.kt dosya başı
                            // notu) — bu yüzden çift tıklama ile kaldır butonu göster/gizle, WidgetCard
                            // ile aynı desen (uzun basış zaten sürükleme için kullanılıyor).
                            .pointerInput(itemId) {
                                detectTapGestures(
                                    onDoubleTap = { showRemoveButton = !showRemoveButton }
                                )
                            }
                    ) {
                        hostView?.let { widgetView ->
                            AndroidView(
                                factory = { widgetView },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (showRemoveButton) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .pointerInput(itemId) {
                                        detectTapGestures(
                                            onTap = {
                                                showRemoveButton = false
                                                onRemoveWidget(widgetId)
                                                viewModel.removePosition(widgetId)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Widget sil",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.mapIndexed { index, measurable ->
                val widgetId = widgetIds.getOrNull(index)
                val span = spans[widgetId] ?: GridPosition(cellX = 0, cellY = 0)
                val widthPx = (cellSizePx * span.spanX).roundToInt()
                val heightPx = (cellSizePx * span.spanY).roundToInt()
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = widthPx.coerceAtLeast(0),
                        minHeight = 0,
                        maxHeight = heightPx.coerceAtLeast(0),
                    )
                )
            }
            layout(constraints.maxWidth, (cellSizePx * rows).roundToInt()) {
                placeables.forEachIndexed { index, placeable ->
                    val widgetId = widgetIds.getOrNull(index) ?: return@forEachIndexed
                    val itemId = widgetGridItemId(widgetId)
                    val span = spans[widgetId] ?: GridPosition(cellX = 0, cellY = 0)
                    val pos = positions[itemId] ?: GridPosition(
                        cellX = 0,
                        cellY = index,
                        spanX = span.spanX,
                        spanY = span.spanY,
                    )
                    placeable.place(
                        x = (pos.cellX * cellSizePx).roundToInt(),
                        y = (pos.cellY * cellSizePx).roundToInt(),
                    )
                }
            }
        }
    }
}
