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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import kotlin.math.roundToInt

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

        // ── Arama İstatistikleri ─────────────────────────────────────────
        // Gizlilik: yalnizca anonim sayaclar (SearchStatsPrefs) - aranan metin/kisi/numara ASLA kaydedilmez.
        item { SettingsSectionTitle("Arama İstatistikleri") }
        item {
            var resetTrigger by remember { mutableIntStateOf(0) }
            val summary = remember(resetTrigger) { SearchStatsPrefs.getSummary(context) }

            if (summary.totalSearches == 0) {
                SettingsCard {
                    SettingsInfoRow(icon = Icons.Default.Search, title = "Arama İstatistikleri", subtitle = "Henüz arama yapılmadı")
                }
            } else {
                val zeroResultPct = (summary.zeroResultCount * 100.0 / summary.totalSearches).roundToInt()
                val firstResultPct = if (summary.totalClicks > 0)
                    (summary.firstResultClicks * 100.0 / summary.totalClicks).roundToInt() else 0
                val typeDistribution = summary.clickCountsByType.entries
                    .sortedByDescending { it.value }
                    .joinToString(", ") { (type, count) ->
                        val pct = if (summary.totalClicks > 0) (count * 100.0 / summary.totalClicks).roundToInt() else 0
                        "${sourceTypeLabel(type)} %$pct"
                    }.ifBlank { "—" }
                val topAction = summary.actionCounts.entries.maxByOrNull { it.value }
                    ?.let { (type, count) -> "${actionTypeLabel(type)} ($count)" } ?: "—"

                SettingsCard {
                    SettingsInfoRow(icon = Icons.Default.Search, title = "Toplam Arama", subtitle = "${summary.totalSearches}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Sıfır Sonuç Oranı", subtitle = "%$zeroResultPct")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.Timer, title = "Ortalama Gecikme", subtitle = "${summary.avgLatencyMs} ms")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.TouchApp, title = "İlk Sonuç Tıklama Oranı", subtitle = "%$firstResultPct")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.PieChart, title = "Tip Dağılımı", subtitle = typeDistribution)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.Bolt, title = "En Çok Kullanılan Aksiyon", subtitle = topAction)
                }
            }

            SettingsCard {
                SettingsButtonRow(
                    icon = Icons.Default.Delete,
                    title = "İstatistikleri Sıfırla",
                    subtitle = "Tüm arama sayaçlarını siler",
                    onClick = {
                        SearchStatsPrefs.reset(context)
                        resetTrigger++
                    },
                )
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

/** SourceType.key -> okunabilir Türkçe etiket (tip dağılımı gösterimi için) */
private fun sourceTypeLabel(sourceType: String): String = when (sourceType) {
    "app" -> "Uygulama"
    "category" -> "Kategori"
    "contact" -> "Kişi"
    "file" -> "Dosya"
    else -> sourceType
}

/** Hızlı aksiyon tipi -> okunabilir Türkçe etiket */
private fun actionTypeLabel(actionType: String): String = when (actionType) {
    "CALL" -> "Ara"
    "WHATSAPP" -> "WhatsApp"
    "SMS" -> "SMS"
    "EMAIL" -> "E-posta"
    "OPEN_APP" -> "Uygulama Aç"
    else -> actionType
}
