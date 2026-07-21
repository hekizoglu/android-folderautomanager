package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.SearchStatsPrefs
import kotlin.math.roundToInt

/**
 * U1: İstatistikler & Raporlar alt ekranı - özet sayılar, Raporlar Merkezi
 * ve Bildirim Raporu kısayolları.
 * İçerik eski SettingsScreen'den birebir taşındı, fonksiyonellik değişmedi.
 */
@Composable
fun SettingsStatsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReportsCenter: () -> Unit = {},
    onNavigateToNotificationReport: () -> Unit = {},
    onNavigateToMissions: () -> Unit = {},
    onNavigateToClassificationReview: () -> Unit = {},
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
                        ?: "-"
                }
            }
            SettingsCard {
                SettingsInfoRow(icon = Icons.Default.Apps, title = "Toplam Uygulama", subtitle = "${state.totalAppsCount}")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsInfoRow(icon = Icons.Default.Folder, title = "Kategori Sayısı", subtitle = "${state.categories.size}")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsButtonRow(icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Sınıflandırılmamış", subtitle = "${otherApps.size} uygulama", onClick = onNavigateToClassificationReview)
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
        item { SettingsSectionTitle(stringResource(R.string.settings_stats_search_section)) }
        item {
            var resetTrigger by remember { mutableIntStateOf(0) }
            val summary = remember(resetTrigger) { SearchStatsPrefs.getSummary(context) }

            if (summary.totalSearches == 0) {
                SettingsCard {
                    SettingsInfoRow(icon = Icons.Default.Search, title = stringResource(R.string.settings_stats_search_empty_title), subtitle = stringResource(R.string.settings_stats_search_empty_subtitle))
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
                    }.ifBlank { "-" }
                val topAction = summary.actionCounts.entries.maxByOrNull { it.value }
                    ?.let { (type, count) -> "${actionTypeLabel(type)} ($count)" } ?: "-"

                SettingsCard {
                    SettingsInfoRow(icon = Icons.Default.Search, title = stringResource(R.string.settings_stats_search_total), subtitle = "${summary.totalSearches}")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.AutoMirrored.Filled.HelpOutline, title = stringResource(R.string.settings_stats_search_zero_result_rate), subtitle = "%$zeroResultPct")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.Timer, title = stringResource(R.string.settings_stats_search_avg_latency), subtitle = stringResource(R.string.settings_stats_search_avg_latency_value, summary.avgLatencyMs))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.TouchApp, title = stringResource(R.string.settings_stats_search_first_click_rate), subtitle = "%$firstResultPct")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.PieChart, title = stringResource(R.string.settings_stats_search_type_distribution), subtitle = typeDistribution)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsInfoRow(icon = Icons.Default.Bolt, title = stringResource(R.string.settings_stats_search_top_action), subtitle = topAction)
                }
            }

            SettingsCard {
                SettingsButtonRow(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.settings_stats_search_reset_title),
                    subtitle = stringResource(R.string.settings_stats_search_reset_subtitle),
                    onClick = {
                        SearchStatsPrefs.reset(context)
                        resetTrigger++
                    },
                )
            }
        }

        // ── Rapor kısayolları ─────────────────────────────────────────────
        item {
            var notifAnalytics by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_NOTIF_ANALYTICS_ENABLED,
                read = { AppPrefs.isNotifAnalyticsEnabled(context) }
            )
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.Insights,
                    title = stringResource(R.string.settings_notif_analytics_title),
                    subtitle = "Bildirim sayilari kaydedilir; icerik saklanmaz",
                    checked = notifAnalytics,
                    onCheckedChange = {
                        notifAnalytics = it
                        AppPrefs.setNotifAnalyticsEnabled(context, it)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsButtonRow(
                    icon = Icons.Default.Dashboard,
                    title = "Raporlar Merkezi",
                    subtitle = "Genel bakış ve kullanım raporlarını tek yerden aç",
                    onClick = onNavigateToReportsCenter,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsButtonRow(
                    icon = Icons.Default.Notifications,
                    title = "Bildirim Raporu",
                    subtitle = "Çok konuşan, rahatsız eden ve dikkat dağıtan uygulamaları aç",
                    onClick = onNavigateToNotificationReport,
                )
            }
        }

        // ── Görevler & Yıldızlar (D257 gamification) ─────────────────────────
        item { SettingsSectionTitle(stringResource(R.string.missions_settings_section)) }
        item {
            var missionsEnabled by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_MISSIONS_ENABLED,
                read = { AppPrefs.isMissionsEnabled(context) }
            )
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.Star,
                    title = stringResource(R.string.missions_settings_toggle_title),
                    subtitle = stringResource(R.string.missions_settings_toggle_subtitle),
                    checked = missionsEnabled,
                    onCheckedChange = {
                        missionsEnabled = it
                        AppPrefs.setMissionsEnabled(context, it)
                    },
                )
                if (missionsEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.EmojiEvents,
                        title = stringResource(R.string.missions_settings_open_title),
                        subtitle = stringResource(R.string.missions_settings_open_subtitle),
                        onClick = onNavigateToMissions,
                    )
                    // Dongu G5 — Kutlama & Mikro-etkilesim: gorev sistemi kapaliyken bu toggle
                    // zaten anlamsiz (gorevler hic gorunmuyor), bu yuzden SADECE missionsEnabled
                    // acikken gosterilir.
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    var celebrationsEnabled by rememberBooleanPreferenceState(
                        context = context,
                        key = AppPrefs.KEY_MISSION_CELEBRATIONS,
                        read = { AppPrefs.isMissionCelebrationsEnabled(context) }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.missions_settings_celebrations_title),
                        subtitle = stringResource(R.string.missions_settings_celebrations_subtitle),
                        checked = celebrationsEnabled,
                        onCheckedChange = {
                            celebrationsEnabled = it
                            AppPrefs.setMissionCelebrationsEnabled(context, it)
                        },
                    )

                    // Zaman-Kisitli Gorev — DAILY_NO_LATE_NIGHT'in kullanici-tanimli saat
                    // araligina genellenmis hali. Kapaliyken TYPE_NO_USAGE_IN_TIME_WINDOW gorev
                    // havuzuna girmez (MissionEngine.isEligible()).
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    var timeWindowMissionEnabled by rememberBooleanPreferenceState(
                        context = context,
                        key = AppPrefs.KEY_TIME_WINDOW_MISSION_ENABLED,
                        read = { AppPrefs.isTimeWindowMissionEnabled(context) }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Default.NightsStay,
                        title = stringResource(R.string.missions_settings_time_window_title),
                        subtitle = stringResource(R.string.missions_settings_time_window_subtitle),
                        checked = timeWindowMissionEnabled,
                        onCheckedChange = {
                            timeWindowMissionEnabled = it
                            AppPrefs.setTimeWindowMissionEnabled(context, it)
                        },
                    )
                    if (timeWindowMissionEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        var timeWindowStartHour by remember { mutableIntStateOf(AppPrefs.getTimeWindowStartHour(context)) }
                        var timeWindowEndHour by remember { mutableIntStateOf(AppPrefs.getTimeWindowEndHour(context)) }
                        var startHourMenuExpanded by remember { mutableStateOf(false) }
                        var endHourMenuExpanded by remember { mutableStateOf(false) }
                        val hourOptions = (0..23).toList()

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { startHourMenuExpanded = true }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.missions_settings_time_window_start_label),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                        )
                                        Text(
                                            "%02d:00".format(timeWindowStartHour),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(expanded = startHourMenuExpanded, onDismissRequest = { startHourMenuExpanded = false }) {
                                    hourOptions.forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text("%02d:00".format(hour)) },
                                            onClick = {
                                                timeWindowStartHour = hour
                                                AppPrefs.setTimeWindowStartHour(context, hour)
                                                startHourMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { endHourMenuExpanded = true }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.missions_settings_time_window_end_label),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                        )
                                        Text(
                                            "%02d:00".format(timeWindowEndHour),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(expanded = endHourMenuExpanded, onDismissRequest = { endHourMenuExpanded = false }) {
                                    hourOptions.forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text("%02d:00".format(hour)) },
                                            onClick = {
                                                timeWindowEndHour = hour
                                                AppPrefs.setTimeWindowEndHour(context, hour)
                                                endHourMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bugün Yüklenenler (EX01, kullanıcı talebi) ───────────────────────
        item { SettingsSectionTitle(stringResource(R.string.recent_installs_settings_section)) }
        item {
            var recentInstallsEnabled by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_RECENT_INSTALLS_ENABLED,
                read = { AppPrefs.isRecentInstallsEnabled(context) }
            )
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.NewReleases,
                    title = stringResource(R.string.recent_installs_settings_toggle_title),
                    subtitle = stringResource(R.string.recent_installs_settings_toggle_subtitle),
                    checked = recentInstallsEnabled,
                    onCheckedChange = {
                        recentInstallsEnabled = it
                        AppPrefs.setRecentInstallsEnabled(context, it)
                    },
                )
            }
        }

        // ── Haftalık Rapor (Wrapped) ayarı ──────────────────────────────────
        item { SettingsSectionTitle(stringResource(R.string.settings_stats_weekly_report_section)) }
        item {
            var wrappedEnabled by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_WRAPPED_ENABLED,
                read = { AppPrefs.isWrappedEnabled(context) }
            )
            var wrappedAiCoachEnabled by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_WRAPPED_AI_COACH_ENABLED,
                read = { AppPrefs.isWrappedAiCoachEnabled(context) }
            )
            var goalsEnabled by rememberBooleanPreferenceState(
                context = context,
                key = AppPrefs.KEY_GOALS_ENABLED,
                read = { AppPrefs.isGoalsEnabled(context) }
            )
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.EmojiEvents,
                    title = stringResource(R.string.settings_stats_weekly_report_title),
                    subtitle = stringResource(R.string.settings_stats_weekly_report_desc),
                    checked = wrappedEnabled,
                    onCheckedChange = {
                        wrappedEnabled = it
                        AppPrefs.setWrappedEnabled(context, it)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSwitchRow(
                    icon = Icons.Default.Psychology,
                    title = "AI Koçu Haftalık Yorumu",
                    subtitle = "Varsayılan kapalı. Açılırsa yalnız agregat Wrapped skorun DeepSeek'e gider; uygulama adı ve paket adı gönderilmez.",
                    checked = wrappedAiCoachEnabled,
                    onCheckedChange = {
                        wrappedAiCoachEnabled = it
                        AppPrefs.setWrappedAiCoachEnabled(context, it)
                    },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsSwitchRow(
                    icon = Icons.Default.Flag,
                    title = "Haftalik Hedef Sistemi",
                    subtitle = "Dashboard'da kategori bazli haftalik dakika hedefleri belirle ve ilerlemeyi izle.",
                    checked = goalsEnabled,
                    onCheckedChange = {
                        goalsEnabled = it
                        AppPrefs.setGoalsEnabled(context, it)
                    },
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
