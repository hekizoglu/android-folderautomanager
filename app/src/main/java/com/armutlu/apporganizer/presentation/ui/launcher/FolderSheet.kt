package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppAnalytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContextMenuSheet(
    folder: AppFolder,
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenAllApps: () -> Unit,
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(folder.category.colorHex))
    }.getOrDefault(Color(0xFF00897B))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.2f)))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
        ) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(catColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(folder.category.iconEmoji, fontSize = 22.sp) }
                Column {
                    Text(folder.category.categoryName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${folder.apps.size} uygulama", color = Color.White.copy(0.55f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.08f)))
            // Klasörü Aç
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFolder)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text("Klasörü Aç", color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.08f)))
            // Tüm Uygulamalar
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAllApps)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Apps, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text("Tüm Uygulamalara Git", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

private val SheetBackground = Color(0xFF1A1A2A)
private val DividerColor    = Color(0xFFFFFFFF).copy(alpha = 0.08f)
private val TealColor       = Color(0xFF00897B)
private val TextSecondary   = Color.White.copy(alpha = 0.55f)

private fun List<AppInfo>.sortedByMode(mode: AllAppsSortMode): List<AppInfo> = when (mode) {
    AllAppsSortMode.ALPHA        -> sortedBy { it.appName.lowercase() }
    AllAppsSortMode.USAGE        -> sortedByDescending { it.usageCount }
    AllAppsSortMode.SIZE_DESC    -> sortedByDescending { it.appSizeBytes }
    AllAppsSortMode.SIZE_ASC     -> sortedBy { it.appSizeBytes }
    AllAppsSortMode.INSTALL_DATE -> sortedByDescending { it.installTime }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSheet(
    folder: AppFolder,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((com.armutlu.apporganizer.domain.models.AppInfo) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var sortMode by remember {
        val saved = com.armutlu.apporganizer.utils.AppPrefs.getFolderSortMode(context)
        mutableStateOf(AllAppsSortMode.entries.firstOrNull { it.name == saved } ?: AllAppsSortMode.ALPHA)
    }
    var searchQuery by remember { mutableStateOf("") }
    val catId = folder.category.categoryId
    var customName by remember(catId) {
        mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomNames(context)[catId] ?: "")
    }
    var customEmoji by remember(catId) {
        mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomEmojis(context)[catId] ?: "")
    }
    var customColor by remember(catId) {
        mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.getFolderCustomColors(context)[catId] ?: "")
    }
    var showEditDialog by remember { mutableStateOf(false) }

    val sortedApps = remember(folder.apps, sortMode, searchQuery) {
        val trLocale = java.util.Locale("tr")
        val base = if (searchQuery.isBlank()) folder.apps
                   else {
                       val q = searchQuery.lowercase(trLocale)
                       folder.apps.filter { it.appName.lowercase(trLocale).contains(q) }
                   }
        base.sortedByMode(sortMode)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SheetBackground,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            val catColor = remember(folder.category.colorHex, customColor) {
                val hex = customColor.ifBlank { null } ?: folder.category.colorHex
                runCatching { Color(android.graphics.Color.parseColor(hex)) }
                    .getOrDefault(TealColor)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(catColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customEmoji.ifBlank { folder.category.iconEmoji },
                        fontSize = 28.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customName.ifBlank { folder.category.categoryName },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${folder.apps.size} uygulama", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(18.dp))
                }
            }

            if (showEditDialog) {
                FolderRenameDialog(
                    currentName = customName.ifBlank { folder.category.categoryName },
                    currentEmoji = customEmoji.ifBlank { folder.category.iconEmoji },
                    currentColor = customColor,
                    onDismiss = { showEditDialog = false },
                    onSave = { newName, newEmoji, newColor ->
                        val nameToSave = if (newName == folder.category.categoryName) "" else newName
                        val emojiToSave = if (newEmoji == folder.category.iconEmoji) "" else newEmoji
                        customName = nameToSave
                        customEmoji = emojiToSave
                        customColor = newColor
                        com.armutlu.apporganizer.utils.AppPrefs.setFolderCustomName(context, catId, nameToSave)
                        com.armutlu.apporganizer.utils.AppPrefs.setFolderCustomEmoji(context, catId, emojiToSave)
                        com.armutlu.apporganizer.utils.AppPrefs.setFolderCustomColor(context, catId, newColor)
                        showEditDialog = false
                    }
                )
            }

            // ── Arama çubuğu ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "${customName.ifBlank { folder.category.categoryName }} icinde ara...",
                            color = TextSecondary, fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        cursorBrush = SolidColor(TealColor),
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" }
                    )
                }
            }

            // ── Bildirim Satırı ───────────────────────────────────────────────
            val badgeCounts by com.armutlu.apporganizer.service.AppNotificationListenerService.badgeCounts.collectAsState()
            val latestTexts by com.armutlu.apporganizer.service.AppNotificationListenerService.latestTexts.collectAsState()
            val appsWithNotifs = remember(folder.apps, badgeCounts) {
                folder.apps.filter { (badgeCounts[it.packageName] ?: 0) > 0 }
            }
            if (appsWithNotifs.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(appsWithNotifs, key = { it.packageName }) { app ->
                        val count = badgeCounts[app.packageName] ?: 0
                        val text = latestTexts[app.packageName] ?: ""
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    AppAnalytics.appLaunched(app.packageName, "folder")
                                    onAppClick(app.packageName)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(TealColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(app.appName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (text.isNotEmpty()) {
                                        Text(text, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
            }

            // ── Sıralama chip'leri ────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                itemsIndexed(AllAppsSortMode.entries, key = { _, mode -> mode.name }) { _, mode ->
                    val active = sortMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) TealColor else Color.White.copy(alpha = 0.12f))
                            .clickable {
                                sortMode = mode
                                com.armutlu.apporganizer.utils.AppPrefs.setFolderSortMode(context, mode.name)
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            mode.label, fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Color.White else TextSecondary
                        )
                    }
                }
            }

            // ── Divider ───────────────────────────────────────────────────────
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

            // ── App grid / Empty state ────────────────────────────────────────
            if (sortedApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (searchQuery.isNotEmpty()) "\"$searchQuery\" bulunamadı"
                        else "Bu klasör boş",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns        = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                    modifier       = Modifier.fillMaxWidth(),
                ) {
                    items(sortedApps, key = { it.packageName }) { app ->
                        AppIconView(
                            app       = app,
                            onClick   = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                AppAnalytics.appLaunched(app.packageName, "folder")
                                onAppClick(app.packageName)
                                onDismiss()
                            },
                            onLongClick = { onAppLongClick?.invoke(app) },
                            iconSize  = 56.dp,
                            showLabel = true,
                        )
                    }
                }
            }
        }
    }
}

private val EMOJI_PICKER = listOf(
    "📁","📝","🎮","👥","🛍️","📰","❤️","💰","🎓","🔧",
    "✈️","🎬","🍔","📸","📦","⭐","🏠","🎵","💼","🎯",
    "🔑","📱","💻","🎁","🌟","🚀","🎪","🏆","💡","📚",
    "🌙","☀️","🎨","🏋️","🐶","🌿","🔔","💬","🗓️","🧩"
)

private val COLOR_PRESETS = listOf(
    "" to "Varsayilan",
    "#00897B" to "Turkuaz",
    "#1976D2" to "Mavi",
    "#7B1FA2" to "Mor",
    "#D32F2F" to "Kirmizi",
    "#F57C00" to "Turuncu",
    "#388E3C" to "Yesil",
    "#C2185B" to "Pembe",
    "#FBC02D" to "Sari",
    "#303F9F" to "Lacivert",
)

@Composable
private fun FolderRenameDialog(
    currentName: String,
    currentEmoji: String,
    currentColor: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, color: String) -> Unit,
) {
    var nameField by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2E),
        title = {
            Text("Klasoru Duzenle", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    label = { Text("Klasor adi", color = Color.White.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00897B),
                        unfocusedBorderColor = Color.White.copy(0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF00897B),
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Emoji sec", color = Color.White.copy(0.6f), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(EMOJI_PICKER, key = { _, emoji -> emoji }) { _, emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (emoji == selectedEmoji) Color(0xFF00897B).copy(0.35f)
                                    else Color.White.copy(0.08f)
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
                Text("Renk sec", color = Color.White.copy(0.6f), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(COLOR_PRESETS, key = { _, preset -> preset.first }) { _, preset ->
                        val hex = preset.first
                        val isSelected = selectedColor == hex
                        val resolvedColor = if (hex.isBlank()) Color.White.copy(0.2f)
                            else runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(resolvedColor)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (nameField.isNotBlank()) onSave(nameField.trim(), selectedEmoji, selectedColor) }) {
                Text("Kaydet", color = Color(0xFF00897B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Iptal", color = Color.White.copy(0.6f))
            }
        }
    )
}
