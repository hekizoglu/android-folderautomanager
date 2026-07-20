package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.presentation.viewmodel.DiagnosticsReportViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent

/**
 * U1: Hakkında & Yedekleme alt ekranı — uygulama bilgisi, gizlilik merkezi,
 * yedekle/geri yükle, debug ve geri bildirim.
 * Mevcut settingsBackupAboutSection LazyListScope uzantısını kendi
 * route'unda sarar, fonksiyonellik değişmedi.
 */
@Composable
fun SettingsAboutScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToUsageReport: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by viewModel.screenState.collectAsState()
    val logs by viewModel.liveDebugLogs.collectAsState()
    val diagnosticsViewModel: DiagnosticsReportViewModel = hiltViewModel()
    val diagnosticsState by diagnosticsViewModel.uiState.collectAsState()

    LaunchedEffect(diagnosticsState.shareIntent) {
        val intent = diagnosticsState.shareIntent ?: return@LaunchedEffect
        context.startActivity(Intent.createChooser(intent, "Talep / oneriyi raporla birlikte gonder"))
        diagnosticsViewModel.consumeShareIntent()
    }
    LaunchedEffect(diagnosticsState.errorMessage) {
        val message = diagnosticsState.errorMessage ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        diagnosticsViewModel.consumeError()
    }

    SettingsSubScreenScaffold(title = "Hakkında & Yedekleme", onNavigateBack = onNavigateBack) {

        // Hakkında (üst) + Yedek/Geri Yükle + Gizlilik + Debug
        settingsBackupAboutSection(
            viewModel = viewModel,
            appCount = state.apps.size,
            categoryCount = state.categories.size,
            logs = logs,
            onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
            onNavigateToUsageReport = onNavigateToUsageReport,
            onNavigateToDashboard = onNavigateToDashboard
        )

        // ── Geri Bildirim ────────────────────────────────────────────────
        item { SettingsSectionTitle("Geri Bildirim") }
        item {
            SettingsCard {
                SettingsButtonRow(
                    icon = Icons.Default.Feedback,
                    title = stringResource(R.string.settings_feedback),
                    subtitle = stringResource(R.string.settings_feedback_desc),
                    showChevron = false,
                    onClick = {
                        diagnosticsViewModel.generateFeedbackReport()
                    }
                )
            }
        }

        // ── Firebase bağlantı testi + sağlık raporu (S4: Dijital Yaşam'dan taşındı) ──
        item { FirebaseHealthCheckSection() }
    }
}
