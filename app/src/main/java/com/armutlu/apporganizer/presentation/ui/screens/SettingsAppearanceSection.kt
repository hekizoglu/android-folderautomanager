package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.armutlu.apporganizer.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.ui.components.ColorPickerDialog
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.IconPackManager
import kotlinx.coroutines.launch

/**
 * Görünüm bölümü: tema, yazı tipi, arka plan, yazı rengi, soluk uygulama ayarları.
 * SettingsScreen LazyColumn içinde item{} bloklarıyla çağrılır.
 */
@Composable
fun SettingsAppearanceSection(
    themePrefs: ThemePreferences,
    currentTheme: AppTheme,
    currentFont: AppFont,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Tema + Yazı Tipi ─────────────────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.appearance_color_theme), fontWeight = FontWeight.Medium, fontSize = 15.sp)
            // DYNAMIC (Material You) yalnızca Android 12+ cihazlarda listelenir
            val availableThemes = AppTheme.entries.filter {
                it != AppTheme.DYNAMIC ||
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(availableThemes, key = { it.name }) { theme ->
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
                                .background(theme.previewBrush)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(theme.labelRes),
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Text(stringResource(R.string.appearance_font), fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppFont.entries.forEach { font ->
                    val isSelected = currentFont == font
                    FilterChip(
                        selected = isSelected,
                        onClick = { scope.launch { themePrefs.setFont(font) } },
                        label = { Text(stringResource(font.labelRes), fontSize = 12.sp) }
                    )
                }
            }
        }
    }

    // ── Arka Plan ────────────────────────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.appearance_wallpaper), fontWeight = FontWeight.Medium, fontSize = 15.sp)
            var bgType by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("wallpaper" to "Duvar Kağıdı", "solid" to "Düz Renk", "gradient" to "Gradyan").forEach { (type, label) ->
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
            if (bgType == "gradient") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text("Gradyan Stili", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                var selectedStyle by remember {
                    mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getHomeBackgroundStyle(context))
                }
                val gradientStyles = listOf(
                    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_TURKUAZ to ("Turkuaz" to Brush.verticalGradient(
                        listOf(Color(0xFF00897B), Color(0xFF26C6DA))
                    )),
                    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_GECE_MAVISI to ("Gece Mavisi" to Brush.verticalGradient(
                        listOf(Color(0xFF0A1128), Color(0xFF1B2A4A))
                    )),
                    com.armutlu.apporganizer.utils.AppPrefs.HOME_BG_MINIMAL_GRI to ("Minimal Koyu Gri" to Brush.verticalGradient(
                        listOf(Color(0xFF1C1C1C), Color(0xFF2E2E2E))
                    )),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    gradientStyles.forEach { (key, pair) ->
                        val (label, brush) = pair
                        val isSelected = selectedStyle == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    selectedStyle = key
                                    com.armutlu.apporganizer.utils.AppPrefs.setHomeBackgroundStyle(context, key)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(56.dp)
                            )
                        }
                    }
                }
            }
            if (bgType == "solid") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                var showBgColorPicker by remember { mutableStateOf(false) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(bgColors, key = { (colorInt, _) -> colorInt }) { (colorInt, colorLabel) ->
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
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showBgColorPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.appearance_custom_color), fontSize = 13.sp) }
                if (showBgColorPicker) {
                    ColorPickerDialog(
                        initialColor = Color(selectedBgColor),
                        onColorSelected = { color ->
                            val colorInt = android.graphics.Color.argb(
                                (color.alpha * 255).toInt(),
                                (color.red * 255).toInt(),
                                (color.green * 255).toInt(),
                                (color.blue * 255).toInt()
                            )
                            selectedBgColor = colorInt
                            com.armutlu.apporganizer.utils.AppPrefs.setBgColor(context, colorInt)
                            val hex = "#%06X".format(colorInt and 0xFFFFFF)
                            scope.launch {
                                runCatching {
                                    com.armutlu.apporganizer.utils.WallpaperHelper.applyColorWallpaper(context, hex)
                                    com.armutlu.apporganizer.utils.AppPrefs.setBgType(context, "wallpaper_color")
                                }
                            }
                        },
                        onDismiss = { showBgColorPicker = false }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            var textAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.appearance_text_opacity), fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                Text(stringResource(R.string.appearance_folder_size), fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            var iconScale by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconScale(context)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("İkon Boyutu", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${(iconScale * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = iconScale,
                onValueChange = {
                    iconScale = it
                    com.armutlu.apporganizer.utils.AppPrefs.setIconScale(context, it)
                },
                valueRange = 0.7f..1.3f,
                steps = 5
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            var pageFolderCount by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getPageSize(context)) }
            val pageSizeOptions = listOf(4, 6, 8, 12)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.appearance_folders_per_page), fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("$pageFolderCount ${stringResource(R.string.appearance_folder_unit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = pageSizeOptions.indexOf(pageFolderCount).coerceAtLeast(0).toFloat(),
                onValueChange = {
                    val selected = pageSizeOptions.getOrElse(it.toInt()) { 8 }
                    pageFolderCount = selected
                    com.armutlu.apporganizer.utils.AppPrefs.setPageSize(context, selected)
                },
                valueRange = 0f..3f,
                steps = 2
            )
        }
    }

    // ── Yazi Rengi Paleti ────────────────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val labelColorPresets = listOf(
                "#FFFFFF" to "Beyaz",
                "#F5F5F5" to "Açık Gri",
                "#FFD700" to "Altın",
                "#80DEEA" to "Turkuaz",
                "#FFAB40" to "Turuncu",
                "#EF9A9A" to "Pembe"
            )
            var selectedLabel by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getLabelColor(context)) }
            var showLabelColorPicker by remember { mutableStateOf(false) }
            Text(stringResource(R.string.appearance_text_color), fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(labelColorPresets, key = { (hex, _) -> hex }) { (hex, _) ->
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
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showLabelColorPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.appearance_custom_color), fontSize = 13.sp) }
            if (showLabelColorPicker) {
                val initialColor = runCatching {
                    Color(android.graphics.Color.parseColor(selectedLabel))
                }.getOrDefault(Color.White)
                ColorPickerDialog(
                    initialColor = initialColor,
                    onColorSelected = { color ->
                        val hex = "#%02X%02X%02X%02X".format(
                            (color.alpha * 255).toInt(),
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        ).let { "#%06X".format(android.graphics.Color.parseColor(it) and 0xFFFFFF) }
                        selectedLabel = hex
                        com.armutlu.apporganizer.utils.AppPrefs.setLabelColor(context, hex)
                    },
                    onDismiss = { showLabelColorPicker = false }
                )
            }
        }
    }

    // ── Kullanılmayan Uygulamalar Gri ────────────────────────────────────
    SettingsCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            var unusedGreyDays by remember {
                mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context))
            }
            val greyOptions = listOf(0 to "Kapalı", 7 to "7 gün", 14 to "14 gün", 30 to "30 gün")
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(stringResource(R.string.appearance_grey_unused), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Hiç açılmamış uygulamalar soluk gösterilir",
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

    // ── Klasör Blur Efekti ─────────────────────────────────────────────────
    var folderBlur by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(context)) }
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.BlurOn,
            title = stringResource(R.string.appearance_blur_effect),
            subtitle = stringResource(R.string.appearance_blur_effect_desc),
            checked = folderBlur,
            onCheckedChange = {
                folderBlur = it
                com.armutlu.apporganizer.utils.AppPrefs.setFolderBlurEnabled(context, it)
            }
        )
    }

    // ── Klasör Şekli ──────────────────────────────────────────────────────
    var folderShape by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderShape(context)) }
    SettingsCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.appearance_folder_shape), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                val shapeOptions = listOf(
                    "circle"   to "Daire",
                    "rounded"  to "Yumuşak",
                    "square"   to "Kare",
                    "triangle" to "Üçgen"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shapeOptions.forEach { (key, label) ->
                        val selected = folderShape == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    folderShape = key
                                    com.armutlu.apporganizer.utils.AppPrefs.setFolderShape(context, key)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(
                                        when (key) {
                                            "square"   -> RoundedCornerShape(0.dp)
                                            "rounded"  -> RoundedCornerShape(12.dp)
                                            "triangle" -> RoundedCornerShape(4.dp)
                                            else       -> CircleShape
                                        }
                                    )
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = when (key) {
                                            "square"   -> RoundedCornerShape(0.dp)
                                            "rounded"  -> RoundedCornerShape(12.dp)
                                            else       -> CircleShape
                                        }
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

    // ── Klasör Rengi Otomatik ─────────────────────────────────────────────
    var autoFolderColor by remember { mutableStateOf(AppPrefs.isAutoFolderColorEnabled(context)) }
    SettingsCard {
        SettingsSwitchRow(
            icon = Icons.Default.ColorLens,
            title = "Klasör Rengi Otomatik",
            subtitle = "Klasör ikonlarından dominant renk hesapla ve otomatik ata",
            checked = autoFolderColor,
            onCheckedChange = {
                autoFolderColor = it
                AppPrefs.setAutoFolderColorEnabled(context, it)
            }
        )
    }

    // ── İkon Pack Seçimi ──────────────────────────────────────────────────
    val iconPacks = remember { IconPackManager.getInstalledIconPacks(context) }
    if (iconPacks.isNotEmpty()) {
        var selectedPack by remember { mutableStateOf(AppPrefs.getIconPack(context)) }
        var showPackMenu by remember { mutableStateOf(false) }
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPackMenu = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("İkon Pack", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(
                        if (selectedPack.isEmpty()) "Varsayılan (sistem ikonları)"
                        else iconPacks.firstOrNull { it.packageName == selectedPack }?.label ?: selectedPack,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                DropdownMenu(expanded = showPackMenu, onDismissRequest = { showPackMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Varsayılan") },
                        onClick = {
                            selectedPack = ""
                            AppPrefs.setIconPack(context, "")
                            IconPackManager.clearCache()
                            showPackMenu = false
                        }
                    )
                    iconPacks.forEach { pack ->
                        DropdownMenuItem(
                            text = { Text(pack.label) },
                            onClick = {
                                selectedPack = pack.packageName
                                AppPrefs.setIconPack(context, pack.packageName)
                                IconPackManager.clearCache()
                                showPackMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
