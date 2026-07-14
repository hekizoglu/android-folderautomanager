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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BiometricHelper

/**
 * U1: Ayarlar hub ekranı — eski tek uzun liste yerine her ana kategori
 * (Görünüm, Launcher, Bildirimler, Arama, Uygulamalar, İstatistikler,
 * Güvenlik, Hakkında) kendi alt route'una gider.
 * Alt ekranlar: SettingsAppearanceScreen, SettingsLauncherScreen,
 * SettingsNotificationsScreen, SearchSettingsScreen, SettingsAppsScreen,
 * SettingsStatsScreen, SettingsSecurityScreen, SettingsAboutScreen.
 */
/**
 * ROADMAP [27] fix (KRİTİK): Biyometrik Ayarlar Kilidi açıkken SettingsScreen
 * her navigasyonda (ör. Haftalık Rapor'dan geri dönüşte) NavHost tarafından
 * yeniden compose ediliyor; `remember{}` state kaybolduğu için biometricUnlocked
 * her seferinde false'a dönüyor ve LaunchedEffect(Unit) yeniden biyometrik
 * doğrulama istiyordu. Doğrulama tek bir yanlış eşleşme/iptal ile
 * `onFailure = { onNavigateBack() }` çağırıp kullanıcıyı Ayarlar'dan tamamen
 * dışlıyordu (geri gitmeye çalıştıkça tekrar tekrar başarısız oluyordu).
 * Çözüm: kilidi process ömrü boyunca tek seferlik composable-dışı bir
 * singleton'da tut — aynı oturumda Ayarlar'a her dönüşte tekrar biyometrik
 * istenmez.
 */
private object SettingsLockSession {
    var unlocked: Boolean = false
}

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
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPermissionsGuide: () -> Unit = {},
) {
    val context = LocalContext.current

    // Biometric Settings Lock — açılışta kilidi doğrula (alt ekranlara
    // yalnızca bu hub üzerinden girildiği için kilit burada yeterli)
    var biometricUnlocked by remember { mutableStateOf(SettingsLockSession.unlocked) }
    LaunchedEffect(Unit) {
        // D27 fix: bu oturumda daha once basariyla acildiysa (ör. Haftalik Rapor'a
        // gidip geri donulduyse) tekrar biyometrik istenmez — asagidaki dal atlanir.
        if (SettingsLockSession.unlocked) {
            biometricUnlocked = true
            return@LaunchedEffect
        }
        val lockEnabled = AppPrefs.isBiometricSettingsLockEnabled(context)
        if (!lockEnabled) {
            biometricUnlocked = true
            SettingsLockSession.unlocked = true
            return@LaunchedEffect
        }
        val activity = context as? FragmentActivity
        if (activity == null || !BiometricHelper.isAvailable(activity)) {
            onNavigateBack()
            return@LaunchedEffect
        }
        BiometricHelper.authenticate(
            activity = activity,
            onSuccess = {
                biometricUnlocked = true
                SettingsLockSession.unlocked = true
            },
            onFailure = { onNavigateBack() }
        )
    }
    if (!biometricUnlocked) return

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

            // ── Kişiselleştirme ──────────────────────────────────────────
            item { SettingsSectionTitle("Kişiselleştirme") }
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
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.Notifications,
                        title = "Bildirimler",
                        subtitle = "Bildirim izni, rozetler ve akıllı bildirimler",
                        onClick = onNavigateToNotifications,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.Search,
                        title = "Arama",
                        subtitle = "Arama kaynakları, hız ve sonuç düzeni",
                        onClick = onNavigateToSearchSettings,
                    )
                }
            }

            // ── Yönetim ──────────────────────────────────────────────────
            item { SettingsSectionTitle("Yönetim") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Apps,
                        title = "Uygulamalar",
                        subtitle = "Uygulama listesi, gizliler ve diğer klasörü",
                        onClick = onNavigateToApps,
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

            // ── Sistem ───────────────────────────────────────────────────
            item { SettingsSectionTitle("Sistem") }
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
                        icon = Icons.Default.Fingerprint,
                        title = "Güvenlik",
                        subtitle = "Ayarlar ekranını kilitle",
                        onClick = onNavigateToSecurity,
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
