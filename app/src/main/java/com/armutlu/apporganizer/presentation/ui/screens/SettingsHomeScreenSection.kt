package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/**
 * Ana Ekran Ozellikleri, Widget ve Ikon Paketi bolumu.
 * SettingsScreen LazyColumn icinde item{} bloklariyla cagirilir.
 */
@Composable
fun SettingsHomeScreenSection() {
    val context = LocalContext.current

    // ── Ana Ekran Ozellikleri ─────────────────────────────────────────────
    SettingsSectionTitle("Ana Ekran Ozellikleri")
    var swipeHintEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)) }
    var newBadgeEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)) }
    var folderCountVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)) }
    var folderSwipeHint    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)) }
    var notifTextEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    var hideNavButtons     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNavButtonsHidden(context)) }
    var allAppsBgAlpha     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)) }
    var suggestionsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
    var recentAppsEnabled  by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabled(context)) }
    var favoritesEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabled(context)) }

    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = "Favoriler",
            subtitle = "Uzun basinca favoriye eklenen uygulamalar ana ekranda gosterilir (varsayilan: kapali)",
            checked = favoritesEnabled,
            onCheckedChange = {
                favoritesEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setFavoritesEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.AutoAwesome,
            title = "Uygulama Onerileri",
            subtitle = "Arama cubugununun altinda son kullanilan 4 uygulama",
            checked = suggestionsEnabled,
            onCheckedChange = {
                suggestionsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setSuggestionsEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.History,
            title = "Son Kullanilanlar",
            subtitle = "Ana ekranda son 8 uygulamayi buyuk ikonlarla goster (varsayilan: kapali)",
            checked = recentAppsEnabled,
            onCheckedChange = {
                recentAppsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setRecentAppsEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.SwipeUp,
            title = "Swipe-up Ipucu",
            subtitle = "Ana ekranda yukari kaydirma animasyonu goster",
            checked = swipeHintEnabled,
            onCheckedChange = {
                swipeHintEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setSwipeHintEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.NewReleases,
            title = "YENI Badge",
            subtitle = "7 gun icinde kurulan uygulamalara rozet goster",
            checked = newBadgeEnabled,
            onCheckedChange = {
                newBadgeEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setNewBadgeEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.FormatListNumbered,
            title = "Klasor Uygulama Sayisi",
            subtitle = "Klasor simgesinin altinda uygulama adedini goster",
            checked = folderCountVisible,
            onCheckedChange = {
                folderCountVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderCountVisible(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Folder,
            title = "Klasor Swipe Ipucu",
            subtitle = "Klasorde en cok kullanilan uygulamayi goster",
            checked = folderSwipeHint,
            onCheckedChange = {
                folderSwipeHint = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderSwipeHintEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Notifications,
            title = "Bildirim Metni",
            subtitle = "Klasor ve uygulamalarin altinda son bildirimi goster",
            checked = notifTextEnabled,
            onCheckedChange = {
                notifTextEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setNotificationTextEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.HideSource,
            title = "Sistem Navigasyonunu Gizle",
            subtitle = "Tam ekran launcher - geri/home/recents butonsuz",
            checked = hideNavButtons,
            onCheckedChange = {
                hideNavButtons = it
                com.armutlu.apporganizer.utils.AppPrefs.setNavButtonsHidden(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Opacity, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tum Uygulamalar Arka Plan", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text("Opaklık: ${(allAppsBgAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = allAppsBgAlpha,
                onValueChange = {
                    allAppsBgAlpha = it
                    com.armutlu.apporganizer.utils.AppPrefs.setAllAppsBgAlpha(context, it)
                },
                valueRange = 0.1f..1.0f,
                steps = 8
            )
        }
    }

    // ── Widget Alani ──────────────────────────────────────────────────────
    SettingsSectionTitle("Widget")
    var widgetAreaEnabledLocal by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)) }
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.Widgets,
            title = "Widget Alani",
            subtitle = "Ana ekranda widget gosterimine izin ver",
            checked = widgetAreaEnabledLocal,
            onCheckedChange = {
                widgetAreaEnabledLocal = it
                com.armutlu.apporganizer.utils.AppPrefs.setWidgetAreaEnabled(context, it)
            }
        )
    }

    // ── Ikon Paketi ───────────────────────────────────────────────────────
    SettingsSectionTitle("Ikon Paketi")
    val iconPacks = remember { com.armutlu.apporganizer.utils.IconPackManager.getInstalledIconPacks(context) }
    var selectedPack by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context)) }
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectedPack = ""
                    com.armutlu.apporganizer.utils.AppPrefs.setIconPack(context, "")
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sistem Ikonlari", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text("Varsayilan uygulama ikonlari", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selectedPack.isEmpty()) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        if (iconPacks.isEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Kurulu ikon paketi bulunamadi. Play Store'dan bir ikon paketi yukleyin.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            iconPacks.forEach { pack ->
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPack = pack.packageName
                            com.armutlu.apporganizer.utils.AppPrefs.setIconPack(context, pack.packageName)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(pack.label, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    if (selectedPack == pack.packageName) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
