package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
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
