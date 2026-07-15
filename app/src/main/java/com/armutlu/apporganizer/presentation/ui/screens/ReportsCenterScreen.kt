package com.armutlu.apporganizer.presentation.ui.screens

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
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence
import com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.presentation.viewmodel.DiagnosticsReportViewModel
import com.armutlu.apporganizer.presentation.viewmodel.PulseClockViewModel
import com.armutlu.apporganizer.utils.AppPrefs

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
    val summaryTitle = if (appCount == 0 && categoryCount == 0) {
        "Veriler yukleniyor"
    } else {
        "$appCount uygulama / $categoryCount kategori"
    }
    val summarySubtitle = "$userAppCount kullanici uygulamasi / $hiddenCount gizli uygulama"

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
                    SettingsButtonRow(
                        icon = Icons.Default.Dashboard,
                        title = "Genel Bakis",
                        subtitle = "Klasor, kategori, gizli uygulama ve verimlilik ozetleri",
                        onClick = onNavigateToDashboard,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.BarChart,
                        title = "Kullanim Raporu",
                        subtitle = "En cok kullanilanlar, hic acilmayanlar ve gizleme onerileri",
                        onClick = onNavigateToUsageReport,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Bildirim Raporu",
                        subtitle = "Hangi uygulama ne kadar bildirim gonderiyor gorebilirsin",
                        onClick = onNavigateToNotificationReport,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.Description,
                        title = if (diagnosticsState.isGenerating) "Saglik raporu hazirlaniyor" else "Saglik Raporu",
                        subtitle = "Paylasilabilir, kisisel veriden arindirilmis TXT tanilama raporu",
                        onClick = { diagnosticsViewModel.generateReport() },
                    )
                }
            }
            if (wrappedEnabled) {
                item {
                    SettingsCard {
                        SettingsButtonRow(
                            icon = Icons.Default.EmojiEvents,
                            title = "🎁 Haftalik Rapor",
                            subtitle = "Dijital yasam skorun, kisilik tipin ve rozetlerin",
                            onClick = onNavigateToWrappedReport,
                        )
                    }
                }
            }
            if (privacyReportEnabled) {
                item {
                    SettingsCard {
                        SettingsButtonRow(
                            icon = Icons.Default.Security,
                            title = "🔐 Gizlilik Analizi",
                            subtitle = "Hangi uygulama kamera, mikrofon, konuma erisebiliyor",
                            onClick = onNavigateToPrivacyReport,
                        )
                    }
                }
            }
            item { SettingsSectionTitle("Hizli Erisim") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Klasor ve Kategori Yogunlugu",
                        subtitle = "Kategori dagilimi ve aktif klasor odagi",
                        onClick = onNavigateToDashboard,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Gizli ve Kullanilmayanlar",
                        subtitle = "Gizli uygulamalar ve uzun sure acilmayanlar",
                        onClick = onNavigateToUsageReport,
                    )
                }
            }
        }
    }
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
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
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
