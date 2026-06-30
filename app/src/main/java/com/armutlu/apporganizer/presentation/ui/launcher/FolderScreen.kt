package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import com.armutlu.apporganizer.service.AppNotificationListenerService
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs

@Composable
fun FolderScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    val folder by viewModel.openFolder.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    BackHandler { onBack() }

    AnimatedVisibility(
        visible = folder != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val f = folder ?: return@AnimatedVisibility

        val primary = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface
        val textSecondary = onSurface.copy(alpha = 0.55f)
        val dividerColor = onSurface.copy(alpha = 0.08f)
        val surface = MaterialTheme.colorScheme.surface

        var sortMode by remember {
            val saved = AppPrefs.getFolderSortMode(context)
            mutableStateOf(AllAppsSortMode.entries.firstOrNull { it.name == saved } ?: AllAppsSortMode.ALPHA)
        }
        var searchQuery by remember { mutableStateOf("") }
        val catId = f.category.categoryId
        var customName by remember(catId) {
            mutableStateOf(AppPrefs.getFolderCustomNames(context)[catId] ?: "")
        }
        var customEmoji by remember(catId) {
            mutableStateOf(AppPrefs.getFolderCustomEmojis(context)[catId] ?: "")
        }
        var customColor by remember(catId) {
            mutableStateOf(AppPrefs.getFolderCustomColors(context)[catId] ?: "")
        }
        var showEditDialog by remember { mutableStateOf(false) }
        var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }

        val catColor = remember(f.category.colorHex, customColor) {
            val hex = customColor.ifBlank { null } ?: f.category.colorHex
            runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(primary)
        }

        val trLocale = java.util.Locale("tr")
        val sortedApps = remember(f.apps, sortMode, searchQuery) {
            val base = if (searchQuery.isBlank()) f.apps
            else {
                val q = searchQuery.lowercase(trLocale)
                f.apps.filter { it.appName.lowercase(trLocale).contains(q) }
            }
            base.sortedByMode(sortMode)
        }

        val badgeCounts by AppNotificationListenerService.badgeCounts.collectAsState()
        val latestTexts by AppNotificationListenerService.latestTexts.collectAsState()
        val appsWithNotifs = remember(f.apps, badgeCounts) {
            f.apps.filter { (badgeCounts[it.packageName] ?: 0) > 0 }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surface.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Üst bar — geri + klasör başlık + düzenle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = onSurface,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = customEmoji.ifBlank { f.category.iconEmoji },
                            fontSize = 22.sp,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customName.ifBlank { f.category.categoryName },
                            color = onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${f.apps.size} uygulama",
                            color = textSecondary,
                            fontSize = 13.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(onSurface.copy(alpha = 0.08f))
                            .clickable { showEditDialog = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            stringResource(R.string.folder_edit),
                            tint = onSurface.copy(0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Arama çubuğu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(onSurface.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "${customName.ifBlank { f.category.categoryName }} içinde ara...",
                                color = textSecondary,
                                fontSize = 13.sp,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            cursorBrush = SolidColor(primary),
                            textStyle = TextStyle(color = onSurface, fontSize = 13.sp),
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            "Aramayı temizle",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp).clickable { searchQuery = "" },
                        )
                    }
                }

                // Bildirim bandı
                if (appsWithNotifs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
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
                                        onClick(label = "Uygulamayı aç") {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            AppAnalytics.appLaunched(app.packageName, "folder_screen")
                                            viewModel.launchApp(context, app.packageName)
                                            true
                                        }
                                    }
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        AppAnalytics.appLaunched(app.packageName, "folder_screen")
                                        viewModel.launchApp(context, app.packageName)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$count",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Column {
                                        Text(app.appName, color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (text.isNotEmpty()) {
                                            Text(text, color = textSecondary, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
                }

                // Sıralama chip'leri
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp),
                ) {
                    itemsIndexed(AllAppsSortMode.entries, key = { _, mode -> mode.name }) { _, mode ->
                        val active = sortMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) primary else onSurface.copy(alpha = 0.12f))
                                .clickable {
                                    sortMode = mode
                                    AppPrefs.setFolderSortMode(context, mode.name)
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                        ) {
                            Text(
                                mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else textSecondary,
                            )
                        }
                    }
                }

                Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

                // Uygulama grid'i
                if (sortedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (searchQuery.isNotEmpty()) "\"$searchQuery\" bulunamadı" else "Bu klasör boş",
                            color = textSecondary,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(sortedApps, key = { it.packageName }) { app ->
                            AppIconView(
                                app = app,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    AppAnalytics.appLaunched(app.packageName, "folder_screen")
                                    viewModel.launchApp(context, app.packageName)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuApp = app
                                },
                                iconSize = 56.dp,
                                showLabel = true,
                            )
                        }
                    }
                }
            }

            // Klasör düzenleme dialog'u
            if (showEditDialog) {
                FolderRenameDialog(
                    currentName = customName.ifBlank { f.category.categoryName },
                    currentEmoji = customEmoji.ifBlank { f.category.iconEmoji },
                    currentColor = customColor,
                    onDismiss = { showEditDialog = false },
                    onSave = { newName, newEmoji, newColor ->
                        val nameToSave = if (newName == f.category.categoryName) "" else newName
                        val emojiToSave = if (newEmoji == f.category.iconEmoji) "" else newEmoji
                        customName = nameToSave
                        customEmoji = emojiToSave
                        customColor = newColor
                        AppPrefs.setFolderCustomName(context, catId, nameToSave)
                        AppPrefs.setFolderCustomEmoji(context, catId, emojiToSave)
                        AppPrefs.setFolderCustomColor(context, catId, newColor)
                        showEditDialog = false
                    },
                )
            }

            // Uygulama context menüsü (uzun bas)
            contextMenuApp?.let { app ->
                val allApps by viewModel.allApps.collectAsState()
                val dockPackages by viewModel.dockPackages.collectAsState()
                val categories by viewModel.categories.collectAsState()
                var categoryPickerApp by remember { mutableStateOf<AppInfo?>(null) }

                AppContextMenu(
                    app = app,
                    isFavorite = false,
                    isDocked = app.packageName in dockPackages,
                    onDismiss = { contextMenuApp = null },
                    onLaunch = {
                        viewModel.launchApp(context, app.packageName)
                        contextMenuApp = null
                    },
                    onAddToDock = {
                        viewModel.addToDock(context, app.packageName)
                        contextMenuApp = null
                    },
                    onRemoveFromDock = {
                        viewModel.removeFromDock(context, app.packageName)
                        contextMenuApp = null
                    },
                    onChangeCategory = {
                        categoryPickerApp = app
                        contextMenuApp = null
                    },
                    onHideApp = { hidden ->
                        viewModel.setAppHidden(app.packageName, hidden)
                        contextMenuApp = null
                    },
                    onSaveNote = { note ->
                        viewModel.saveAppNote(app.packageName, note)
                        contextMenuApp = null
                    },
                    onToggleFavorite = { _ ->
                        viewModel.toggleFavorite(context, app.packageName)
                        contextMenuApp = null
                    },
                )

                categoryPickerApp?.let { pickerApp ->
                    CategoryPickerSheet(
                        app = pickerApp,
                        categories = categories,
                        onDismiss = { categoryPickerApp = null },
                        onCategorySelected = { catIdSelected ->
                            viewModel.updateAppCategory(pickerApp.packageName, catIdSelected)
                            categoryPickerApp = null
                        },
                    )
                }
            }
        }
    }
}
