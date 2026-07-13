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
import kotlinx.coroutines.launch

/**
 * Ana Ekran Özellikleri, Widget ve İkon Paketi bölümü.
 * SettingsScreen LazyColumn içinde item{} bloklarıyla çağrılır.
 */
@Composable
fun SettingsHomeScreenSection(
    onNavigateToSearchSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

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

    // Genişleyebilir kart — 13 ayar tek başlık altında, scroll yorgunluğunu azaltır (D199)
    SettingsExpandableCard(
        icon = Icons.Default.Home,
        title = "Ana Ekran Ayarları",
        subtitle = "Ana ekranın görünümü ve davranışları",
        initiallyExpanded = false
    ) {
        HomeSettingsGroupTitle("Arama")
        SettingsButtonRow(
            icon = Icons.Default.Search,
            title = "Arama Ayarları",
            subtitle = "Arama kaynakları, hız ve sonuç düzeni",
            onClick = onNavigateToSearchSettings,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        var tickerEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isTickerEnabled(context)) }
        SettingsSwitchRow(
            icon = Icons.Default.Campaign,
            title = "Haber Şeridi",
            subtitle = "Günün akışı ve kısa hatırlatmalar için hareketli şerit",
            checked = tickerEnabled,
            onCheckedChange = {
                tickerEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setTickerEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        var shineEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSearchShineEnabled(context)) }
        SettingsSwitchRow(
            icon = Icons.Default.AutoAwesome,
            title = "Arama Çubuğu Parlaması",
            subtitle = "Arama alanını arada kısa bir ışık geçişiyle vurgular",
            checked = shineEnabled,
            onCheckedChange = {
                shineEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setSearchShineEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        var folderSearchEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSearchEnabled(context)) }
        SettingsSwitchRow(
            icon = Icons.Default.FolderOpen,
            title = "Klasör İçi Arama",
            subtitle = "Klasör içinde hızlı arama alanı göster",
            checked = folderSearchEnabled,
            onCheckedChange = {
                folderSearchEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderSearchEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        HomeSettingsGroupTitle("Öneriler ve bildirimler")
        // "Bildirim Analizi" toggle'ı Ayarlar > Bildirimler ekranına taşındı (Döngü 226) —
        // ana ekran ayarı değil, bildirim veri toplama ayarıdır.
        var folderBadgeEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderBadgeEnabled(context)) }
        SettingsSwitchRow(
            icon = Icons.Default.Notifications,
            title = "Klasör Bildirim Rozeti",
            subtitle = "Klasör üstünde toplam bildirim sayısını göster",
            checked = folderBadgeEnabled,
            onCheckedChange = {
                folderBadgeEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderBadgeEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = "Favoriler",
            subtitle = "Sık açtığın uygulamaları ana ekranda öne çıkar",
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
            subtitle = "En çok kullandığın uygulamaları arama alanının altında gösterir",
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
                        "Son kullanımlarınıza göre sıralama yapılır:\n" +
                        "• Daha yeni açılanlar öne çıkabilir\n" +
                        "• Sık açtıkların daha görünür olur\n" +
                        "• Gün içindeki alışkanlıklarına göre sıralama uyarlanır",
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
            subtitle = "Son açtığın uygulamaları büyük ikonlarla göster",
            checked = recentAppsEnabled,
            onCheckedChange = {
                recentAppsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setRecentAppsEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        HomeSettingsGroupTitle("Temel davranışlar")
        SettingsSwitchRow(
            icon = Icons.Default.SwipeUp,
            title = "Yukarı Kaydırma İpucu",
            subtitle = "Yukarı kaydırma hareketini kısa bir ipucu ile göster",
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
            subtitle = "Kullanımına göre kısa öneri kartları gösterir",
            checked = assistantCardsEnabled,
            onCheckedChange = {
                assistantCardsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setAssistantCardsEnabled(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        HomeSettingsGroupTitle("Görsel")
        SettingsSwitchRow(
            icon = Icons.Default.NewReleases,
            title = "YENİ Rozeti",
            subtitle = "Yeni kurulan uygulamaları işaretler",
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
            subtitle = "Klasörleri ekrana daha rahat sığdırır",
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
            subtitle = "Klasör altında içerik sayısını göster",
            checked = folderCountVisible,
            onCheckedChange = {
                folderCountVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderCountVisible(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.SwipeUp,
            title = "Klasör Alt Yazısı",
            subtitle = "Klasör altında kısa ad satırı göster",
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
            subtitle = "Son bildirimin kısa özetini göster",
            checked = notifTextEnabled,
            onCheckedChange = {
                notifTextEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setNotificationTextEnabled(context, it)
                if (!it) {
                    // Gizlilik: ayar kapatılınca DB'de kayıtlı tüm bildirim metinlerini temizle
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            com.armutlu.apporganizer.data.local.AppDatabase.getInstance(context)
                                .appDao()
                                .clearAllNotificationTexts()
                        }
                    }
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.HideSource,
            title = "Sistem Navigasyonunu Gizle",
            subtitle = "Daha sade bir tam ekran görünüm sağlar",
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
                    Text("Arka plan görünürlüğü: ${(allAppsBgAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // ── Saat ve Dijital Nabız (Pulse Clock, D244) ────────────────────────
    SettingsSectionTitle(androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_clock_pulse_title))
    var clockStyle by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getClockStyle(context)) }
    var homeScoreVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeScoreVisible(context)) }
    var homeInsightVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeInsightVisible(context)) }
    SettingsCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_clock_style_title),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val styles = listOf(
                    com.armutlu.apporganizer.utils.AppPrefs.CLOCK_STYLE_MINIMAL to com.armutlu.apporganizer.R.string.settings_clock_style_minimal,
                    com.armutlu.apporganizer.utils.AppPrefs.CLOCK_STYLE_PULSE to com.armutlu.apporganizer.R.string.settings_clock_style_pulse,
                    com.armutlu.apporganizer.utils.AppPrefs.CLOCK_STYLE_GLASS to com.armutlu.apporganizer.R.string.settings_clock_style_glass,
                )
                styles.forEach { (styleKey, labelRes) ->
                    val selected = clockStyle == styleKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            )
                            .clickable {
                                clockStyle = styleKey
                                com.armutlu.apporganizer.utils.AppPrefs.setClockStyle(context, styleKey)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            androidx.compose.ui.res.stringResource(labelRes),
                            fontSize = 13.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_score_visible_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_score_visible_desc),
            checked = homeScoreVisible,
            onCheckedChange = {
                homeScoreVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setHomeScoreVisible(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Info,
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_insight_visible_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_insight_visible_desc),
            checked = homeInsightVisible,
            onCheckedChange = {
                homeInsightVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setHomeInsightVisible(context, it)
            }
        )
    }

    // ── Tüm Uygulamalar Ekranı ────────────────────────────────────────────
    SettingsSectionTitle("Tüm Uygulamalar")
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = "Favoriler (Tüm Uygulamalar)",
            subtitle = "Tüm uygulamalar ekranında favori satırını göster",
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
            subtitle = "Tüm uygulamalar ekranında son kullanılanları göster",
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
            subtitle = "Ana ekranda widget alanına izin ver",
            checked = widgetAreaEnabledLocal,
            onCheckedChange = {
                widgetAreaEnabledLocal = it
                com.armutlu.apporganizer.utils.AppPrefs.setWidgetAreaEnabled(context, it)
            }
        )
        SettingsSwitchRow(
            icon = Icons.Default.AspectRatio,
            title = "Otomatik Widget Boyutu",
            subtitle = "Widget alanını ekrana göre uyumlu tut",
            checked = widgetAutoResizeLocal,
            onCheckedChange = {
                widgetAutoResizeLocal = it
                com.armutlu.apporganizer.utils.AppPrefs.setWidgetAutoResizeEnabled(context, it)
            }
        )
    }

    // ── İkon Paketi ───────────────────────────────────────────────────────
    // Tek sahip: Görünüm ekranı (SettingsAppearanceSection.kt) — burada sadece yönlendirme kartı gösterilir.
    SettingsSectionTitle("İkon Paketi")
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("İkon Paketi Seçimi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text("Bu seçim Görünüm ekranında yapılır", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeSettingsGroupTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp, end = 16.dp)
    )
}
