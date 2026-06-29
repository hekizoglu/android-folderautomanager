package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import androidx.fragment.app.FragmentActivity
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BiometricHelper
import com.armutlu.apporganizer.utils.DockPrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToUsageReport: () -> Unit = {}
) {
    val showSystemApps  by viewModel.showSystemApps.collectAsState()
    val state           by viewModel.screenState.collectAsState()
    val logs            by viewModel.liveDebugLogs.collectAsState()
    val hiddenApps      by viewModel.hiddenApps.collectAsState()
    val otherApps       by viewModel.otherApps.collectAsState()
    val llmCategorizing  by viewModel.llmCategorizing.collectAsState()
    val llmProgress      by viewModel.llmProgress.collectAsState()
    val classifyResult   by viewModel.classifyResult.collectAsState()
    val context          = LocalContext.current

    // Biometric Settings Lock — açılışta kilidi doğrula
    var biometricUnlocked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val lockEnabled = AppPrefs.isBiometricSettingsLockEnabled(context)
        if (!lockEnabled) {
            biometricUnlocked = true
            return@LaunchedEffect
        }
        val activity = context as? FragmentActivity
        if (activity == null || !BiometricHelper.isAvailable(activity)) {
            biometricUnlocked = true
            return@LaunchedEffect
        }
        BiometricHelper.authenticate(
            activity = activity,
            onSuccess = { biometricUnlocked = true },
            onFailure = { onNavigateBack() }
        )
    }
    if (!biometricUnlocked) return

    // classifyResult değişince Toast göster
    LaunchedEffect(classifyResult) {
        if (classifyResult.isNotBlank()) {
            android.widget.Toast.makeText(context, classifyResult, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val themePrefs      = remember { ThemePreferences(context) }
    val currentTheme    by themePrefs.themeFlow.collectAsState(initial = AppTheme.TEAL)
    val currentFont     by themePrefs.fontFlow.collectAsState(initial = AppFont.DEFAULT)

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    var isDefault by remember(context.packageName) { mutableStateOf(isDefaultLauncher()) }

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

            // ── Bildirim İzni ─────────────────────────────────────────────────
            item { SettingsSectionTitle("Bildirim") }
            item {
                val notifListenerOk = remember {
                    val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
                    flat.contains(context.packageName)
                }
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications, null,
                            tint = if (notifListenerOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Bildirim Erişimi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                if (notifListenerOk) "Aktif — badge sayıları çalışıyor" else "Kapalı — badge'ler görünmez",
                                fontSize = 12.sp,
                                color = if (notifListenerOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!notifListenerOk) {
                            Button(
                                onClick = {
                                    val i = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    runCatching { context.startActivity(i) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) { Text("Bildirim Erişimini Aç", fontSize = 13.sp) }
                        }
                    }
                    if (!notifListenerOk) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text(
                                "Tüm bildirim verileri yalnızca cihazınızda kalır — hiçbir veri dışarı çıkmaz.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Badge Intelligence toggle
            item {
                var badgeIntelligence by remember { mutableStateOf(AppPrefs.isBadgeIntelligenceEnabled(context)) }
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Akıllı Badge Rengi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Mesaj=yeşil · Alarm=kırmızı · Güncelleme=sarı", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = badgeIntelligence,
                            onCheckedChange = {
                                badgeIntelligence = it
                                AppPrefs.setBadgeIntelligenceEnabled(context, it)
                            }
                        )
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
                                    val removed = DockPrefs.removeFromDock(context, pkg)
                                    dockPkgs = DockPrefs.getDockPackages(context)
                                    val message = if (removed) "Dock uygulamasi kaldirildi" else "Dock uygulamasi kaldirilamadi"
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Kaldır", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    var showDockResetDialog by remember { mutableStateOf(false) }
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = stringResource(R.string.settings_reset_defaults),
                        subtitle = stringResource(R.string.settings_dock_default_apps),
                        showChevron = false,
                        onClick = { showDockResetDialog = true }
                    )
                    if (showDockResetDialog) {
                        AlertDialog(
                            onDismissRequest = { showDockResetDialog = false },
                            title = { Text("Dock uygulamalarını sıfırla") },
                            text = { Text("Dock'daki tüm uygulamalar kaldırılacak. Devam etmek istiyor musunuz?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDockResetDialog = false
                                    DockPrefs.saveDockPackages(context, emptyList())
                                    dockPkgs = DockPrefs.getDockPackages(context)
                                }) { Text("Sıfırla", color = MaterialTheme.colorScheme.error) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDockResetDialog = false }) { Text("İptal") }
                            }
                        )
                    }
                }
            }

            // ── Gesture Aksiyonları ────────────────────────────────────────────
            item { SettingsGestureSection() }

            // ── Widget Önerileri ──────────────────────────────────────────────
            item { WidgetSuggestionSection(viewModel = viewModel) }

            // ── Ana Ekran / Widget / Ikon Paketi ──────────────────────────────
            item { SettingsHomeScreenSection() }

            // Uygulama Listesi / Yönetimi / Gizli / Diğer → SettingsAppsSection.kt
            settingsAppsSection(
                showSystemApps = showSystemApps,
                viewModel = viewModel,
                hiddenApps = hiddenApps,
                otherApps = otherApps,
                llmCategorizing = llmCategorizing,
                llmProgress = llmProgress
            )

            // ── İstatistikler ─────────────────────────────────────────────────
            item { SettingsSectionTitle("İstatistikler") }
            item {
                val lastBackupMs = AppPrefs.getLastBackupTime(context)
                val lastBackupText = if (lastBackupMs == 0L) "Henüz yedeklenmedi"
                else {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("tr"))
                    sdf.format(java.util.Date(lastBackupMs))
                }
                val topCategory by remember(state.categoryStats, state.categories) {
                    derivedStateOf {
                        state.categoryStats
                            .maxByOrNull { it.value }
                            ?.let { (id, count) -> state.categories.find { it.categoryId == id }?.categoryName?.let { "$it ($count)" } }
                            ?: "—"
                    }
                }
                SettingsCard {
                    SettingsInfoRow(icon = Icons.Default.Apps, title = "Toplam Uygulama", subtitle = "${state.totalAppsCount}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.Folder, title = "Kategori Sayısı", subtitle = "${state.categories.size}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.HelpOutline, title = "Sınıflandırılmamış", subtitle = "${otherApps.size} uygulama")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.VisibilityOff, title = "Gizli Uygulama", subtitle = "${hiddenApps.size}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.BarChart, title = "En Çok Dolu Kategori", subtitle = topCategory)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.Backup, title = "Son Yedekleme", subtitle = lastBackupText)
                }
            }

            // Hakkında (üst) + Yedek/Geri Yükle + Hakkında (gizlilik) + Debug → SettingsBackupAboutSection.kt
            settingsBackupAboutSection(
                viewModel = viewModel,
                appCount = state.apps.size,
                categoryCount = state.categories.size,
                logs = logs,
                onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                onNavigateToUsageReport = onNavigateToUsageReport
            )


            // ── Geri Bildirim ────────────────────────────────────────────────
            // ── Güvenlik ──────────────────────────────────────────────────────
            item { SettingsSectionTitle("Güvenlik") }
            item {
                var biometricLock by remember { mutableStateOf(AppPrefs.isBiometricSettingsLockEnabled(context)) }
                val activity = context as? FragmentActivity
                val biometricAvailable = remember(context) {
                    activity != null && BiometricHelper.isAvailable(activity)
                }
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ayarlar Kilidi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                if (biometricAvailable) "Ayarlar açılışında parmak izi / yüz doğrulama" else "Cihazda biyometrik doğrulama bulunamadı",
                                fontSize = 12.sp,
                                color = if (biometricAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = biometricLock,
                            enabled = biometricAvailable,
                            onCheckedChange = {
                                biometricLock = it
                                AppPrefs.setBiometricSettingsLockEnabled(context, it)
                            }
                        )
                    }
                }
            }

            item { SettingsSectionTitle("Geri Bildirim") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Feedback,
                        title = stringResource(R.string.settings_feedback),
                        subtitle = stringResource(R.string.settings_feedback_desc),
                        showChevron = false,
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

        }
    }
}



