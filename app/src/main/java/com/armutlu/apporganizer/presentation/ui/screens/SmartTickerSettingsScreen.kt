package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryManager
import com.armutlu.apporganizer.utils.AppPrefs
import java.text.DateFormat
import java.util.Date

/**
 * Döngü T05 — Akıllı Nabız Şeridi ayarları
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1848-1905).
 *
 * Ayarlar > Ana Ekran > Akıllı Nabız Şeridi. Ticker'ın uzun basma menüsündeki
 * "Akıllı Nabız ayarları" (ticker_menu_settings) de doğrudan buraya açılır.
 *
 * CLAUDE.md "Ayarlar Metin ve Kod İnceleme Kuralı": burada kullanıcıya SmartTickerType iç enum
 * adları hiçbir yerde gösterilmez — yalnızca AppPrefs.isSmartTickerTypeVisible() ile eşlenen
 * kullanıcı dili grup adları (Yapılması gerekenler, Görev uyarıları ve başarılar, ...) görünür.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTickerSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current

    var tickerEnabled by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_ENABLED) {
        AppPrefs.isTickerEnabled(context)
    }
    var actionsVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_ACTIONS) {
        AppPrefs.isSmartTickerActionsVisible(context)
    }
    var missionsVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_MISSIONS) {
        AppPrefs.isSmartTickerMissionsVisible(context)
    }
    var pulseVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_PULSE) {
        AppPrefs.isSmartTickerPulseVisible(context)
    }
    var reportsVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_REPORTS) {
        AppPrefs.isSmartTickerReportsVisible(context)
    }
    var contextualVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_CONTEXTUAL) {
        AppPrefs.isSmartTickerContextualVisible(context)
    }
    var discoveryVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_DISCOVERY) {
        AppPrefs.isSmartTickerDiscoveryVisible(context)
    }
    var healthVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_SMART_TICKER_HEALTH) {
        AppPrefs.isSmartTickerHealthVisible(context)
    }
    var autoAdvance by rememberBooleanPreferenceState(context, AppPrefs.KEY_TICKER_AUTO_ADVANCE) {
        AppPrefs.isTickerAutoAdvanceEnabled(context)
    }
    var intervalSeconds by remember { mutableStateOf(AppPrefs.getTickerIntervalSeconds(context)) }
    var sensitiveVisible by rememberBooleanPreferenceState(context, AppPrefs.KEY_TICKER_SENSITIVE_VISIBLE) {
        AppPrefs.isTickerSensitiveVisible(context)
    }
    var mutedUntil by remember { mutableStateOf(AppPrefs.getTickerMutedUntil(context)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_ticker_settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Campaign,
                        title = stringResource(R.string.smart_ticker_settings_master_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_master_desc),
                        checked = tickerEnabled,
                        onCheckedChange = {
                            tickerEnabled = it
                            AppPrefs.setTickerEnabled(context, it)
                        },
                    )
                }
            }

            // ── Gösterilecek içerikler — roadmap mock satır 1857-1864 ─────────
            item { SettingsSectionTitle(stringResource(R.string.smart_ticker_settings_content_section)) }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.CheckCircle,
                        title = stringResource(R.string.smart_ticker_settings_actions_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_actions_desc),
                        checked = actionsVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            actionsVisible = it
                            AppPrefs.setSmartTickerActionsVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.ACTION_REQUIRED))
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.EmojiEvents,
                        title = stringResource(R.string.smart_ticker_settings_missions_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_missions_desc),
                        checked = missionsVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            missionsVisible = it
                            AppPrefs.setSmartTickerMissionsVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.MISSION_PROGRESS))
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.smart_ticker_settings_pulse_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_pulse_desc),
                        checked = pulseVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            pulseVisible = it
                            AppPrefs.setSmartTickerPulseVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.PULSE_CHANGE))
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.smart_ticker_settings_reports_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_reports_desc),
                        checked = reportsVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            reportsVisible = it
                            AppPrefs.setSmartTickerReportsVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.WEEKLY_REPORT))
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Lightbulb,
                        title = stringResource(R.string.smart_ticker_settings_contextual_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_contextual_desc),
                        checked = contextualVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            contextualVisible = it
                            AppPrefs.setSmartTickerContextualVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.CONTEXTUAL_SUGGESTION))
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Lightbulb,
                        title = stringResource(R.string.smart_ticker_settings_discovery_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_discovery_desc),
                        checked = discoveryVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            discoveryVisible = it
                            AppPrefs.setSmartTickerDiscoveryVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.FEATURE_DISCOVERY))
                        },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.smart_ticker_settings_health_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_health_desc),
                        checked = healthVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            healthVisible = it
                            AppPrefs.setSmartTickerHealthVisible(context, it)
                            if (!it) TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(TelemetryEvent.TickerItemType.CRITICAL_HEALTH))
                        },
                    )
                }
            }

            // ── Akış davranışı — otomatik geçiş + süre + hassas bilgi ─────────
            item { SettingsSectionTitle(stringResource(R.string.smart_ticker_settings_behavior_section)) }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Speed,
                        title = stringResource(R.string.smart_ticker_settings_auto_advance_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_auto_advance_desc),
                        checked = autoAdvance,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            autoAdvance = it
                            AppPrefs.setTickerAutoAdvanceEnabled(context, it)
                        },
                    )
                    if (autoAdvance) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row {
                                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text(stringResource(R.string.smart_ticker_settings_interval_title), fontWeight = FontWeight.Medium)
                                    Text(
                                        stringResource(R.string.smart_ticker_settings_interval_desc, intervalSeconds),
                                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Slider(
                                value = intervalSeconds.toFloat(),
                                onValueChange = {
                                    intervalSeconds = it.toInt()
                                    AppPrefs.setTickerIntervalSeconds(context, it.toInt())
                                },
                                valueRange = 5f..20f,
                                steps = 14,
                                enabled = tickerEnabled,
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Visibility,
                        title = stringResource(R.string.smart_ticker_settings_sensitive_title),
                        subtitle = stringResource(R.string.smart_ticker_settings_sensitive_desc),
                        checked = sensitiveVisible,
                        enabled = tickerEnabled,
                        onCheckedChange = {
                            sensitiveVisible = it
                            AppPrefs.setTickerSensitiveVisible(context, it)
                        },
                    )
                }
            }

            // ── Sessiz saatler — mevcut basılı-tut mute mekanizmasının durumu ─
            item { SettingsSectionTitle(stringResource(R.string.smart_ticker_settings_quiet_section)) }
            item {
                SettingsCard {
                    val isMuted = mutedUntil > System.currentTimeMillis()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = true)) {
                            Text(
                                if (isMuted) {
                                    stringResource(
                                        R.string.smart_ticker_settings_quiet_active,
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(mutedUntil)),
                                    )
                                } else {
                                    stringResource(R.string.smart_ticker_settings_quiet_inactive)
                                },
                                fontWeight = FontWeight.Medium,
                            )
                            if (isMuted) {
                                TextButton(onClick = {
                                    AppPrefs.clearTickerMutedUntil(context)
                                    mutedUntil = 0L
                                }) {
                                    Text(stringResource(R.string.smart_ticker_settings_quiet_clear_action))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)) {
                    Text(
                        stringResource(R.string.smart_ticker_settings_hidden_types_note),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
