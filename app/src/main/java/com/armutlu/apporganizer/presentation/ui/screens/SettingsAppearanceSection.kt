package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.utils.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Görünüm ayarları: tema, yazı tipi, arka plan, renk, metin saydamlığı,
 * klasör boyutu, yazı rengi.
 */
internal fun LazyListScope.settingsAppearanceSection(
    themePrefs: ThemePreferences,
    scope: CoroutineScope
) {
    item { SettingsSectionTitle("Görünüm") }

    // ── Tema & Yazı Tipi ────────────────────────────────────────────────
    item {
        val currentTheme by themePrefs.themeFlow.collectAsState(initial = AppTheme.TEAL)
        val currentFont  by themePrefs.fontFlow.collectAsState(initial = AppFont.DEFAULT)
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Renk Teması", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AppTheme.entries.toList()) { theme ->
                        val isSelected = currentTheme == theme
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { scope.launch { themePrefs.setTheme(theme) } }.padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                theme.label, fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text("Yazı Tipi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFont.entries.forEach { font ->
                        FilterChip(
                            selected = currentFont == font,
                            onClick = { scope.launch { themePrefs.setFont(font) } },
                            label = { Text(font.label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }

    // ── Arka Plan ────────────────────────────────────────────────────────
    item {
        val context = LocalContext.current
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ana Ekran Arka Planı", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                var bgType by remember { mutableStateOf(AppPrefs.getBgType(context)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("wallpaper" to "Duvar Kağıdı", "solid" to "Düz Renk").forEach { (type, label) ->
                        FilterChip(
                            selected = bgType == type,
                            onClick = { bgType = type; AppPrefs.setBgType(context, type) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                if (bgType == "solid") {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Text("Arka Plan Rengi", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    val bgColors = listOf(
                        0xFF1A1A2E.toInt() to "Koyu Lacivert",
                        0xFF121212.toInt() to "Siyah",
                        0xFF0A1628.toInt() to "Gece Mavisi",
                        0xFF1C1008.toInt() to "Koyu Kahve",
                        0xFF0F2027.toInt() to "Derin Okyanus",
                        0xFF1A1025.toInt() to "Derin Mor"
                    )
                    var selectedBgColor by remember { mutableStateOf(AppPrefs.getBgColor(context)) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(bgColors) { (colorInt, colorLabel) ->
                            val isSelected = selectedBgColor == colorInt
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        selectedBgColor = colorInt
                                        AppPrefs.setBgColor(context, colorInt)
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt.toLong() or 0xFF000000L))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(colorLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                // Metin saydamlığı
                var textAlpha by remember { mutableStateOf(AppPrefs.getTextAlpha(context)) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TextFields, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Metin Saydamlığı", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("Klasör adı opaklığı: ${(textAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Slider(value = textAlpha, onValueChange = { textAlpha = it; AppPrefs.setTextAlpha(context, it) }, valueRange = 0.3f..1.0f, steps = 6)
            }
        }
    }

    // ── Klasör Boyutu & Yazı Rengi ───────────────────────────────────────
    item {
        val context = LocalContext.current
        SettingsCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Klasör boyutu slider
                var folderSize by remember { mutableStateOf(AppPrefs.getFolderSize(context).toFloat()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Klasör Simge Boyutu", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        Text("${folderSize.toInt()} dp", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Slider(value = folderSize, onValueChange = { folderSize = it; AppPrefs.setFolderSize(context, it.toInt()) }, valueRange = 56f..96f, steps = 4)
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                // Yazı rengi
                var labelColor by remember { mutableStateOf(AppPrefs.getLabelColor(context)) }
                val labelColors = listOf(
                    "#FFFFFF" to "Beyaz", "#000000" to "Siyah",
                    "#E0E0E0" to "Gri", "#00897B" to "Turkuaz",
                    "#FFD54F" to "Sarı", "#EF9A9A" to "Pembe"
                )
                Text("Yazı Rengi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labelColors.forEach { (hex, label) ->
                        val isSelected = labelColor == hex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { labelColor = hex; AppPrefs.setLabelColor(context, hex) }.padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White))
                                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape)
                            )
                            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
