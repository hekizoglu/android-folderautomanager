package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.launcher.FolderMergeViewModel

@Composable
fun FolderMergeReviewScreen(
    viewModel: FolderMergeViewModel,
    onNavigateBack: () -> Unit,
    onApproveComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsSubScreenScaffold(
        title = stringResource(R.string.folder_merge_review_title),
        onNavigateBack = onNavigateBack,
    ) {
        item {
            if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            uiState.error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.folder_merge_preview_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        stringResource(
                            R.string.folder_merge_source_target,
                            uiState.sourceFolderApps.firstOrNull()?.categoryId ?: "Unknown",
                            uiState.targetFolderId ?: "Unknown",
                        ),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.folder_merge_before),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${uiState.sourceFolderApps.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.folder_merge_after),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${uiState.sourceFolderApps.size - uiState.selectedAppsToMove.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.folder_merge_movable_apps),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        items(uiState.selectableApps, key = { it.packageName }) { app ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(app.appName, modifier = Modifier.weight(1f))
                    if (uiState.selectedAppsToMove.contains(app.packageName)) {
                        OutlinedButton(onClick = { viewModel.toggleAppSelection(app.packageName) }) {
                            Text("Seçili", fontSize = 12.sp)
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.toggleAppSelection(app.packageName) }) {
                            Text("Seç", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (uiState.sourceFolderApps.any { it.isCategoryLocked }) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.folder_merge_locked_apps),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            items(
                uiState.sourceFolderApps.filter { it.isCategoryLocked },
                key = { it.packageName },
            ) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(20.dp),
                        )
                        Text(app.appName, modifier = Modifier.weight(1f))
                        Text("Kilitli", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (uiState.selectedAppsToMove.size >= 20) {
            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            stringResource(R.string.folder_merge_large_transfer_warning),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.rejectMerge()
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.folder_merge_cancel))
                }
                Button(
                    onClick = {
                        viewModel.approveMerge()
                        onApproveComplete()
                    },
                    enabled = uiState.isReadyToApprove && uiState.selectedAppsToMove.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.folder_merge_approve))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
