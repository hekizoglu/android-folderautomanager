package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.armutlu.apporganizer.utils.WidgetHostManager
import kotlin.math.roundToInt

/**
 * Ana ekrandaki widget alanı. Uzun basınca silme, sürükle-bırak ile sıra değiştirme.
 */
@Composable
fun WidgetArea(
    widgetIds: List<Int>,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgets: ((List<Int>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (widgetIds.isEmpty()) return

    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggingIds by remember { mutableStateOf<List<Int>?>(null) }
    val displayIds = draggingIds ?: widgetIds

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        displayIds.forEachIndexed { index, id ->
            val isDragging = dragFromIndex == index
            val itemModifier = Modifier
                .fillMaxWidth()
                .then(if (isDragging) Modifier.offset { IntOffset(0, dragOffsetY.roundToInt()) } else Modifier)
                .scale(if (isDragging) 1.03f else 1f)
                .alpha(if (dragFromIndex != null && !isDragging) 0.75f else 1f)

            Box(modifier = itemModifier) {
                WidgetCard(
                    widgetId = id,
                    onRemove = { onRemoveWidget(id) },
                    isDraggable = (widgetIds.size > 1),
                    onDragStart = {
                        dragFromIndex = index
                        draggingIds = displayIds.toMutableList()
                        dragOffsetY = 0f
                    },
                    onDrag = { delta ->
                        dragOffsetY += delta
                        // Tahmini hedef index — 120dp'lik widget yüksekliği varsayımı
                        val estimatedItemHeightPx = 120 * 3f  // yaklaşık px
                        val toIndex = (index + (dragOffsetY / estimatedItemHeightPx).roundToInt())
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
    isDraggable: Boolean = false,
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
            minHeightDp = (info.minHeight / density).toInt().coerceAtLeast(80)
        }
    }

    hostView?.let { view ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .then(
                    if (isDraggable)
                        Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onDrag = { _, dragAmount -> onDrag(dragAmount.y) }
                            )
                        }
                    else
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showRemoveButton = !showRemoveButton }
                            )
                        }
                )
        ) {
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeightDp.dp)
                    .wrapContentHeight()
            )

            if (isDraggable) {
                // Sürükleme tutacağı — sağ üst köşe
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Taşı",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            if (showRemoveButton || !isDraggable) {
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
