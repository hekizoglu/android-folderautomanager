package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.armutlu.apporganizer.utils.WidgetHostManager
import kotlin.math.roundToInt

/**
 * Döngü P07 madde 6: Android widget'ları (`AppWidgetHostView`, native `View`) kendi içinde dikey
 * scroll alabilir (ör. liste widget'ları) — bu durumda widget'ın pointer alanı, Compose'un ana
 * gövde `pointerInput("drag")`/`nestedScroll` swipe-up algılamasından DOĞAL olarak hariç kalır:
 * `AndroidView` widget View'ı kendi `onTouchEvent`'ini tüketir, tükettiği dokunuşlar Compose
 * gesture zincirine `available` olarak yansımaz. Ayrıca ek bir "gesture bölgesi" tanımına gerek
 * yoktur (bkz. `DashboardLayoutPolicy.kt` dosya başı notu, `SmartDashboardPage.kt`).
 */
@Composable
fun WidgetArea(
    widgetIds: List<Int>,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgets: ((List<Int>) -> Unit)? = null,
    editMode: Boolean = false,
    autoResize: Boolean = false,
    screenHeightDp: Int = 800,
    modifier: Modifier = Modifier
) {
    if (widgetIds.isEmpty()) return

    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggingIds by remember { mutableStateOf<List<Int>?>(null) }
    var estimatedItemHeightPx by remember { mutableFloatStateOf(0f) }
    val displayIds = draggingIds ?: widgetIds

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayIds.forEachIndexed { index, id ->
            val isDragging = dragFromIndex == index
            val itemModifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    if (size.height > 0) estimatedItemHeightPx = size.height.toFloat()
                }
                .then(if (isDragging) Modifier.offset { IntOffset(0, dragOffsetY.roundToInt()) } else Modifier)
                .scale(if (isDragging) 1.03f else 1f)
                .alpha(if (dragFromIndex != null && !isDragging) 0.75f else 1f)

            Box(modifier = itemModifier) {
                WidgetCard(
                    widgetId = id,
                    onRemove = { onRemoveWidget(id) },
                    editMode = editMode,
                    isDraggable = editMode && widgetIds.size > 1,
                    autoResize = autoResize,
                    screenHeightDp = screenHeightDp,
                    onDragStart = {
                        dragFromIndex = index
                        draggingIds = displayIds.toMutableList()
                        dragOffsetY = 0f
                    },
                    onDrag = { delta ->
                        dragOffsetY += delta
                        val stepHeight = estimatedItemHeightPx.takeIf { it > 0f } ?: 1f
                        val toIndex = (index + (dragOffsetY / stepHeight).roundToInt())
                            .coerceIn(0, displayIds.lastIndex)
                        if (toIndex != dragFromIndex) {
                            val mutable = (draggingIds ?: displayIds).toMutableList()
                            val from = dragFromIndex ?: index
                            if (from in mutable.indices && toIndex in mutable.indices) {
                                val item = mutable.removeAt(from)
                                mutable.add(toIndex, item)
                                draggingIds = mutable
                                dragFromIndex = toIndex
                                dragOffsetY = 0f
                            }
                        }
                    },
                    onDragEnd = {
                        draggingIds?.let { onReorderWidgets?.invoke(it) }
                        dragFromIndex = null
                        draggingIds = null
                        dragOffsetY = 0f
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetCard(
    widgetId: Int,
    onRemove: () -> Unit,
    editMode: Boolean = false,
    isDraggable: Boolean = false,
    autoResize: Boolean = false,
    screenHeightDp: Int = 800,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRemoveButton by remember { mutableStateOf(false) }
    var hostView by remember { mutableStateOf<android.appwidget.AppWidgetHostView?>(null) }
    var minHeightDp by remember { mutableIntStateOf(100) }

    LaunchedEffect(widgetId) {
        hostView = WidgetHostManager.createView(context, widgetId)
        val awm = context.getSystemService(android.content.Context.APPWIDGET_SERVICE) as? android.appwidget.AppWidgetManager
        awm?.getAppWidgetInfo(widgetId)?.let { info ->
            val density = context.resources.displayMetrics.density
            val infoMinHeight = (info.minHeight / density).toInt().coerceAtLeast(80)
            minHeightDp = if (autoResize) {
                maxOf(infoMinHeight, (screenHeightDp * 0.22f).toInt())
            } else {
                infoMinHeight
            }
        }
    }

    hostView?.let { widgetView ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .then(
                    if (!editMode) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showRemoveButton = !showRemoveButton }
                            )
                        }
                    } else Modifier
                )
        ) {
            AndroidView(
                factory = { widgetView },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeightDp.dp)
                    .wrapContentHeight()
            )

            if (editMode) {
                // AppWidgetHostView consumes its own pointer events. This transparent layer
                // keeps widget actions inert while the shared layout editor owns gestures.
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(widgetId, isDraggable) {
                            if (isDraggable) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart() },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd,
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.y)
                                    },
                                )
                            } else {
                                detectTapGestures(onTap = {})
                            }
                        }
                )
            }

            if (isDraggable && !showRemoveButton) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Widget sırasını değiştirmek için sürükle",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
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
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onRemove() })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Widget sil",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

internal enum class WidgetInteractionMode { LIVE, EDIT_SINGLE, EDIT_REORDER }

internal fun widgetInteractionMode(editMode: Boolean, widgetCount: Int): WidgetInteractionMode = when {
    !editMode -> WidgetInteractionMode.LIVE
    widgetCount > 1 -> WidgetInteractionMode.EDIT_REORDER
    else -> WidgetInteractionMode.EDIT_SINGLE
}

internal fun moveWidget(ids: List<Int>, widgetId: Int, direction: Int): List<Int> {
    val from = ids.indexOf(widgetId)
    val to = from + direction.coerceIn(-1, 1)
    if (from < 0 || direction == 0 || to !in ids.indices) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}
