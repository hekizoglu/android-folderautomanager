package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageReportScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsState()
    val allApps = screenState.apps
    val hasPermission = remember { UsageStatsHelper.hasPermission(context) }

    val usageTimes = remember(hasPermission) {
        if (hasPermission) UsageStatsHelper.getUsageCounts(context, days = 30) else emptyMap()
    }

    val now = System.currentTimeMillis()
    val thirtyDaysMs = TimeUnit.DAYS.toMillis(30)
    val sevenDaysMs = TimeUnit.DAYS.toMillis(7)

    val appsSorted = remember(allApps, usageTimes) {
        allApps.filter { !it.isHidden }.map { app ->
            val foregroundMs = usageTimes[app.packageName] ?: 0L
            app to foregroundMs
        }.sortedByDescending { (_, ms) -> ms }
    }

    val unusedApps = remember(allApps) {
        allApps.filter { app ->
            !app.isHidden &&
            app.lastUsedTimestamp > 0L &&
            (now - app.lastUsedTimestamp) > thirtyDaysMs
        }.sortedBy { it.lastUsedTimestamp }
    }

    val neverUsed = remember(allApps) {
        allApps.filter { it.lastUsedTimestamp == 0L && !it.isHidden }
    }

    val maxMs = appsSorted.firstOrNull()?.second ?: 1L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kullanım Raporu") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Warning, null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Kullanım İzni Gerekli", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Uygulama kullanım verilerine erişmek için izin gerekiyor.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { UsageStatsHelper.openPermissionSettings(context) }) {
                    Text("İzin Ver")
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Özet kartı
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Son 30 Gün Özeti", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SummaryChip("Toplam", "${allApps.size} uygulama")
                            SummaryChip("Kullanılan", "${appsSorted.count { it.second > 0 }}")
                            SummaryChip("30g+ Açılmadı", "${unusedApps.size}")
                        }
                    }
                }
            }

            // En çok kullanılanlar
            if (appsSorted.any { it.second > 0 }) {
                item {
                    Text("En Çok Kullanılan (30 gün)", fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
                itemsIndexed(appsSorted.filter { it.second > 0 }.take(10)) { index, (app, ms) ->
                    UsageRow(app = app, foregroundMs = ms, maxMs = maxMs, rank = index + 1)
                }
            }

            // 30 gün açılmayanlar
            if (unusedApps.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("30 Gündür Açılmadı (${unusedApps.size})",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                itemsIndexed(unusedApps.take(15)) { _, app ->
                    UnusedRow(app = app, now = now, onHide = {
                        viewModel.setAppHidden(app.packageName, true)
                    })
                }
            }

            // Hiç açılmamış
            if (neverUsed.isNotEmpty()) {
                item {
                    Text("Hiç Kullanılmadı (${neverUsed.size})",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                itemsIndexed(neverUsed.take(10)) { _, app ->
                    UnusedRow(app = app, now = now, onHide = {
                        viewModel.setAppHidden(app.packageName, true)
                    })
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UsageRow(app: AppInfo, foregroundMs: Long, maxMs: Long, rank: Int) {
    val minutes = foregroundMs / 60000
    val barFraction = (foregroundMs.toFloat() / maxMs).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$rank", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(app.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(barFraction).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (minutes >= 60) "${minutes / 60}s ${minutes % 60}d" else "${minutes}d",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun UnusedRow(app: AppInfo, now: Long, onHide: () -> Unit) {
    val daysSince = if (app.lastUsedTimestamp > 0L)
        TimeUnit.MILLISECONDS.toDays(now - app.lastUsedTimestamp)
    else -1L

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.appName, fontSize = 13.sp)
            Text(
                text = if (daysSince >= 0) "$daysSince gün önce" else "Hiç açılmadı",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onHide,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.VisibilityOff, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Gizle", fontSize = 12.sp)
        }
    }
}
