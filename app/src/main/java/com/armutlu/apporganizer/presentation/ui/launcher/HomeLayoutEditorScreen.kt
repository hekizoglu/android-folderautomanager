package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
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
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.HomeLayoutConfig
import com.armutlu.apporganizer.domain.models.HomeLayoutItem
import com.armutlu.apporganizer.domain.models.HomeLayoutZone
import com.armutlu.apporganizer.domain.models.HomeSectionId
import com.armutlu.apporganizer.utils.HomeLayoutPrefs

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutEditorScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var editorState by rememberSaveable(stateSaver = editorStateSaver) {
        mutableStateOf(HomeLayoutEditorState(HomeLayoutPrefs.read(context).config))
    }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    fun requestClose() {
        if (editorState.hasUnsavedChanges) showDiscardDialog = true else onClose()
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
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val orderedItems = editorState.draft.items.sortedWith(compareBy({ it.zone.ordinal }, { it.order }))
                items(orderedItems.filter { it.visible },
                    key = { it.sectionId.name }) { item ->
                    EditableHomeSection(
                        item = item,
                        position = orderedItems.filter { it.visible }.indexOf(item) + 1,
                        onVisibilityChange = { visible ->
                            editorState = editorState.copy(
                                draft = editorState.draft.withSectionVisibility(item.sectionId, visible),
                            )
                        },
                    )
                }
                val hiddenItems = orderedItems.filterNot { it.visible }
                if (hiddenItems.isNotEmpty()) {
                    item { Text(stringResource(R.string.home_layout_hidden_sections), Modifier.padding(16.dp)) }
                    items(hiddenItems, key = { "hidden_${it.sectionId.name}" }) { item ->
                        HiddenHomeSection(item) {
                            editorState = editorState.copy(
                                draft = editorState.draft.withSectionVisibility(item.sectionId, true),
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

@Composable
private fun EditableHomeSection(
    item: HomeLayoutItem,
    position: Int,
    onVisibilityChange: (Boolean) -> Unit,
) {
    val name = stringResource(sectionName(item.sectionId))
    val description = stringResource(R.string.home_layout_section_position, name, position)
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).semantics {
        contentDescription = description
    }) {
        ListItem(
            leadingContent = {
                Icon(
                    if (item.locked || !item.sectionId.movable) Icons.Default.Lock else Icons.Default.DragHandle,
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
