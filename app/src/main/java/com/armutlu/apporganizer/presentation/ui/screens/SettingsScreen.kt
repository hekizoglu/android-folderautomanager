package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import timber.log.Timber

/**
 * SettingsScreen - App settings and preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit = {},
    onSendBugReport: () -> Unit = {}
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
            
            // Debug section
            item {
                SectionHeader("Debug")
            }

            item {
                val state by viewModel.screenState.collectAsState()
                val logs  by viewModel.liveDebugLogs.collectAsState()
                DebugInfoCard(
                    appCount      = state.apps.size,
                    categoryCount = state.categories.size,
                    error         = state.error,
                    logs          = logs,
                    launcherInfo  = viewModel.detectedLauncher.displayName,
                    a11yActive    = com.armutlu.apporganizer.service.LauncherAccessibilityService.isRunning,
                    onSendBugReport = onSendBugReport,
                    onClearLogs   = { viewModel.clearDebugLogs() }
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

@Composable
fun DebugInfoCard(
    appCount: Int,
    categoryCount: Int,
    error: String?,
    logs: List<String>,
    launcherInfo: String,
    a11yActive: Boolean,
    onSendBugReport: () -> Unit,
    onClearLogs: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── Durum satırları ──────────────────────────────────────────
            StatusRow("DB Uygulama",   "$appCount adet",         true)
            StatusRow("Kategori",      "$categoryCount adet",    true)
            StatusRow("Launcher",      launcherInfo,             true)
            StatusRow("Accessibility", if (a11yActive) "Aktif ✅" else "Kapalı ❌", a11yActive)

            if (error != null) {
                Text(
                    "⚠️ Hata: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Canlı Log Paneli ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Canlı Loglar", style = MaterialTheme.typography.labelMedium)
                Row {
                    IconButton(onClick = onClearLogs, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, "Temizle", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(logs.joinToString("\n")))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Kopyala", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Log scroll alanı
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color(0xFF1A1A2E),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val scrollState = rememberScrollState(Int.MAX_VALUE)
                // Yeni log gelince otomatik en alta kaydır
                LaunchedEffect(logs.size) { scrollState.animateScrollTo(Int.MAX_VALUE) }

                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            "Henüz log yok...",
                            color = Color(0xFF888888),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    } else {
                        logs.forEach { line ->
                            val color = when {
                                line.contains("ERROR") || line.contains("❌") -> Color(0xFFFF6B6B)
                                line.contains("WARN")  || line.contains("⚠") -> Color(0xFFFFD93D)
                                line.contains("✅")                          -> Color(0xFF6BCB77)
                                else -> Color(0xFFCCCCCC)
                            }
                            Text(
                                text = line,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Butonlar ─────────────────────────────────────────────────
            Button(onClick = onSendBugReport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.BugReport, null)
                Spacer(Modifier.width(8.dp))
                Text("Hata Raporu Gönder (GitHub)")
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = if (ok) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.error,
            fontFamily = FontFamily.Monospace)
    }
}
