package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.usecase.privacy.PrivacyAnalyzer
import com.armutlu.apporganizer.presentation.viewmodel.PrivacyReportUiState
import com.armutlu.apporganizer.presentation.viewmodel.PrivacyReportViewModel

/**
 * Gizlilik Analizi ekranı — PrivacyAnalyzer'in ürettiği hassas izin grup raporunu gösterir.
 * Tüm veri cihazda hesaplanır (PackageManager), sunucuya hiçbir şey gönderilmez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyReportScreen(
    viewModel: PrivacyReportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_report_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.privacy_report_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is PrivacyReportUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is PrivacyReportUiState.AnalyticsDisabled -> ReportStatusPane(
                padding = padding,
                icon = Icons.Default.Lock,
                title = stringResource(R.string.privacy_report_disabled_title),
                description = stringResource(R.string.privacy_report_disabled_desc),
                buttonText = stringResource(R.string.privacy_report_disabled_btn),
                onButtonClick = { viewModel.enableAnalytics() }
            )

            is PrivacyReportUiState.Ready -> PrivacyReportContent(
                padding = padding,
                report = s.report,
                context = context,
            )
        }
    }
}

/** Tam-ekran durum paneli — NotificationReportScreen ile aynı görsel dil. */
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

private fun openAppDetailsSettings(context: Context, packageName: String) {
    runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
private fun PrivacyReportContent(
    padding: PaddingValues,
    report: PrivacyAnalyzer.PrivacyReport,
    context: Context,
) {
    if (report.isAllClear) {
        EmptyPrivacyState(padding)
        return
    }

    val totalGrantedApps = report.groups
        .flatMap { it.grantedApps }
        .map { it.packageName }
        .distinct()
        .size

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummaryCard(totalGrantedApps) }

        items(report.groups, key = { it.group.id }) { groupReport ->
            if (groupReport.grantedCount > 0 || groupReport.requestedNotGrantedCount > 0) {
                PermissionGroupCard(groupReport, onAppClick = { pkg -> openAppDetailsSettings(context, pkg) })
            }
        }

        item {
            Text(
                stringResource(R.string.privacy_report_footer_note),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun EmptyPrivacyState(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Shield, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.privacy_report_empty_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.privacy_report_empty_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SummaryCard(totalGrantedApps: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (totalGrantedApps > 0) {
                    stringResource(R.string.privacy_report_summary, totalGrantedApps)
                } else {
                    stringResource(R.string.privacy_report_summary_none)
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun PermissionGroupCard(
    groupReport: PrivacyAnalyzer.PermissionGroupReport,
    onAppClick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = groupReport.grantedCount > 0) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(groupReport.group.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(groupReport.group.label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    if (groupReport.grantedCount > 0) {
                        Text(
                            stringResource(R.string.privacy_report_group_granted_count, groupReport.grantedCount),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (groupReport.requestedNotGrantedCount > 0) {
                        Text(
                            stringResource(
                                R.string.privacy_report_group_requested_not_granted,
                                groupReport.requestedNotGrantedCount
                            ),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (groupReport.grantedCount > 0) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    groupReport.grantedApps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAppClick(app.packageName) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.appName, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
