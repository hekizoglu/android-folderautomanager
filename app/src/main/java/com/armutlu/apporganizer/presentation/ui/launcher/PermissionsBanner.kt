package com.armutlu.apporganizer.presentation.ui.launcher

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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

private val BannerBg   = Color(0xCC1A1A2E)
private val WarnColor  = Color(0xFFFFB300)
private val TealColor  = Color(0xFF00897B)

private fun isNotifGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PermissionChecker.PERMISSION_GRANTED
    else true

private fun isNotificationListenerGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    return flat.contains(context.packageName)
}

private fun isDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == context.packageName
}

data class PermissionIssue(val label: String, val action: () -> Unit)

private fun isBannerSnoozed(context: Context): Boolean {
    val prefs = context.getSharedPreferences("app_organizer_prefs", Context.MODE_PRIVATE)
    val snoozedUntil = prefs.getLong("banner_snoozed_until", 0L)
    return System.currentTimeMillis() < snoozedUntil
}

private fun snoozeBanner(context: Context) {
    val prefs = context.getSharedPreferences("app_organizer_prefs", Context.MODE_PRIVATE)
    prefs.edit().putLong("banner_snoozed_until", System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000).apply()
}

@Composable
fun PermissionsBanner() {
    val context = LocalContext.current
    var dismissed by remember { mutableStateOf(isBannerSnoozed(context)) }

    var notifGranted       by remember { mutableStateOf(isNotifGranted(context)) }
    var launcherSet        by remember { mutableStateOf(isDefaultLauncher(context)) }
    var notifListenerOk    by remember { mutableStateOf(isNotificationListenerGranted(context)) }

    @Suppress("DEPRECATION")

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted    = isNotifGranted(context)
                launcherSet     = isDefaultLauncher(context)
                notifListenerOk = isNotificationListenerGranted(context)
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
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        })
        if (!notifGranted) add(PermissionIssue("Bildirim izni verilmemiş") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        })
        if (!notifListenerOk) add(PermissionIssue("Bildirim badge'leri için izin gerekli") {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        })
    }

    AnimatedVisibility(
        visible = issues.isNotEmpty() && !dismissed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BannerBg)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Warning, null, tint = WarnColor, modifier = Modifier.size(16.dp))
                Text(
                    "  Eksik izinler var",
                    color = WarnColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Close, "Kapat",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            snoozeBanner(context)
                            dismissed = true
                        }
                )
            }
            issues.forEach { issue ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable { issue.action() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(issue.label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Text("Düzelt →", color = TealColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!notifListenerOk) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒", fontSize = 12.sp)
                    Text(
                        "Tüm veriler yalnızca cihazınızda kalır — dışarı çıkmaz.",
                        color = TealColor.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
