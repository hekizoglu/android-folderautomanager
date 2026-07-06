package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Radyal "Quick Wheel" — uzun bas ile açılan uygulama çarkı.
 * En çok kullanılan MAX_ITEMS uygulamayı press noktası etrafında gösterir.
 * Dokunulan uygulama başlar, dışarı tıklanırsa kapanır.
 */
@Composable
fun QuickWheelOverlay(
    apps: List<AppInfo>,
    pressX: Float,
    pressY: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    onLaunch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val MAX_ITEMS = 6
    val RADIUS_DP = 90.dp
    val ICON_DP = 52.dp
    val density = LocalDensity.current

    val topApps = remember(apps) {
        apps.sortedByDescending { it.usageCount }.take(MAX_ITEMS)
    }

    val scaleAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scaleAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
        launch { alphaAnim.animateTo(1f, spring(stiffness = Spring.StiffnessMedium)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f * alphaAnim.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        val radiusPx = with(density) { RADIUS_DP.toPx() }
        val iconPx   = with(density) { ICON_DP.toPx() }
        val count    = topApps.size
        if (count == 0) return@Box

        topApps.forEachIndexed { i, app ->
            val angle = (2 * PI / count * i) - PI / 2  // üstten başla
            val cx = pressX + radiusPx * cos(angle).toFloat()
            val cy = pressY + radiusPx * sin(angle).toFloat()

            // Ekran sınırı klamp
            val clampedX = cx.coerceIn(iconPx / 2, screenWidthPx - iconPx / 2)
            val clampedY = cy.coerceIn(iconPx / 2, screenHeightPx - iconPx / 2)

            QuickWheelItem(
                app = app,
                iconSize = ICON_DP,
                modifier = Modifier
                    .offset { IntOffset((clampedX - iconPx / 2).roundToInt(), (clampedY - iconPx / 2).roundToInt()) }
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value),
                onClick = {
                    onLaunch(app.packageName)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun QuickWheelItem(
    app: AppInfo,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { iconSize.toPx().toInt() }
    val icon by produceState<ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(app.packageName).toBitmap(px, px).asImageBitmap()
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier
            .size(iconSize + 24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            icon?.let { bmp ->
                Image(bitmap = bmp, contentDescription = app.appName, modifier = Modifier.size(iconSize - 8.dp))
            } ?: Text(app.appName.take(1), fontSize = 20.sp, color = Color.White)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = app.appName,
            fontSize = 9.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
