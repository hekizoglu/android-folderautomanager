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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.armutlu.apporganizer.utils.AppPrefs

// İzin türü tanımı
enum class ContextualPermission(
    val permission: String?,
    val icon: ImageVector,
    val title: String,
    val reason: String,
    val privacyNote: String,
    val actionLabel: String = "İzin Ver"
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
    ),
    CONTACTS(
        permission = android.Manifest.permission.READ_CONTACTS,
        icon = Icons.Default.Info,
        title = "Kişi Erişimi",
        reason = "Aramada kişi kartlarını gösterebilmemiz için rehberinizi okumamız gerekiyor.",
        privacyNote = "Kişi verileri sadece cihaz içinde indekslenir, sunucuya gönderilmez."
    ),
    FILES(
        permission = null,
        icon = Icons.Default.Info,
        title = "Dosya Adları",
        reason = "Dosya aramasını açınca cihazınızdaki medya ve indirme adlarını yerel arama indeksine ekleriz.",
        privacyNote = "Yalnızca ad ve klasör yolu okunur; dosya içeriği açılmaz veya dışarı aktarılmaz.",
        actionLabel = "İndekslemeyi Aç"
    )
}

@Composable
fun ContextualPermissionDialog(
    permission: ContextualPermission,
    onGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val permissionPrefs = remember {
        context.getSharedPreferences(AppPrefs.PREFS_NAME, Activity.MODE_PRIVATE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Callback'ten donen boolean bazi OEM'lerde/multi-window'da guvenilmez olabilir —
        // sistemden checkSelfPermission ile yeniden dogrula (madde 1 fix, buton takili kalma).
        val actuallyGranted = granted || (permission.permission != null &&
            ContextCompat.checkSelfPermission(context, permission.permission) == PermissionChecker.PERMISSION_GRANTED)
        if (actuallyGranted) onGranted() else onDismiss()
    }
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) onGranted() else onDismiss()
    }

    // Kullanici sistem Ayarlar'a yonlendirilip (USAGE_ACCESS/NOTIFICATION_LISTENER/manuel izin)
    // geri donebilir — dialog acikken ON_RESUME'da izni yeniden kontrol et, butonun
    // loading/stuck gorunmesini engelle (madde 1 fix).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && permission.permission != null) {
                val granted = ContextCompat.checkSelfPermission(context, permission.permission) ==
                    PermissionChecker.PERMISSION_GRANTED
                if (granted) onGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                                    val askedBefore = permissionPrefs.getBoolean("asked_post_notifications", false)
                                    val shouldShow = (context as? Activity)
                                        ?.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) != false
                                    if (!askedBefore || shouldShow) {
                                        permissionPrefs.edit().putBoolean("asked_post_notifications", true).apply()
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.parse("package:${context.packageName}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        onDismiss()
                                    }
                                } else onGranted()
                            }
                            ContextualPermission.CONTACTS -> {
                                val alreadyGranted = ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.READ_CONTACTS
                                ) == PermissionChecker.PERMISSION_GRANTED
                                if (alreadyGranted) {
                                    onGranted()
                                    return@Button
                                }
                                val askedBefore = permissionPrefs.getBoolean("asked_read_contacts", false)
                                val activity = context as? Activity
                                val shouldShow = activity?.shouldShowRequestPermissionRationale(
                                    android.Manifest.permission.READ_CONTACTS
                                ) != false
                                if (!askedBefore || shouldShow) {
                                    permissionPrefs.edit().putBoolean("asked_read_contacts", true).apply()
                                    permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    onDismiss()
                                }
                            }
                            ContextualPermission.NOTIFICATION_LISTENER -> {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                onDismiss()
                            }
                            ContextualPermission.FILES -> {
                                val permissions = fileSearchPermissions()
                                if (permissions.isEmpty()) {
                                    onGranted()
                                    return@Button
                                }
                                val alreadyGranted = permissions.any { permissionName ->
                                    ContextCompat.checkSelfPermission(context, permissionName) ==
                                        PermissionChecker.PERMISSION_GRANTED
                                }
                                if (alreadyGranted) {
                                    onGranted()
                                    return@Button
                                }
                                if (permissions.size == 1) {
                                    permissionLauncher.launch(permissions.first())
                                } else {
                                    multiplePermissionLauncher.launch(permissions)
                                }
                            }
                        }
                    }) {
                        Text(permission.actionLabel)
                    }
                }
            }
        }
    }
}

private fun fileSearchPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_VIDEO,
        android.Manifest.permission.READ_MEDIA_AUDIO,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
    )
    else -> emptyArray()
}
