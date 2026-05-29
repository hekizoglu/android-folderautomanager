package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import timber.log.Timber

/**
 * SettingsScreen - App settings and preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Display settings section
            item {
                SectionHeader("Display")
            }
            
            item {
                SettingSwitch(
                    title = "Show System Apps",
                    description = "Display built-in system applications",
                    checked = showSystemApps,
                    onCheckedChange = { viewModel.toggleShowSystemApps() }
                )
            }
            
            // Organization settings section
            item {
                SectionHeader("Organization")
            }
            
            item {
                SettingButton(
                    title = "Auto-classify Apps",
                    description = "Automatically categorize unclassified apps",
                    onClick = {
                        Timber.d("Auto-classify clicked")
                        viewModel.classifyUnclassifiedApps()
                    }
                )
            }
            
            item {
                SettingButton(
                    title = "Reset Categories",
                    description = "Reset all categories to default",
                    onClick = {
                        Timber.d("Reset categories clicked")
                        viewModel.resetFilters()
                    }
                )
            }
            
            // About section
            item {
                SectionHeader("About")
            }
            
            item {
                SettingInfo(
                    title = "App Organizer",
                    description = "v0.1.0 beta"
                )
            }
            
            item {
                SettingInfo(
                    title = "Developer",
                    description = "Hüseyin (Armutlu Nabız)"
                )
            }
            
            item {
                SettingInfo(
                    title = "Open Source",
                    description = "GitHub: armutlu-app-organizer"
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Section header component
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

/**
 * Toggle switch setting
 */
@Composable
fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

/**
 * Clickable button setting
 */
@Composable
fun SettingButton(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TextButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Info display setting (read-only)
 */
@Composable
fun SettingInfo(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
