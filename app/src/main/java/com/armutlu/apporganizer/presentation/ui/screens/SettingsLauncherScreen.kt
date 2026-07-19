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
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.presentation.ui.common.rememberStringPreferenceState
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
    onNavigateToSmartTickerSettings: () -> Unit = {},
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
            var contextualDock by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_CONTEXTUAL_DOCK,
                read = { AppPrefs.isContextualDockEnabled(context) }
            )
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Akilli Dock",
                    subtitle = "Sectigin uygulamalar aynen korunur; bos kalan dock slotlarini saate ve kullanim aliskanligina gore doldurur",
                    checked = contextualDock,
                    onCheckedChange = {
                        contextualDock = it
                        AppPrefs.setContextualDockEnabled(context, it)
                    }
                )
            }
        }
        item {
            var folderPageInsights by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_FOLDER_PAGE_INSIGHTS_ENABLED,
                read = { AppPrefs.isFolderPageInsightsEnabled(context) }
            )
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Klasor sayfasi onerileri",
                    subtitle = "Sayfa ozetinde klasor birlestirme ve uygulama tanimlama onerilerini goster",
                    checked = folderPageInsights,
                    onCheckedChange = {
                        folderPageInsights = it
                        AppPrefs.setFolderPageInsightsEnabled(context, it)
                        if (it) AppPrefs.muteFolderPageInsights(context, 0L)
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
                        val folderId = DockPrefs.folderId(pkg)
                        val label = remember(pkg) {
                            if (folderId != null) {
                                val category = Category.getDefaultCategories().firstOrNull { it.categoryId == folderId }
                                val defaultName = category?.categoryName ?: folderId
                                val customName = AppPrefs.getFolderCustomNames(context)[folderId]
                                val customEmoji = AppPrefs.getFolderCustomEmojis(context)[folderId]
                                listOfNotNull(customEmoji?.takeIf { it.isNotBlank() }, customName ?: defaultName)
                                    .joinToString(" ")
                            } else {
                                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                            }
                        }
                        if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (folderId != null) Icons.Default.Folder else Icons.Default.Apps,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, Modifier.weight(1f), fontSize = 14.sp)
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
        item {
            SettingsHomeScreenSection(
                onNavigateToSearchSettings = onNavigateToSearchSettings,
                onNavigateToSmartTickerSettings = onNavigateToSmartTickerSettings,
            )
        }

        item { SettingsSectionTitle("Klasor Gecisleri") }
        item {
            var folderCarousel by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_FOLDER_CAROUSEL_ENABLED,
                read = { AppPrefs.isFolderCarouselEnabled(context) }
            )
            var folderCarouselPosition by rememberStringPreferenceState(
                context = context,
                key = AppPrefs.KEY_FOLDER_CAROUSEL_POSITION,
                read = { AppPrefs.getFolderCarouselPosition(context) }
            )
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
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Fihrist konumu", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FolderCarouselPositionChip(
                            label = "Ust",
                            selected = folderCarouselPosition == AppPrefs.FOLDER_CAROUSEL_POS_TOP,
                            enabled = folderCarousel,
                            onClick = {
                                folderCarouselPosition = AppPrefs.FOLDER_CAROUSEL_POS_TOP
                                AppPrefs.setFolderCarouselPosition(context, folderCarouselPosition)
                            },
                        )
                        FolderCarouselPositionChip(
                            label = "Orta",
                            selected = folderCarouselPosition == AppPrefs.FOLDER_CAROUSEL_POS_MIDDLE,
                            enabled = folderCarousel,
                            onClick = {
                                folderCarouselPosition = AppPrefs.FOLDER_CAROUSEL_POS_MIDDLE
                                AppPrefs.setFolderCarouselPosition(context, folderCarouselPosition)
                            },
                        )
                        FolderCarouselPositionChip(
                            label = "Alt",
                            selected = folderCarouselPosition == AppPrefs.FOLDER_CAROUSEL_POS_BOTTOM,
                            enabled = folderCarousel,
                            onClick = {
                                folderCarouselPosition = AppPrefs.FOLDER_CAROUSEL_POS_BOTTOM
                                AppPrefs.setFolderCarouselPosition(context, folderCarouselPosition)
                            },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Varsayilan: Alt. Ust baslikta, orta ekran kenarlarinda, alt altyazi gibi gosterir.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )
                var folderTransitionEffect by rememberStringPreferenceState(
                    context = context,
                    key = AppPrefs.KEY_FOLDER_TRANSITION_EFFECT,
                    read = { AppPrefs.getFolderTransitionEffect(context) }
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.settings_folder_transition_effect_title),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_folder_transition_effect_desc),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FolderCarouselPositionChip(
                            label = stringResource(R.string.settings_folder_transition_android_smooth),
                            selected = folderTransitionEffect == AppPrefs.FOLDER_TRANSITION_ANDROID_SMOOTH,
                            enabled = folderCarousel,
                            onClick = {
                                folderTransitionEffect = AppPrefs.FOLDER_TRANSITION_ANDROID_SMOOTH
                                AppPrefs.setFolderTransitionEffect(context, folderTransitionEffect)
                            },
                        )
                        FolderCarouselPositionChip(
                            label = stringResource(R.string.settings_folder_transition_ios_zoom_fade),
                            selected = folderTransitionEffect == AppPrefs.FOLDER_TRANSITION_IOS_ZOOM_FADE,
                            enabled = folderCarousel,
                            onClick = {
                                folderTransitionEffect = AppPrefs.FOLDER_TRANSITION_IOS_ZOOM_FADE
                                AppPrefs.setFolderTransitionEffect(context, folderTransitionEffect)
                            },
                        )
                    }
                }
            }
        }

        // ── Quick Wheel + Focus Mode ──────────────────────────────────────
        // Ana ekran davranış ayarları — mantıksal gruplama (D199)
        item { SettingsSectionTitle("Hızlı Erişim") }
        item {
            var quickWheel by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_QUICK_WHEEL,
                read = { AppPrefs.isQuickWheelEnabled(context) }
            )
            var focusMode by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_FOCUS_MODE,
                read = { AppPrefs.isFocusModeEnabled(context) }
            )
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
                // P18 — Odak Modu artık klasörleri/aramayı gizleyen ayrı bir ekran değil, ana
                // ekranın sade bir görünümüdür: saat küçülür, görev/skor ve öneri/haber şeridi
                // kartları kapanır; klasör sayfaları ve arama her zaman açık kalır.
                SettingsSwitchRow(
                    icon = Icons.Default.DoNotDisturb,
                    title = "Search-first / Odak Modu",
                    subtitle = "Saat küçülür, görev/öneri kartları kapanır — klasörler ve arama açık kalır",
                    checked = focusMode,
                    onCheckedChange = {
                        focusMode = it
                        AppPrefs.setFocusModeEnabled(context, it)
                        // Dongu G3a — DAILY_FOCUS_SESSION gorevi icin basit sure olcumu:
                        // acilista baslangic zamani kaydedilir, kapanista gecen sure gunun
                        // toplamina eklenir (bkz. AppPrefs.startFocusSession/endFocusSession).
                        if (it) AppPrefs.startFocusSession(context) else AppPrefs.endFocusSession(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderCarouselPositionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
