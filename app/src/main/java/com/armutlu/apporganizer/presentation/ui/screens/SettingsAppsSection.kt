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
    // ── Görünüm → Sistem Uygulamaları ─────────────────────────────────
    item { SettingsSectionTitle("Görünüm") }
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
    item {
        val context = LocalContext.current
        var unusedGreyDays by remember { mutableStateOf(AppPrefs.getUnusedGreyDays(context)) }
        val greyOptions = listOf(0 to "Kapalı", 7 to "7 gün", 14 to "14 gün", 30 to "30 gün")
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kullanılmayan Uygulamalar Gri", style = MaterialTheme.typography.bodyLarge)
                        Text("Hiç açılmamış uygulamalar soluk gösterilir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    greyOptions.forEach { (days, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (days == unusedGreyDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { unusedGreyDays = days; AppPrefs.setUnusedGreyDays(context, days) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(label, fontSize = 12.sp,
                                color = if (days == unusedGreyDays) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (days == unusedGreyDays) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }

    // ── Uygulama Yönetimi ───────────────────────────────────────────────
    item { SettingsSectionTitle("Uygulama Yönetimi") }
    item {
        val context = LocalContext.current
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
                onClick = { viewModel.classifyUnclassifiedApps() })
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsButtonRow(icon = Icons.Default.RestartAlt,
                title = "Tüm Kategorileri Sıfırla",
                subtitle = "Tüm atamaları sil ve yeniden sınıflandır",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { viewModel.resetAndReclassifyAllApps() })
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
