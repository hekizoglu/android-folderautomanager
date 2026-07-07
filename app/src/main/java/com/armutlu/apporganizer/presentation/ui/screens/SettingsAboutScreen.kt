package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

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
                        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:")
                            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("huseyinekizoglu@gmail.com"))
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "AppOrganizer - Talep / Öneri")
                            putExtra(android.content.Intent.EXTRA_TEXT, "\n\n---\nCihaz: $device")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                )
            }
        }
    }
}
