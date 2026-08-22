package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * HomeScreen'da gösterilecek bildirim rozeti izin kartı — "Bildirim Erişimi kapalı" + "Etkinleştir" buton
 * + "Daha sonra" + "Hiçbir zaman" seçenekleri.
 */
@Composable
fun NotificationBadgePermissionCard(
    onEnabledClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Başlık + İkon
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        "Bildirim Rözeti",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Şu anda kapalı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Açıklama
            Text(
                "Uygulama simgelerinde bildirim sayısını gösterebilmek için Bildirim Erişim izni gerekli.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
            )

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = {
                        NotificationListenerPermissionHelper.snoozeNotificationBadgePermissionCard(context)
                        onDismiss()
                    },
                ) {
                    Text("Daha Sonra", fontSize = 12.sp)
                }
                TextButton(
                    onClick = {
                        NotificationListenerPermissionHelper.dismissNotificationBadgePermissionCard(context)
                        onDismiss()
                    },
                ) {
                    Text("Hiçbir Zaman", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        NotificationListenerPermissionHelper.snoozeNotificationBadgePermissionCard(context)
                        onEnabledClick()
                    },
                    modifier = Modifier.height(36.dp),
                ) {
                    Text("Ayarlar", fontSize = 12.sp)
                }
            }
        }
    }
}
