package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.NotificationHistoryEntity
import com.armutlu.apporganizer.presentation.navigation.NotificationReportLaunchContract
import com.armutlu.apporganizer.presentation.viewmodel.NotificationHistoryUiState
import timber.log.Timber
import com.armutlu.apporganizer.presentation.viewmodel.NotificationReportUiState
import com.armutlu.apporganizer.presentation.viewmodel.NotificationReportViewModel
import com.armutlu.apporganizer.presentation.viewmodel.NotificationReportRange
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Bildirim Raporu — sealed UI-state ile net durum ayrımı (Döngü 224):
 * izin kapalı / analiz kapalı / veri toplanıyor / rapor. Her boş durum kendi
 * açıklaması ve eylem butonuyla gösterilir; sistem ayarından dönüşte ON_RESUME
 * ile otomatik yenilenir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationReportScreen(
    viewModel: NotificationReportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val initialTab = remember(context) {
        NotificationReportLaunchContract.consumeInitialTab(context)
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val selectedRange by viewModel.range.collectAsState()
    var customPickerTarget by remember { mutableStateOf<CustomPickerTarget?>(null) }
    var customStartMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    // D257: "Bildirim raporunu incele" gorevi — ekran ziyareti gorev tamamlama sayilir.
    LaunchedEffect(Unit) {
        com.armutlu.apporganizer.utils.TaskScoreManager.record(
            context,
            com.armutlu.apporganizer.utils.TaskScoreManager.EventType.NotificationReportViewed,
        )
    }

    // Sistem izin ekranından dönüşte raporu yenile — kullanıcı izni verip
    // geri geldiğinde "izin gerekli" ekranında takılı kalmasın.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notif_report_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.notif_report_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.notif_report_tab_report)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.notif_report_tab_history)) },
                )
            }
            if (selectedTab == 0) {
                when (val s = state) {
                    is NotificationReportUiState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    is NotificationReportUiState.PermissionMissing -> ReportStatusPane(
                        padding = PaddingValues(0.dp),
                        icon = Icons.Default.NotificationsOff,
                        title = stringResource(R.string.notif_report_perm_title),
                        description = stringResource(R.string.notif_report_perm_desc),
                        buttonText = stringResource(R.string.notif_report_perm_btn),
                        onButtonClick = { openNotificationListenerSettings(context) }
                    )

                    is NotificationReportUiState.AnalyticsDisabled -> ReportStatusPane(
                        padding = PaddingValues(0.dp),
                        icon = Icons.Default.NotificationsOff,
                        title = stringResource(R.string.notif_report_disabled_title),
                        description = stringResource(R.string.notif_report_disabled_desc),
                        buttonText = stringResource(R.string.notif_report_disabled_btn),
                        onButtonClick = { viewModel.enableAnalytics() }
                    )

                    is NotificationReportUiState.CollectingData -> ReportStatusPane(
                        padding = PaddingValues(0.dp),
                        icon = Icons.Default.HourglassEmpty,
                        title = stringResource(R.string.notif_report_collecting_title),
                        description = stringResource(R.string.notif_report_collecting_desc),
                        buttonText = null,
                        onButtonClick = null
                    )

                    is NotificationReportUiState.Error -> ReportStatusPane(
                        padding = PaddingValues(0.dp),
                        icon = Icons.Default.Warning,
                        title = "Rapor yuklenemedi",
                        description = s.message,
                        buttonText = "Tekrar dene",
                        onButtonClick = { viewModel.refresh() }
                    )

                    is NotificationReportUiState.Ready -> ReportContent(
                        padding = PaddingValues(0.dp),
                        state = s,
                        selectedRange = selectedRange,
                        onGrantPermission = { openNotificationListenerSettings(context) },
                        onEnableAnalytics = { viewModel.enableAnalytics() },
                        onRangeSelected = { range ->
                            if (range == NotificationReportRange.CUSTOM) {
                                customPickerTarget = CustomPickerTarget.START
                            } else {
                                viewModel.setRange(range)
                            }
                        },
                        onExport = { report ->
                            shareNotificationReport(context, report, reportRangeLabel(selectedRange))
                        },
                        onExportAi = { report ->
                            shareAiNotificationData(context, report, reportRangeLabel(selectedRange))
                        },
                    )
                }
            } else {
                NotificationHistoryTab(viewModel = viewModel)
            }
        }
    }

    customPickerTarget?.let { target ->
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { customPickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = pickerState.selectedDateMillis ?: return@TextButton
                        if (target == CustomPickerTarget.START) {
                            customStartMillis = selected
                            customPickerTarget = CustomPickerTarget.END
                        } else {
                            customStartMillis?.let { start -> viewModel.setCustomRange(start, selected) }
                            customPickerTarget = null
                        }
                    },
                ) { Text(stringResource(R.string.notif_report_date_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { customPickerTarget = null }) {
                    Text(stringResource(R.string.notif_report_date_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private enum class CustomPickerTarget { START, END }

private fun reportRangeLabel(range: NotificationReportRange): String = when (range) {
    NotificationReportRange.LAST_24_HOURS -> "Son 24 saat"
    NotificationReportRange.LAST_7_DAYS -> "Son 7 gün"
    NotificationReportRange.CUSTOM -> "Özel tarih"
}

@Composable
private fun NotificationHistoryTab(viewModel: NotificationReportViewModel) {
    val historyState by viewModel.historyUiState.collectAsState()
    val historyEnabled = viewModel.historyEnabled
    var pendingDelete by remember { mutableStateOf<NotificationHistoryEntity?>(null) }

    if (!historyEnabled) {
        ReportStatusPane(
            padding = PaddingValues(0.dp),
            icon = Icons.Default.NotificationsOff,
            title = stringResource(R.string.notif_history_disabled_title),
            description = stringResource(R.string.notif_history_disabled_desc),
            buttonText = null,
            onButtonClick = null,
        )
        return
    }

    if (historyState.totalCount == 0) {
        ReportStatusPane(
            padding = PaddingValues(0.dp),
            icon = Icons.Default.HourglassEmpty,
            title = stringResource(R.string.notif_history_empty_title),
            description = stringResource(R.string.notif_history_empty_desc),
            buttonText = null,
            onButtonClick = null,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.notif_history_filter_hint),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
        )
        Text(
            text = stringResource(R.string.notif_history_long_press_hint),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.notif_history_retention_hint),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = historyState.selectedPackageName == null,
                    onClick = { viewModel.selectHistoryPackage(null) },
                    label = {
                        Text(stringResource(R.string.notif_history_filter_all, historyState.totalCount))
                    },
                )
            }
            items(historyState.filters, key = { it.packageName }) { filter ->
                FilterChip(
                    selected = historyState.selectedPackageName == filter.packageName,
                    onClick = { viewModel.selectHistoryPackage(filter.packageName) },
                    label = {
                        Text(stringResource(R.string.notif_history_filter_app, filter.appName, filter.count))
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(historyState.entries, key = { it.id }) { entry ->
                val appName = NotificationHistoryUiState.resolveAppName(
                    entry.packageName,
                    historyState.appNames,
                )
                NotificationHistoryRow(
                    entry = entry,
                    appName = appName,
                    onClick = { viewModel.markHistoryRead(entry.id) },
                    onLongClick = { pendingDelete = entry },
                )
            }
        }
    }

    pendingDelete?.let { entry ->
        val appName = NotificationHistoryUiState.resolveAppName(
            entry.packageName,
            historyState.appNames,
        )
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.notif_history_delete_title)) },
            text = {
                Text(stringResource(R.string.notif_history_delete_desc, appName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHistory(entry.id)
                        pendingDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.notif_history_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.notif_history_delete_cancel))
                }
            },
        )
    }
}

private val historyTimeFormat = SimpleDateFormat("dd MMM HH:mm", Locale("tr"))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationHistoryRow(
    entry: NotificationHistoryEntity,
    appName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (entry.isRead) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    appName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    historyTimeFormat.format(java.util.Date(entry.postedAt)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                entry.packageName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                entry.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (entry.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.text,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

private fun openNotificationListenerSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Tam-ekran durum paneli — ikon + başlık + açıklama + opsiyonel eylem butonu. */
@Composable
private fun ReportStatusPane(
    padding: PaddingValues,
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String?,
    onButtonClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (buttonText != null && onButtonClick != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onButtonClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun ReportContent(
    padding: PaddingValues,
    state: NotificationReportUiState.Ready,
    selectedRange: NotificationReportRange,
    onGrantPermission: () -> Unit,
    onEnableAnalytics: () -> Unit,
    onRangeSelected: (NotificationReportRange) -> Unit,
    onExport: (NotificationAnalyzer.Report) -> Unit,
    onExportAi: (NotificationAnalyzer.Report) -> Unit,
) {
    val r = state.report
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NotificationReportRangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = onRangeSelected,
            )
        }
        if (state.permissionMissing) {
            item {
                WarningBanner(
                    text = stringResource(R.string.notif_report_perm_banner),
                    buttonText = stringResource(R.string.notif_report_perm_banner_btn),
                    onClick = onGrantPermission
                )
            }
        }
        if (state.analyticsDisabled) {
            item {
                WarningBanner(
                    text = stringResource(R.string.notif_report_paused_banner),
                    buttonText = stringResource(R.string.notif_report_disabled_btn),
                    onClick = onEnableAnalytics
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = { onExport(r) }) {
                    Text(stringResource(R.string.notif_report_export_txt))
                }
                Button(onClick = { onExportAi(r) }) {
                    Text("AI Dışarı Aktar")
                }
            }
        }
        item { SummaryCard(r, reportRangeLabel(selectedRange)) }
        item { NotificationReportV2Overview(r) }

        item { SectionTitle(stringResource(R.string.notif_report_section_talkative)) }
        // Aynı paket birden fazla bölümde olabilir — LazyColumn key'leri tüm liste
        // genelinde benzersiz olmalı, bölüm öneki olmadan duplicate key crash'i oluşur.
        items(r.mostTalkative, key = { "talkative_${it.packageName}" }) { stat -> TalkativeRow(stat) }

        item { SectionTitle(stringResource(R.string.notif_report_section_disturbing)) }
        if (r.disturbing.isEmpty()) {
            item { EmptyStateText(stringResource(R.string.notif_report_no_disturbing)) }
        } else {
            items(r.disturbing, key = { "disturbing_${it.packageName}" }) { stat -> DisturbingRow(stat) }
        }

        item {
            Column {
                SectionTitle(stringResource(R.string.notif_report_section_distracting))
                Text(
                    stringResource(R.string.notif_report_distracting_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }
        }
        if (r.distracting.isEmpty()) {
            item { EmptyStateText(stringResource(R.string.notif_report_no_distracting)) }
        } else {
            items(r.distracting, key = { "distracting_${it.packageName}" }) { stat -> DistractingRow(stat) }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Rapor üstü uyarı bandı — durum + tek eylem butonu. */
@Composable
private fun WarningBanner(text: String, buttonText: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun NotificationReportRangeSelector(
    selectedRange: NotificationReportRange,
    onRangeSelected: (NotificationReportRange) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.notif_report_period_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedRange == NotificationReportRange.LAST_24_HOURS,
                    onClick = { onRangeSelected(NotificationReportRange.LAST_24_HOURS) },
                    label = { Text(stringResource(R.string.notif_report_period_24h)) },
                )
            }
            item {
                FilterChip(
                    selected = selectedRange == NotificationReportRange.LAST_7_DAYS,
                    onClick = { onRangeSelected(NotificationReportRange.LAST_7_DAYS) },
                    label = { Text(stringResource(R.string.notif_report_period_7d)) },
                )
            }
            item {
                FilterChip(
                    selected = selectedRange == NotificationReportRange.CUSTOM,
                    onClick = { onRangeSelected(NotificationReportRange.CUSTOM) },
                    label = { Text(stringResource(R.string.notif_report_period_custom)) },
                )
            }
        }
    }
}

private fun shareNotificationReport(
    context: Context,
    report: NotificationAnalyzer.Report,
    periodLabel: String,
) {
    runCatching {
        val text = buildString {
            appendLine("AppOrganizer - Bildirim Raporu")
            appendLine("Dönem: $periodLabel")
            appendLine("Toplam bildirim: ${report.totalNotifications}")
            appendLine("Eyleme değer: ${report.actionableCount}")
            appendLine("Bastırılan: ${report.suppressedCount}")
            appendLine()
            appendLine("En çok bildirim gönderen uygulamalar:")
            report.mostTalkative.forEach { stat ->
                appendLine("- ${stat.appName}: ${stat.total}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AppOrganizer Bildirim Raporu - $periodLabel")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.notif_report_export_txt)))
    }.onFailure { e ->
        Timber.e(e, "Notification report export failed")
        android.widget.Toast.makeText(context, "Dışa aktarma başarısız", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun shareAiNotificationData(
    context: Context,
    report: NotificationAnalyzer.Report,
    periodLabel: String,
) {
    runCatching {
        val text = buildString {
            appendLine("--- APPORGANIZER BİLDİRİM VERİSİ (YAPAY ZEKA ANALİZİ İÇİN) ---")
            appendLine("Dönem: $periodLabel")
            appendLine("Özet Metrics:")
            appendLine("- Toplam Bildirim: ${report.totalNotifications}")
            appendLine("- Eyleme Değer Bildirim: ${report.actionableCount}")
            appendLine("- Bastırılan Bildirim: ${report.suppressedCount}")
            appendLine()
            appendLine("Uygulama İstatistikleri:")
            report.mostTalkative.forEach { stat ->
                val distractionFormatted = String.format(java.util.Locale.US, "%.1f", stat.distractionScore)
                appendLine("App: ${stat.appName} (${stat.packageName}) | Toplam: ${stat.total} | Gece Oranı: ${(stat.nightRatio * 100).toInt()}% | Dikkat Dağıtma Skoru: $distractionFormatted")
            }
            appendLine()
            appendLine("--- VERİ SONU ---")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AppOrganizer AI Bildirim Verisi Export - $periodLabel")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Yapay Zeka İçin Dışarı Aktar"))
    }.onFailure { e ->
        Timber.e(e, "AI notification export failed")
        android.widget.Toast.makeText(context, "Dışa aktarma başarısız", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun SummaryCard(report: NotificationAnalyzer.Report, periodLabel: String) {
    val topApp = report.mostTalkative.firstOrNull()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                periodLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${report.totalNotifications}",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.notif_report_summary_total),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (topApp != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.notif_report_summary_top, topApp.appName, topApp.total),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyStateText(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private fun launchAppOrSettings(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        runCatching { context.startActivity(launchIntent) }
            .onFailure { openAppInfoSettings(context, packageName) }
    } else {
        openAppInfoSettings(context, packageName)
    }
}

@Composable
private fun TalkativeRow(stat: NotificationAnalyzer.AppNotifStats) {
    val context = LocalContext.current
    val dayInitials = remember { listOf("P", "S", "Ç", "P", "C", "C", "P") }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launchAppOrSettings(context, stat.packageName) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${stat.total}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            val maxCount = (stat.dailyCounts.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                stat.dailyCounts.forEachIndexed { index, count ->
                    val fraction = count.toFloat() / maxCount
                    val height = (24.dp * fraction).coerceAtLeast(2.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dayInitials.getOrElse(index % 7) { "" },
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisturbingRow(stat: NotificationAnalyzer.AppNotifStats) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launchAppOrSettings(context, stat.packageName) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.notif_report_night_ratio, (stat.nightRatio * 100).toInt()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    stringResource(R.string.notif_report_burst, stat.maxBurstPerHour),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DistractingRow(stat: NotificationAnalyzer.AppNotifStats) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { launchAppOrSettings(context, stat.packageName) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.notif_report_count_usage, stat.total, stat.usageMinutes),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "%.1f".format(stat.distractionScore),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
