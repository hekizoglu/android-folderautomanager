package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo

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
) {
    val haptic = LocalHapticFeedback.current
    var sortMode by remember { mutableStateOf(FolderSortMode.ALPHA) }

    val sortedApps = remember(folder.apps, sortMode) { folder.apps.sortedBy(sortMode) }

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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = folder.category.iconEmoji, fontSize = 36.sp)
                Column {
                    Text(folder.category.categoryName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${folder.apps.size} uygulama", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
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
                    Text("Bu klasör boş", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
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
                            iconSize  = 56.dp,
                            showLabel = true,
                        )
                    }
                }
            }
        }
    }
}
