package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BackupManager
import com.armutlu.apporganizer.utils.DockPrefs
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {},
    onSendBugReport: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val showSystemApps  by viewModel.showSystemApps.collectAsState()
    val state           by viewModel.screenState.collectAsState()
    val logs            by viewModel.liveDebugLogs.collectAsState()
    val hiddenApps      by viewModel.hiddenApps.collectAsState()
    val otherApps       by viewModel.otherApps.collectAsState()
    val llmCategorizing by viewModel.llmCategorizing.collectAsState()
    val llmProgress     by viewModel.llmProgress.collectAsState()
    val clipboard       = LocalClipboardManager.current
    val context         = LocalContext.current
    var debugExpanded   by remember { mutableStateOf(false) }
    val scope           = rememberCoroutineScope()
    val themePrefs      = remember { ThemePreferences(context) }
    val currentTheme    by themePrefs.themeFlow.collectAsState(initial = AppTheme.TEAL)
    val currentFont     by themePrefs.fontFlow.collectAsState(initial = AppFont.DEFAULT)

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    var isDefault by remember { mutableStateOf(isDefaultLauncher()) }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefault = isDefaultLauncher() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ── Gorunum ───────────────────────────────────────────────────────
            item { SettingsSectionTitle("Görünüm") }
            item {
                SettingsAppearanceSection(
                    themePrefs = themePrefs,
                    currentTheme = currentTheme,
                    currentFont = currentFont,
                )
            }

            // ── Launcher ─────────────────────────────────────────────────────
            item { SettingsSectionTitle("Launcher") }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home, null,
                            tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Varsayılan Launcher", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                if (isDefault) "Aktif" else "Ayarlanmadı",
                                fontSize = 12.sp,
                                color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val launcherAction: () -> Unit = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val rm = context.getSystemService(RoleManager::class.java)
                                if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                                    roleRequestLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                                }
                            } else {
                                val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                runCatching { context.startActivity(i) }
                                Unit
                            }
                        }
                        if (isDefault) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                OutlinedButton(
                                    onClick = launcherAction,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Değiştir", fontSize = 12.sp) }
                            }
                        } else {
                            Button(
                                onClick = launcherAction,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) { Text("Ayarla", fontSize = 13.sp) }
                        }
                    }
                }
            }

            // ── Dock Yönetimi ─────────────────────────────────────────────────
            item { SettingsSectionTitle("Dock Uygulamaları") }
            item {
                var dockPkgs by remember { mutableStateOf(DockPrefs.getDockPackages(context)) }
                val pm = context.packageManager
                SettingsCard {
                    if (dockPkgs.isEmpty()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Dock boş", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        dockPkgs.forEachIndexed { index, pkg ->
                            val appName = remember(pkg) {
                                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                            }
                            if (index > 0) Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(appName, Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = {
                                    DockPrefs.removeFromDock(context, pkg)
                                    dockPkgs = DockPrefs.getDockPackages(context)
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Kaldır", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Varsayılana Sıfırla",
                        subtitle = "Telefon, Mesaj, Kamera, Tarayıcı",
                        onClick = {
                            DockPrefs.saveDockPackages(context, emptyList())
                            dockPkgs = DockPrefs.getDockPackages(context)
                        }
                    )
                }
            }

            // Sistem uygulama toggle — AppearanceSection'dan ayri cunku viewModel gerekiyor
            item { SettingsSectionTitle("Görünüm") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Visibility,
                        title = "Sistem Uygulamalarini Goster",
                        subtitle = "Dahili sistem uygulamalarini listele",
                        checked = showSystemApps,
                        onCheckedChange = { viewModel.toggleShowSystemApps() }
                    )
                }
            }

            // ── Ana Ekran / Widget / Ikon Paketi ──────────────────────────────
            item { SettingsHomeScreenSection() }

            item { SettingsSectionTitle("Uygulama Yönetimi") }
            item {
                SettingsCard {
                    var manufacturerClassify by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isManufacturerClassifyEnabled(context)) }
                    SettingsSwitchRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Üretici Sınıflandırması",
                        subtitle = "Samsung/Huawei/Xiaomi uygulamalarını otomatik kategorilendir",
                        checked = manufacturerClassify,
                        onCheckedChange = {
                            manufacturerClassify = it
                            com.armutlu.apporganizer.utils.AppPrefs.setManufacturerClassifyEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.AutoFixHigh,
                        title = "Sınıflandırılmamışları Sınıflandır",
                        subtitle = "Kategorisiz uygulamaları otomatik ata",
                        onClick = { viewModel.classifyUnclassifiedApps() }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Tüm Kategorileri Sıfırla",
                        subtitle = "Tüm atamaları sil ve yeniden sınıflandır",
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.resetAndReclassifyAllApps() }
                    )
                }
            }

            // â"€â"€ Hakkında â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            // ── Gizli Uygulamalar ─────────────────────────────────────────────
            if (hiddenApps.isNotEmpty()) {
                item { SettingsSectionTitle("Gizli Uygulamalar (${hiddenApps.size})") }
                item {
                    SettingsCard {
                        hiddenApps.forEachIndexed { index, app ->
                            if (index > 0) Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(app.appName, Modifier.weight(1f), fontSize = 14.sp)
                                OutlinedButton(
                                    onClick = { viewModel.unhideApp(app.packageName) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Göster", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }

            // ── Diğer Klasörü (Bilinmeyen Uygulamalar) ───────────────────────
            if (otherApps.isNotEmpty()) {
                item { SettingsSectionTitle("Diğer Klasörü — Bilinmeyenler (${otherApps.size})") }
                item {
                    SettingsCard {
                        // DeepSeek LLM kategorize paneli
                        var apiKeyInput by remember { mutableStateOf(AppPrefs.getDeepSeekApiKey(context)) }
                        var showApiKey  by remember { mutableStateOf(false) }
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                "Bu uygulamalar otomatik kategorilendirilemeyen uygulamalardır. " +
                                "DeepSeek AI ile otomatik olarak kategorilendirilebilir.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    AppPrefs.setDeepSeekApiKey(context, it)
                                },
                                label = { Text("DeepSeek API Key", fontSize = 12.sp) },
                                placeholder = { Text("sk-...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.categorizeDigerWithLLM(apiKeyInput) },
                                enabled = !llmCategorizing && apiKeyInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (llmCategorizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Kategorize ediliyor...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("DeepSeek ile Kategorize Et", fontSize = 13.sp)
                                }
                            }
                            if (llmProgress.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    llmProgress,
                                    fontSize = 12.sp,
                                    color = if (llmProgress.startsWith("Hata") || llmProgress.contains("hata"))
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                        otherApps.take(20).forEachIndexed { index, app ->
                            if (index > 0) Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.appName, fontSize = 14.sp, maxLines = 1)
                                    Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                        if (otherApps.size > 20) {
                            Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("...ve ${otherApps.size - 20} uygulama daha", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { SettingsSectionTitle("Hakkında") }
            item {
                SettingsCard {
                    SettingsInfoRow(
                        icon = Icons.Default.Apps,
                        title = "App Organizer",
                        subtitle = "v1.0 beta"
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Person,
                        title = "Geliştirici",
                        subtitle = "Hüseyin Ekizoğlu"
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Code,
                        title = "Kaynak Kod",
                        subtitle = "github.com/hekizoglu/android-folderautomanager"
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Storage,
                        title = "Veritabanı",
                        subtitle = "${state.apps.size} uygulama · ${state.categories.size} kategori"
                    )
                }
            }

            // Backup & Restore
            item { SettingsSectionTitle("Yedek / Geri Yukle") }
            item {
                var autoBackup by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAutoBackupEnabled(context)) }
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Otomatik Yedekleme", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Uygulama acildiginda otomatik JSON yedegi al", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoBackup, onCheckedChange = {
                            autoBackup = it
                            com.armutlu.apporganizer.utils.AppPrefs.setAutoBackupEnabled(context, it)
                        })
                    }
                }
            }
            item {
                var backupLoading by remember { mutableStateOf(false) }
                val filePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        backupLoading = true
                        runCatching {
                            val json = context.contentResolver.openInputStream(uri)
                                ?.bufferedReader()?.readText() ?: return@runCatching
                            val result = viewModel.importBackup(json)
                            android.widget.Toast.makeText(
                                context,
                                if (result.success) "${result.updatedCount} uygulama geri yuklendi"
                                else "Geri yukleme basarisiz: ${result.error}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        backupLoading = false
                    }
                }
                val shareLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {}

                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !backupLoading) {
                                scope.launch {
                                    backupLoading = true
                                    val intent = viewModel.exportBackup(context)
                                    if (intent != null) {
                                        shareLauncher.launch(Intent.createChooser(intent, "Yedegi paylas"))
                                    } else {
                                        android.widget.Toast.makeText(context, "Disa aktarma basarisiz", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    backupLoading = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Yedek Al", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Kategori atamalarini JSON olarak disa aktar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (backupLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !backupLoading) { filePickerLauncher.launch("application/json") }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Geri Yukle", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("JSON yedek dosyasindan kategorileri ice aktar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Hakkinda ─────────────────────────────────────────────────────
            item { SettingsSectionTitle("Hakkında") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Gizlilik Politikasi",
                        subtitle = "Veri toplama ve kullanim hakkinizda bilgi alin",
                        onClick = onNavigateToPrivacyPolicy
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = "Versiyon",
                        subtitle = "AppOrganizer 1.0.0 — Haziran 2026"
                    )
                }
            }

            // ── Debug ────────────────────────────────────────────────────────
            if (logs.isNotEmpty()) {
                item { SettingsSectionTitle("Debug") }
                item {
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { debugExpanded = !debugExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BugReport,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Loglar", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text("${logs.size} satır", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                if (debugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (debugExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                // Son 30 log
                                logs.takeLast(30).forEach { line ->
                                    Text(
                                        line,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(viewModel.getDebugLogs()))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Kopyala", fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.clearDebugLogs() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
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
    }
}



