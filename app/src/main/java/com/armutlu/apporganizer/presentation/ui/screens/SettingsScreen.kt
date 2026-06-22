package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.armutlu.apporganizer.R
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val showSystemApps  by viewModel.showSystemApps.collectAsState()
    val state           by viewModel.screenState.collectAsState()
    val logs            by viewModel.liveDebugLogs.collectAsState()
    val hiddenApps      by viewModel.hiddenApps.collectAsState()
    val otherApps       by viewModel.otherApps.collectAsState()
    val llmCategorizing  by viewModel.llmCategorizing.collectAsState()
    val llmProgress      by viewModel.llmProgress.collectAsState()
    val classifyLoading  by viewModel.classifyLoading.collectAsState()
    val classifyResult   by viewModel.classifyResult.collectAsState()
    val clipboard        = LocalClipboardManager.current
    val context          = LocalContext.current

    // classifyResult değişince Toast göster
    LaunchedEffect(classifyResult) {
        if (classifyResult.isNotBlank()) {
            android.widget.Toast.makeText(context, classifyResult, android.widget.Toast.LENGTH_LONG).show()
        }
    }
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
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                            Text(stringResource(R.string.settings_default_launcher), fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
                                ) { Text(stringResource(R.string.settings_launcher_change), fontSize = 12.sp) }
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
                            Text(stringResource(R.string.settings_dock_empty), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        dockPkgs.forEachIndexed { index, pkg ->
                            val appName = remember(pkg) {
                                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                            }
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
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
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = stringResource(R.string.settings_reset_defaults),
                        subtitle = stringResource(R.string.settings_dock_default_apps),
                        onClick = {
                            DockPrefs.saveDockPackages(context, emptyList())
                            dockPkgs = DockPrefs.getDockPackages(context)
                        }
                    )
                }
            }

            // ── Ana Ekran / Widget / Ikon Paketi ──────────────────────────────
            item { SettingsHomeScreenSection() }

            // ── Uygulama Görünümü ─────────────────────────────────────────────
            item { SettingsSectionTitle("Uygulama Listesi") }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Visibility,
                        title = stringResource(R.string.settings_show_system_apps),
                        subtitle = stringResource(R.string.settings_show_system_apps_desc),
                        checked = showSystemApps,
                        onCheckedChange = { viewModel.toggleShowSystemApps() }
                    )
                }
            }

            item { SettingsSectionTitle("Uygulama Yönetimi") }
            item {
                var manufacturerClassify by remember { mutableStateOf(AppPrefs.isManufacturerClassifyEnabled(context)) }
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.settings_classify_by_vendor),
                        subtitle = stringResource(R.string.settings_classify_by_vendor_desc),
                        checked = manufacturerClassify,
                        onCheckedChange = {
                            manufacturerClassify = it
                            AppPrefs.setManufacturerClassifyEnabled(context, it)
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !classifyLoading) { viewModel.classifyUnclassifiedApps() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (classifyLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(Icons.Default.AutoFixHigh, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_classify_uncategorized),
                                fontWeight = FontWeight.Medium, fontSize = 15.sp,
                                color = if (classifyLoading) MaterialTheme.colorScheme.onSurface.copy(0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (classifyLoading) "Sınıflandırılıyor..." else stringResource(R.string.settings_classify_uncategorized_desc),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = stringResource(R.string.settings_reset_categories),
                        subtitle = stringResource(R.string.settings_reset_categories_desc),
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
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
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
                                ) { Text(stringResource(R.string.settings_show), fontSize = 12.sp) }
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
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                        otherApps.take(20).forEachIndexed { index, app ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Help, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.appName, fontSize = 14.sp, maxLines = 1)
                                    Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                        if (otherApps.size > 20) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
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
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.settings_developer),
                        subtitle = stringResource(R.string.settings_developer_name)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Code,
                        title = "Kaynak Kod",
                        subtitle = "github.com/hekizoglu/android-folderautomanager"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Storage,
                        title = stringResource(R.string.settings_database),
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
                            if (it) com.armutlu.apporganizer.workers.BackupWorker.schedule(context)
                            else com.armutlu.apporganizer.workers.BackupWorker.cancel(context)
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
                                        shareLauncher.launch(Intent.createChooser(intent, context.getString(R.string.settings_share_backup)))
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.settings_export_failed), android.widget.Toast.LENGTH_SHORT).show()
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
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = "Versiyon",
                        subtitle = "AppOrganizer 1.0.0 — Haziran 2026"
                    )
                }
            }

            // ── Geri Bildirim ────────────────────────────────────────────────
            item { SettingsSectionTitle("Geri Bildirim") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Feedback,
                        title = stringResource(R.string.settings_feedback),
                        subtitle = stringResource(R.string.settings_feedback_desc),
                        onClick = {
                            val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:")
                                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("huseyinekizoglu@gmail.com"))
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "AppOrganizer - Talep / Öneri")
                                putExtra(android.content.Intent.EXTRA_TEXT, "\n\n---\nCihaz: $device")
                            }
                            runCatching { context.startActivity(intent) }
                        }
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



