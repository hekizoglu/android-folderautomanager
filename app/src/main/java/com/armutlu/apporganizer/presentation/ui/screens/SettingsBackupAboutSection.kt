package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.workers.BackupWorker
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

/**
 * Hakkında (bilgi) + Yedek/Geri Yükle + Hakkında (gizlilik/versiyon) + Debug bölümleri
 */
internal fun LazyListScope.settingsBackupAboutSection(
    viewModel: AppListViewModel,
    appCount: Int,
    categoryCount: Int,
    logs: List<String>,
    onNavigateToPrivacyPolicy: () -> Unit
) {
    // ── Hakkında (üst) ──────────────────────────────────────────────────
    item { SettingsSectionTitle("Hakkında") }
    item {
        SettingsCard {
            SettingsInfoRow(Icons.Default.Apps, "App Organizer", "v1.0 beta")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsInfoRow(Icons.Default.Person, "Geliştirici", "Hüseyin Ekizoğlu")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsInfoRow(Icons.Default.Code, "Kaynak Kod", "github.com/hekizoglu/android-folderautomanager")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsInfoRow(Icons.Default.Storage, "Veritabanı", "$appCount uygulama · $categoryCount kategori")
        }
    }

    // ── Yedek / Geri Yükle ─────────────────────────────────────────────
    item { SettingsSectionTitle("Yedek / Geri Yükle") }
    item {
        val context = LocalContext.current
        var autoBackup by remember { mutableStateOf(AppPrefs.isAutoBackupEnabled(context)) }
        SettingsCard {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Otomatik Yedekleme", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("Haftalık periyodik JSON yedeği al", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoBackup, onCheckedChange = {
                    autoBackup = it
                    AppPrefs.setAutoBackupEnabled(context, it)
                    if (it) BackupWorker.schedule(context) else BackupWorker.cancel(context)
                })
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            val lastBackupMs = AppPrefs.getLastBackupTime(context)
            val lastBackupText = if (lastBackupMs == 0L) "Henüz yedeklenmedi"
                else java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("tr")).format(java.util.Date(lastBackupMs))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Son yedekleme: $lastBackupText", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Yedekleme zamanı: Pazartesi 03:00", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    // ── Google Drive / SAF Klasör Seçimi ───────────────────────────────────
    item {
        val context = LocalContext.current
        var driveFolderUri by remember { mutableStateOf(AppPrefs.getDriveFolderUri(context)) }
        val driveFolderPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            if (uri != null) {
                // Kalıcı okuma+yazma izni al
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                AppPrefs.setDriveFolderUri(context, uri.toString())
                driveFolderUri = uri.toString()
                android.widget.Toast.makeText(context, "Drive klasörü seçildi", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { driveFolderPickerLauncher.launch(null) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Drive Yedekleme Klasörü", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(
                        if (driveFolderUri != null) "Klasör seçildi — yedekler otomatik kopyalanır"
                        else "Google Drive klasörü seçin (SAF)",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (driveFolderUri != null) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            if (driveFolderUri != null) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppPrefs.setDriveFolderUri(context, null)
                            driveFolderUri = null
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Drive Bağlantısını Kaldır", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    item {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var backupLoading by remember { mutableStateOf(false) }
        var showRestoreDialog by remember { mutableStateOf(false) }
        var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var missingPackages by remember { mutableStateOf<List<String>>(emptyList()) }
        var showMissingDialog by remember { mutableStateOf(false) }
        val clipboardManager = LocalClipboardManager.current
        val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            pendingRestoreUri = uri
            showRestoreDialog = true
        }
        val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
        SettingsCard {
            Row(modifier = Modifier.fillMaxWidth()
                    .clickable(enabled = !backupLoading) {
                        coroutineScope.launch {
                            backupLoading = true
                            val intent = viewModel.exportBackup(context)
                            if (intent != null) shareLauncher.launch(Intent.createChooser(intent, "Yedeği paylaş"))
                            else android.widget.Toast.makeText(context, "Dışa aktarma başarısız", android.widget.Toast.LENGTH_SHORT).show()
                            backupLoading = false
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Yedek Al", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("Kategori atamalarını JSON olarak dışa aktar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (backupLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            Row(modifier = Modifier.fillMaxWidth()
                    .clickable(enabled = !backupLoading) { filePickerLauncher.launch("application/json") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Geri Yükle", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("JSON yedek dosyasından kategorileri içe aktar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // Eksik uygulama dialogu — restore sonrası yedekte olup cihazda yüklü olmayanlar
        if (showMissingDialog && missingPackages.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showMissingDialog = false },
                title = { Text("${missingPackages.size} Eksik Uygulama") },
                text = {
                    Column {
                        Text(
                            "Yedekte bulunan ancak bu cihazda yüklü olmayan uygulamalar:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            androidx.compose.foundation.lazy.items(missingPackages) { pkg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                                            )
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingBag, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(pkg, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Hepsini sırayla Play Store'da aç
                        missingPackages.forEach { pkg ->
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                        showMissingDialog = false
                    }) { Text("Hepsini Play Store'da Aç") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Listeyi panoya kopyala
                        val text = missingPackages.joinToString("\n")
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                        android.widget.Toast.makeText(context, "Kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("Kopyala") }
                }
            )
        }

        if (showRestoreDialog && pendingRestoreUri != null) {
            AlertDialog(
                onDismissRequest = {
                    showRestoreDialog = false
                    pendingRestoreUri = null
                },
                title = { Text("Yedeği geri yükle") },
                text = { Text("Bu işlem mevcut kategori atamalarınızı ve gizleme durumlarınızı seçilen yedekleme ile değiştirecek. Devam etmek istiyor musunuz?") },
                confirmButton = {
                    TextButton(onClick = {
                        showRestoreDialog = false
                        coroutineScope.launch {
                            backupLoading = true
                            runCatching {
                                val json = context.contentResolver.openInputStream(pendingRestoreUri!!)?.bufferedReader()?.readText() ?: return@runCatching
                                val result = viewModel.importBackup(json)
                                if (result.success) {
                                    android.widget.Toast.makeText(context,
                                        "${result.updatedCount} uygulama geri yüklendi",
                                        android.widget.Toast.LENGTH_SHORT).show()
                                    if (result.missingPackages.isNotEmpty()) {
                                        missingPackages = result.missingPackages
                                        showMissingDialog = true
                                    }
                                } else {
                                    android.widget.Toast.makeText(context,
                                        "Geri yükleme başarısız: ${result.error}",
                                        android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                            backupLoading = false
                            pendingRestoreUri = null
                        }
                    }) { Text("Yüklemeyi Başlat", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRestoreDialog = false
                        pendingRestoreUri = null
                    }) { Text("İptal") }
                }
            )
        }
    }

    // ── Hakkında (gizlilik + versiyon) ──────────────────────────────────
    item { SettingsSectionTitle("Hakkında") }
    item {
        SettingsCard {
            SettingsButtonRow(Icons.Default.PrivacyTip, "Gizlilik Politikası",
                "Veri toplama ve kullanım hakkınızda bilgi alın",
                showChevron = true,
                onClick = onNavigateToPrivacyPolicy)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsInfoRow(Icons.Default.Info, "Versiyon", "AppOrganizer 1.0.0 — Haziran 2026")
        }
    }

    // ── Debug ────────────────────────────────────────────────────────────
    if (logs.isNotEmpty()) {
        item { SettingsSectionTitle("Debug") }
        item {
            val clipboard = LocalClipboardManager.current
            var debugExpanded by remember { mutableStateOf(false) }
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth()
                        .clickable { debugExpanded = !debugExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Loglar", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("${logs.size} satır", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (debugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (debugExpanded) {
                    Column(modifier = Modifier.fillMaxWidth().animateContentSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        logs.takeLast(30).forEach { line ->
                            Text(line, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 1.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(viewModel.getDebugLogs())) },
                                modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Kopyala", fontSize = 13.sp)
                            }
                            OutlinedButton(onClick = { viewModel.clearDebugLogs() }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Temizle", fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
