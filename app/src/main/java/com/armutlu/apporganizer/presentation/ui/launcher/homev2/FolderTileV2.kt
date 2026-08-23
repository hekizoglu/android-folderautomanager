package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppIconView

/**
 * Home V2 klasör kartı — SAF GÖRSEL bileşen.
 *
 * Tüm jestler (açma, hızlı başlat, sürükle-sırala) [FolderPageV2] hücre sarmalayıcısında
 * yaşar; kart yalnız render durumlarını alır:
 *  - [lifted]: sürüklenen kart (büyütme + yükseklik)
 *  - [dropHighlight]: bırakma hedefi kart (vurgu halkası)
 *  - [interactionsEnabled]: false iken tıklama kapalı (sıralama sürerken)
 */
@Composable
internal fun FolderTileV2(
    tile: FolderTileState,
    previewApps: List<AppInfo>,
    onOpen: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((String) -> Unit)? = null,
    textAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
    dropHighlight: Boolean = false,
    interactionsEnabled: Boolean = true,
) {
    val accent = remember(tile.colorHex) { parseFolderColor(tile.colorHex) }

    Card(
        onClick = onOpen,
        enabled = interactionsEnabled,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = if (lifted) 1.04f else 1f
                scaleY = if (lifted) 1.04f else 1f
            },
        shape = RoundedCornerShape(24.dp),
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
                            shape = RoundedCornerShape(24.dp),
                        )
                        tile.hasUrgentNotification -> Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(24.dp),
                        )
                        else -> Modifier
                    },
                )
                .padding(12.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tile.emoji, fontSize = 17.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tile.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha.coerceIn(0f, 1f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${tile.appCount} uygulama",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha.coerceIn(0f, 1f)),
                        )
                    }
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
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Adaptif ikon boyutu: hücre daraldıkça ikonlar küçülür, taşma olmaz.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                    val previewIconSize = ((maxWidth - 12.dp) / previewApps.size.coerceAtLeast(1)).coerceAtMost(34.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        previewApps.forEach { app ->
                            AppIconView(
                                app = app,
                                onClick = { if (interactionsEnabled) onAppClick(app.packageName) },
                                onLongClick = if (interactionsEnabled) {
                                    onAppLongClick?.let { callback ->
                                        { callback(app.packageName) }
                                    }
                                } else {
                                    null
                                },
                                modifier = Modifier.size(previewIconSize),
                                showLabel = false,
                                iconSize = previewIconSize,
                                newBadgeEnabled = false,
                                notificationBadgeEnabled = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "#RRGGBB" / "#AARRGGBB" → Color; bozuk girdide tema primary'e düşer. */
internal fun parseFolderColor(hex: String, fallback: Color = Color(0xFF6200EE)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
