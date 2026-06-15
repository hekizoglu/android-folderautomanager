package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import kotlinx.coroutines.launch

/**
 * Gorunum bolumu: tema, yazi tipi, arka plan, yazi rengi, soluk uygulama ayarlari.
 * SettingsScreen LazyColumn icinde item{} bloklariyla cagirilir.
 */
@Composable
fun SettingsAppearanceSection(
    themePrefs: ThemePreferences,
    currentTheme: AppTheme,
    currentFont: AppFont,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Tema + Yazi Tipi ─────────────────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Renk Temasi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AppTheme.entries.toList()) { theme ->
                    val isSelected = currentTheme == theme
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { scope.launch { themePrefs.setTheme(theme) } }
                            .padding(4.dp)
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
                            theme.label,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Text("Yazi Tipi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFont.entries.forEach { font ->
                    val isSelected = currentFont == font
                    FilterChip(
                        selected = isSelected,
                        onClick = { scope.launch { themePrefs.setFont(font) } },
                        label = { Text(font.label, fontSize = 12.sp) }
                    )
                }
            }
        }
    }

    // ── Arka Plan ────────────────────────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ana Ekran Arka Plani", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            var bgType by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("wallpaper" to "Duvar Kagidi", "solid" to "Duz Renk").forEach { (type, label) ->
                    FilterChip(
                        selected = bgType == type,
                        onClick = {
                            bgType = type
                            com.armutlu.apporganizer.utils.AppPrefs.setBgType(context, type)
                        },
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
                var selectedBgColor by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgColor(context)) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bgColors) { (colorInt, colorLabel) ->
                        val isSelected = selectedBgColor == colorInt
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    selectedBgColor = colorInt
                                    com.armutlu.apporganizer.utils.AppPrefs.setBgColor(context, colorInt)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                colorLabel,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            var textAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Yazi Opaklik", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${(textAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = textAlpha,
                onValueChange = {
                    textAlpha = it
                    com.armutlu.apporganizer.utils.AppPrefs.setTextAlpha(context, it)
                },
                valueRange = 0.4f..1.0f,
                steps = 11
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            var folderSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Klasor Boyutu", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${folderSizeDp}dp", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = folderSizeDp.toFloat(),
                onValueChange = {
                    folderSizeDp = it.toInt()
                    com.armutlu.apporganizer.utils.AppPrefs.setFolderSizeDp(context, it.toInt())
                },
                valueRange = 56f..96f,
                steps = 7
            )
        }
    }

    // ── Yazi Rengi Paleti ────────────────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val labelColorPresets = listOf(
                "#FFFFFF" to "Beyaz",
                "#F5F5F5" to "Acik Gri",
                "#FFD700" to "Altin",
                "#80DEEA" to "Turkuaz",
                "#FFAB40" to "Turuncu",
                "#EF9A9A" to "Pembe"
            )
            var selectedLabel by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getLabelColor(context)) }
            Text("Yazi Rengi", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(labelColorPresets) { (hex, _) ->
                    val color = runCatching {
                        Color(android.graphics.Color.parseColor(hex))
                    }.getOrDefault(Color.White)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selectedLabel == hex)
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                selectedLabel = hex
                                com.armutlu.apporganizer.utils.AppPrefs.setLabelColor(context, hex)
                            }
                    )
                }
            }
        }
    }

    // ── Kullanilmayan Uygulamalar Gri ────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            var unusedGreyDays by remember {
                mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context))
            }
            val greyOptions = listOf(0 to "Kapali", 7 to "7 gun", 14 to "14 gun", 30 to "30 gun")
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text("Kullanilmayan Uygulamalar Gri", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Hic acilmamis uygulamalar soluk gosterilir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                greyOptions.forEach { (days, label) ->
                    val selected = unusedGreyDays == days
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                unusedGreyDays = days
                                com.armutlu.apporganizer.utils.AppPrefs.setUnusedGreyDays(context, days)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
