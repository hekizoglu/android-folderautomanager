package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import com.armutlu.apporganizer.workers.WeeklyDigestWorker
import java.io.BufferedReader
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToUsageReport: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
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

    // ── Gizlilik Merkezi ─────────────────────────────────────────────────
    item { SettingsSectionTitle("Gizlilik") }
    item {
        val context = LocalContext.current
        var showResetDialog by remember { mutableStateOf(false) }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Tüm Kullanım Verisini Sıfırla") },
                text = { Text("Kullanım sayıları, son açılma zamanları, bildirim geçmişi ve notlar silinir. Bu işlem geri alınamaz.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetDialog = false
                            viewModel.resetAllPrivacyData(context)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sıfırla") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("İptal") }
                }
            )
        }

        SettingsCard {
            // Gizlilik maddeleri
            val privacyItems = listOf(
                Icons.Default.PhoneAndroid to "Uygulama listesi cihazda kalır",
                Icons.Default.CloudOff to "İnternete veri gönderilmez",
                Icons.Default.ToggleOff to "Online kategori DB varsayılan kapalı",
                Icons.Default.Visibility to "Bildirim içeriği okunmaz (sadece sayı)",
                Icons.Default.Security to "Reklamcılık veya izleme yok"
            )
            privacyItems.forEachIndexed { i, (icon, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                if (i < privacyItems.size - 1)
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            // Sıfırlama butonu
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showResetDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tüm Kullanım Verisini Sıfırla", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                    Text("Kullanım sayıları, notlar ve bildirim geçmişi silinir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
                Text("Son yedekleme: $lastBackupText", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))

            // ── Yedekleme gün/saat/dakika seçimi ──
            val gunAdlari = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")
            var backupDay by remember { mutableStateOf(AppPrefs.getBackupDayOfWeek(context)) }
            var backupHour by remember { mutableStateOf(AppPrefs.getBackupHour(context)) }
            var backupMinute by remember { mutableStateOf(AppPrefs.getBackupMinute(context)) }
            var showDayMenu by remember { mutableStateOf(false) }
            var showHourMenu by remember { mutableStateOf(false) }
            var showMinuteMenu by remember { mutableStateOf(false) }

            fun rescheduleIfEnabled() {
                if (AppPrefs.isAutoBackupEnabled(context)) BackupWorker.schedule(context)
            }

            val backupDayIndex = (backupDay - 1).coerceIn(0, gunAdlari.lastIndex)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Yedekleme zamanı: ${gunAdlari[backupDayIndex]} %02d:%02d".format(backupHour, backupMinute),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gün seçici
                Box {
                    AssistChip(onClick = { showDayMenu = true }, label = { Text(gunAdlari[backupDayIndex]) })
                    DropdownMenu(expanded = showDayMenu, onDismissRequest = { showDayMenu = false }) {
                        gunAdlari.forEachIndexed { index, gunAdi ->
                            DropdownMenuItem(
                                text = { Text(gunAdi) },
                                onClick = {
                                    backupDay = index + 1
                                    AppPrefs.setBackupDayOfWeek(context, index + 1)
                                    showDayMenu = false
                                    rescheduleIfEnabled()
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Saat seçici
                Box {
                    AssistChip(onClick = { showHourMenu = true }, label = { Text("%02d".format(backupHour)) })
                    DropdownMenu(expanded = showHourMenu, onDismissRequest = { showHourMenu = false }) {
                        (0..23).forEach { h ->
                            DropdownMenuItem(
                                text = { Text("%02d".format(h)) },
                                onClick = {
                                    backupHour = h
                                    AppPrefs.setBackupHour(context, h)
                                    showHourMenu = false
                                    rescheduleIfEnabled()
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(":", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                // Dakika seçici
                Box {
                    AssistChip(onClick = { showMinuteMenu = true }, label = { Text("%02d".format(backupMinute)) })
                    DropdownMenu(expanded = showMinuteMenu, onDismissRequest = { showMinuteMenu = false }) {
                        listOf(0, 15, 30, 45).forEach { m ->
                            DropdownMenuItem(
                                text = { Text("%02d".format(m)) },
                                onClick = {
                                    backupMinute = m
                                    AppPrefs.setBackupMinute(context, m)
                                    showMinuteMenu = false
                                    rescheduleIfEnabled()
                                }
                            )
                        }
                    }
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
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                    AppPrefs.setDriveFolderUri(context, uri.toString())
                    driveFolderUri = uri.toString()
                    android.widget.Toast.makeText(context, "Drive klasörü seçildi", android.widget.Toast.LENGTH_SHORT).show()
                }.onFailure {
                    android.widget.Toast.makeText(context, "Klasör izni alınamadı", android.widget.Toast.LENGTH_SHORT).show()
                }
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
            var selectedMissing by remember(missingPackages) {
                mutableStateOf(missingPackages.toSet())
            }
            // Bir sonraki açılacak paketin missingPackages içindeki index'i
            var nextOpenIndex by remember(missingPackages) { mutableStateOf(0) }
            var openedSoFar by remember(missingPackages) { mutableStateOf(0) }

            AlertDialog(
                onDismissRequest = { showMissingDialog = false },
                title = { Text("Restore sonrası eksik uygulamalar (${missingPackages.size})") },
                text = {
                    Column {
                        Text(
                            "Yedekte bulunan ancak bu cihazda yüklü olmayan uygulamalar. Play Store'da açmak istediklerini işaretle:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(missingPackages) { pkg ->
                                val isChecked = pkg in selectedMissing
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedMissing = if (isChecked) selectedMissing - pkg else selectedMissing + pkg
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = isChecked, onCheckedChange = { checked ->
                                        selectedMissing = if (checked) selectedMissing + pkg else selectedMissing - pkg
                                    })
                                    Icon(
                                        Icons.Default.ShoppingBag, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = pkg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        if (openedSoFar > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$openedSoFar/${selectedMissing.size} açıldı",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    val hasNext = com.armutlu.apporganizer.utils.PlayStoreQueueHelper.nextSelectedIndex(
                        missingPackages, selectedMissing, nextOpenIndex
                    ) != null
                    TextButton(
                        enabled = hasNext,
                        onClick = {
                            val idx = com.armutlu.apporganizer.utils.PlayStoreQueueHelper.nextSelectedIndex(
                                missingPackages, selectedMissing, nextOpenIndex
                            )
                            if (idx != null) {
                                val pkg = missingPackages[idx]
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(com.armutlu.apporganizer.utils.PlayStoreQueueHelper.playStoreUrl(pkg))
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                nextOpenIndex = idx + 1
                                openedSoFar += 1
                            }
                        }
                    ) {
                        Text(
                            if (openedSoFar == 0) "Seçilenleri Play Store'da Aç"
                            else "Sonraki Uygulamayı Aç ($openedSoFar/${selectedMissing.size})"
                        )
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            // Sadece seçilenleri panoya kopyala
                            val text = selectedMissing.joinToString("\n")
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                            android.widget.Toast.makeText(context, "Kopyalandı", android.widget.Toast.LENGTH_SHORT).show()
                        }) { Text("Kopyala") }
                        TextButton(onClick = { showMissingDialog = false }) { Text("Kapat") }
                    }
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
                                val json = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(pendingRestoreUri!!)
                                        ?.bufferedReader()
                                        ?.use(BufferedReader::readText)
                                } ?: return@runCatching
                                val result = viewModel.importBackup(context, json)
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

    // ── Neden AppOrganizer? ──────────────────────────────────────────────
    item { SettingsSectionTitle("Neden AppOrganizer?") }
    item {
        SettingsCard {
            val features = listOf(
                Icons.Default.FolderOpen to "Otomatik Klasörleme" to "Pixel Launcher'da yok — 3700+ uygulama otomatik kategorilere ayrılır",
                Icons.Default.Backup to "Çapraz Cihaz Yedekleme" to "Google Drive ile tüm kategoriler yeni cihaza taşınır",
                Icons.Default.Brush to "İkon Pack Desteği" to "3. parti ikon paketleri uygulanabilir",
                Icons.Default.Gesture to "Özelleştirilebilir Jestler" to "Kaydırma yönleri ve kısayollar ayarlanabilir",
                Icons.Default.Category to "App Drawer Kategorileri" to "Uygulama çekmecesinde kategoriye göre filtreleme",
                Icons.Default.PhotoSizeSelectLarge to "İkon Boyutu" to "%70-%130 arasında ikon boyutu ayarı"
            )
            features.forEachIndexed { i, (iconTitle, desc) ->
                val (icon, title) = iconTitle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (i < features.size - 1) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                }
            }
        }
    }

    // ── Dashboard ──────────────────────────────────────────────────────
    item {
        SettingsCard {
            SettingsButtonRow(
                icon = Icons.Default.Dashboard,
                title = "AppOrganizer Dashboard",
                subtitle = "Klasor, kategori ve kullanim istatistikleri",
                showChevron = true,
                onClick = onNavigateToDashboard
            )
            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f)
            )
            SettingsButtonRow(
                icon = Icons.Default.BarChart,
                title = "Kullanim Raporu",
                subtitle = "En cok/az kullanilan uygulamalar, gizleme onerileri",
                showChevron = true,
                onClick = onNavigateToUsageReport
            )
        }
    }

    // ── Haftalık Digest Bildirimi ────────────────────────────────────────
    item {
        val context = LocalContext.current
        var weeklyDigest by remember { mutableStateOf(AppPrefs.isWeeklyDigestEnabled(context)) }
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Haftalık Uygulama Raporu", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("7+ gündür açılmayan uygulamalar için haftalık bildirim — Bildirimler > Akıllı Bildirimler'deki \"Kullanılmayan Uygulamalar\" (3+ hafta) ile benzer amaçlı, farklı eşik", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = weeklyDigest,
                    onCheckedChange = {
                        weeklyDigest = it
                        AppPrefs.setWeeklyDigestEnabled(context, it)
                        if (it) WeeklyDigestWorker.schedule(context)
                        else WeeklyDigestWorker.cancel(context)
                    }
                )
            }
        }
    }

    // ── Hakkında (gizlilik + versiyon) ──────────────────────────────────
    item { SettingsSectionTitle("Hakkında") }
    item {
        val context = LocalContext.current
        var showRestartDialog by remember { mutableStateOf(false) }
        SettingsCard {
            SettingsButtonRow(Icons.Default.PrivacyTip, "Gizlilik Politikası",
                "Veri toplama ve kullanım özetini aç",
                showChevron = true,
                onClick = onNavigateToPrivacyPolicy)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsButtonRow(Icons.Default.RestartAlt, "Kurulum Sihirbazını Yeniden Başlat",
                "İlk kurulum adımlarını sıfırla ve başa dön",
                showChevron = false,
                onClick = { showRestartDialog = true }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsInfoRow(Icons.Default.Info, "Versiyon", "AppOrganizer 1.0.2 — Haziran 2026")
        }
        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                title = { Text("Kurulum Sihirbazı") },
                text = { Text("Onboarding adımları sıfırlanacak. Uygulama yeniden başlatılacak.") },
                confirmButton = {
                    TextButton(onClick = {
                        context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean(AppPrefs.KEY_ONBOARDING_DONE, false).apply()
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        if (intent != null) context.startActivity(intent)
                    }) { Text("Sıfırla ve Başlat") }
                },
                dismissButton = { TextButton(onClick = { showRestartDialog = false }) { Text("İptal") } }
            )
        }
    }

    // ── Crash Raporları ──────────────────────────────────────────────────
    item {
        val context = LocalContext.current
        val crashLogs = remember { com.armutlu.apporganizer.utils.CrashReporter.getAllCrashLogs(context) }
        var showCrashDialog by remember { mutableStateOf(false) }
        val isSafeMode = remember { com.armutlu.apporganizer.utils.CrashReporter.isSafeModeActive(context) }
        if (crashLogs.isNotEmpty() || isSafeMode) {
            SettingsCard {
                if (isSafeMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            com.armutlu.apporganizer.utils.CrashReporter.exitSafeMode(context)
                        }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Güvenli Mod Aktif", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                            Text("Uygulama güvenli modda başlatıldı. Çıkmak için dokun.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (crashLogs.isNotEmpty()) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                }
                if (crashLogs.isNotEmpty()) {
                    val clipboard = LocalClipboardManager.current
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showCrashDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hata Raporları", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("${crashLogs.size} crash kaydedildi — ayrıntıları aç", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (showCrashDialog) {
                        AlertDialog(
                            onDismissRequest = { showCrashDialog = false },
                            title = { Text("Son Crash Raporu") },
                            text = {
                                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                    item {
                                        Text(
                                            text = crashLogs.first().take(2000),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(crashLogs.first()))
                                    showCrashDialog = false
                                }) { Text("Kopyala") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    com.armutlu.apporganizer.utils.CrashReporter.clearCrashLogs(context)
                                    showCrashDialog = false
                                }) { Text("Temizle", color = MaterialTheme.colorScheme.error) }
                            }
                        )
                    }
                }
            }
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
