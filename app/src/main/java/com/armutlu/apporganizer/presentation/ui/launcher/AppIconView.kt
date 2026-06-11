package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Process-level icon cache — tüm launcher composable'ları tarafından paylaşılır
internal val iconCacheInternal = androidx.collection.LruCache<String, ImageBitmap>(200)
private val iconCache get() = iconCacheInternal

// Label altına scrim gradient — her türlü duvar kağıdında okunabilirlik
private val LabelScrim = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
)

// Text shadow modifier — beyaz yazı + koyu gölge
private fun Modifier.textShadow(
    color: Color = Color.Black.copy(alpha = 0.6f),
    offsetX: Float = 0f,
    offsetY: Float = 1.5f,
    blurRadius: Float = 4f
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blurRadius, offsetX, offsetY, color.toArgb())
            }
        }
        canvas.drawRect(
            left = 0f, top = 0f,
            right = size.width, bottom = size.height,
            paint = paint
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconView(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    iconSize: Dp = 56.dp
) {
    val context = LocalContext.current
    val px = (iconSize.value * context.resources.displayMetrics.density).toInt()
    val cacheKey = "${app.packageName}_$px"

    val icon: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCache[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(app.packageName)
                        .toBitmap(px, px)
                        .asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCache.put(cacheKey, loaded)
            value = loaded
        }
    }

    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kullanım bazlı greyscale — kullanıcı ayarına bağlı (unusedGreyDays > 0 ise aktif)
        val unusedGreyDays = remember { com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context) }
        val isUnused = unusedGreyDays > 0 && app.usageCount == 0L
        val saturation = when {
            isUnused             -> 0f
            app.usageCount < 5L && unusedGreyDays > 0 -> 0.4f + (app.usageCount * 0.12f).coerceAtMost(0.6f)
            else                 -> 1f
        }
        val iconAlpha = if (isUnused) 0.45f else 1f
        val greyFilter = if (saturation < 1f)
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) })
        else null

        // İkon + bildirim badge
        Box {
            if (icon != null) {
                Image(
                    bitmap = icon!!,
                    contentDescription = app.appName,
                    colorFilter = greyFilter,
                    alpha = iconAlpha,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Gray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }
            }
            // Bildirim badge
            if (app.notificationCount > 0) {
                val badgeText = if (app.notificationCount > 99) "99+" else app.notificationCount.toString()
                val badgeWidth = if (app.notificationCount > 9) 20.dp else 16.dp
                Box(
                    modifier = Modifier
                        .size(badgeWidth, 16.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        style = TextStyle(textAlign = TextAlign.Center)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(3.dp))
            // Scrim + text shadow ile her duvar kağıdında okunabilir label
            Box(
                modifier = Modifier
                    .width(iconSize + 8.dp)
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(LabelScrim)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.7f),
                            offset = Offset(0f, 1f),
                            blurRadius = 4f
                        )
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(iconSize + 4.dp)
                )
            }
        }
    }
}
