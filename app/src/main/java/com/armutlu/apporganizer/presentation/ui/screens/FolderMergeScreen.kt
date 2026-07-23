package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.FolderMergeViewModel

/**
 * FolderMergeScreen — Compare UI for folder merge suggestions.
 * Shows source folder (left), movable apps (center with selection), target folder (right).
 */
@Composable
fun FolderMergeScreen(
    viewModel: FolderMergeViewModel = hiltViewModel(),
    categories: List<Category> = emptyList(),
    onClose: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Klasör Birleştirme",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Compare Layout: 3-column
            if (uiState.selectedSuggestionId != null) {
                CompareLayout(
                    uiState = uiState,
                    categories = categories,
                    onToggleApp = { viewModel.toggleAppSelection(it) },
                    onSelectTarget = { viewModel.selectTargetFolder(it) }
                )
            } else {
                // Suggestions List
                SuggestionsList(
                    suggestions = uiState.suggestions,
                    onSelectSuggestion = { viewModel.selectSuggestion(it) }
                )
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.selectedSuggestionId != null) {
                    OutlinedButton(
                        onClick = { viewModel.rejectMerge() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal")
                    }

                    Button(
                        onClick = { viewModel.approveMerge() },
                        enabled = uiState.isReadyToApprove && !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Birleştir")
                    }
                }

                if (canUndo) {
                    Button(
                        onClick = { viewModel.undoLastMerge() },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Geri Al")
                    }
                }

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Kapat")
                }
            }

            // Error message
            if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compare Layout: Source | Apps | Target
 */
@Composable
private fun CompareLayout(
    uiState: FolderMergeUiState,
    categories: List<Category>,
    onToggleApp: (String) -> Unit,
    onSelectTarget: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left: Source Folder
        FolderCompareCard(
            modifier = Modifier.weight(1f),
            categoryId = uiState.sourceFolderApps.firstOrNull()?.categoryId ?: "unknown",
            appCount = uiState.sourceFolderApps.size,
            categories = categories,
            title = "Kaynak"
        )

        // Center: Arrow + Apps
        Column(
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Transfer",
                modifier = Modifier.padding(top = 24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            // Selectable Apps List
            Card(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.selectableApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = app.packageName in uiState.selectedAppsToMove,
                                onCheckedChange = { onToggleApp(app.packageName) }
                            )
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Text(
                text = "${uiState.selectedAppsToMove.size}/${uiState.selectableApps.size}",
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Right: Target Folder
        TargetFolderSelector(
            modifier = Modifier.weight(1f),
            categories = categories,
            selectedCategoryId = uiState.targetFolderId,
            onSelectTarget = onSelectTarget,
            excludeCategoryId = uiState.sourceFolderApps.firstOrNull()?.categoryId
        )
    }
}

/**
 * Left panel: Display source folder info
 */
@Composable
private fun FolderCompareCard(
    modifier: Modifier = Modifier,
    categoryId: String,
    appCount: Int,
    categories: List<Category>,
    title: String
) {
    val category = categories.find { it.categoryId == categoryId }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall)
            Text(text = category?.iconEmoji ?: "📁", style = MaterialTheme.typography.displaySmall)
            Text(
                text = category?.categoryName ?: categoryId,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$appCount uygulama",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Right panel: Target folder selector
 */
@Composable
private fun TargetFolderSelector(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelectTarget: (String) -> Unit,
    excludeCategoryId: String? = null
) {
    val availableCategories = categories.filter {
        it.categoryId != excludeCategoryId && it.categoryId != "uncategorized"
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Hedef",
                style = MaterialTheme.typography.labelSmall
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(availableCategories) { cat ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .background(
                                if (selectedCategoryId == cat.categoryId)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                        onClick = { onSelectTarget(cat.categoryId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.iconEmoji)
                            Text(
                                text = cat.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Suggestions list view (initial state)
 */
@Composable
private fun SuggestionsList(
    suggestions: List<com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestion>,
    onSelectSuggestion: (String) -> Unit
) {
    if (suggestions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Birleştirme önerisi yok",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { onSelectSuggestion(suggestion.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = suggestion.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = suggestion.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Güven: %${suggestion.confidence}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
