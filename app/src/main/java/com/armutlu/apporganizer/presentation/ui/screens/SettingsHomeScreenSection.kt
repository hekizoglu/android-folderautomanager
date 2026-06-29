package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Ana Ekran Özellikleri, Widget ve İkon Paketi bölümü.
 * SettingsScreen LazyColumn içinde item{} bloklarıyla çağrılır.
 */
@Composable
fun SettingsHomeScreenSection(
    onNavigateToSearchSettings: () -> Unit = {},
) {
    val context = LocalContext.current

    // ── Ana Ekran Özellikleri ─────────────────────────────────────────────
    SettingsSectionTitle("Ana Ekran Özellikleri")
    var swipeHintEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)) }
    var newBadgeEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)) }
    var folderCountVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)) }
    var folderSwipeHint    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)) }
    var notifTextEnabled   by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
    var hideNavButtons     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNavButtonsHidden(context)) }
    var allAppsBgAlpha     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)) }
    var suggestionsEnabled       by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
    var recentAppsEnabled        by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabled(context)) }
    var favoritesEnabled         by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabled(context)) }
    var recentAppsEnabledAllApps by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentAppsEnabledAllApps(context)) }
    var favoritesEnabledAllApps  by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavoritesEnabledAllApps(context)) }
    var autoFolderSizeEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAutoFolderSizeEnabled(context)) }
    var assistantCardsEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAssistantCardsEnabled(context)) }
    var contextualDockEnabled    by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isContextualDockEnabled(context)) }

    SettingsCard {
        SettingsButtonRow(
            icon = Icons.Default.Search,
            title = "Arama Ayarlari",
            subtitle = "Kaynaklar, gecmis, cift tikla arama ve sonuc profilleri",
            onClick = onNavigateToSearchSettings,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = "Favoriler",
            subtitle = "Uzun basınca favoriye eklenen uygulamalar ana ekranda gösterilir (varsayılan: kapalı)",
            checked = favoritesEnabled,
            onCheckedChange = {
                favoritesEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setFavoritesEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.AutoAwesome,
            title = "Uygulama Önerileri",
            subtitle = "Arama çubuğunun altında en çok kullanılan 4 uygulama — günün saatine göre değişir",
            checked = suggestionsEnabled,
            onCheckedChange = {
                suggestionsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setSuggestionsEnabled(context, it)
            }
        )
        if (suggestionsEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "🧠 Öneri Algoritması",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Son 28 günlük kullanım verisi analiz edilir:\n" +
                        "• Yenilik (40%) — ne zaman son kullandın?\n" +
                        "• Sıklık (40%) — kaç kez açtın?\n" +
                        "• Zaman dilimi (20%) — sabah/öğle/akşam/gece alışkanlıkları\n\n" +
                        "Skor = yenilik × 0.4 + sıklık × 0.4 + zaman dilimi × 0.2",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.History,
            title = "Son Kullanılanlar",
            subtitle = "Ana ekranda son 8 uygulamayı büyük ikonlarla göster (varsayılan: kapalı)",
            checked = recentAppsEnabled,
            onCheckedChange = {
                recentAppsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setRecentAppsEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.SwipeUp,
            title = "Yukarı Kaydırma İpucu",
            subtitle = "Ana ekranda yukarı kaydırma animasyonu göster",
            checked = swipeHintEnabled,
            onCheckedChange = {
                swipeHintEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setSwipeHintEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Info,
            title = "Assistant Kartları",
            subtitle = "Ana ekranda kullanım alışkanlığınıza göre içgörü kartları gösterir",
            checked = assistantCardsEnabled,
            onCheckedChange = {
                assistantCardsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setAssistantCardsEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Layers,
            title = "Akıllı Dock",
            subtitle = "İlk 2 slot sabit, son 2 slot kullanım alışkanlığına göre otomatik değişir",
            checked = contextualDockEnabled,
            onCheckedChange = {
                contextualDockEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setContextualDockEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.NewReleases,
            title = "YENİ Rozeti",
            subtitle = "7 gün içinde kurulan uygulamalara rozet göster",
            checked = newBadgeEnabled,
            onCheckedChange = {
                newBadgeEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setNewBadgeEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.AspectRatio,
            title = "Otomatik Boyut Ayarla",
            subtitle = "Klasörleri ekran genişliğine göre otomatik boyutlandır — taşma ve üst üste binmeyi önler",
            checked = autoFolderSizeEnabled,
            onCheckedChange = {
                autoFolderSizeEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setAutoFolderSizeEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.FormatListNumbered,
            title = "Klasör Uygulama Sayısı",
            subtitle = "Klasör simgesinin altında uygulama adedini göster",
            checked = folderCountVisible,
            onCheckedChange = {
                folderCountVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderCountVisible(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.SwipeUp,
            title = "Yukarı Kaydırma İpucu",
            subtitle = "Klasör altında uygulama adını göster",
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
            subtitle = "Klasör ve uygulamaların altında son bildirimi göster",
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
            subtitle = "Tam ekran launcher — geri/home/recents butonları gizle",
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
                    Text("Tüm Uygulamalar Arka Plan", fontWeight = FontWeight.Medium, fontSize = 15.sp)
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

    // ── Tüm Uygulamalar Ekranı ────────────────────────────────────────────
    SettingsSectionTitle("Tüm Uygulamalar")
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = "Favoriler (Tüm Uygulamalar)",
            subtitle = "Tüm Uygulamalar ekranında favori satırını göster",
            checked = favoritesEnabledAllApps,
            onCheckedChange = {
                favoritesEnabledAllApps = it
                com.armutlu.apporganizer.utils.AppPrefs.setFavoritesEnabledAllApps(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.History,
            title = "Son Kullanılanlar (Tüm Uygulamalar)",
            subtitle = "Tüm Uygulamalar ekranında son kullanılanlar satırını göster",
            checked = recentAppsEnabledAllApps,
            onCheckedChange = {
                recentAppsEnabledAllApps = it
                com.armutlu.apporganizer.utils.AppPrefs.setRecentAppsEnabledAllApps(context, it)
            }
        )
    }

    // ── Widget Alanı ──────────────────────────────────────────────────────
    SettingsSectionTitle("Widget")
    var widgetAreaEnabledLocal by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)) }
    var widgetAutoResizeLocal by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAutoResizeEnabled(context)) }
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.Widgets,
            title = "Widget Alanı",
            subtitle = "Ana ekranda widget gösterimine izin ver",
            checked = widgetAreaEnabledLocal,
            onCheckedChange = {
                widgetAreaEnabledLocal = it
                com.armutlu.apporganizer.utils.AppPrefs.setWidgetAreaEnabled(context, it)
            }
        )
        SettingsSwitchRow(
            icon = Icons.Default.AspectRatio,
            title = "Otomatik Widget Boyutu",
            subtitle = "Widget yüksekliğini ekran boyutuna göre otomatik ayarla (%22)",
            checked = widgetAutoResizeLocal,
            onCheckedChange = {
                widgetAutoResizeLocal = it
                com.armutlu.apporganizer.utils.AppPrefs.setWidgetAutoResizeEnabled(context, it)
            }
        )
    }

    // ── İkon Paketi ───────────────────────────────────────────────────────
    SettingsSectionTitle("İkon Paketi")
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
                Text("Sistem İkonları", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text("Varsayılan uygulama ikonları", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Kurulu ikon paketi bulunamadı. Play Store'dan bir ikon paketi yükleyin.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
