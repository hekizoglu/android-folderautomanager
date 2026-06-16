package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import android.os.Build
import android.app.role.RoleManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.DockPrefs
import com.armutlu.apporganizer.utils.IconPackManager

/**
 * Launcher + Dock + Ana Ekran Özellikleri + Widget + İkon Paketi bölümleri
 */
internal fun LazyListScope.settingsHomeSection(
    isDefault: Boolean,
    onRequestRole: () -> Unit
) {
    // ── Launcher ────────────────────────────────────────────────────────
    item { SettingsSectionTitle("Launcher") }
    item {
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Home, null,
                    tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Varsayılan Launcher", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(if (isDefault) "Aktif" else "Ayarlanmadı", fontSize = 12.sp,
                        color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isDefault) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        OutlinedButton(onClick = onRequestRole, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Değiştir", fontSize = 12.sp)
                        }
                    }
                } else {
                    Button(onClick = onRequestRole,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                        Text("Ayarla", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ── Dock Uygulamaları ───────────────────────────────────────────────
    item { SettingsSectionTitle("Dock Uygulamaları") }
    item {
        val context = LocalContext.current
        val pm = context.packageManager
        var dockPkgs by remember { mutableStateOf(DockPrefs.getDockPackages(context)) }
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
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
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
            SettingsButtonRow(icon = Icons.Default.RestartAlt,
                title = "Varsayılana Sıfırla",
                subtitle = "Telefon, Mesaj, Kamera, Tarayıcı",
                onClick = {
                    DockPrefs.saveDockPackages(context, emptyList())
                    dockPkgs = DockPrefs.getDockPackages(context)
                })
        }
    }

    // ── Ana Ekran Özellikleri ───────────────────────────────────────────
    item { SettingsSectionTitle("Ana Ekran Özellikleri") }
    item {
        val context = LocalContext.current
        var favoritesEnabled    by remember { mutableStateOf(AppPrefs.isFavoritesEnabled(context)) }
        var suggestionsEnabled  by remember { mutableStateOf(AppPrefs.isSuggestionsEnabled(context)) }
        var recentAppsEnabled   by remember { mutableStateOf(AppPrefs.isRecentAppsEnabled(context)) }
        var swipeHintEnabled    by remember { mutableStateOf(AppPrefs.isSwipeHintEnabled(context)) }
        var newBadgeEnabled     by remember { mutableStateOf(AppPrefs.isNewBadgeEnabled(context)) }
        var folderCountVisible  by remember { mutableStateOf(AppPrefs.isFolderCountVisible(context)) }
        var folderSwipeHint     by remember { mutableStateOf(AppPrefs.isFolderSwipeHintEnabled(context)) }
        var notifTextEnabled    by remember { mutableStateOf(AppPrefs.isNotificationTextEnabled(context)) }
        var hideNavButtons      by remember { mutableStateOf(AppPrefs.isNavButtonsHidden(context)) }
        var allAppsBgAlpha      by remember { mutableStateOf(AppPrefs.getAllAppsBgAlpha(context)) }
        SettingsCard {
            SettingsSwitchRow(Icons.Default.Star, "Favoriler",
                "Uzun basınca favoriye eklenen uygulamalar ana ekranda gösterilir",
                favoritesEnabled) { favoritesEnabled = it; AppPrefs.setFavoritesEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.AutoAwesome, "Uygulama Önerileri",
                "Arama çubuğunun altında son kullanılan 4 uygulama",
                suggestionsEnabled) { suggestionsEnabled = it; AppPrefs.setSuggestionsEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.History, "Son Kullanılanlar",
                "Ana ekranda + tüm uygulamalarda son 4 uygulamayı göster",
                recentAppsEnabled) { recentAppsEnabled = it; AppPrefs.setRecentAppsEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.SwipeUp, "Swipe-up İpucu",
                "Ana ekranda yukarı kaydırma animasyonu göster",
                swipeHintEnabled) { swipeHintEnabled = it; AppPrefs.setSwipeHintEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.NewReleases, "YENİ Badge",
                "7 gün içinde kurulan uygulamalara rozet göster",
                newBadgeEnabled) { newBadgeEnabled = it; AppPrefs.setNewBadgeEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.FormatListNumbered, "Klasör Uygulama Sayısı",
                "Klasör simgesinin altında uygulama adedini göster",
                folderCountVisible) { folderCountVisible = it; AppPrefs.setFolderCountVisible(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.Folder, "Klasör Swipe İpucu",
                "Klasörde en çok kullanılan uygulamayı göster",
                folderSwipeHint) { folderSwipeHint = it; AppPrefs.setFolderSwipeHintEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.Notifications, "Bildirim Metni",
                "Klasör ve uygulamaların altında son bildirimi göster",
                notifTextEnabled) { notifTextEnabled = it; AppPrefs.setNotificationTextEnabled(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(Icons.Default.HideSource, "Sistem Navigasyonunu Gizle",
                "Tam ekran launcher — geri/home/recents butonsuz",
                hideNavButtons) { hideNavButtons = it; AppPrefs.setNavButtonsHidden(context, it) }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Opacity, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tüm Uygulamalar Arka Plan", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Opaklık: ${(allAppsBgAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Slider(value = allAppsBgAlpha,
                    onValueChange = { allAppsBgAlpha = it; AppPrefs.setAllAppsBgAlpha(context, it) },
                    valueRange = 0.1f..1.0f, steps = 8)
            }
        }
    }

    // ── Widget Alanı ────────────────────────────────────────────────────
    item { SettingsSectionTitle("Widget") }
    item {
        val context = LocalContext.current
        var widgetAreaEnabled by remember { mutableStateOf(AppPrefs.isWidgetAreaEnabled(context)) }
        SettingsCard {
            SettingsSwitchRow(Icons.Default.Widgets, "Widget Alanı",
                "Ana ekranda widget gösterimine izin ver",
                widgetAreaEnabled) { widgetAreaEnabled = it; AppPrefs.setWidgetAreaEnabled(context, it) }
        }
    }

    // ── İkon Paketi ─────────────────────────────────────────────────────
    item { SettingsSectionTitle("İkon Paketi") }
    item {
        val context = LocalContext.current
        val iconPacks = remember { IconPackManager.getInstalledIconPacks(context) }
        var selectedPack by remember { mutableStateOf(AppPrefs.getIconPack(context)) }
        SettingsCard {
            Row(modifier = Modifier.fillMaxWidth()
                    .clickable { selectedPack = ""; AppPrefs.setIconPack(context, "") }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sistem İkonları", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("Varsayılan uygulama ikonları", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selectedPack.isEmpty()) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            if (iconPacks.isEmpty()) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Kurulu ikon paketi bulunamadı. Play Store'dan bir ikon paketi yükleyin.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                iconPacks.forEach { pack ->
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    Row(modifier = Modifier.fillMaxWidth()
                            .clickable { selectedPack = pack.packageName; AppPrefs.setIconPack(context, pack.packageName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(pack.label, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        if (selectedPack == pack.packageName) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
