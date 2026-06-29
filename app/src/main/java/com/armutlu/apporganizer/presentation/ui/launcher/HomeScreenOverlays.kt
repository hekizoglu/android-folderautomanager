package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.Category

@Composable
internal fun FolderStatsRow(
    folders: List<AppFolder>,
    onOpenFolderStats: () -> Unit = {},
    onOpenAppStats: () -> Unit = {},
) {
    val totalApps = folders.sumOf { it.apps.size }
    val totalFolders = folders.size
    if (totalFolders > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$totalFolders klasör",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable(onClick = onOpenFolderStats),
            )
            Text(
                text = "  ·  ",
                color = Color.White.copy(alpha = 0.30f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = "$totalApps uygulama",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable(onClick = onOpenAppStats),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenOverlays(
    allApps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
    folders: List<AppFolder>,
    dockPackages: List<String>,
    openFolder: AppFolder?,
    folderSheetState: androidx.compose.material3.SheetState,
    dockEditOpen: Boolean,
    contextMenuApp: com.armutlu.apporganizer.domain.models.AppInfo?,
    favoritePackages: Set<String>,
    categories: List<Category>,
    categoryPickerApp: com.armutlu.apporganizer.domain.models.AppInfo?,
    homeLongPressOpen: Boolean,
    folderContextMenu: AppFolder?,
    @Suppress("UNUSED_PARAMETER") haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    @Suppress("UNUSED_PARAMETER") scope: kotlinx.coroutines.CoroutineScope,
    onDockEditDismiss: () -> Unit,
    onContextMenuDismiss: () -> Unit,
    onCategoryPickerDismiss: () -> Unit,
    onHomeLongPressDismiss: () -> Unit,
    onFolderContextMenuDismiss: () -> Unit,
    onDockAdd: (String) -> Unit,
    onDockRemove: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onAddToDock: (String) -> Unit,
    onRemoveFromDock: (String) -> Unit,
    onChangeCategory: (com.armutlu.apporganizer.domain.models.AppInfo) -> Unit,
    onHideApp: (com.armutlu.apporganizer.domain.models.AppInfo, Boolean) -> Unit,
    onSaveNote: (com.armutlu.apporganizer.domain.models.AppInfo, String) -> Unit,
    onToggleFavorite: (com.armutlu.apporganizer.domain.models.AppInfo) -> Unit,
    onCategorySelected: (com.armutlu.apporganizer.domain.models.AppInfo, String) -> Unit,
    onFolderDismiss: () -> Unit,
    onFolderAppLongClick: (com.armutlu.apporganizer.domain.models.AppInfo) -> Unit,
    onOpenFolder: (AppFolder) -> Unit,
    onOpenAllApps: () -> Unit,
    onMoveFolder: (AppFolder, Int) -> Unit,
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
    onDockEdit: () -> Unit,
    onAddWidget: () -> Unit,
) {
    if (dockEditOpen) {
        DockEditSheet(
            allApps = allApps,
            dockPackages = dockPackages,
            onAdd = onDockAdd,
            onRemove = onDockRemove,
            onDismiss = onDockEditDismiss,
        )
    }

    contextMenuApp?.let { app ->
        AppContextMenu(
            app = app,
            isFavorite = app.packageName in favoritePackages,
            isDocked = app.packageName in dockPackages,
            onDismiss = onContextMenuDismiss,
            onLaunch = { onLaunchApp(app.packageName) },
            onAddToDock = { onAddToDock(app.packageName) },
            onRemoveFromDock = { onRemoveFromDock(app.packageName) },
            onChangeCategory = { onChangeCategory(app) },
            onHideApp = { hidden -> onHideApp(app, hidden) },
            onSaveNote = { note -> onSaveNote(app, note) },
            onToggleFavorite = { _ -> onToggleFavorite(app) },
        )
    }

    categoryPickerApp?.let { app ->
        CategoryPickerSheet(
            app = app,
            categories = categories,
            onDismiss = onCategoryPickerDismiss,
            onCategorySelected = { catId -> onCategorySelected(app, catId) },
        )
    }

    openFolder?.let { folder ->
        FolderSheet(
            folder = folder,
            sheetState = folderSheetState,
            onDismiss = onFolderDismiss,
            onAppClick = onLaunchApp,
            onAppLongClick = onFolderAppLongClick,
        )
    }

    if (homeLongPressOpen) {
        HomeLongPressSheet(
            onDismiss = onHomeLongPressDismiss,
            onWallpaper = onWallpaper,
            onSettings = onSettings,
            onDockEdit = onDockEdit,
            onAddWidget = onAddWidget,
        )
    }

    folderContextMenu?.let { folder ->
        FolderContextMenuSheet(
            folder = folder,
            allFolders = folders,
            onDismiss = onFolderContextMenuDismiss,
            onOpenFolder = { onOpenFolder(folder) },
            onOpenAllApps = onOpenAllApps,
            onMove = { newIndex -> onMoveFolder(folder, newIndex) },
        )
    }
}
