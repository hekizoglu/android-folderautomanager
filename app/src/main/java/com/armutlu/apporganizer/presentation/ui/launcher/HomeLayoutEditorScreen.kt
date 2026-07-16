package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(editorState.draft.items.sortedWith(compareBy({ it.zone.ordinal }, { it.order })),
                    key = { it.sectionId.name }) { item ->
                    ListItem(
                        headlineContent = { Text(item.sectionId.name.replace('_', ' ')) },
                        supportingContent = { Text(item.zone.name) },
                    )
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
}
