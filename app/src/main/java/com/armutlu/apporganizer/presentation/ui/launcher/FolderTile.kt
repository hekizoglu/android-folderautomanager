package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache get() = iconCacheInternal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderTile(
    folder: AppFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hapticFeedback = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folderTileScale"
    )

    val previewApps = folder.apps.take(4)
    val totalBadge  = folder.apps.sumOf { it.notificationCount }

    Column(
        modifier = modifier
            .width(72.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 60dp circle — Pixel frosted glass style + badge
        Box {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            if (folder.apps.isEmpty()) {
                Text(
                    text = folder.category.iconEmoji,
                    fontSize = 24.sp
                )
            } else {
                // 2x2 mini icon grid
                MiniIconGrid(
                    apps = previewApps,
                    iconSize = 22.dp
                )
            }
        }
        // Klasör badge
        if (totalBadge > 0) {
            val badgeText = if (totalBadge > 99) "99+" else totalBadge.toString()
            val badgeW = if (totalBadge > 9) 20.dp else 16.dp
            Box(
                modifier = Modifier
                    .size(badgeW, 16.dp)
                    .align(Alignment.TopEnd)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Text(badgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        } // outer Box

        Spacer(Modifier.height(4.dp))

        Text(
            text = folder.category.categoryName,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp)
        )
    }
}

@Composable
private fun MiniIconGrid(
    apps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
    iconSize: Dp
) {
    // Pad to 4 slots (null = empty)
    val slots: List<com.armutlu.apporganizer.domain.models.AppInfo?> = buildList {
        addAll(apps)
        repeat(4 - apps.size) { add(null) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: slots 0, 1
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniAppIcon(app = slots[0], size = iconSize)
            MiniAppIcon(app = slots[1], size = iconSize)
        }
        // Row 2: slots 2, 3
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniAppIcon(app = slots[2], size = iconSize)
            MiniAppIcon(app = slots[3], size = iconSize)
        }
    }
}

@Composable
private fun MiniAppIcon(
    app: com.armutlu.apporganizer.domain.models.AppInfo?,
    size: Dp
) {
    if (app == null) {
        // Empty slot — transparent placeholder
        Box(modifier = Modifier.size(size))
        return
    }

    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pxSize = with(density) { size.roundToPx() }
    val cacheKey = "${app.packageName}_$pxSize"

    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCache[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(app.packageName)
                        .toBitmap(pxSize, pxSize)
                        .asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCache.put(cacheKey, loaded)
            value = loaded
        }
    }

    val bitmapSnapshot = bitmap
    if (bitmapSnapshot != null) {
        Image(
            bitmap = bitmapSnapshot,
            contentDescription = app.appName,
            modifier = Modifier.size(size)
        )
    } else {
        // Fallback: small circle with first letter
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                color = Color.White,
                fontSize = 8.sp
            )
        }
    }
}
