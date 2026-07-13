package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.armutlu.apporganizer.presentation.viewmodel.NotificationReportUiState
import com.armutlu.apporganizer.presentation.viewmodel.NotificationReportViewModel
import com.armutlu.apporganizer.utils.NotificationAnalyzer

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
        when (val s = state) {
            is NotificationReportUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is NotificationReportUiState.PermissionMissing -> ReportStatusPane(
                padding = padding,
                icon = Icons.Default.NotificationsOff,
                title = stringResource(R.string.notif_report_perm_title),
                description = stringResource(R.string.notif_report_perm_desc),
                buttonText = stringResource(R.string.notif_report_perm_btn),
                onButtonClick = { openNotificationListenerSettings(context) }
            )

            is NotificationReportUiState.AnalyticsDisabled -> ReportStatusPane(
                padding = padding,
                icon = Icons.Default.NotificationsOff,
                title = stringResource(R.string.notif_report_disabled_title),
                description = stringResource(R.string.notif_report_disabled_desc),
                buttonText = stringResource(R.string.notif_report_disabled_btn),
                onButtonClick = { viewModel.enableAnalytics() }
            )

            is NotificationReportUiState.CollectingData -> ReportStatusPane(
                padding = padding,
                icon = Icons.Default.HourglassEmpty,
                title = stringResource(R.string.notif_report_collecting_title),
                description = stringResource(R.string.notif_report_collecting_desc),
                buttonText = null,
                onButtonClick = null
            )

            is NotificationReportUiState.Error -> ReportStatusPane(
                padding = padding,
                icon = Icons.Default.Warning,
                title = "Rapor yuklenemedi",
                description = s.message,
                buttonText = "Tekrar dene",
                onButtonClick = { viewModel.refresh() }
            )

            is NotificationReportUiState.Ready -> ReportContent(
                padding = padding,
                state = s,
                onGrantPermission = { openNotificationListenerSettings(context) },
                onEnableAnalytics = { viewModel.enableAnalytics() }
            )
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
    onGrantPermission: () -> Unit,
    onEnableAnalytics: () -> Unit
) {
    val r = state.report
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        item { SummaryCard(r) }

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
private fun SummaryCard(report: NotificationAnalyzer.Report) {
    val topApp = report.mostTalkative.firstOrNull()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.notif_report_summary_period),
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

@Composable
private fun TalkativeRow(stat: NotificationAnalyzer.AppNotifStats) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openAppInfoSettings(context, stat.packageName) }
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
                stat.dailyCounts.forEach { count ->
                    val fraction = count.toFloat() / maxCount
                    val height = (24.dp * fraction).coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(height)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
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
            .clickable { openAppInfoSettings(context, stat.packageName) }
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
            .clickable { openAppInfoSettings(context, stat.packageName) }
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
