package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.UsageStatsHelper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    val visibleUserApps = remember(allApps) { allApps.filter { !it.isHidden && !it.isSystemApp } }
    var hasPermission by remember { mutableStateOf(UsageStatsHelper.hasPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = UsageStatsHelper.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val now = System.currentTimeMillis()
    val thirtyDaysMs = TimeUnit.DAYS.toMillis(30)

    // Kullanım metriği toggle: Süre (ön plan ms) / Adet (kaç kez açıldı) — madde 1
    var usageMetric by remember { mutableStateOf(UsageMetric.DURATION) }

    val appsSorted = remember(visibleUserApps, usageMetric) {
        visibleUserApps.map { app ->
            val value = when (usageMetric) {
                UsageMetric.DURATION -> app.usageCount   // ön plan süresi (ms)
                UsageMetric.COUNT -> app.launchCount     // kaç kez açıldı
            }
            app to value
        }.sortedByDescending { (_, v) -> v }
    }

    val unusedApps = remember(visibleUserApps) {
        visibleUserApps.filter { app ->
            app.lastUsedTimestamp > 0L &&
            (now - app.lastUsedTimestamp) > thirtyDaysMs
        }.sortedBy { it.lastUsedTimestamp }
    }

    val neverUsed = remember(visibleUserApps) {
        visibleUserApps.filter { it.lastUsedTimestamp == 0L }
    }

    val maxValue = (appsSorted.firstOrNull()?.second ?: 1L).coerceAtLeast(1L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kullanım Raporu") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
                            SummaryChip("Toplam", "${visibleUserApps.size} uygulama")
                            SummaryChip("Kullanılan", "${appsSorted.count { it.second > 0 }}")
                            SummaryChip("30g+ Açılmadı", "${unusedApps.size}")
                        }
                    }
                }
            }

            // Metrik toggle: Süre / Adet
            item {
                UsageMetricToggle(selected = usageMetric, onSelect = { usageMetric = it })
            }

            // En çok kullanılanlar
            if (appsSorted.any { it.second > 0 }) {
                item {
                    // D245: baslik yanina toplam hesaplama rozeti — daha "havali" ve bilgilendirici.
                    val topTen = appsSorted.filter { it.second > 0 }.take(10)
                    val totalValue = topTen.sumOf { it.second }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (usageMetric == UsageMetric.DURATION)
                                "En Çok Kullanılan · Süre (30 gün)"
                            else
                                "En Çok Açılan · Adet (30 gün)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        if (totalValue > 0) {
                            Text(
                                "Toplam: ${formatUsageMetric(totalValue, usageMetric)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                itemsIndexed(appsSorted.filter { it.second > 0 }.take(10)) { index, (app, value) ->
                    UsageRow(
                        app = app,
                        value = value,
                        maxValue = maxValue,
                        rank = index + 1,
                        metric = usageMetric,
                        onClick = { openAppInfoSettings(context, app.packageName) }
                    )
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
                    UnusedRow(app = app, now = now,
                        onClick = { openAppInfoSettings(context, app.packageName) },
                        onHide = { viewModel.setAppHidden(app.packageName, true) })
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
                    UnusedRow(app = app, now = now,
                        onClick = { openAppInfoSettings(context, app.packageName) },
                        onHide = { viewModel.setAppHidden(app.packageName, true) })
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
private fun UsageRow(
    app: AppInfo,
    value: Long,
    maxValue: Long,
    rank: Int,
    metric: UsageMetric,
    onClick: () -> Unit
) {
    val barFraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
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
            text = formatUsageMetric(value, metric),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun UnusedRow(app: AppInfo, now: Long, onClick: () -> Unit, onHide: () -> Unit) {
    val daysSince = if (app.lastUsedTimestamp > 0L)
        TimeUnit.MILLISECONDS.toDays(now - app.lastUsedTimestamp)
    else -1L

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp),
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

// ── Metrik toggle (Süre / Adet) ──────────────────────────────────────────────

/** Kullanım metriği: DURATION = ön plan süresi (ms), COUNT = kaç kez açıldı (adet). */
internal enum class UsageMetric { DURATION, COUNT }

@Composable
private fun UsageMetricToggle(selected: UsageMetric, onSelect: (UsageMetric) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == UsageMetric.DURATION,
            onClick = { onSelect(UsageMetric.DURATION) },
            label = { Text("Süre") }
        )
        FilterChip(
            selected = selected == UsageMetric.COUNT,
            onClick = { onSelect(UsageMetric.COUNT) },
            label = { Text("Adet") }
        )
    }
}

/** Süre modunda "2sa 15dk", adet modunda "12 kez" formatlar. */
internal fun formatUsageMetric(value: Long, metric: UsageMetric): String = when (metric) {
    UsageMetric.COUNT -> "$value kez"
    UsageMetric.DURATION -> {
        val minutes = value / 60_000
        if (minutes >= 60) "${minutes / 60}sa ${minutes % 60}dk" else "${minutes}dk"
    }
}

/** Uygulamanın sistem "Uygulama Bilgisi" ekranını açar (bazı cihazlarda desteklenmeyebilir). */
internal fun openAppInfoSettings(context: Context, packageName: String) {
    runCatching {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }
}
