package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.presentation.viewmodel.DiagnosticsReportViewModel
import com.armutlu.apporganizer.presentation.viewmodel.PulseClockViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REPORT_ROUTE_DASHBOARD = "dashboard"
private const val REPORT_ROUTE_USAGE = "usage_report"
private const val REPORT_ROUTE_NOTIFICATION = "notification_report"
private const val REPORT_ROUTE_DIAGNOSTICS = "diagnostics_report"
private const val REPORT_ROUTE_WRAPPED = "wrapped_report"
private const val REPORT_ROUTE_PRIVACY = "privacy_report"

internal data class ReportsCenterEntry(
    val route: String,
    val title: String,
    val description: String,
    val dataPeriod: String,
    val lastUpdated: String,
    val unavailableReason: String? = null,
) {
    val isAvailable: Boolean
        get() = unavailableReason == null
}

internal fun buildReportsCenterEntries(
    apps: List<AppInfo>,
    categories: List<Category>,
    wrappedEnabled: Boolean,
    privacyReportEnabled: Boolean,
    diagnosticsGenerating: Boolean,
    nowMs: Long,
    context: Context? = null,
): List<ReportsCenterEntry> {
    val appCount = apps.size
    val categoryCount = categories.size
    val hiddenCount = apps.count { it.isHidden }
    val latestUsage = apps.maxOfOrNull { it.lastUsedTimestamp }
    val notificationTrackedApps = apps.count { it.notificationCount > 0 }

    val usageTitle = context?.getString(R.string.reports_center_entry_usage_title)
        ?: "Uygulama Duzeni"
    val usageDesc = context?.getString(
        R.string.reports_center_entry_usage_desc,
        appCount,
        categoryCount,
        hiddenCount,
    ) ?: "$appCount uygulama, $categoryCount kategori ve $hiddenCount gizli uygulama ozetlenir. " +
        "En cok kullanilanlar, hic acilmayanlar ve gizleme onerileri tek ekranda."
    val wrappedTitle = context?.getString(R.string.reports_center_entry_wrapped_title)
        ?: "Haftalik Ozet"
    val diagnosticsTitle = if (diagnosticsGenerating) {
        context?.getString(R.string.reports_center_entry_diagnostics_title_generating)
            ?: "Teknik Tanilama hazirlaniyor"
    } else {
        context?.getString(R.string.reports_center_entry_diagnostics_title)
            ?: "Teknik Tanilama"
    }

    return listOf(
        ReportsCenterEntry(
            route = REPORT_ROUTE_USAGE,
            title = usageTitle,
            description = usageDesc,
            dataPeriod = "Veri donemi: son 30 gun",
            lastUpdated = "Son kullanim: ${formatReportTimestamp(latestUsage, nowMs)}",
        ),
        ReportsCenterEntry(
            route = REPORT_ROUTE_NOTIFICATION,
            title = "Bildirim Raporu",
            description = "Bildirim yogunlugu ve dikkat dagitan uygulamalar gorunur.",
            dataPeriod = "Veri donemi: son 24 saat ve son 7 gun sinyalleri",
            lastUpdated = if (notificationTrackedApps > 0) {
                "Son guncelleme: $notificationTrackedApps uygulama icin bildirim verisi var"
            } else {
                "Bos durum: henuz bildirim verisi toplanmadi"
            },
        ),
        ReportsCenterEntry(
            route = REPORT_ROUTE_DIAGNOSTICS,
            title = diagnosticsTitle,
            description = "Paylasilabilir, kisisel veriden arindirilmis TXT tanilama raporu uretir.",
            dataPeriod = "Veri donemi: o anki cihaz ve uygulama durumu",
            lastUpdated = if (diagnosticsGenerating) {
                "Son guncelleme: rapor simdi uretiliyor"
            } else {
                "Son guncelleme: istege bagli uretilir"
            },
        ),
        ReportsCenterEntry(
            route = REPORT_ROUTE_WRAPPED,
            title = wrappedTitle,
            description = "Dijital yasam skoru, kisilik tipi ve rozetlerin haftalik ozeti.",
            dataPeriod = "Veri donemi: son 7 gun",
            lastUpdated = if (wrappedEnabled) {
                "Son guncelleme: haftalik veri hazir oldugunda acilir"
            } else {
                "Bos durum: haftalik rapor ayarlardan kapali"
            },
            unavailableReason = if (wrappedEnabled) null else "Haftalik rapor ayarlardan acilmadan gosterilemez."
        ),
        ReportsCenterEntry(
            route = REPORT_ROUTE_PRIVACY,
            title = "Gizlilik Analizi",
            description = "Kamera, mikrofon ve konum gibi hassas izinler cihaz ustunde analiz edilir.",
            dataPeriod = "Veri donemi: mevcut kurulu uygulamalar ve izinleri",
            lastUpdated = if (privacyReportEnabled) {
                "Son guncelleme: ekran acildiginda anlik tarama"
            } else {
                "Bos durum: gizlilik analizi ayarlardan kapali"
            },
            unavailableReason = if (privacyReportEnabled) null else "Gizlilik analizi ayarlardan etkinlestirilmeden acilmaz."
        ),
    )
}

internal fun formatReportTimestamp(timestampMs: Long?, nowMs: Long): String {
    if (timestampMs == null || timestampMs <= 0L) return "veri yok"
    val diffMs = (nowMs - timestampMs).coerceAtLeast(0L)
    val hourMs = 60L * 60L * 1000L
    val dayMs = 24L * hourMs
    return when {
        diffMs < hourMs -> "son 1 saat icinde"
        diffMs < dayMs -> "bugun"
        diffMs < 2 * dayMs -> "dun"
        diffMs < 7 * dayMs -> "${diffMs / dayMs} gun once"
        else -> SimpleDateFormat("d MMM", Locale("tr")).format(Date(timestampMs))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsCenterScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsageReport: () -> Unit,
    onNavigateToNotificationReport: () -> Unit = {},
    onNavigateToWrappedReport: () -> Unit = {},
    onNavigateToPrivacyReport: () -> Unit = {},
) {
    val context = LocalContext.current
    val pulseViewModel: PulseClockViewModel = hiltViewModel()
    val diagnosticsViewModel: DiagnosticsReportViewModel = hiltViewModel()
    val pulseState by pulseViewModel.uiState.collectAsState()
    val diagnosticsState by diagnosticsViewModel.uiState.collectAsState()
    val wrappedEnabled = AppPrefs.isWrappedEnabled(context)
    val privacyReportEnabled = AppPrefs.isPrivacyReportEnabled(context)
    val screenState by viewModel.screenState.collectAsState()
    val appCount = screenState.apps.size
    val categoryCount = screenState.categories.size
    val hiddenCount = screenState.apps.count { it.isHidden }
    val userAppCount = screenState.apps.count { !it.isSystemApp }
    val nowMs = System.currentTimeMillis()
    val summaryTitle = if (appCount == 0 && categoryCount == 0) {
        "Veriler yukleniyor"
    } else {
        "$appCount uygulama / $categoryCount kategori"
    }
    val summarySubtitle = "$userAppCount kullanici uygulamasi / $hiddenCount gizli uygulama"
    val reportEntries = buildReportsCenterEntries(
        apps = screenState.apps,
        categories = screenState.categories,
        wrappedEnabled = wrappedEnabled,
        privacyReportEnabled = privacyReportEnabled,
        diagnosticsGenerating = diagnosticsState.isGenerating,
        nowMs = nowMs,
        context = context,
    )

    LaunchedEffect(diagnosticsState.shareIntent) {
        val intent = diagnosticsState.shareIntent ?: return@LaunchedEffect
        context.startActivity(Intent.createChooser(intent, "Saglik raporunu paylas"))
        diagnosticsViewModel.consumeShareIntent()
    }

    LaunchedEffect(diagnosticsState.errorMessage) {
        val message = diagnosticsState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        diagnosticsViewModel.consumeError()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Raporlar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
            item { SettingsSectionTitle("Rapor Merkezi") }
            item {
                SettingsCard {
                    ReportsPulseSummaryCard(
                        summaryTitle = summaryTitle,
                        summarySubtitle = summarySubtitle,
                        pulseState = pulseState,
                    )
                }
            }
            item {
                SettingsCard {
                    reportEntries.forEachIndexed { index, entry ->
                        ReportsCenterEntryRow(
                            entry = entry,
                            icon = entry.icon(),
                            onClick = {
                                when (entry.route) {
                                    REPORT_ROUTE_DASHBOARD -> onNavigateToDashboard()
                                    REPORT_ROUTE_USAGE -> onNavigateToUsageReport()
                                    REPORT_ROUTE_NOTIFICATION -> onNavigateToNotificationReport()
                                    REPORT_ROUTE_DIAGNOSTICS -> diagnosticsViewModel.generateReport()
                                    REPORT_ROUTE_WRAPPED -> onNavigateToWrappedReport()
                                    REPORT_ROUTE_PRIVACY -> onNavigateToPrivacyReport()
                                }
                            },
                        )
                        if (index != reportEntries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsCenterEntryRow(
    entry: ReportsCenterEntry,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val subtitle = buildString {
        append(entry.description)
        append("\n")
        append(entry.dataPeriod)
        append("\n")
        append(entry.lastUpdated)
        entry.unavailableReason?.let {
            append("\n")
            append("Neden bos: ")
            append(it)
        }
    }
    if (entry.isAvailable) {
        SettingsButtonRow(
            icon = icon,
            title = entry.title,
            subtitle = subtitle,
            onClick = onClick,
        )
    } else {
        SettingsInfoRow(
            icon = icon,
            title = entry.title,
            subtitle = subtitle,
        )
    }
}

private fun ReportsCenterEntry.icon(): ImageVector = when (route) {
    REPORT_ROUTE_DASHBOARD -> Icons.Default.Dashboard
    REPORT_ROUTE_USAGE -> Icons.Default.BarChart
    REPORT_ROUTE_NOTIFICATION -> Icons.Default.NotificationsActive
    REPORT_ROUTE_DIAGNOSTICS -> Icons.Default.Description
    REPORT_ROUTE_WRAPPED -> Icons.Default.EmojiEvents
    REPORT_ROUTE_PRIVACY -> Icons.Default.Security
    else -> Icons.Default.FolderOpen
}

@Composable
private fun ReportsPulseSummaryCard(
    summaryTitle: String,
    summarySubtitle: String,
    pulseState: PulseClockViewModel.PulseClockUiState,
) {
    val pulse = pulseState.subScores
    val scoreText = pulseState.score?.toString() ?: "--"
    val confidenceText = when (pulseState.confidence) {
        DataConfidence.HIGH -> "Yuksek guven"
        DataConfidence.MEDIUM -> "Orta guven"
        DataConfidence.LOW -> "Dusuk guven"
    }
    val scorePairs = pulse?.pulseScorePairs().orEmpty()
    val strongest = scorePairs.maxByOrNull { it.second }
    val weakest = scorePairs.minByOrNull { it.second }
    val recommendation = pulseState.insightText ?: "Veri birikiyor; raporlar kullanimla netlesir."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summaryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = summarySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = confidenceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = recommendation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (scorePairs.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            strongest?.let {
                ReportsMiniSignal(label = "Guclu alan", value = "${it.first} ${it.second}/100")
            }
            weakest?.let {
                ReportsMiniSignal(label = "Zayif alan", value = "${it.first} ${it.second}/100")
            }
            Spacer(Modifier.height(8.dp))
            scorePairs.forEach { (label, value) ->
                ReportsScoreProgress(label = label, value = value)
            }
        }
    }
}

private fun DigitalPulseScore.pulseScorePairs(): List<Pair<String, Int>> = listOf(
    "Duzen" to organization,
    "Dikkat" to attention,
    "Denge" to balance,
    "Temizlik" to cleanup,
    "Istikrar" to consistency,
)

@Composable
private fun ReportsMiniSignal(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReportsScoreProgress(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
