package com.armutlu.apporganizer.presentation.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.DockPrefs

/**
 * U1: Launcher alt ekranı — varsayılan launcher, dock yönetimi, gesture
 * aksiyonları, widget önerileri, ana ekran özellikleri ve hızlı erişim.
 * İçerik eski SettingsScreen'den birebir taşındı, fonksiyonellik değişmedi.
 */
@Composable
fun SettingsLauncherScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSearchSettings: () -> Unit = {},
) {
    val context = LocalContext.current

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    var isDefault by remember(context.packageName) { mutableStateOf(isDefaultLauncher()) }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefault = isDefaultLauncher() }

    SettingsSubScreenScaffold(title = "Launcher", onNavigateBack = onNavigateBack) {

        // ── Varsayılan Launcher ───────────────────────────────────────────
        item { SettingsSectionTitle("Varsayılan Launcher") }
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
                            if (isDefault) "Aktif" else "Henüz seçilmedi",
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
                        ) { Text("Seç", fontSize = 13.sp) }
                    }
                }
            }
        }

        // ── Dock Yönetimi ─────────────────────────────────────────────────
        item { SettingsSectionTitle("Dock Uygulamaları") }
        item {
            var contextualDock by remember { mutableStateOf(AppPrefs.isContextualDockEnabled(context)) }
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Akilli Dock",
                    subtitle = "Ilk 2 sabit uygulamayi korur, son 2 slotu saate ve kullanim aliskanligina gore onerir",
                    checked = contextualDock,
                    onCheckedChange = {
                        contextualDock = it
                        AppPrefs.setContextualDockEnabled(context, it)
                    }
                )
            }
        }
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

        // ── Gesture Aksiyonları ───────────────────────────────────────────
        item { SettingsGestureSection() }

        // ── Widget Önerileri ──────────────────────────────────────────────
        item { WidgetSuggestionSection(viewModel = viewModel) }

        // ── Ana Ekran / Widget / İkon Paketi ──────────────────────────────
        item { SettingsHomeScreenSection(onNavigateToSearchSettings = onNavigateToSearchSettings) }

        item { SettingsSectionTitle("Klasor Gecisleri") }
        item {
            var folderCarousel by remember { mutableStateOf(AppPrefs.isFolderCarouselEnabled(context)) }
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.Folder,
                    title = "Fihrist Klasor Gecisi",
                    subtitle = "Klasor acikken sag/sol kaydirarak onceki ve sonraki klasore animasyonlu gec",
                    checked = folderCarousel,
                    onCheckedChange = {
                        folderCarousel = it
                        AppPrefs.setFolderCarouselEnabled(context, it)
                    }
                )
            }
        }

        // ── Quick Wheel + Focus Mode ──────────────────────────────────────
        // Ana ekran davranış ayarları — mantıksal gruplama (D199)
        item { SettingsSectionTitle("Hızlı Erişim") }
        item {
            var quickWheel by remember { mutableStateOf(AppPrefs.isQuickWheelEnabled(context)) }
            var focusMode by remember { mutableStateOf(AppPrefs.isFocusModeEnabled(context)) }
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.Widgets,
                    title = "Quick Wheel (Radyal Çark)",
                    subtitle = "Boş alana uzun basınca sık kullanılanlar çember halinde açılır",
                    checked = quickWheel,
                    onCheckedChange = {
                        quickWheel = it
                        AppPrefs.setQuickWheelEnabled(context, it)
                    }
                )
                HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f))
                SettingsSwitchRow(
                    icon = Icons.Default.DoNotDisturb,
                    title = "Search-first / Odak Modu",
                    subtitle = "Daha sade bir ana ekran görünümü sağlar",
                    checked = focusMode,
                    onCheckedChange = {
                        focusMode = it
                        AppPrefs.setFocusModeEnabled(context, it)
                    }
                )
            }
        }
    }
}
