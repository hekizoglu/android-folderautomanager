package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.utils.UsageStatsHelper

/**
 * Madde 11 + S4: "Tam Performans / Gerekli İzinler" ekranı.
 *
 * Uygulamanın tam performansla çalışması için gereken her izni tek ekranda listeler:
 * her izin için NEDEN gerekli olduğu ve KAPALIYKEN NE ÇALIŞMAZ (S4) açıklamasıyla.
 *
 * Durum kontrol fonksiyonları SettingsPermissionsSection.kt'den yeniden kullanılır
 * (isDefaultLauncher / isNotifGranted / isNotificationListenerGranted / UsageStatsHelper).
 * Durum ON_RESUME'da yeniden hesaplanır — kullanıcı ayardan dönünce ✓ olur.
 */

private val OkColor = Color(0xFF2E7D32)
private val MissingColor = Color(0xFFFFB300)

private data class PermissionGuideItem(
    val icon: ImageVector,
    val title: String,
    val granted: Boolean,
    val why: String,
    val whenOff: String,
    val actionLabel: String,
    val onAction: () -> Unit,
)

@Composable
fun PermissionsGuideScreen(onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current

    var launcherSet     by remember { mutableStateOf(isDefaultLauncher(context)) }
    var usageStatsOk     by remember { mutableStateOf(UsageStatsHelper.hasPermission(context)) }
    var notifListenerOk by remember { mutableStateOf(isNotificationListenerGranted(context)) }
    var notifGranted     by remember { mutableStateOf(isNotifGranted(context)) }

    // ON_RESUME'da yeniden kontrol — izin verilip dönünce durum ✓ olur (mevcut pattern)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                launcherSet     = isDefaultLauncher(context)
                usageStatsOk     = UsageStatsHelper.hasPermission(context)
                notifListenerOk = isNotificationListenerGranted(context)
                notifGranted     = isNotifGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifGranted = it }

    val items = listOf(
        PermissionGuideItem(
            icon = Icons.Default.Home,
            title = "Varsayılan Başlatıcı",
            granted = launcherSet,
            why = "Ana ekran tuşu uygulamayı açsın, otomatik klasörler görünsün.",
            whenOff = "Uygulama sadece normal app olarak açılır, ana ekran deneyimi devre dışı.",
            actionLabel = "Ayarları Aç",
            onAction = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            },
        ),
        PermissionGuideItem(
            icon = Icons.Default.QueryStats,
            title = "Kullanım Erişimi",
            granted = usageStatsOk,
            why = "Kullanım sürelerini ve 'bu saatte en çok' önerilerini hesaplar.",
            whenOff = "Öneriler ve kullanım raporları boş gelir.",
            actionLabel = "İzin Ver",
            onAction = { UsageStatsHelper.openPermissionSettings(context) },
        ),
        PermissionGuideItem(
            icon = Icons.Default.NotificationsActive,
            title = "Bildirim Erişimi",
            granted = notifListenerOk,
            why = "Klasörlerde bildirim rozetlerini gösterir.",
            whenOff = "Rozet ve bildirim özeti çalışmaz.",
            actionLabel = "İzin Ver",
            onAction = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            },
        ),
        PermissionGuideItem(
            icon = Icons.Default.Notifications,
            title = "Bildirim İzni",
            granted = notifGranted,
            why = "Haftalık rapor, yeni uygulama ve akıllı öneri bildirimleri.",
            whenOff = "Hiçbir bildirim gösterilemez.",
            actionLabel = "İzin Ver",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            },
        ),
    )

    val missingCount = items.count { !it.granted }

    SettingsSubScreenScaffold(
        title = "Tam Performans / İzinler",
        onNavigateBack = onNavigateBack,
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    if (missingCount == 0)
                        "Tüm izinler verildi — uygulama tam performansla çalışıyor."
                    else
                        "Tam performans için $missingCount izin eksik. Her kartta neyin çalışmadığını görebilirsin.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items.forEach { permItem ->
            item { PermissionGuideCard(permItem) }
        }
        item {
            Text(
                "🔒 Tüm veriler yalnızca cihazınızda kalır — dışarı çıkmaz.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun PermissionGuideCard(item: PermissionGuideItem) {
    val statusColor = if (item.granted) OkColor else MissingColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Başlık satırı: ikon + izin adı + durum
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                item.icon, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (item.granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (item.granted) "Verildi" else "Eksik",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }
        }

        // Neden gerekli
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Neden gerekli:  ", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(item.why, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Kapalıyken ne çalışmaz (S4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MissingColor.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text("Kapalıyken:  ", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MissingColor)
            Text(item.whenOff, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        // Eksikse aksiyon butonu
        if (!item.granted) {
            Button(
                onClick = item.onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(item.actionLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
