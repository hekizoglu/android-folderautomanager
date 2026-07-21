package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.armutlu.apporganizer.domain.models.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenOverlays(
    allApps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
    folders: List<AppFolder>,
    dockPackages: List<String>,
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
    onOpenFolder: (AppFolder) -> Unit,
    onOpenAllApps: () -> Unit,
    onMoveFolder: (AppFolder, Int) -> Unit,
    onWallpaper: () -> Unit,
    onEditHomeLayout: () -> Unit,
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

    if (homeLongPressOpen) {
        HomeLongPressSheet(
            onDismiss = onHomeLongPressDismiss,
            onEditHomeLayout = onEditHomeLayout,
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
