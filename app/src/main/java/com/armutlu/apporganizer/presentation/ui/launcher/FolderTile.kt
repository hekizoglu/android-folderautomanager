package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun FolderTile(
    folder: AppFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folderTileScale"
    )

    val previewApps = folder.apps.take(4)

    Column(
        modifier = modifier
            .width(72.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 60dp circle — Pixel frosted glass style
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

        Spacer(Modifier.height(4.dp))

        Text(
            text = folder.category.categoryName,
            color = Color.White,
            fontSize = 11.sp,
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

    val bitmap: ImageBitmap? = remember(app.packageName, pxSize) {
        runCatching {
            context.packageManager
                .getApplicationIcon(app.packageName)
                .toBitmap(pxSize, pxSize)
                .asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
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
