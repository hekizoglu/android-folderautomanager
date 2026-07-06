package com.armutlu.apporganizer.presentation.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.utils.UsageStatsHelper

/**
 * Eksik izinler kartı — SettingsScreen'in EN ÜSTÜNDE görünür (sadece eksik izin varsa).
 * Ana ekrandaki PermissionsBanner'ın yerini aldı: ana ekran temiz kalır, izin yönetimi
 * Ayarlar'da yaşar. ON_RESUME'da yeniden kontrol edilir (izin verilince kart kaybolur).
 */

private val WarnColor = Color(0xFFFFB300)

data class PermissionIssue(val label: String, val action: () -> Unit)

fun isNotifGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED
    else true

fun isNotificationListenerGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    return flat.contains(context.packageName)
}

fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == context.packageName
}

@Composable
fun SettingsPermissionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var notifGranted      by remember { mutableStateOf(isNotifGranted(context)) }
    var launcherSet       by remember { mutableStateOf(isDefaultLauncher(context)) }
    var notifListenerOk   by remember { mutableStateOf(isNotificationListenerGranted(context)) }
    var usageStatsGranted by remember { mutableStateOf(UsageStatsHelper.hasPermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted      = isNotifGranted(context)
                launcherSet       = isDefaultLauncher(context)
                notifListenerOk   = isNotificationListenerGranted(context)
                usageStatsGranted = UsageStatsHelper.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifGranted = it }

    val issues = buildList {
        if (!launcherSet) add(PermissionIssue("Varsayılan launcher ayarlı değil") {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        })
        if (!notifGranted) add(PermissionIssue("Bildirim izni verilmemiş") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        })
        if (!notifListenerOk) add(PermissionIssue("Bildirim badge'leri için erişim gerekli") {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        })
        if (!usageStatsGranted) add(PermissionIssue("Öneriler için kullanım erişimi gerekli") {
            UsageStatsHelper.openPermissionSettings(context)
        })
    }

    AnimatedVisibility(visible = issues.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(WarnColor.copy(alpha = 0.12f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Warning, null, tint = WarnColor, modifier = Modifier.size(18.dp))
                Text(
                    "  Eksik izinler",
                    color = WarnColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            issues.forEach { issue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .clickable { issue.action() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(issue.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("Düzelt →", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!notifListenerOk) {
                Text(
                    "🔒 Tüm veriler yalnızca cihazınızda kalır — dışarı çıkmaz.",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
