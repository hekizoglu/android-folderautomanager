package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme
import com.armutlu.apporganizer.presentation.ui.theme.ThemePreferences
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.BackupManager
import com.armutlu.apporganizer.utils.DockPrefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {},
    onSendBugReport: () -> Unit = {}
) {
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val state          by viewModel.screenState.collectAsState()
    val logs           by viewModel.liveDebugLogs.collectAsState()
    val hiddenApps     by viewModel.hiddenApps.collectAsState()
    val otherApps      by viewModel.otherApps.collectAsState()
    val clipboard      = LocalClipboardManager.current
    val context        = LocalContext.current
    var debugExpanded  by remember { mutableStateOf(false) }
    val scope          = rememberCoroutineScope()
    val themePrefs     = remember { ThemePreferences(context) }
    val currentTheme   by themePrefs.themeFlow.collectAsState(initial = AppTheme.TEAL)
    val currentFont    by themePrefs.fontFlow.collectAsState(initial = AppFont.DEFAULT)

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }

    var isDefault by remember { mutableStateOf(isDefaultLauncher()) }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefault = isDefaultLauncher() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // â"€â"€ Görünüm â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            // ── Tema ─────────────────────────────────────────────────────────
            item { SettingsSectionTitle("Görünüm") }
            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Renk Teması", fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
                                    Text(theme.label, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Text("Yazı Tipi", fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
            }

            // ── Arka Plan ────────────────────────────────────────────────────
            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ana Ekran Arka Planı", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        // Duvar kağıdı / Düz renk seçimi
                        var bgType by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getBgType(context)) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("wallpaper" to "Duvar Kağıdı", "solid" to "Düz Renk").forEach { (type, label) ->
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
                        // Düz renk arka plan rengi seçimi
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
                                        Text(colorLabel, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.width(52.dp))
                                    }
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        // Yazı transparanlığı
                        var textAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getTextAlpha(context)) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Yazı Opaklığı", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("${(textAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        androidx.compose.material3.Slider(
                            value = textAlpha,
                            onValueChange = {
                                textAlpha = it
                                com.armutlu.apporganizer.utils.AppPrefs.setTextAlpha(context, it)
                            },
                            valueRange = 0.4f..1.0f,
                            steps = 11
                        )
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        var folderSizeDp by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderSizeDp(context)) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Klasör Boyutu", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("${folderSizeDp}dp", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        androidx.compose.material3.Slider(
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
            }

            // Yazı rengi paleti
            item {
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
                        Text("Yazı Rengi", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(labelColorPresets) { (hex, name) ->
                                val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(androidx.compose.ui.graphics.Color.White)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(if (selectedLabel == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                        .clickable {
                                            selectedLabel = hex
                                            com.armutlu.apporganizer.utils.AppPrefs.setLabelColor(context, hex)
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // ── Launcher ─────────────────────────────────────────────────────
            item { SettingsSectionTitle("Launcher") }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home, null,
                            tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Varsayılan Launcher", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                if (isDefault) "Aktif" else "Ayarlanmadı",
                                fontSize = 12.sp,
                                color = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val launcherAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val rm = context.getSystemService(RoleManager::class.java)
                                if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                                    roleRequestLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                                }
                            } else {
                                val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(i)
                            }
                        }
                        if (isDefault) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                OutlinedButton(
                                    onClick = launcherAction,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Değiştir", fontSize = 12.sp) }
                            }
                        } else {
                            Button(
                                onClick = launcherAction,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) { Text("Ayarla", fontSize = 13.sp) }
                        }
                    }
                }
            }

            // ── Dock Yönetimi ─────────────────────────────────────────────────
            item { SettingsSectionTitle("Dock Uygulamaları") }
            item {
                var dockPkgs by remember { mutableStateOf(DockPrefs.getDockPackages(context)) }
                val pm = context.packageManager
                SettingsCard {
                    if (dockPkgs.isEmpty()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Dock boş", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        dockPkgs.forEachIndexed { index, pkg ->
                            val appName = remember(pkg) {
                                runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                            }
                            if (index > 0) Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(appName, Modifier.weight(1f), fontSize = 14.sp)
                                IconButton(onClick = {
                                    DockPrefs.removeFromDock(context, pkg)
                                    dockPkgs = DockPrefs.getDockPackages(context)
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, "Kaldır", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Varsayılana Sıfırla",
                        subtitle = "Telefon, Mesaj, Kamera, Tarayıcı",
                        onClick = {
                            DockPrefs.saveDockPackages(context, emptyList())
                            dockPkgs = DockPrefs.getDockPackages(context)
                        }
                    )
                }
            }

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
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    // Kullanılmayan uygulamaları gri göster
                    var unusedGreyDays by remember {
                        mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getUnusedGreyDays(context))
                    }
                    val greyOptions = listOf(0 to "Kapalı", 7 to "7 gün", 14 to "14 gün", 30 to "30 gün")
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Kullanılmayan Uygulamalar Gri", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Hiç açılmamış uygulamalar soluk gösterilir",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

            // â"€â"€ Uygulama Yönetimi â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            // ── Ana Ekran Özellikleri ─────────────────────────────────────────
            item { SettingsSectionTitle("Ana Ekran Özellikleri") }
            item {
                var swipeHintEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSwipeHintEnabled(context)) }
                var newBadgeEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNewBadgeEnabled(context)) }
                var folderCountVisible by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderCountVisible(context)) }
                var folderSwipeHint by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFolderSwipeHintEnabled(context)) }
                var notifTextEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNotificationTextEnabled(context)) }
                var hideNavButtons by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isNavButtonsHidden(context)) }
                var allAppsBgAlpha by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getAllAppsBgAlpha(context)) }
                var suggestionsEnabled by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isSuggestionsEnabled(context)) }
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Uygulama Onerileri",
                        subtitle = "Arama cubugununun altinda son kullanilan 4 uygulama",
                        checked = suggestionsEnabled,
                        onCheckedChange = {
                            suggestionsEnabled = it
                            com.armutlu.apporganizer.utils.AppPrefs.setSuggestionsEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.SwipeUp,
                        title = "Swipe-up Ipucu",
                        subtitle = "Ana ekranda yukari kaydirma animasyonu goster",
                        checked = swipeHintEnabled,
                        onCheckedChange = {
                            swipeHintEnabled = it
                            com.armutlu.apporganizer.utils.AppPrefs.setSwipeHintEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.NewReleases,
                        title = "YENI Badge",
                        subtitle = "7 gun icinde kurulan uygulamalara rozet goster",
                        checked = newBadgeEnabled,
                        onCheckedChange = {
                            newBadgeEnabled = it
                            com.armutlu.apporganizer.utils.AppPrefs.setNewBadgeEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.FormatListNumbered,
                        title = "Klasor Uygulama Sayisi",
                        subtitle = "Klasor simgesinin altinda uygulama adedini goster",
                        checked = folderCountVisible,
                        onCheckedChange = {
                            folderCountVisible = it
                            com.armutlu.apporganizer.utils.AppPrefs.setFolderCountVisible(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Folder,
                        title = "Klasor Swipe Ipucu",
                        subtitle = "Klasorde en cok kullanilan uygulamayi goster",
                        checked = folderSwipeHint,
                        onCheckedChange = {
                            folderSwipeHint = it
                            com.armutlu.apporganizer.utils.AppPrefs.setFolderSwipeHintEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Bildirim Metni",
                        subtitle = "Klasor ve uygulamalarin altinda son bildirimi goster",
                        checked = notifTextEnabled,
                        onCheckedChange = {
                            notifTextEnabled = it
                            com.armutlu.apporganizer.utils.AppPrefs.setNotificationTextEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.HideSource,
                        title = "Sistem Navigasyonunu Gizle",
                        subtitle = "Tam ekran launcher - geri/home/recents butonsuz",
                        checked = hideNavButtons,
                        onCheckedChange = {
                            hideNavButtons = it
                            com.armutlu.apporganizer.utils.AppPrefs.setNavButtonsHidden(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Opacity, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Tum Uygulamalar Arka Plan", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text("Opaklık: ${(allAppsBgAlpha * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = allAppsBgAlpha,
                            onValueChange = {
                                allAppsBgAlpha = it
                                com.armutlu.apporganizer.utils.AppPrefs.setAllAppsBgAlpha(context, it)
                            },
                            valueRange = 0.1f..1.0f,
                            steps = 8
                        )
                    }
                }
            }

            // ── Widget Alani ──────────────────────────────────────────────────
            item { SettingsSectionTitle("Widget") }
            item {
                var widgetAreaEnabledLocal by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isWidgetAreaEnabled(context)) }
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Widgets,
                        title = "Widget Alani",
                        subtitle = "Ana ekranda widget gosterimine izin ver",
                        checked = widgetAreaEnabledLocal,
                        onCheckedChange = {
                            widgetAreaEnabledLocal = it
                            com.armutlu.apporganizer.utils.AppPrefs.setWidgetAreaEnabled(context, it)
                        }
                    )
                }
            }

            // ── İkon Paketi ───────────────────────────────────────────────────
            item { SettingsSectionTitle("İkon Paketi") }
            item {
                val iconPacks = remember {
                    com.armutlu.apporganizer.utils.IconPackManager.getInstalledIconPacks(context)
                }
                var selectedPack by remember {
                    mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getIconPack(context))
                }
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPack = ""
                                com.armutlu.apporganizer.utils.AppPrefs.setIconPack(context, "")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Android, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sistem İkonları", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Varsayılan uygulama ikonları", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selectedPack.isEmpty()) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (iconPacks.isEmpty()) {
                        Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Kurulu ikon paketi bulunamadı. Play Store'dan bir ikon paketi yükleyin.",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        iconPacks.forEach { pack ->
                            Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPack = pack.packageName
                                        com.armutlu.apporganizer.utils.AppPrefs.setIconPack(context, pack.packageName)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(pack.label, Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                if (selectedPack == pack.packageName) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { SettingsSectionTitle("Uygulama Yönetimi") }
            item {
                SettingsCard {
                    var manufacturerClassify by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isManufacturerClassifyEnabled(context)) }
                    SettingsSwitchRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Üretici Sınıflandırması",
                        subtitle = "Samsung/Huawei/Xiaomi uygulamalarını otomatik kategorilendir",
                        checked = manufacturerClassify,
                        onCheckedChange = {
                            manufacturerClassify = it
                            com.armutlu.apporganizer.utils.AppPrefs.setManufacturerClassifyEnabled(context, it)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.AutoFixHigh,
                        title = "Sınıflandırılmamışları Sınıflandır",
                        subtitle = "Kategorisiz uygulamaları otomatik ata",
                        onClick = { viewModel.classifyUnclassifiedApps() }
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsButtonRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Tüm Kategorileri Sıfırla",
                        subtitle = "Tüm atamaları sil ve yeniden sınıflandır",
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.resetAndReclassifyAllApps() }
                    )
                }
            }

            // â"€â"€ Hakkında â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€
            // ── Gizli Uygulamalar ─────────────────────────────────────────────
            if (hiddenApps.isNotEmpty()) {
                item { SettingsSectionTitle("Gizli Uygulamalar (${hiddenApps.size})") }
                item {
                    SettingsCard {
                        hiddenApps.forEachIndexed { index, app ->
                            if (index > 0) Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(app.appName, Modifier.weight(1f), fontSize = 14.sp)
                                OutlinedButton(
                                    onClick = { viewModel.unhideApp(app.packageName) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Göster", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }

            // ── Diğer Klasörü (Bilinmeyen Uygulamalar) ───────────────────────
            if (otherApps.isNotEmpty()) {
                item { SettingsSectionTitle("Diğer Klasörü — Bilinmeyenler (${otherApps.size})") }
                item {
                    SettingsCard {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(
                                "Bu uygulamalar otomatik kategorilendirilemeyen uygulamalardır. " +
                                "İleride web sorgusu ile otomatik atanacak.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                        otherApps.take(20).forEachIndexed { index, app ->
                            if (index > 0) Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(app.appName, fontSize = 14.sp, maxLines = 1)
                                    Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                        if (otherApps.size > 20) {
                            Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                Text("...ve ${otherApps.size - 20} uygulama daha", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { SettingsSectionTitle("Hakkında") }
            item {
                SettingsCard {
                    SettingsInfoRow(
                        icon = Icons.Default.Apps,
                        title = "App Organizer",
                        subtitle = "v1.0 beta"
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Person,
                        title = "Geliştirici",
                        subtitle = "Hüseyin Ekizoğlu"
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Code,
                        title = "Kaynak Kod",
                        subtitle = "github.com/hekizoglu/android-folderautomanager"
                    )
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    SettingsInfoRow(
                        icon = Icons.Default.Storage,
                        title = "Veritabanı",
                        subtitle = "${state.apps.size} uygulama · ${state.categories.size} kategori"
                    )
                }
            }

            // Backup & Restore
            item { SettingsSectionTitle("Yedek / Geri Yukle") }
            item {
                var autoBackup by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isAutoBackupEnabled(context)) }
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Autorenew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Otomatik Yedekleme", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Uygulama acildiginda otomatik JSON yedegi al", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoBackup, onCheckedChange = {
                            autoBackup = it
                            com.armutlu.apporganizer.utils.AppPrefs.setAutoBackupEnabled(context, it)
                        })
                    }
                }
            }
            item {
                var backupLoading by remember { mutableStateOf(false) }
                val filePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        backupLoading = true
                        runCatching {
                            val json = context.contentResolver.openInputStream(uri)
                                ?.bufferedReader()?.readText() ?: return@runCatching
                            val result = viewModel.importBackup(json)
                            android.widget.Toast.makeText(
                                context,
                                if (result.success) "${result.updatedCount} uygulama geri yuklendi"
                                else "Geri yukleme basarisiz: ${result.error}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        backupLoading = false
                    }
                }
                val shareLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {}

                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !backupLoading) {
                                scope.launch {
                                    backupLoading = true
                                    val intent = viewModel.exportBackup(context)
                                    if (intent != null) {
                                        shareLauncher.launch(Intent.createChooser(intent, "Yedegi paylas"))
                                    } else {
                                        android.widget.Toast.makeText(context, "Disa aktarma basarisiz", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    backupLoading = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Yedek Al", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Kategori atamalarini JSON olarak disa aktar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (backupLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !backupLoading) { filePickerLauncher.launch("application/json") }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Geri Yukle", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("JSON yedek dosyasindan kategorileri ice aktar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Debug ────────────────────────────────────────────────────────
            if (logs.isNotEmpty()) {
                item { SettingsSectionTitle("Debug") }
                item {
                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { debugExpanded = !debugExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BugReport,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Loglar", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text("${logs.size} satır", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(
                                if (debugExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (debugExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                // Son 30 log
                                logs.takeLast(30).forEach { line ->
                                    Text(
                                        line,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(viewModel.getDebugLogs()))
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Kopyala", fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.clearDebugLogs() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Temizle", fontSize = 13.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Yardımcı bileşenler ────────────────────────────────────────────────────

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 6.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun SettingsButtonRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Eski uyumluluk
@Composable
fun SectionHeader(title: String) = SettingsSectionTitle(title)

@Composable
fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) =
    SettingsSwitchRow(Icons.Default.Settings, title, description, checked, onCheckedChange)

@Composable
fun SettingButton(title: String, description: String, onClick: () -> Unit) =
    SettingsButtonRow(Icons.Default.ChevronRight, title, description, onClick = onClick)

@Composable
fun SettingInfo(title: String, description: String) =
    SettingsInfoRow(Icons.Default.Info, title, description)

@Composable
fun DebugInfoCard(
    appCount: Int, categoryCount: Int, error: String?, logs: List<String>,
    launcherInfo: String, a11yActive: Boolean,
    onSendBugReport: () -> Unit, onClearLogs: () -> Unit
) {}

