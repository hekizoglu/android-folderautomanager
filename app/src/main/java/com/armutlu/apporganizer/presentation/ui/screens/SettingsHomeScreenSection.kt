package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.launcher.HomePageTelemetryPolicy
import com.armutlu.apporganizer.utils.AppAnalytics
import kotlinx.coroutines.launch

/**
 * Ana Ekran Özellikleri, Widget ve İkon Paketi bölümü.
 * SettingsScreen LazyColumn içinde item{} bloklarıyla çağrılır.
 */
@Composable
fun SettingsHomeScreenSection(
    onNavigateToSearchSettings: () -> Unit = {},
    onNavigateToSmartTickerSettings: () -> Unit = {},
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
    var previewBlocklistOpen by remember { mutableStateOf(false) }
    var previewBlocklistDraft by remember {
        mutableStateOf(
            com.armutlu.apporganizer.utils.AppPrefs.getNotificationPreviewBlockedPackages(context)
                .sorted()
                .joinToString("\n")
        )
    }
    var hideNavButtons     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNavButtonsHidden(context)) }
    var allAppsBgAlpha     by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)) }
    var suggestionsEnabled       by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
    var recentNotificationAppsRowEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isRecentNotificationAppsRowEnabled(context)) }
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
        // T05: eskiden burada tek "Haber Şeridi" aç/kapat switch'i vardı — artık kullanıcı
        // içerik türlerini, otomatik geçişi ve hassas bilgi gösterimini ayrı ayrı kontrol
        // edebildiği için dedike ekrana taşındı (SmartTickerSettingsScreen).
        SettingsButtonRow(
            icon = Icons.Default.Campaign,
            title = "Akıllı Nabız Şeridi",
            subtitle = "Günün akışı, hatırlatmalar ve gösterilecek içerik türleri",
            onClick = onNavigateToSmartTickerSettings,
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
        // S3 — çekmece sadeleştirme: varsayılan kapalı, kapalıyken çekmecede tek menü butonu kullanılır
        var drawerChipRowsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isDrawerChipRowsEnabled(context)) }
        SettingsSwitchRow(
            icon = Icons.Default.Tune,
            title = stringResource(R.string.drawer_chip_rows_settings_toggle_title),
            subtitle = stringResource(R.string.drawer_chip_rows_settings_toggle_subtitle),
            checked = drawerChipRowsEnabled,
            onCheckedChange = {
                drawerChipRowsEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setDrawerChipRowsEnabled(context, it)
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
            var suggestionsIconSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getSuggestionsIconSizeDp(context)) }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Öneriler Bölümü Boyutu: ${suggestionsIconSizeDp}dp", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = suggestionsIconSizeDp.toFloat(),
                    onValueChange = {
                        suggestionsIconSizeDp = it.toInt()
                        com.armutlu.apporganizer.utils.AppPrefs.setSuggestionsIconSizeDp(context, it.toInt())
                    },
                    valueRange = 32f..52f,
                    steps = 4
                )
            }
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
            icon = Icons.Default.NotificationsActive,
            title = "Son Bildirim Alanlar",
            subtitle = "Son 24 saatte bildirim gelen uygulamaları ana ekranda göster",
            checked = recentNotificationAppsRowEnabled,
            onCheckedChange = {
                recentNotificationAppsRowEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setRecentNotificationAppsRowEnabled(context, it)
            }
        )
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
            subtitle = "Aktif bildirimlerden en fazla 2 kısa önizleme göster",
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
        SettingsButtonRow(
            icon = Icons.Default.Lock,
            title = "Hassas Uygulama Engeli",
            subtitle = "İçerik yerine sadece bildirim sayısı gösterilecek paketleri düzenle",
            onClick = {
                previewBlocklistDraft = com.armutlu.apporganizer.utils.AppPrefs
                    .getNotificationPreviewBlockedPackages(context)
                    .sorted()
                    .joinToString("\n")
                previewBlocklistOpen = true
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
    // D03: eski "Ana Ekranda Skor Göster" (pulse ring) toggle'ı yerini Dijital Yaşam kartı
    // görünürlüğüne bıraktı — skor artık PulseClockWidget'ta değil, HomeScreen'deki
    // DigitalLifeCard'da tek yerde gösteriliyor. isDigitalLifeCardVisible() ilk çağrıda
    // eski KEY_HOME_SCORE_VISIBLE değerini bir kez migrate eder.
    var digitalLifeCardVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isDigitalLifeCardVisible(context)) }
    var homeInsightVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeInsightVisible(context)) }
    var homeUsageChartVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeUsageChartVisible(context)) }
    var homeWeatherEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeWeatherEnabled(context)) }
    var homeWeatherUseLocation by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isHomeWeatherUseLocation(context)) }
    var weatherCity by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getHomeWeatherManualCity(context)) }
    var weatherCityDialogOpen by remember { mutableStateOf(false) }
    val coarseLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            homeWeatherUseLocation = true
            com.armutlu.apporganizer.utils.AppPrefs.setHomeWeatherUseLocation(context, true)
        }
    }

    if (previewBlocklistOpen) {
        AlertDialog(
            onDismissRequest = { previewBlocklistOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    com.armutlu.apporganizer.utils.AppPrefs.setNotificationPreviewBlockedPackages(
                        context,
                        previewBlocklistDraft.lines().map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    )
                    previewBlocklistOpen = false
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewBlocklistOpen = false }) {
                    Text("İptal")
                }
            },
            title = { Text("Hassas Uygulama Engeli") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Her satıra bir paket adı yaz. Listedeki uygulamalarda içerik kapatılır ve yalnızca bildirim sayısı görünür.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = previewBlocklistDraft,
                        onValueChange = { previewBlocklistDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        label = { Text("Örnek: com.whatsapp") }
                    )
                }
            }
        )
    }
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
        var searchBarPosition by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getSearchBarPosition(context)) }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_search_bar_position_title),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_search_bar_position_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val positions = listOf(
                    com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_TOP to com.armutlu.apporganizer.R.string.settings_search_bar_position_top,
                    com.armutlu.apporganizer.utils.AppPrefs.SEARCH_BAR_POS_BOTTOM to com.armutlu.apporganizer.R.string.settings_search_bar_position_bottom,
                )
                positions.forEach { (posKey, labelRes) ->
                    val selected = searchBarPosition == posKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            )
                            .clickable {
                                searchBarPosition = posKey
                                com.armutlu.apporganizer.utils.AppPrefs.setSearchBarPosition(context, posKey)
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
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_digital_life_card_visible_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_digital_life_card_visible_desc),
            checked = digitalLifeCardVisible,
            onCheckedChange = {
                digitalLifeCardVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setDigitalLifeCardVisible(context, it)
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
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Info,
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_usage_chart_visible_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_usage_chart_visible_desc),
            checked = homeUsageChartVisible,
            onCheckedChange = {
                homeUsageChartVisible = it
                com.armutlu.apporganizer.utils.AppPrefs.setHomeUsageChartVisible(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Cloud,
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_visible_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_visible_desc),
            checked = homeWeatherEnabled,
            onCheckedChange = {
                homeWeatherEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setHomeWeatherEnabled(context, it)
            }
        )
        if (homeWeatherEnabled) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsSwitchRow(
                icon = Icons.Default.MyLocation,
                title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_use_location_title),
                subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_use_location_desc),
                checked = homeWeatherUseLocation,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            homeWeatherUseLocation = true
                            com.armutlu.apporganizer.utils.AppPrefs.setHomeWeatherUseLocation(context, true)
                        } else {
                            coarseLocationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    } else {
                        homeWeatherUseLocation = false
                        com.armutlu.apporganizer.utils.AppPrefs.setHomeWeatherUseLocation(context, false)
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsButtonRow(
                icon = Icons.Default.LocationCity,
                title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_city_title),
                subtitle = weatherCity.ifBlank {
                    androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_city_empty)
                },
                onClick = { weatherCityDialogOpen = true },
            )
        }
    }

    if (weatherCityDialogOpen) {
        var draftCity by remember(weatherCity) { mutableStateOf(weatherCity) }
        AlertDialog(
            onDismissRequest = { weatherCityDialogOpen = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_city_title)) },
            text = {
                OutlinedTextField(
                    value = draftCity,
                    onValueChange = { draftCity = it },
                    singleLine = true,
                    label = { Text(androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_weather_city_hint)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    weatherCity = draftCity.trim()
                    com.armutlu.apporganizer.utils.AppPrefs.setHomeWeatherManualCity(context, weatherCity)
                    weatherCityDialogOpen = false
                }) {
                    Text(androidx.compose.ui.res.stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { weatherCityDialogOpen = false }) {
                    Text(androidx.compose.ui.res.stringResource(android.R.string.cancel))
                }
            },
        )
    }

    // ── Ana Sayfa Yapısı (Döngü P17) ─────────────────────────────────────
    // Dashboard istemeyen kullanıcıların hızlı klasör deneyimini korur — bu ayarlar tercihi
    // HomePagePrefs'e yazar; Akıllı Dashboard'ın pager'da fiilen görünmesi HomePagerRolloutPolicy
    // üzerinden bu tercihe bağlıdır (P24/P25 tamamlandı, HomeScreen.kt dashboardEnabled).
    SettingsSectionTitle(androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_home_structure_title))
    var smartDashboardEnabled by remember {
        mutableStateOf(com.armutlu.apporganizer.utils.HomePagePrefs.isSmartDashboardEnabled(context))
    }
    var startPageMode by remember {
        mutableStateOf(com.armutlu.apporganizer.utils.HomePagePrefs.getStartPageMode(context))
    }
    // Görev S1 — tek "BUGÜN" kartı; varsayılan AÇIK (yeni özellik=ayar kuralı).
    var todayCardEnabled by remember {
        mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isTodayCardEnabled(context))
    }
    // Görev S2 — Usta (100⭐) ödülü: toggle yalnızca MASTER seviyesine ulaşıldıysa görünür.
    // Basit tek seferlik okuma yeterli — Ayarlar ekranı her açılışta yeniden compose olur,
    // agresif reaktiflik gerektirmez (ekran girişte güncel veriyi gösterir).
    val totalStarsForMasterReward by androidx.compose.runtime.produceState(initialValue = 0, context) {
        value = runCatching {
            com.armutlu.apporganizer.data.local.AppDatabase.getInstance(context)
                .missionHistoryDao()
                .getTotalStars()
        }.getOrDefault(0)
    }
    val masterRewardUnlocked = com.armutlu.apporganizer.domain.usecase.missions.MasterRewardPolicy
        .isMasterUnlocked(totalStarsForMasterReward)
    var masterClockStyleEnabled by remember {
        mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isMasterClockStyleEnabled(context))
    }
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.Dashboard,
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_smart_dashboard_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_smart_dashboard_desc),
            checked = smartDashboardEnabled,
            onCheckedChange = { enabled ->
                smartDashboardEnabled = enabled
                com.armutlu.apporganizer.utils.HomePagePrefs.setSmartDashboardEnabled(context, enabled)
                AppAnalytics.smartDashboardToggled(enabled)
                // Roadmap P17 madde 3: Dashboard kapatılırken SMART_DASHBOARD seçiliyse
                // FIRST_FOLDER_PAGE'e normalize edilir — kapalı bir sayfaya "başlangıç" denemez.
                if (!enabled && startPageMode == com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.SMART_DASHBOARD) {
                    startPageMode = com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.FIRST_FOLDER_PAGE
                    com.armutlu.apporganizer.utils.HomePagePrefs.setStartPageMode(context, startPageMode)
                    AppAnalytics.homeStartModeChanged(HomePageTelemetryPolicy.startMode(startPageMode))
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_start_page_mode_title),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_start_page_mode_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf(
                    com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.SMART_DASHBOARD to com.armutlu.apporganizer.R.string.settings_start_page_mode_dashboard,
                    com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.FIRST_FOLDER_PAGE to com.armutlu.apporganizer.R.string.settings_start_page_mode_first_folder,
                    com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE to com.armutlu.apporganizer.R.string.settings_start_page_mode_restore_last,
                )
                modes.forEach { (mode, labelRes) ->
                    // Dashboard kapalıyken Dashboard seçeneği devre dışı — kullanıcıyı kapalı
                    // bir sayfaya yönlendiren tutarsız bir tercihe izin vermeyiz.
                    val optionEnabled = smartDashboardEnabled ||
                        mode != com.armutlu.apporganizer.utils.HomePagePrefs.StartPageMode.SMART_DASHBOARD
                    val selected = startPageMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            )
                            .clickable(enabled = optionEnabled) {
                                startPageMode = mode
                                com.armutlu.apporganizer.utils.HomePagePrefs.setStartPageMode(context, mode)
                                AppAnalytics.homeStartModeChanged(HomePageTelemetryPolicy.startMode(mode))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            androidx.compose.ui.res.stringResource(labelRes),
                            fontSize = 13.sp,
                            color = when {
                                selected -> MaterialTheme.colorScheme.onPrimary
                                !optionEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        SettingsSwitchRow(
            icon = Icons.Default.Star,
            title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_today_card_enabled_title),
            subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_today_card_enabled_desc),
            checked = todayCardEnabled,
            onCheckedChange = {
                todayCardEnabled = it
                com.armutlu.apporganizer.utils.AppPrefs.setTodayCardEnabled(context, it)
            }
        )
        // Görev S2 — Usta (100⭐) ödülü: yalnızca kilit açıldıysa görünür (MasterRewardPolicy).
        if (masterRewardUnlocked) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            SettingsSwitchRow(
                icon = Icons.Default.Star,
                title = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_master_clock_style_title),
                subtitle = androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_master_clock_style_desc),
                checked = masterClockStyleEnabled,
                onCheckedChange = {
                    masterClockStyleEnabled = it
                    com.armutlu.apporganizer.utils.AppPrefs.setMasterClockStyleEnabled(context, it)
                }
            )
        }
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

    // ── Görev Temposu (G1/G7) ────────────────────────────────────────────
    // Kisisel gorev hedefi formulunun katsayisini belirler — hedef = son 7 gun medyani x tempo.
    SettingsSectionTitle("Görev Temposu")
    var missionTempo by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getMissionTempo(context)) }
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Görevler senin geçmiş kullanımına göre kişiselleşir. Ne kadar zorlayıcı olsun?",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.RAHAT to "Rahat",
                    com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.DENGELI to "Dengeli",
                    com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.IDDIALI to "İddialı",
                )
                options.forEach { (tempo, label) ->
                    FilterChip(
                        selected = missionTempo == tempo,
                        onClick = {
                            missionTempo = tempo
                            com.armutlu.apporganizer.utils.AppPrefs.setMissionTempo(context, tempo)
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            Text(
                when (missionTempo) {
                    com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.RAHAT ->
                        "Hedefler alışkanlığına yakın — kolay başarılır."
                    com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.DENGELI ->
                        "Hedefler alışkanlığından biraz daha iyisini ister."
                    com.armutlu.apporganizer.utils.AppPrefs.MissionTempo.IDDIALI ->
                        "Hedefler seni daha çok zorlar — hızlı ilerleme için."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
