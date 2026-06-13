package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.armutlu.apporganizer.utils.WidgetHostManager

/**
 * Ana ekrandaki widget alanı. Her widget ID için bir WidgetView render eder.
 * Uzun basınca silme butonu belirir.
 */
@Composable
fun WidgetArea(
    widgetIds: List<Int>,
    onRemoveWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (widgetIds.isEmpty()) return

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        widgetIds.forEach { id ->
            WidgetCard(
                widgetId = id,
                onRemove = { onRemoveWidget(id) }
            )
        }
    }
}

@Composable
private fun WidgetCard(
    widgetId: Int,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var showRemoveButton by remember { mutableStateOf(false) }
    var hostView by remember { mutableStateOf<android.appwidget.AppWidgetHostView?>(null) }
    var minHeightDp by remember { mutableIntStateOf(100) }

    LaunchedEffect(widgetId) {
        hostView = WidgetHostManager.createView(context, widgetId)
        // Widget'ın minimum yüksekliğini AppWidgetProviderInfo'dan oku
        val awm = context.getSystemService(android.content.Context.APPWIDGET_SERVICE) as? android.appwidget.AppWidgetManager
        awm?.getAppWidgetInfo(widgetId)?.let { info ->
            val density = context.resources.displayMetrics.density
            minHeightDp = (info.minHeight / density).toInt().coerceAtLeast(80)
        }
    }

    // hostView null iken placeholder, hazir oldugunda widget goster
    hostView?.let { view ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showRemoveButton = !showRemoveButton }
                    )
                }
        ) {
            AndroidView(
                factory = { view },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeightDp.dp)
                    .wrapContentHeight()
            )

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
