package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
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
    llmProgress: String,
    onNavigateToClassificationReview: () -> Unit,
    onNavigateToFolderSuggestions: () -> Unit
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
    // Tek sahip: Görünüm ekranı (SettingsAppearanceSection.kt) - burada yalnızca yönlendirme bilgisi gösterilir.
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

    // ── Sınıflandırma Modu (P0.6) — paralel toggle'lar yerine tek seçici ────────
    item { SettingsSectionTitle(stringResource(R.string.classification_mode_title)) }
    item {
        val context = LocalContext.current
        var mode by remember { mutableStateOf(AppPrefs.getClassificationMode(context)) }
        SettingsCard {
            Text(
                stringResource(R.string.classification_mode_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            ClassificationModeOption(
                titleRes = R.string.classification_mode_local_only_title,
                descRes = R.string.classification_mode_local_only_desc,
                selected = mode == AppPrefs.ClassificationMode.LOCAL_ONLY,
                onSelect = { mode = AppPrefs.ClassificationMode.LOCAL_ONLY; AppPrefs.setClassificationMode(context, mode) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            ClassificationModeOption(
                titleRes = R.string.classification_mode_local_with_manufacturer_title,
                descRes = R.string.classification_mode_local_with_manufacturer_desc,
                selected = mode == AppPrefs.ClassificationMode.LOCAL_WITH_MANUFACTURER,
                onSelect = { mode = AppPrefs.ClassificationMode.LOCAL_WITH_MANUFACTURER; AppPrefs.setClassificationMode(context, mode) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            ClassificationModeOption(
                titleRes = R.string.classification_mode_local_with_llm_fallback_title,
                descRes = R.string.classification_mode_local_with_llm_fallback_desc,
                selected = mode == AppPrefs.ClassificationMode.LOCAL_WITH_LLM_FALLBACK,
                onSelect = { mode = AppPrefs.ClassificationMode.LOCAL_WITH_LLM_FALLBACK; AppPrefs.setClassificationMode(context, mode) }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            ClassificationModeOption(
                titleRes = R.string.classification_mode_manual_review_only_title,
                descRes = R.string.classification_mode_manual_review_only_desc,
                selected = mode == AppPrefs.ClassificationMode.MANUAL_REVIEW_ONLY,
                onSelect = { mode = AppPrefs.ClassificationMode.MANUAL_REVIEW_ONLY; AppPrefs.setClassificationMode(context, mode) }
            )
        }
    }

    // Uygulama Yönetimi
    item { SettingsSectionTitle("Uygulama Yönetimi") }
    item {
        val context = LocalContext.current
        var resetConfirmStep by remember { mutableStateOf(0) } // 0=kapalı, 1=ilk onay, 2=son onay
        SettingsCard {
            var overrideSuggestions by remember { mutableStateOf(AppPrefs.isOverrideSuggestionsEnabled(context)) }
            var folderSuggestionsEnabled by remember { mutableStateOf(AppPrefs.isFolderSuggestionsEnabled(context)) }
            var lowConfidenceReview by remember { mutableStateOf(AppPrefs.isLowConfidenceReviewEnabled(context)) }
            SettingsSwitchRow(
                icon = Icons.Default.Lightbulb,
                title = "Benzer Uygulama Önerileri",
                subtitle = "Elle taşıdığın uygulamalardan öğrenip benzerlerini önerir",
                checked = overrideSuggestions,
                onCheckedChange = {
                    overrideSuggestions = it
                    AppPrefs.setOverrideSuggestionsEnabled(context, it)
                }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(
                icon = Icons.Default.Folder,
                title = "Klasor Onerileri",
                subtitle = "Yeni kurulumda acik gelir; buyuk, kucuk veya atil klasorler icin review onerileri uretir",
                checked = folderSuggestionsEnabled,
                onCheckedChange = {
                    folderSuggestionsEnabled = it
                    AppPrefs.setFolderSuggestionsEnabled(context, it)
                    viewModel.dismissFolderSuggestionsInfo()
                }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsSwitchRow(
                icon = Icons.Default.Rule,
                title = "Düşük Güvenli Kararları Sor",
                subtitle = "Güven skoru düşükse otomatik taşımak yerine Kontrol Bekleyenler'e gönderir",
                checked = lowConfidenceReview,
                onCheckedChange = {
                    lowConfidenceReview = it
                    AppPrefs.setLowConfidenceReviewEnabled(context, it)
                }
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsButtonRow(icon = Icons.Default.CheckCircle,
                title = "Kontrol Bekleyenler",
                subtitle = "Dusuk guvenli siniflandirmalari onayla veya duzelt",
                onClick = onNavigateToClassificationReview)
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            SettingsButtonRow(icon = Icons.Default.Folder,
                title = "Klasor Onerileri",
                subtitle = "Kalabalik, kucuk veya uzun suredir kullanilmayan klasorleri duzenle",
                onClick = onNavigateToFolderSuggestions)
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
                onClick = { resetConfirmStep = 1 })
            if (resetConfirmStep == 1) {
                AlertDialog(
                    onDismissRequest = { resetConfirmStep = 0 },
                    title = { Text("Tüm kategorileri sıfırla") },
                    text = { Text("Tüm uygulama kategorileri silinecek ve yeniden sınıflandırma başlayacak. Emin misiniz?") },
                    confirmButton = {
                        TextButton(onClick = { resetConfirmStep = 2 }) {
                            Text("Devam Et", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { resetConfirmStep = 0 }) { Text("İptal") }
                    }
                )
            } else if (resetConfirmStep == 2) {
                AlertDialog(
                    onDismissRequest = { resetConfirmStep = 0 },
                    title = { Text("Son onay") },
                    text = { Text("Bu işlem geri alınamaz — tüm kategori atamaları silinip yeniden sınıflandırılacak. Gerçekten sıfırlamak istiyor musunuz?") },
                    confirmButton = {
                        TextButton(onClick = {
                            resetConfirmStep = 0
                            viewModel.resetAndReclassifyAllApps()
                        }) { Text("Evet, Sıfırla", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { resetConfirmStep = 0 }) { Text("İptal") }
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
        item { SettingsSectionTitle("Diğer Klasörü - Bilinmeyenler (${otherApps.size})") }
        item {
            val context = LocalContext.current
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Bu uygulamalar otomatik kategorilendirilemeyen uygulamalardır. DeepSeek AI ile kategorilendirilebilir. DeepSeek API anahtarını Gizlilik & Veri ayarlarından girebilirsin.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    val apiKeyInput = AppPrefs.getDeepSeekApiKey(context)
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

// ── Sınıflandırma Modu — tek seçim (radio) satırı (P0.6) ────────────────────
@Composable
private fun ClassificationModeOption(
    titleRes: Int,
    descRes: Int,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(titleRes),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(descRes),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
