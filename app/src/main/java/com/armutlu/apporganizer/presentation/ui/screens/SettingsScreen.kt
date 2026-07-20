package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R

/**
 * U1: Ayarlar hub ekranı — eski tek uzun liste yerine her ana kategori
 * (Görünüm, Launcher, Bildirimler, Arama, Uygulamalar, İstatistikler,
 * Güvenlik, Hakkında) kendi alt route'una gider.
 * Alt ekranlar: SettingsAppearanceScreen, SettingsLauncherScreen,
 * SettingsNotificationsScreen, SearchSettingsScreen, SettingsAppsScreen,
 * SettingsStatsScreen, SettingsSecurityScreen, SettingsAboutScreen.
 */
/**
 * F2: Biyometrik Ayarlar Kilidi artık NavHost seviyesinde `SettingsLockGate`
 * (presentation/ui/security/SettingsLockGuard.kt) tarafından uygulanıyor —
 * bu composable'a girilebilmesi zaten kilidin açılmış olduğu anlamına gelir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToLauncher: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearchSettings: () -> Unit = {},
    onNavigateToApps: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToUsageData: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPermissionsGuide: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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

            // ── Eksik İzinler — sadece eksik izin varsa görünür ──────────
            item { SettingsPermissionsCard() }

            // ── Ana Ekran ──────────────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.settings_hub_home_screen_title)) }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Palette,
                        title = "Görünüm",
                        subtitle = "Görünümünü sana uygun hale getir",
                        onClick = onNavigateToAppearance,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.Home,
                        title = "Launcher",
                        subtitle = "Ana ekran davranışı, dock, hareketler ve widget alanı",
                        onClick = onNavigateToLauncher,
                    )
                }
            }

            // ── Arama & Çekmece ────────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.settings_hub_search_drawer_title)) }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.settings_hub_search_drawer_title),
                        subtitle = stringResource(R.string.settings_hub_search_drawer_subtitle),
                        onClick = onNavigateToSearchSettings,
                    )
                }
            }

            // ── Otomatik Düzenleme ──────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.settings_hub_auto_organize_title)) }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Apps,
                        title = stringResource(R.string.settings_hub_auto_organize_subtitle),
                        subtitle = "Uygulama listesi, gizliler ve diğer klasörü",
                        onClick = onNavigateToApps,
                    )
                }
            }

            // ── Dijital Yaşam ────────────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.settings_hub_digital_life_title)) }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Analytics,
                        title = stringResource(R.string.usage_data_title),
                        subtitle = stringResource(R.string.usage_data_hub_subtitle),
                        onClick = onNavigateToUsageData,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.Notifications,
                        title = "Bildirimler",
                        subtitle = "Bildirim izni, rozetler ve akıllı bildirimler",
                        onClick = onNavigateToNotifications,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.BarChart,
                        title = "İstatistikler & Raporlar",
                        subtitle = "Özetler, raporlar ve kullanım içgörüleri",
                        onClick = onNavigateToStats,
                    )
                }
            }

            // ── Gizlilik & Veri ──────────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.settings_hub_privacy_data_title)) }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.settings_hub_privacy_data_title),
                        subtitle = stringResource(R.string.settings_hub_privacy_data_subtitle),
                        onClick = onNavigateToSecurity,
                    )
                }
            }

            // ── Gelişmiş & Destek ─────────────────────────────────────────
            item { SettingsSectionTitle(stringResource(R.string.settings_hub_advanced_support_title)) }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.VerifiedUser,
                        title = "Tam Performans / İzinler",
                        subtitle = "Gerekli izinler, neden gerekli ve kapalıyken ne çalışmaz",
                        onClick = onNavigateToPermissionsGuide,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.Info,
                        title = "Hakkında & Yedekleme",
                        subtitle = "Hakkında, gizlilik, yedekleme ve geri bildirim",
                        onClick = onNavigateToAbout,
                    )
                }
            }
        }
    }
}
