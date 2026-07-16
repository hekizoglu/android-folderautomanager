package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.viewmodel.ConnectionTestStatus
import com.armutlu.apporganizer.presentation.viewmodel.UsageDataViewModel
import com.armutlu.apporganizer.telemetry.TelemetryConsentManager
import com.armutlu.apporganizer.utils.AppPrefs
import com.google.firebase.FirebaseApp

@Composable
fun SettingsUsageDataScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val model: UsageDataViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UsageDataViewModel(
            initialSharingEnabled = AppPrefs.isTelemetryEnabled(context),
            persistSharingEnabled = { TelemetryConsentManager.setConsent(context, it) },
            testConnection = {
                // B4 only exposes a safe configuration probe. B5 owns the real service round-trip.
                val configured = FirebaseApp.getApps(context).any { app ->
                    app.options.applicationId.isNotBlank() && app.options.apiKey.isNotBlank()
                }
                if (configured) ConnectionTestStatus.PARTIAL_SUCCESS else ConnectionTestStatus.FAILED
            },
        ) as T
    })
    val state by model.uiState.collectAsState()

    SettingsSubScreenScaffold(stringResource(R.string.usage_data_title), onNavigateBack) {
        item { SettingsSectionTitle(stringResource(R.string.usage_data_sharing_section)) }
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.usage_data_sharing_title))
                        Text(stringResource(R.string.usage_data_sharing_description), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val switchState = if (state.sharingEnabled) {
                        stringResource(R.string.usage_data_enabled)
                    } else stringResource(R.string.usage_data_disabled)
                    Switch(
                        checked = state.sharingEnabled,
                        onCheckedChange = model::setSharingEnabled,
                        modifier = Modifier.semantics { stateDescription = switchState },
                    )
                }
            }
        }
        item { UsageDataListCard(R.string.usage_data_collected_title, collectedItems()) }
        item { UsageDataListCard(R.string.usage_data_not_collected_title, notCollectedItems()) }
        item { SettingsSectionTitle(stringResource(R.string.usage_data_connection_title)) }
        item {
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(connectionStatusText(state.connectionStatus))
                    Button(
                        onClick = model::runConnectionTest,
                        enabled = state.connectionStatus != ConnectionTestStatus.TESTING,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.connectionStatus == ConnectionTestStatus.TESTING) {
                            CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        }
                        Text(stringResource(R.string.usage_data_test_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageDataListCard(title: Int, items: List<String>) {
    SettingsSectionTitle(stringResource(title))
    SettingsCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { Text("• $it", fontSize = 14.sp) }
        }
    }
}

@Composable
private fun collectedItems() = listOf(
    stringResource(R.string.usage_data_collected_feature_counts),
    stringResource(R.string.usage_data_collected_crashes),
    stringResource(R.string.usage_data_collected_durations),
    stringResource(R.string.usage_data_collected_versions),
    stringResource(R.string.usage_data_collected_health_codes),
)

@Composable
private fun notCollectedItems() = listOf(
    stringResource(R.string.usage_data_not_collected_search),
    stringResource(R.string.usage_data_not_collected_notifications),
    stringResource(R.string.usage_data_not_collected_contacts),
    stringResource(R.string.usage_data_not_collected_files),
    stringResource(R.string.usage_data_not_collected_apps),
    stringResource(R.string.usage_data_not_collected_folders),
)

@Composable
private fun connectionStatusText(status: ConnectionTestStatus): String = stringResource(when (status) {
    ConnectionTestStatus.IDLE -> R.string.usage_data_status_idle
    ConnectionTestStatus.TESTING -> R.string.usage_data_status_testing
    ConnectionTestStatus.SUCCESS -> R.string.usage_data_status_success
    ConnectionTestStatus.PARTIAL_SUCCESS -> R.string.usage_data_status_partial
    ConnectionTestStatus.FAILED -> R.string.usage_data_status_failed
})
