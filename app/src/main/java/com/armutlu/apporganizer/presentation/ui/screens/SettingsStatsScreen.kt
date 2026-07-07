package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs

/**
 * U1: İstatistikler & Raporlar alt ekranı — özet sayılar, Raporlar Merkezi
 * ve Bildirim Raporu kısayolları.
 * İçerik eski SettingsScreen'den birebir taşındı, fonksiyonellik değişmedi.
 */
@Composable
fun SettingsStatsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReportsCenter: () -> Unit = {},
    onNavigateToNotificationReport: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by viewModel.screenState.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val otherApps by viewModel.otherApps.collectAsState()

    SettingsSubScreenScaffold(title = "İstatistikler & Raporlar", onNavigateBack = onNavigateBack) {

        // ── İstatistikler ─────────────────────────────────────────────────
        item { SettingsSectionTitle("İstatistikler") }
        item {
            val lastBackupMs = AppPrefs.getLastBackupTime(context)
            val lastBackupText = if (lastBackupMs == 0L) "Henüz yedeklenmedi"
            else {
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale("tr"))
                sdf.format(java.util.Date(lastBackupMs))
            }
            val topCategory by remember {
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
                SettingsInfoRow(icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Sınıflandırılmamış", subtitle = "${otherApps.size} uygulama")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsInfoRow(icon = Icons.Default.VisibilityOff, title = "Gizli Uygulama", subtitle = "${hiddenApps.size}")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsInfoRow(icon = Icons.Default.BarChart, title = "En Çok Dolu Kategori", subtitle = topCategory)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsInfoRow(icon = Icons.Default.Backup, title = "Son Yedekleme", subtitle = lastBackupText)
            }
        }

        // ── Rapor kısayolları ─────────────────────────────────────────────
        item {
            SettingsCard {
                SettingsButtonRow(
                    icon = Icons.Default.Dashboard,
                    title = "Raporlar Merkezi",
                    subtitle = "Genel bakis ve kullanim raporlarini tek yerden ac",
                    onClick = onNavigateToReportsCenter,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsButtonRow(
                    icon = Icons.Default.Notifications,
                    title = "Bildirim Raporu",
                    subtitle = "Çok konuşan, rahatsız eden ve dikkat dağıtan uygulamalar",
                    onClick = onNavigateToNotificationReport,
                )
            }
        }
    }
}
