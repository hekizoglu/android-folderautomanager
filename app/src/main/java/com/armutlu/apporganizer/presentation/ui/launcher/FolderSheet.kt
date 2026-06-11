package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo

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

enum class FolderSortMode(val label: String) {
    ALPHA("A–Z"),
    USAGE("Kullanım"),
    SIZE_DESC("Boyut ↓"),
    SIZE_ASC("Boyut ↑"),
    INSTALL_DATE("Yükleme")
}

private fun List<AppInfo>.sortedBy(mode: FolderSortMode): List<AppInfo> = when (mode) {
    FolderSortMode.ALPHA        -> sortedBy { it.appName.lowercase() }
    FolderSortMode.USAGE        -> sortedByDescending { it.usageCount }
    FolderSortMode.SIZE_DESC    -> sortedByDescending { it.appSizeBytes }
    FolderSortMode.SIZE_ASC     -> sortedBy { it.appSizeBytes }
    FolderSortMode.INSTALL_DATE -> sortedByDescending { it.installTime }
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
    var sortMode by remember { mutableStateOf(FolderSortMode.ALPHA) }
    var searchQuery by remember { mutableStateOf("") }

    val sortedApps = remember(folder.apps, sortMode, searchQuery) {
        val base = if (searchQuery.isBlank()) folder.apps
                   else folder.apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        base.sortedBy(sortMode)
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
            val catColor = remember(folder.category.colorHex) {
                runCatching { Color(android.graphics.Color.parseColor(folder.category.colorHex)) }
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
                    Text(text = folder.category.iconEmoji, fontSize = 28.sp)
                }
                Column {
                    Text(folder.category.categoryName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${folder.apps.size} uygulama", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
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
                        Text("${folder.category.categoryName} içinde ara...", color = TextSecondary, fontSize = 13.sp)
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

            // ── Sıralama chip'leri ────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                itemsIndexed(FolderSortMode.entries) { _, mode ->
                    val active = sortMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) TealColor else Color.White.copy(alpha = 0.12f))
                            .clickable { sortMode = mode }
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
