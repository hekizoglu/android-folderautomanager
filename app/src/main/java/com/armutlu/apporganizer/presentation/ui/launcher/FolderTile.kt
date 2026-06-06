package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.armutlu.apporganizer.domain.models.AppInfo

@Composable
fun FolderTile(
    folder: AppFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "folderTileScale"
    )

    val categoryColor = remember(folder.category.colorHex) {
        runCatching { Color(folder.category.colorHex.toColorInt()) }
            .getOrDefault(Color.Gray)
    }

    val previewApps = folder.apps.take(9)
    val appCount = folder.apps.size

    Column(
        modifier = modifier
            .scale(scale)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        val pressed = tryAwaitRelease()
                        if (pressed) onClick()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(categoryColor.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            if (appCount == 0) {
                // Show category emoji when folder is empty
                Text(
                    text = folder.category.iconEmoji,
                    fontSize = 32.sp
                )
            } else {
                // 3x3 grid of mini icons
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .size(72.dp)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = false
                ) {
                    items(
                        items = buildList {
                            addAll(previewApps)
                            // pad with nulls to fill up to 9 slots
                            repeat(9 - previewApps.size) { add(null) }
                        }
                    ) { app: AppInfo? ->
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app != null) {
                                AppIconView(
                                    app = app,
                                    onClick = {},
                                    iconSize = 20.dp,
                                    showLabel = false
                                )
                            }
                        }
                    }
                }
            }

            // App count badge in top-right corner
            if (appCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (appCount > 99) "99+" else appCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = folder.category.categoryName,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
