package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppAnalytics

private val FolderSheetScrim = Color(0x66000000)

internal fun List<AppInfo>.sortedByMode(mode: AllAppsSortMode): List<AppInfo> = when (mode) {
    AllAppsSortMode.ALPHA            -> sortedBy { it.appName.lowercase() }
    AllAppsSortMode.ALPHA_DESC       -> sortedByDescending { it.appName.lowercase() }
    AllAppsSortMode.USAGE            -> sortedByDescending { it.usageCount }
    AllAppsSortMode.USAGE_ASC        -> sortedBy { it.usageCount }
    AllAppsSortMode.SIZE_DESC        -> sortedByDescending { it.appSizeBytes }
    AllAppsSortMode.SIZE_ASC         -> sortedBy { it.appSizeBytes }
    AllAppsSortMode.INSTALL_DATE     -> sortedByDescending { it.installTime }
    AllAppsSortMode.INSTALL_DATE_ASC -> sortedBy { it.installTime }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderSheet(
    folder: AppFolder,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit,
    onAppLongClick: ((AppInfo) -> Unit)? = null,
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

    val blurEnabled = remember { com.armutlu.apporganizer.utils.AppPrefs.isFolderBlurEnabled(context) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = if (blurEnabled) surface.copy(alpha = 0.9f) else surface,
        scrimColor       = FolderSheetScrim,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
        ) {
            val catColor = remember(folder.category.colorHex, customColor) {
                val hex = customColor.ifBlank { null } ?: folder.category.colorHex
                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(primary)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.size(52.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(catColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(text = customEmoji.ifBlank { folder.category.iconEmoji }, fontSize = 28.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(customName.ifBlank { folder.category.categoryName }, color = onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${folder.apps.size} uygulama", color = onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier.size(36.dp)
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
                    currentName  = customName.ifBlank { folder.category.categoryName },
                    currentEmoji = customEmoji.ifBlank { folder.category.iconEmoji },
                    currentColor = customColor,
                    onDismiss    = { showEditDialog = false },
                    onSave       = { newName, newEmoji, newColor ->
                        val nameToSave  = if (newName  == folder.category.categoryName) "" else newName
                        val emojiToSave = if (newEmoji == folder.category.iconEmoji)    "" else newEmoji
                        customName  = nameToSave
                        customEmoji = emojiToSave
                        customColor = newColor
                        com.armutlu.apporganizer.utils.AppPrefs.setFolderCustomName(context, catId, nameToSave)
                        com.armutlu.apporganizer.utils.AppPrefs.setFolderCustomEmoji(context, catId, emojiToSave)
                        com.armutlu.apporganizer.utils.AppPrefs.setFolderCustomColor(context, catId, newColor)
                        showEditDialog = false
                    }
                )
            }

            // Klasör içi arama — varsayılan KAPALI, Ayarlar'dan açılabilir
            val folderSearchEnabled = remember { com.armutlu.apporganizer.utils.AppPrefs.isFolderSearchEnabled(context) }
            if (folderSearchEnabled) Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(onSurface.copy(alpha = 0.10f)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = textSecondary, modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("${customName.ifBlank { folder.category.categoryName }} içinde ara...", color = textSecondary, fontSize = 13.sp)
                    }
                    BasicTextField(value = searchQuery, onValueChange = { searchQuery = it },
                        singleLine = true, cursorBrush = SolidColor(primary),
                        textStyle = TextStyle(color = onSurface, fontSize = 13.sp))
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(Icons.Default.Close, "Aramayı temizle", tint = textSecondary,
                        modifier = Modifier.size(16.dp).clickable { searchQuery = "" })
                }
            }

            val badgeCounts by com.armutlu.apporganizer.service.AppNotificationListenerService.badgeCounts.collectAsState()
            val latestTexts by com.armutlu.apporganizer.service.AppNotificationListenerService.latestTexts.collectAsState()
            val appsWithNotifs = remember(folder.apps, badgeCounts) {
                folder.apps.filter { (badgeCounts[it.packageName] ?: 0) > 0 }
            }
            if (appsWithNotifs.isNotEmpty()) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    items(appsWithNotifs, key = { it.packageName }) { app ->
                        val count = badgeCounts[app.packageName] ?: 0
                        val text  = latestTexts[app.packageName] ?: ""
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                .background(onSurface.copy(alpha = 0.10f))
                                .semantics {
                                    contentDescription = "${app.appName}, $count bildirim"
                                    onClick(label = "Uygulamayı aç") { onAppClick(app.packageName); onDismiss(); true }
                                }
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    AppAnalytics.appLaunched(app.packageName, "folder")
                                    onAppClick(app.packageName); onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(20.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape).background(primary),
                                    contentAlignment = Alignment.Center) {
                                    Text("$count", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(app.appName, color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (text.isNotEmpty()) Text(text, color = onSurface.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                itemsIndexed(AllAppsSortMode.entries, key = { _, mode -> mode.name }) { _, mode ->
                    val active = sortMode == mode
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (active) primary else onSurface.copy(alpha = 0.12f))
                            .clickable {
                                sortMode = mode
                                com.armutlu.apporganizer.utils.AppPrefs.setFolderSortMode(context, mode.name)
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(mode.label, fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else textSecondary)
                    }
                }
            }

            Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

            if (sortedApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isNotEmpty()) "\"$searchQuery\" bulunamadı" else "Bu klasör boş",
                        color = onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                    modifier = Modifier.fillMaxWidth()) {
                    items(sortedApps, key = { it.packageName }) { app ->
                        AppIconView(
                            app       = app,
                            onClick   = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                AppAnalytics.appLaunched(app.packageName, "folder")
                                onAppClick(app.packageName); onDismiss()
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
