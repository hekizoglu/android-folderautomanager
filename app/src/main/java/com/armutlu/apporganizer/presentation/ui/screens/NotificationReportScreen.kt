package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.presentation.viewmodel.NotificationReportViewModel
import com.armutlu.apporganizer.utils.NotificationAnalyzer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationReportScreen(
    viewModel: NotificationReportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val report by viewModel.uiState.collectAsState()
    val permissionGranted by viewModel.listenerPermissionGranted.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bildirim Raporu") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        if (report == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val r = report!!

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!permissionGranted) {
                item {
                    PermissionWarningCard {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }

            item { SummaryCard(r) }

            item { SectionTitle("📢 Çok Konuşanlar") }
            items(r.mostTalkative) { stat -> TalkativeRow(stat) }

            item { SectionTitle("🌙 Rahatsız Edenler") }
            if (r.disturbing.isEmpty()) {
                item { EmptyStateText("Gece rahatsız eden uygulama yok 🎉") }
            } else {
                items(r.disturbing) { stat -> DisturbingRow(stat) }
            }

            item {
                Column {
                    SectionTitle("🎯 Dikkat Dağıtanlar")
                    Text(
                        "Çok bildirim gönderip az kullanılan uygulamalar — kapatmayı düşünebilirsin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
            }
            if (r.distracting.isEmpty()) {
                item { EmptyStateText("Dikkat dağıtan uygulama tespit edilmedi ✨") }
            } else {
                items(r.distracting) { stat -> DistractingRow(stat) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PermissionWarningCard(onGrantClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning, null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bildirim erişimi verilmemiş — rapor için Ayarlar'dan izin ver",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrantClick) {
                Text("İzin Ver")
            }
        }
    }
}

@Composable
private fun SummaryCard(report: NotificationAnalyzer.Report) {
    val topApp = report.mostTalkative.firstOrNull()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Son 7 gün", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "${report.totalNotifications}",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "toplam bildirim",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (topApp != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "${topApp.appName} ${topApp.total} bildirim gönderdi",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyStateText(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun TalkativeRow(stat: NotificationAnalyzer.AppNotifStats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${stat.total}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            val maxCount = (stat.dailyCounts.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                stat.dailyCounts.forEach { count ->
                    val fraction = count.toFloat() / maxCount
                    val height = (24.dp * fraction).coerceAtLeast(2.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(height)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisturbingRow(stat: NotificationAnalyzer.AppNotifStats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "gece %${(stat.nightRatio * 100).toInt()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "saatte en çok ${stat.maxBurstPerHour}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DistractingRow(stat: NotificationAnalyzer.AppNotifStats) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stat.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${stat.total} bildirim / ${stat.usageMinutes} dk kullanım",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "%.1f".format(stat.distractionScore),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
