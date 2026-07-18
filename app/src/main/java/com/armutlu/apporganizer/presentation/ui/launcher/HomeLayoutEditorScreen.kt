package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.unit.dp
import android.provider.Settings
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import com.armutlu.apporganizer.domain.models.withSearchZone
import com.armutlu.apporganizer.utils.HomeLayoutPrefs
import com.armutlu.apporganizer.utils.DockPrefs
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration

internal data class HomeLayoutEditorState(
    val original: HomeLayoutConfig,
    val draft: HomeLayoutConfig = original,
) {
    val hasUnsavedChanges: Boolean get() = draft != original
}

internal fun HomeLayoutConfig.withSectionVisibility(sectionId: HomeSectionId, visible: Boolean): HomeLayoutConfig {
    val section = items.firstOrNull { it.sectionId == sectionId } ?: return this
    if (!section.sectionId.hideable && !visible) return this
    return copy(items = items.map { if (it.sectionId == sectionId) it.copy(visible = visible) else it })
}

internal fun HomeLayoutEditorState.resetDraft(): HomeLayoutEditorState = copy(draft = HomeLayoutConfig.DEFAULT)

/**
 * P16 — editörün "Akıllı Ana Ekran bölümleri" listesi: yalnız CONTENT zone'daki, FOLDER_GRID
 * hariç Dashboard section'ları (roadmap madde 2: FOLDER_GRID kartı tamamen kaldırılır; madde 3:
 * klasör sırası ayrı bir başlık altında yönetilir). MAIN_SEARCH (HEADER/FOOTER) ve DOCK (FOOTER)
 * bu listede yer almaz — onlar "Global alanlar" bölümünde ayrı kontrollerle yönetilir.
 */
internal fun HomeLayoutConfig.dashboardSectionItems(): List<HomeLayoutItem> = items
    .filter { it.zone == HomeLayoutZone.CONTENT && it.sectionId != HomeSectionId.FOLDER_GRID }
    .sortedBy(HomeLayoutItem::order)

internal fun HomeLayoutConfig.moveSection(sectionId: HomeSectionId, direction: Int): HomeLayoutConfig {
    if (direction == 0) return this
    val source = items.firstOrNull { it.sectionId == sectionId } ?: return this
    if (!source.visible || !source.sectionId.movable || source.locked) return this
    val zoneItems = items.filter { it.zone == source.zone && it.visible }
        .sortedBy { it.order }
    val sourceIndex = zoneItems.indexOfFirst { it.sectionId == sectionId }
    val targetIndex = sourceIndex + direction.coerceIn(-1, 1)
    if (sourceIndex < 0 || targetIndex !in zoneItems.indices) return this
    val target = zoneItems[targetIndex]
    if (!target.sectionId.movable || target.locked) return this
    val reordered = zoneItems.toMutableList().apply {
        add(targetIndex, removeAt(sourceIndex))
    }
    val newOrders = reordered.mapIndexed { index, item -> item.sectionId to index }.toMap()
    return copy(items = items.map { item ->
        newOrders[item.sectionId]?.let { item.copy(order = it) } ?: item
    })
}

internal fun HomeLayoutConfig.moveSectionToZoneStart(sectionId: HomeSectionId): HomeLayoutConfig {
    var result = this
    while (true) {
        val moved = result.moveSection(sectionId, -1)
        if (moved == result) return result
        result = moved
    }
}

private class ReorderState(
    private val onMove: (HomeSectionId, Int) -> Boolean,
) {
    var draggedId by mutableStateOf<HomeSectionId?>(null)
        private set
    var dragOffset by mutableStateOf(0f)
        private set

    fun start(id: HomeSectionId) { draggedId = id; dragOffset = 0f }
    fun drag(id: HomeSectionId, delta: Float, threshold: Float): Boolean {
        if (draggedId != id || threshold <= 0f) return false
        dragOffset += delta
        if (kotlin.math.abs(dragOffset) < threshold) return false
        val moved = onMove(id, if (dragOffset > 0f) 1 else -1)
        if (moved) dragOffset = 0f
        return moved
    }
    fun stop() { draggedId = null; dragOffset = 0f }
}

private val editorStateSaver = listSaver<HomeLayoutEditorState, String>(
    save = { state ->
        listOf(encodeConfig(state.original), encodeConfig(state.draft))
    },
    restore = { saved ->
        if (saved.size != 2) null else runCatching {
            HomeLayoutEditorState(decodeConfig(saved[0]), decodeConfig(saved[1]))
        }.getOrNull()
    },
)

private fun encodeConfig(config: HomeLayoutConfig): String = buildString {
    append(config.version)
    config.items.forEach { item ->
        append(';').append(item.sectionId.name).append('|').append(item.zone.name)
            .append('|').append(item.order).append('|').append(item.visible)
    }
}

private fun decodeConfig(raw: String): HomeLayoutConfig {
    val tokens = raw.split(';')
    val version = tokens.first().toInt()
    val items = tokens.drop(1).map { token ->
        val fields = token.split('|')
        HomeLayoutItem(
            sectionId = HomeSectionId.valueOf(fields[0]),
            zone = HomeLayoutZone.valueOf(fields[1]),
            order = fields[2].toInt(),
            visible = fields[3].toBooleanStrict(),
        )
    }
    return HomeLayoutConfig(version, items)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeLayoutEditorScreen(viewModel: LauncherViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()
    val dockItems by viewModel.dockPackages.collectAsState()
    var editorState by rememberSaveable(stateSaver = editorStateSaver) {
        mutableStateOf(HomeLayoutEditorState(HomeLayoutPrefs.read(context).config))
    }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var originalFolderIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var draftFolderIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var originalWidgetIds by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var draftWidgetIds by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    var dockInitialized by rememberSaveable { mutableStateOf(false) }
    var originalDockItems by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var draftDockItems by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val haptics = LocalHapticFeedback.current
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val reorderState = remember {
        ReorderState { sectionId, direction ->
            val moved = editorState.draft.moveSection(sectionId, direction)
            if (moved == editorState.draft) false else {
                editorState = editorState.copy(draft = moved)
                true
            }
        }
    }
    val folderReorderState = remember {
        FolderOrderReorderState { folderId, direction ->
            val moved = moveFolder(draftFolderIds, folderId, direction)
            if (moved == draftFolderIds) false else {
                draftFolderIds = moved
                true
            }
        }
    }
    val dockReorderState = remember {
        DockOrderReorderState { item, direction ->
            val moved = moveDockItem(draftDockItems, item, direction)
            if (moved == draftDockItems) false else {
                draftDockItems = moved
                true
            }
        }
    }
    val currentFolderIds = folders.map { it.category.categoryId }
    LaunchedEffect(currentFolderIds) {
        if (originalFolderIds.isEmpty() && currentFolderIds.isNotEmpty()) {
            originalFolderIds = currentFolderIds
            draftFolderIds = currentFolderIds
        }
    }
    LaunchedEffect(widgetIds) {
        if (originalWidgetIds.isEmpty() && widgetIds.isNotEmpty()) {
            originalWidgetIds = widgetIds
            draftWidgetIds = widgetIds
        }
    }
    LaunchedEffect(dockItems) {
        if (!dockInitialized) {
            originalDockItems = dockItems
            draftDockItems = dockItems
            dockInitialized = true
        }
    }

    fun requestClose() {
        if (editorState.hasUnsavedChanges || draftFolderIds != originalFolderIds ||
            draftWidgetIds != originalWidgetIds || draftDockItems != originalDockItems
        ) showDiscardDialog = true else onClose()
    }

    BackHandler { requestClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_layout_editor_title)) },
                navigationIcon = {
                    TextButton(onClick = ::requestClose) {
                        Text(stringResource(R.string.home_layout_editor_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        HomeLayoutPrefs.write(context, HomeLayoutPrefs.State(editorState.draft, customized = true))
                        val foldersById = folders.associateBy { it.category.categoryId }
                        viewModel.reorderFolders(
                            context,
                            draftFolderIds.mapNotNull(foldersById::get) +
                                folders.filterNot { it.category.categoryId in draftFolderIds },
                        )
                        if (draftWidgetIds != originalWidgetIds) {
                            viewModel.reorderWidgets(context, draftWidgetIds)
                        }
                        if (draftDockItems != originalDockItems) {
                            viewModel.saveDockPackages(context, draftDockItems)
                        }
                        onClose()
                    }) { Text(stringResource(R.string.home_layout_editor_done)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = stringResource(R.string.home_layout_editor_intro),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TextButton(onClick = { showResetDialog = true }) {
                Text(stringResource(R.string.home_layout_reset))
            }
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            val dashboardSectionItems = editorState.draft.dashboardSectionItems()
            val visibleDashboardItems = dashboardSectionItems.filter { it.visible }
            val hiddenDashboardItems = dashboardSectionItems.filterNot { it.visible }
            val mainSearchItem = editorState.draft.items.single { it.sectionId == HomeSectionId.MAIN_SEARCH }
            if (isSmallDeviceForDashboard(screenHeightDp, visibleDashboardItems.size)) {
                Text(
                    text = stringResource(R.string.home_layout_small_device_warning),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // P16 madde 1 — "Global alanlar": Her Şeyi Ara (konum seçimi) ve Dock (sabit).
                item(key = "section_header_global") {
                    Text(
                        text = stringResource(R.string.home_layout_section_global),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                item(key = "main_search_position") {
                    MainSearchPositionCard(
                        item = mainSearchItem,
                        onZoneSelected = { zone ->
                            editorState = editorState.copy(draft = editorState.draft.withSearchZone(zone))
                        },
                        onVisibilityChange = { visible ->
                            editorState = editorState.copy(
                                draft = editorState.draft.withSectionVisibility(HomeSectionId.MAIN_SEARCH, visible),
                            )
                        },
                    )
                }
                item(key = "dock_fixed_notice") {
                    DockFixedNoticeCard()
                }
                if (draftDockItems.isNotEmpty()) {
                    item(key = "dock_editor") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(draftDockItems, key = { "dock_$it" }) { dockItem ->
                                DockOrderCard(
                                    modifier = Modifier.animateItemPlacement(),
                                    item = dockItem,
                                    folder = DockPrefs.folderId(dockItem)?.let { id ->
                                        folders.firstOrNull { it.category.categoryId == id }
                                    },
                                    reorderState = dockReorderState,
                                    onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    onItemMoved = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                                )
                            }
                        }
                    }
                }

                // P16 madde 1/7 — "Akıllı Ana Ekran bölümleri": yalnız CONTENT zone, FOLDER_GRID hariç.
                item(key = "section_header_dashboard") {
                    Text(
                        text = stringResource(R.string.home_layout_section_dashboard),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(visibleDashboardItems, key = { it.sectionId.name }) { item ->
                    EditableHomeSection(
                        modifier = if (reduceMotion) Modifier else Modifier.animateItemPlacement(),
                        item = item,
                        position = visibleDashboardItems.indexOf(item) + 1,
                        reorderState = reorderState,
                        onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onItemMoved = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                        reduceMotion = reduceMotion,
                        onAccessibilityMove = { direction ->
                            val moved = if (direction == Int.MIN_VALUE) {
                                editorState.draft.moveSectionToZoneStart(item.sectionId)
                            } else {
                                editorState.draft.moveSection(item.sectionId, direction)
                            }
                            if (moved == editorState.draft) false else {
                                editorState = editorState.copy(draft = moved)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                true
                            }
                        },
                        onVisibilityChange = { visible ->
                            editorState = editorState.copy(
                                draft = editorState.draft.withSectionVisibility(item.sectionId, visible),
                            )
                        },
                    )
                }
                if (visibleDashboardItems.any { it.sectionId == HomeSectionId.ANDROID_WIDGETS } && draftWidgetIds.isNotEmpty()) {
                    item(key = "widget_editor") {
                        WidgetArea(
                            widgetIds = draftWidgetIds,
                            onRemoveWidget = {},
                            onReorderWidgets = { draftWidgetIds = it },
                            editMode = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (hiddenDashboardItems.isNotEmpty()) {
                    item(key = "hidden_header") {
                        Text(stringResource(R.string.home_layout_hidden_sections), Modifier.padding(16.dp))
                    }
                    items(hiddenDashboardItems, key = { "hidden_${it.sectionId.name}" }) { item ->
                        HiddenHomeSection(item) {
                            editorState = editorState.copy(
                                draft = editorState.draft.withSectionVisibility(item.sectionId, true),
                            )
                        }
                    }
                }

                // P16 madde 3 — "Klasör Sayfaları": FOLDER_GRID kartı kaldırıldığı için ayrı,
                // her zaman görünen başlık altında (klasör varsa) yönetilir.
                if (draftFolderIds.isNotEmpty()) {
                    item(key = "section_header_folders") {
                        Text(
                            text = stringResource(R.string.home_layout_section_folders),
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(draftFolderIds, key = { "folder_$it" }) { folderId ->
                        folders.firstOrNull { it.category.categoryId == folderId }?.let { folder ->
                            FolderOrderCard(
                                modifier = Modifier.animateItemPlacement(),
                                folder = folder,
                                reorderState = folderReorderState,
                                onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onItemMoved = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.home_layout_editor_discard_title)) },
            text = { Text(stringResource(R.string.home_layout_editor_discard_message)) },
            confirmButton = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.home_layout_editor_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.home_layout_editor_keep_editing))
                }
            },
        )
    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.home_layout_reset_title)) },
            text = { Text(stringResource(R.string.home_layout_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    editorState = editorState.resetDraft()
                    showResetDialog = false
                }) { Text(stringResource(R.string.home_layout_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.home_layout_editor_cancel))
                }
            },
        )
    }
}

internal fun moveFolder(ids: List<String>, folderId: String, direction: Int): List<String> {
    val from = ids.indexOf(folderId)
    val to = from + direction.coerceIn(-1, 1)
    if (from < 0 || direction == 0 || to !in ids.indices) return ids
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

internal fun moveDockItem(items: List<String>, item: String, direction: Int): List<String> {
    val from = items.indexOf(item)
    val to = from + direction.coerceIn(-1, 1)
    if (from < 0 || direction == 0 || to !in items.indices) return items
    return items.toMutableList().apply { add(to, removeAt(from)) }
}

private class DockOrderReorderState(
    private val onMove: (String, Int) -> Boolean,
) {
    var draggedItem by mutableStateOf<String?>(null)
    var dragOffset by mutableStateOf(0f)
    fun start(item: String) { draggedItem = item; dragOffset = 0f }
    fun drag(item: String, delta: Float, threshold: Float): Boolean {
        if (draggedItem != item || threshold <= 0f) return false
        dragOffset += delta
        if (kotlin.math.abs(dragOffset) < threshold) return false
        return onMove(item, if (dragOffset > 0f) 1 else -1).also { if (it) dragOffset = 0f }
    }
    fun stop() { draggedItem = null; dragOffset = 0f }
}

@Composable
private fun DockOrderCard(
    modifier: Modifier = Modifier,
    item: String,
    folder: AppFolder?,
    reorderState: DockOrderReorderState,
    onDragStarted: () -> Unit,
    onItemMoved: () -> Unit,
) {
    val context = LocalContext.current
    val label = folder?.category?.categoryName ?: remember(item) {
        runCatching {
            val info = context.packageManager.getApplicationInfo(item, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(item)
    }
    var itemWidth by remember { mutableStateOf(0) }
    val dragging = reorderState.draggedItem == item
    Card(
        modifier = modifier.height(72.dp)
            .onSizeChanged { itemWidth = it.width }
            .graphicsLayer { if (dragging) { translationX = reorderState.dragOffset; alpha = 0.92f } },
    ) {
        Box(
            Modifier.height(72.dp).padding(horizontal = 16.dp)
                .pointerInput(item) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { reorderState.start(item); onDragStarted() },
                        onDragEnd = reorderState::stop,
                        onDragCancel = reorderState::stop,
                        onDrag = { change, amount ->
                            change.consume()
                            if (reorderState.drag(item, amount.x, itemWidth / 2f)) onItemMoved()
                        },
                    )
                }
                .semantics { contentDescription = label },
        ) {
            Text(if (folder == null) label else "${folder.category.iconEmoji} $label", Modifier.padding(top = 24.dp))
        }
    }
}

private class FolderOrderReorderState(
    private val onMove: (String, Int) -> Boolean,
) {
    var draggedId by mutableStateOf<String?>(null)
    var dragOffset by mutableStateOf(0f)
    fun start(id: String) { draggedId = id; dragOffset = 0f }
    fun drag(id: String, delta: Float, threshold: Float): Boolean {
        if (draggedId != id || threshold <= 0f) return false
        dragOffset += delta
        if (kotlin.math.abs(dragOffset) < threshold) return false
        return onMove(id, if (dragOffset > 0f) 1 else -1).also { if (it) dragOffset = 0f }
    }
    fun stop() { draggedId = null; dragOffset = 0f }
}

@Composable
private fun FolderOrderCard(
    modifier: Modifier = Modifier,
    folder: AppFolder,
    reorderState: FolderOrderReorderState,
    onDragStarted: () -> Unit,
    onItemMoved: () -> Unit,
) {
    val id = folder.category.categoryId
    var itemHeight by remember { mutableStateOf(0) }
    val dragging = reorderState.draggedId == id
    Card(
        modifier.fillMaxWidth().padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
            .onSizeChanged { itemHeight = it.height }
            .graphicsLayer { if (dragging) { translationY = reorderState.dragOffset; alpha = 0.92f } },
    ) {
        ListItem(
            leadingContent = {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.home_layout_drag_handle),
                    modifier = Modifier.pointerInput(id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { reorderState.start(id); onDragStarted() },
                            onDragEnd = reorderState::stop,
                            onDragCancel = reorderState::stop,
                            onDrag = { change, amount ->
                                change.consume()
                                if (reorderState.drag(id, amount.y, itemHeight / 2f)) onItemMoved()
                            },
                        )
                    },
                )
            },
            headlineContent = { Text(folder.category.categoryName) },
        )
    }
}

@Composable
private fun EditableHomeSection(
    modifier: Modifier = Modifier,
    item: HomeLayoutItem,
    position: Int,
    reorderState: ReorderState,
    onDragStarted: () -> Unit,
    onItemMoved: () -> Unit,
    reduceMotion: Boolean,
    onAccessibilityMove: (Int) -> Boolean,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val name = stringResource(sectionName(item.sectionId))
    val description = stringResource(R.string.home_layout_section_position, name, position)
    val moveUpLabel = stringResource(R.string.home_layout_move_up)
    val moveDownLabel = stringResource(R.string.home_layout_move_down)
    val moveTopLabel = stringResource(R.string.home_layout_move_top)
    var itemHeight by remember { mutableStateOf(0) }
    val isDragging = reorderState.draggedId == item.sectionId
    Card(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
        .onSizeChanged { itemHeight = it.height }
        .graphicsLayer { if (isDragging && !reduceMotion) { translationY = reorderState.dragOffset; alpha = 0.92f } }
        .semantics {
            contentDescription = description
            if (item.sectionId.movable && !item.locked) {
                customActions = listOf(
                    CustomAccessibilityAction(moveUpLabel) { onAccessibilityMove(-1) },
                    CustomAccessibilityAction(moveDownLabel) { onAccessibilityMove(1) },
                    CustomAccessibilityAction(moveTopLabel) { onAccessibilityMove(Int.MIN_VALUE) },
                )
            }
        }) {
        ListItem(
            leadingContent = {
                Icon(
                    if (item.locked || !item.sectionId.movable) Icons.Default.Lock else Icons.Default.DragHandle,
                    modifier = Modifier.pointerInput(item.sectionId, item.sectionId.movable, item.locked) {
                        if (item.sectionId.movable && !item.locked) detectDragGesturesAfterLongPress(
                            onDragStart = { reorderState.start(item.sectionId); onDragStarted() },
                            onDragEnd = reorderState::stop,
                            onDragCancel = reorderState::stop,
                            onDrag = { change, amount ->
                                change.consume()
                                if (reorderState.drag(item.sectionId, amount.y, itemHeight / 2f)) onItemMoved()
                            },
                        )
                    },
                    contentDescription = stringResource(
                        if (item.locked || !item.sectionId.movable) R.string.home_layout_locked else R.string.home_layout_drag_handle,
                    ),
                )
            },
            headlineContent = { Text(name) },
            trailingContent = {
                IconButton(
                    enabled = item.sectionId.hideable,
                    onClick = { onVisibilityChange(false) },
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (item.sectionId.hideable) R.string.home_layout_hide_section else R.string.home_layout_required_section,
                            name,
                        ),
                    )
                }
            },
        )
    }
}

/**
 * P16 madde 4 — MAIN_SEARCH için tek etkileşim: üstte sabit ya da altta (dock üstünde) sabit
 * konum seçimi. Sürükle/reorder yerine iki seçenekli `FilterChip` kullanılır çünkü zone'un tek
 * üyesi olduğu için drag zaten no-op'tur (bkz. HomeLayoutConfig.moveSection).
 */
@Composable
private fun MainSearchPositionCard(
    item: HomeLayoutItem,
    onZoneSelected: (HomeLayoutZone) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val name = stringResource(R.string.home_section_main_search)
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = { Text(stringResource(R.string.home_layout_main_search_description)) },
            trailingContent = {
                IconButton(onClick = { onVisibilityChange(!item.visible) }) {
                    Icon(
                        if (item.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = stringResource(
                            if (item.visible) R.string.home_layout_hide_section else R.string.home_layout_show_section,
                            name,
                        ),
                    )
                }
            },
        )
        if (item.visible) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = item.zone == HomeLayoutZone.HEADER,
                    onClick = { onZoneSelected(HomeLayoutZone.HEADER) },
                    label = { Text(stringResource(R.string.home_layout_search_position_top)) },
                )
                FilterChip(
                    selected = item.zone == HomeLayoutZone.FOOTER,
                    onClick = { onZoneSelected(HomeLayoutZone.FOOTER) },
                    label = { Text(stringResource(R.string.home_layout_search_position_bottom)) },
                )
            }
        }
    }
}

/** P16 madde 5 — Dock'un konumu/varlığı sabit olduğunu açıklayan bilgi kartı (etkileşimsiz). */
@Composable
private fun DockFixedNoticeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.home_section_dock), fontWeight = FontWeight.SemiBold)
                Text(
                    text = stringResource(R.string.home_layout_dock_description),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun HiddenHomeSection(item: HomeLayoutItem, onShow: () -> Unit) {
    val name = stringResource(sectionName(item.sectionId))
    ListItem(
        headlineContent = { Text(name) },
        trailingContent = {
            IconButton(onClick = onShow) {
                Icon(Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.home_layout_show_section, name))
            }
        },
    )
}

@StringRes
private fun sectionName(id: HomeSectionId): Int = when (id) {
    HomeSectionId.CLOCK -> R.string.home_section_clock
    HomeSectionId.MISSIONS_AND_SCORE -> R.string.home_section_missions_score
    HomeSectionId.MAIN_SEARCH -> R.string.home_section_main_search
    HomeSectionId.GOOGLE_SEARCH -> R.string.home_section_google_search
    HomeSectionId.FAVORITES -> R.string.home_section_favorites
    HomeSectionId.SUGGESTIONS -> R.string.home_section_suggestions
    HomeSectionId.RECENT_NOTIFICATIONS -> R.string.home_section_recent_notifications
    HomeSectionId.RECENT_APPS -> R.string.home_section_recent_apps
    HomeSectionId.ANDROID_WIDGETS -> R.string.home_section_widgets
    HomeSectionId.ASSISTANT_INSIGHTS -> R.string.home_section_assistant
    HomeSectionId.TICKER_OR_STATS -> R.string.home_section_ticker
    HomeSectionId.FOLDER_GRID -> R.string.home_section_folders
    HomeSectionId.DOCK -> R.string.home_section_dock
}
