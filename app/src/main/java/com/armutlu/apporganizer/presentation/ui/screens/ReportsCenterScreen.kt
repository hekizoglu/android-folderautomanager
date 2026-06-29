package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToUsageReport: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Raporlar", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { SettingsSectionTitle("Rapor Merkezi") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.Dashboard,
                        title = "Genel Bakis",
                        subtitle = "Klasor, kategori, gizli uygulama ve verimlilik ozetleri",
                        onClick = onNavigateToDashboard,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.BarChart,
                        title = "Kullanim Raporu",
                        subtitle = "En cok kullanilanlar, hic acilmayanlar ve gizleme onerileri",
                        onClick = onNavigateToUsageReport,
                    )
                }
            }
            item { SettingsSectionTitle("Hizli Erisim") }
            item {
                SettingsCard {
                    SettingsButtonRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Klasor ve Kategori Yogunlugu",
                        subtitle = "Kategori dagilimi ve aktif klasor odagi",
                        onClick = onNavigateToDashboard,
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsButtonRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Gizli ve Kullanilmayanlar",
                        subtitle = "Gizli uygulamalar ve uzun sure acilmayanlar",
                        onClick = onNavigateToUsageReport,
                    )
                }
            }
        }
    }
}
