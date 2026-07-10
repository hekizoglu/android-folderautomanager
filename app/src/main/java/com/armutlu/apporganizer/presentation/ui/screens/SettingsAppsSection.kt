package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.AppPrefs

/**
 * Kullanılmayan gri chip'leri + Uygulama Yönetimi + Gizli Uygulamalar + Diğer Klasörü
 */
internal fun LazyListScope.settingsAppsSection(
    showSystemApps: Boolean,
    viewModel: AppListViewModel,
    hiddenApps: List<AppInfo>,
    otherApps: List<AppInfo>,
    llmCategorizing: Boolean,
    llmProgress: String
) {
    // ── Uygulama Listesi → Sistem Uygulamaları ─────────────────────────────────
    item { SettingsSectionTitle("Uygulama Listesi") }
    item {
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.Visibility,
                title = "Sistem Uygulamalarını Göster",
                subtitle = "Dahili sistem uygulamalarını listele",
                checked = showSystemApps,
                onCheckedChange = { viewModel.toggleShowSystemApps() }
            )
        }
    }

    // ── Kullanılmayan gri seçeneği ───────────────────────────────────────
    // Tek sahip: Görünüm ekranı (SettingsAppearanceSection.kt) — burada yalnızca yönlendirme bilgisi gösterilir.
    item {
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kullanılmayan Uygulamalar Gri", style = MaterialTheme.typography.bodyLarge)
                    Text("Ayar Görünüm ekranında", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Uygulama Yönetimi
    item { SettingsSectionTitle("Uygulama Yönetimi") }
    item {
        val context = LocalContext.current
        var showResetDialog by remember { mutableStateOf(false) }
        SettingsCard {
            var manufacturerClassify by remember { mutableStateOf(AppPrefs.isManufacturerClassifyEnabled(context)) }
            SettingsSwitchRow(
                icon = Icons.Default.PhoneAndroid,
                title = "Üretici Sınıflandırması",
                subtitle = "Samsung/Huawei/Xiaomi uygulamalarını otomatik kategorilendir",
                checked = manufacturerClassify,
                onCheckedChange = { manufacturerClassify = it; AppPrefs.setManufacturerClassifyEnabled(context, it) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsButtonRow(icon = Icons.Default.AutoFixHigh,
                title = "Sınıflandırılmamışları Sınıflandır",
                subtitle = "Kategorisiz uygulamaları otomatik ata",
                showChevron = false,
                onClick = { viewModel.classifyUnclassifiedApps() })
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsButtonRow(icon = Icons.Default.RestartAlt,
                title = "Tüm Kategorileri Sıfırla",
                subtitle = "Tüm atamaları sil ve yeniden sınıflandır",
                iconTint = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = { showResetDialog = true })
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Tüm kategorileri sıfırla") },
                    text = { Text("Tüm uygulama kategorileri silinecek ve yeniden sınıflandırma başlayacak. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showResetDialog = false
                            viewModel.resetAndReclassifyAllApps()
                        }) { Text("Sıfırla", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("İptal") }
                    }
                )
            }
        }
    }

    // ── Gizli Uygulamalar ───────────────────────────────────────────────
    if (hiddenApps.isNotEmpty()) {
        item { SettingsSectionTitle("Gizli Uygulamalar (${hiddenApps.size})") }
        item {
            SettingsCard {
                hiddenApps.forEachIndexed { index, app ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(app.appName, Modifier.weight(1f), fontSize = 14.sp)
                        OutlinedButton(onClick = { viewModel.unhideApp(app.packageName) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Göster", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // ── Diğer Klasörü ───────────────────────────────────────────────────
    if (otherApps.isNotEmpty()) {
        item { SettingsSectionTitle("Diğer Klasörü — Bilinmeyenler (${otherApps.size})") }
        item {
            val context = LocalContext.current
            SettingsCard {
                var apiKeyInput by remember { mutableStateOf(AppPrefs.getDeepSeekApiKey(context)) }
                var showApiKey  by remember { mutableStateOf(false) }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Bu uygulamalar otomatik kategorilendirilemeyen uygulamalardır. DeepSeek AI ile kategorilendirilebilir.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it; AppPrefs.setDeepSeekApiKey(context, it) },
                        label = { Text("DeepSeek API Key", fontSize = 12.sp) },
                        placeholder = { Text("sk-...", fontSize = 12.sp) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.categorizeDigerWithLLM(apiKeyInput) },
                        enabled = !llmCategorizing && apiKeyInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()) {
                        if (llmCategorizing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Kategorize ediliyor...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("DeepSeek ile Kategorize Et", fontSize = 13.sp)
                        }
                    }
                    if (llmProgress.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(llmProgress, fontSize = 12.sp,
                            color = if (llmProgress.startsWith("Hata") || llmProgress.contains("hata"))
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                otherApps.take(20).forEachIndexed { index, app ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Help, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.appName, fontSize = 14.sp, maxLines = 1)
                            Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                if (otherApps.size > 20) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("...ve ${otherApps.size - 20} uygulama daha", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
