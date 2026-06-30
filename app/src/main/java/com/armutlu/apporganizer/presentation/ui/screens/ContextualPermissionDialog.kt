package com.armutlu.apporganizer.presentation.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

// İzin türü tanımı
enum class ContextualPermission(
    val permission: String?,
    val icon: ImageVector,
    val title: String,
    val reason: String,
    val privacyNote: String
) {
    USAGE_ACCESS(
        permission = null,
        icon = Icons.Default.Info,
        title = "Kullanım İstatistikleri",
        reason = "Son kullanılan uygulamaları göstermek ve 'Önerilen' listesini kişiselleştirmek için bu izne ihtiyaç var.",
        privacyNote = "Veriler sadece cihazınızda işlenir, hiçbir yere gönderilmez."
    ),
    NOTIFICATIONS(
        permission = android.Manifest.permission.POST_NOTIFICATIONS,
        icon = Icons.Default.Info,
        title = "Bildirimler",
        reason = "Uygulama güncellemeleri ve yedekleme hatırlatıcıları için bildirim göndermek istiyoruz.",
        privacyNote = "Yalnızca önemli sistem bildirimleri gönderilir."
    ),
    NOTIFICATION_LISTENER(
        permission = null,
        icon = Icons.Default.Info,
        title = "Bildirim Erişimi",
        reason = "Uygulama simgelerinde bildirim sayısını göstermek için bu izne ihtiyaç var.",
        privacyNote = "Bildirim içerikleri okunmaz, sadece sayılar takip edilir."
    )
}

@Composable
fun ContextualPermissionDialog(
    permission: ContextualPermission,
    onGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted() else onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // İkon + Başlık
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(permission.icon, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("İzin Gerekli", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Text(permission.title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Neden gerekli
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(permission.reason, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    }
                }

                // Gizlilik notu
                Text("🔒 ${permission.privacyNote}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.55f), lineHeight = 18.sp)

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Daha Sonra", color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                    Button(onClick = {
                        when (permission) {
                            ContextualPermission.USAGE_ACCESS -> {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                onDismiss()
                            }
                            ContextualPermission.NOTIFICATIONS -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val alreadyGranted = ContextCompat.checkSelfPermission(
                                        context, android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == PermissionChecker.PERMISSION_GRANTED
                                    if (alreadyGranted) { onGranted(); return@Button }
                                    val shouldShow = (context as? Activity)
                                        ?.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) != false
                                    if (shouldShow) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.parse("package:${context.packageName}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        onDismiss()
                                    }
                                } else onGranted()
                            }
                            ContextualPermission.NOTIFICATION_LISTENER -> {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                onDismiss()
                            }
                        }
                    }) {
                        Text("İzin Ver")
                    }
                }
            }
        }
    }
}
