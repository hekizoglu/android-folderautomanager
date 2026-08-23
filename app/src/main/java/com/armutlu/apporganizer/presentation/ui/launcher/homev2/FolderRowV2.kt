package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppIconView

/**
 * Klasör YATAY SATIRI (tur 27): her klasör sayfaya tam genişlik yayılır.
 * Klasör adı küçülüp tek satıra iner; asıl vurgu klasör içeriğindedir (uygulama
 * ikonları satır boyunca adaptif sayıda gösterilir). Sağda bildirim rozeti ve açma oku.
 */
@Composable
internal fun FolderRowV2(
    tile: FolderTileState,
    previewApps: List<AppInfo>,
    onOpen: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((String) -> Unit)? = null,
    textAlpha: Float = 1f,
    lifted: Boolean = false,
    dropHighlight: Boolean = false,
    interactionsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val accent = remember(tile.colorHex) { parseFolderColor(tile.colorHex) }
    val alpha = textAlpha.coerceIn(0f, 1f)

    Card(
        onClick = onOpen,
        enabled = interactionsEnabled,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = if (lifted) 1.02f else 1f
                scaleY = if (lifted) 1.02f else 1f
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (lifted) 8.dp else 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when {
                        dropHighlight -> Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(20.dp),
                        )
                        tile.hasUrgentNotification -> Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        else -> Modifier
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Klasör emoji rozeti
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tile.emoji, fontSize = 19.sp)
                }
                Spacer(Modifier.width(10.dp))

                // Klasör adı + uygulama sayısı (küçülebilir, tek satır)
                Column(modifier = Modifier.width(96.dp)) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${tile.appCount} uygulama",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(10.dp))

                // Klasör içeriği: uygulama ikonları (adaptif sayıda, satır boyunca)
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val iconSize = 40.dp
                    val gap = 6.dp
                    val fitCount = ((maxWidth + gap) / (iconSize + gap)).toInt().coerceAtLeast(1)
                    val shown = previewApps.take(fitCount)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        shown.forEach { app ->
                            AppIconView(
                                app = app,
                                onClick = { if (interactionsEnabled) onAppClick(app.packageName) },
                                onLongClick = if (interactionsEnabled) {
                                    onAppLongClick?.let { callback -> { callback(app.packageName) } }
                                } else {
                                    null
                                },
                                modifier = Modifier.size(iconSize),
                                showLabel = false,
                                iconSize = iconSize,
                                newBadgeEnabled = false,
                                notificationBadgeEnabled = false,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))

                // Bildirim rozeti + açma oku
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    if (tile.notificationTotal > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tile.hasUrgentNotification) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (tile.notificationTotal > 9) "9+" else tile.notificationTotal.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                    Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}
