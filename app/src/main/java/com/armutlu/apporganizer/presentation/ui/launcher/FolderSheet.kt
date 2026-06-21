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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppAnalytics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContextMenuSheet(
    folder: AppFolder,
    allFolders: List<AppFolder> = emptyList(),
    onDismiss: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenAllApps: () -> Unit,
    onMove: ((newIndex: Int) -> Unit)? = null,
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(folder.category.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface

    var showMoveDialog by remember { mutableStateOf(false) }
    var selectedMoveIndex by remember { mutableStateOf(-1) }
    val currentIndex = allFolders.indexOfFirst { it.category.categoryId == folder.category.categoryId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(onSurface.copy(0.2f)))
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
                    Text(folder.category.categoryName, color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${folder.apps.size} uygulama${if (currentIndex >= 0) " · ${currentIndex + 1}. sıra" else ""}", color = onSurface.copy(0.55f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(onSurface.copy(0.08f)))
            // Klasörü Aç
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenFolder)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, null, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.folder_open), color = onSurface, fontSize = 15.sp)
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(onSurface.copy(0.08f)))
            // Klasörü Taşı
            if (allFolders.size > 1 && onMove != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showMoveDialog = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, null, tint = onSurface.copy(0.7f), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.folder_move), color = onSurface, fontSize = 15.sp)
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(onSurface.copy(0.08f)))
            }
            // Tüm Uygulamalar
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAllApps)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Apps, null, tint = onSurface.copy(0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.folder_goto_all_apps), color = onSurface, fontSize = 15.sp)
            }
        }
    }

    if (showMoveDialog) {
        FolderPositionPickerSheet(
            allFolders = allFolders,
            currentIndex = currentIndex,
            selectedIndex = selectedMoveIndex,
            onSelectIndex = { selectedMoveIndex = it },
            onConfirm = {
                if (selectedMoveIndex >= 0 && selectedMoveIndex != currentIndex) {
                    onMove?.invoke(selectedMoveIndex)
                    showMoveDialog = false
                    selectedMoveIndex = -1
                    onDismiss()
                }
            },
            onDismiss = { showMoveDialog = false; selectedMoveIndex = -1 }
        )
    }
}

private const val FOLDERS_PER_PAGE = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPositionPickerSheet(
    allFolders: List<AppFolder>,
    currentIndex: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primary   = MaterialTheme.colorScheme.primary
    val surface   = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val haptic    = LocalHapticFeedback.current

    val pageCount = ((allFolders.size - 1) / FOLDERS_PER_PAGE) + 1
    var currentPage by remember { mutableStateOf(if (currentIndex >= 0) currentIndex / FOLDERS_PER_PAGE else 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(onSurface.copy(0.2f)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Başlık
            Text(
                text = "Yeni konum seç",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Text(
                text = "Mevcut: ${currentIndex + 1}. sıra · Toplam: ${allFolders.size} klasör",
                fontSize = 12.sp,
                color = onSurface.copy(0.55f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Klasör konum kutucukları — sayfadaki 8 adet
            val pageStart = currentPage * FOLDERS_PER_PAGE
            val pageEnd   = minOf(pageStart + FOLDERS_PER_PAGE, allFolders.size)
            val pageItems = allFolders.subList(pageStart, pageEnd)

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                itemsIndexed(pageItems) { localIdx, f ->
                    val globalIdx = pageStart + localIdx
                    val isCurrent  = globalIdx == currentIndex
                    val isSelected = globalIdx == selectedIndex
                    val bgColor = when {
                        isSelected -> primary
                        isCurrent  -> primary.copy(alpha = 0.18f)
                        else       -> onSurface.copy(alpha = 0.07f)
                    }
                    val borderColor = when {
                        isSelected -> primary
                        isCurrent  -> primary.copy(alpha = 0.6f)
                        else       -> Color.Transparent
                    }

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isCurrent) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectIndex(globalIdx)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = f.category.iconEmoji,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${globalIdx + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) surface else onSurface
                        )
                        Text(
                            text = f.category.categoryName,
                            fontSize = 9.sp,
                            color = (if (isSelected) surface else onSurface).copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sayfa seçici — birden fazla sayfa varsa göster
            if (pageCount > 1) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Sayfa",
                    fontSize = 11.sp,
                    color = onSurface.copy(0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(pageCount) { pageIdx ->
                        val isActivePage = pageIdx == currentPage
                        val rangeStart = pageIdx * FOLDERS_PER_PAGE + 1
                        val rangeEnd   = minOf((pageIdx + 1) * FOLDERS_PER_PAGE, allFolders.size)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActivePage) primary else onSurface.copy(0.1f))
                                .clickable { currentPage = pageIdx }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$rangeStart–$rangeEnd",
                                fontSize = 11.sp,
                                fontWeight = if (isActivePage) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActivePage) surface else onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Onayla / İptal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel), color = onSurface.copy(0.7f))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onConfirm,
                    enabled = selectedIndex >= 0 && selectedIndex != currentIndex
                ) {
                    Text(
                        stringResource(R.string.folder_move_confirm),
                        color = if (selectedIndex >= 0 && selectedIndex != currentIndex) primary else onSurface.copy(0.3f)
                    )
                }
            }
        }
    }
}

// Sabit renk — tema bağımsız overlay
private val FolderSheetScrim = Color(0x66000000)

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
    val haptic    = LocalHapticFeedback.current
    val context   = LocalContext.current
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface
    val textSecondary = onSurface.copy(alpha = 0.55f)
    val dividerColor  = onSurface.copy(alpha = 0.08f)
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

    val blurEnabled = com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(LocalContext.current)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = if (blurEnabled) surface.copy(alpha = 0.9f) else surface,
        scrimColor       = FolderSheetScrim,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            val catColor = remember(folder.category.colorHex, customColor) {
                val hex = customColor.ifBlank { null } ?: folder.category.colorHex
                runCatching { Color(android.graphics.Color.parseColor(hex)) }
                    .getOrDefault(primary)
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
                        color = onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${folder.apps.size} uygulama", color = onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(onSurface.copy(alpha = 0.08f))
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, stringResource(R.string.folder_edit), tint = onSurface.copy(0.6f), modifier = Modifier.size(18.dp))
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
                    .background(onSurface.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = textSecondary, modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "${customName.ifBlank { folder.category.categoryName }} içinde ara...",
                            color = textSecondary, fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        cursorBrush = SolidColor(primary),
                        textStyle = TextStyle(color = onSurface, fontSize = 13.sp)
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, "Aramayı temizle",
                        tint = textSecondary,
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
                                .background(onSurface.copy(alpha = 0.10f))
                                .semantics {
                                    contentDescription = "${app.appName}, $count bildirim"
                                    onClick(label = "Aç") { onAppClick(app.packageName); onDismiss(); true }
                                }
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
                                        .background(primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$count", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(app.appName, color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (text.isNotEmpty()) {
                                        Text(text, color = onSurface.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
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
                            .background(if (active) primary else onSurface.copy(alpha = 0.12f))
                            .clickable {
                                sortMode = mode
                                com.armutlu.apporganizer.utils.AppPrefs.setFolderSortMode(context, mode.name)
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(
                            mode.label, fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else textSecondary
                        )
                    }
                }
            }

            // ── Divider ───────────────────────────────────────────────────────
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

            // ── App grid / Empty state ────────────────────────────────────────
            if (sortedApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (searchQuery.isNotEmpty()) "\"$searchQuery\" bulunamadı"
                        else "Bu klasör boş",
                        color = onSurface.copy(alpha = 0.5f), fontSize = 16.sp
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
    "" to "Varsayılan",
    "#00897B" to "Turkuaz",
    "#1976D2" to "Mavi",
    "#7B1FA2" to "Mor",
    "#D32F2F" to "Kırmızı",
    "#F57C00" to "Turuncu",
    "#388E3C" to "Yeşil",
    "#C2185B" to "Pembe",
    "#FBC02D" to "Sarı",
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
    val primary   = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surface,
        title = {
            Text(stringResource(R.string.folder_edit), color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    label = { Text(stringResource(R.string.folder_rename_hint), color = onSurface.copy(0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = onSurface.copy(0.25f),
                        focusedTextColor = onSurface,
                        unfocusedTextColor = onSurface,
                        cursorColor = primary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.folder_emoji_pick), color = onSurface.copy(0.6f), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(EMOJI_PICKER, key = { _, emoji -> emoji }) { _, emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (emoji == selectedEmoji) primary.copy(0.35f)
                                    else onSurface.copy(0.08f)
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 20.sp)
                        }
                    }
                }
                Text(stringResource(R.string.folder_color_pick), color = onSurface.copy(0.6f), fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(COLOR_PRESETS, key = { _, preset -> preset.first }) { _, preset ->
                        val hex = preset.first
                        val isSelected = selectedColor == hex
                        val resolvedColor = if (hex.isBlank()) onSurface.copy(0.2f)
                            else runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(onSurface)
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
                Text(stringResource(R.string.btn_save), color = primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = onSurface.copy(0.6f))
            }
        }
    )
}
