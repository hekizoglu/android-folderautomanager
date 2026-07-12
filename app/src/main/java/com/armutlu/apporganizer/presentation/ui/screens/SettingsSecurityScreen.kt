package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.BiometricHelper

/**
 * U1: Güvenlik alt ekranı — biyometrik ayarlar kilidi.
 * İçerik eski SettingsScreen'den birebir taşındı, fonksiyonellik değişmedi.
 */
@Composable
fun SettingsSecurityScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    SettingsSubScreenScaffold(title = "Güvenlik", onNavigateBack = onNavigateBack) {
        item {
            var biometricLock by remember { mutableStateOf(AppPrefs.isBiometricSettingsLockEnabled(context)) }
            val activity = context as? FragmentActivity
            val biometricAvailable = remember(context) {
                activity != null && BiometricHelper.isAvailable(activity)
            }
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ayarlar Kilidi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text(
                            if (biometricAvailable) "Ayarlar açılışında parmak izi / yüz doğrulama" else "Cihazda biyometrik doğrulama bulunamadı",
                            fontSize = 12.sp,
                            color = if (biometricAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = biometricLock,
                        enabled = biometricAvailable,
                        onCheckedChange = {
                            biometricLock = it
                            AppPrefs.setBiometricSettingsLockEnabled(context, it)
                        }
                    )
                }
            }
        }
        item {
            var privacyReportEnabled by remember { mutableStateOf(AppPrefs.isPrivacyReportEnabled(context)) }
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_privacy_report_toggle_title),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            androidx.compose.ui.res.stringResource(com.armutlu.apporganizer.R.string.settings_privacy_report_toggle_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = privacyReportEnabled,
                        onCheckedChange = {
                            privacyReportEnabled = it
                            AppPrefs.setPrivacyReportEnabled(context, it)
                        }
                    )
                }
            }
        }
    }
}
