package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.utils.UsageStatsHelper
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOrganizerDashboardScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUsageReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsState()
    val allApps = screenState.apps
    val categories = screenState.categories

    val hasUsagePermission = remember { UsageStatsHelper.hasPermission(context) }

    val usageTimes = remember(hasUsagePermission) {
        if (hasUsagePermission) UsageStatsHelper.getUsageCounts(context, days = 7) else emptyMap()
    }

    val now = System.currentTimeMillis()
    val sevenDaysMs = TimeUnit.DAYS.toMillis(7)
    val thirtyDaysMs = TimeUnit.DAYS.toMillis(30)

    val stats = remember(allApps, usageTimes) {
        DashboardStats.compute(allApps, categories, usageTimes, now, sevenDaysMs, thirtyDaysMs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AppOrganizer Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HeroStatsRow(stats) }
            item { DashSectionHeader("Bu Hafta En Cok Kullanilanlar") }
            item { TopAppsCard(stats.topApps, hasUsagePermission) }
            item { DashSectionHeader("Kategori Yogunlugu") }
            item { CategoryBreakdownCard(stats.categoryBreakdown) }
            item { DashSectionHeader("Kullanilmayan Uygulamalar") }
            item { UnusedAppsCard(stats.unusedCount, stats.neverUsedCount) }
            item { DashSectionHeader("Verimlilik Ozeti") }
            item { EfficiencyCard(stats) }
            // Dashboard = genel ozet, UsageReport = uygulama bazli detay (spec Risk 6)
            item {
                TextButton(
                    onClick = onNavigateToUsageReport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Detaylı Rapor →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Veri modeli ──────────────────────────────────────────────────────────────

private data class DashboardStats(
    val totalApps: Int,
    val totalCategories: Int,
    val hiddenApps: Int,
    val topApps: List<Pair<AppInfo, Long>>,
    val categoryBreakdown: List<Pair<String, Int>>,
    val unusedCount: Int,
    val neverUsedCount: Int,
    val totalUsageMinutesThisWeek: Long,
    val organizedPercent: Int
) {
    companion object {
        fun compute(
            apps: List<AppInfo>,
            categories: List<Category>,
            usageTimes: Map<String, Long>,
            now: Long,
            sevenDaysMs: Long,
            thirtyDaysMs: Long
        ): DashboardStats {
            val visible = apps.filter { !it.isHidden && !it.isSystemApp }

            val topApps = visible
                .map { it to (usageTimes[it.packageName] ?: 0L) }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .take(5)

            val catNameMap = categories.associate { it.categoryId to it.categoryName }
            val catBreakdown = visible
                .groupBy { it.categoryId }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(6)
                .map { (catId, count) -> (catNameMap[catId] ?: catId) to count }

            val unusedCount = visible.count { app ->
                app.lastUsedTimestamp > 0L &&
                (now - app.lastUsedTimestamp) > thirtyDaysMs
            }
            val neverUsedCount = visible.count { it.lastUsedTimestamp == 0L }

            val totalMs = usageTimes.values.sumOf { it }
            val totalMinutes = totalMs / 60_000

            val organizedPercent = if (apps.isEmpty()) 0
            else (visible.count { it.categoryId.isNotBlank() } * 100 / visible.size.coerceAtLeast(1))

            return DashboardStats(
                totalApps = visible.size,
                totalCategories = categories.size,
                hiddenApps = apps.count { it.isHidden },
                topApps = topApps,
                categoryBreakdown = catBreakdown,
                unusedCount = unusedCount,
                neverUsedCount = neverUsedCount,
                totalUsageMinutesThisWeek = totalMinutes,
                organizedPercent = organizedPercent
            )
        }
    }
}

// ── Bilesenler ───────────────────────────────────────────────────────────────

@Composable
private fun HeroStatsRow(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HeroChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PhoneAndroid,
            value = "${stats.totalApps}",
            label = "Yonetilen\nUygulama",
            color = MaterialTheme.colorScheme.primary
        )
        HeroChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.FolderOpen,
            value = "${stats.totalCategories}",
            label = "Aktif\nKlasor",
            color = Color(0xFF26C6DA)
        )
        HeroChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.VisibilityOff,
            value = "${stats.hiddenApps}",
            label = "Gizlenen\nUygulama",
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun HeroChip(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            Text(label, fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp)
        }
    }
}

@Composable
private fun DashSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
    )
}

@Composable
private fun TopAppsCard(topApps: List<Pair<AppInfo, Long>>, hasPermission: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!hasPermission) {
                Text(
                    "Kullanim istatistigi icin Ayarlar > Ozel Erisim > Kullanim Verileri iznini etkinlestir.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            if (topApps.isEmpty()) {
                Text("Bu hafta kullanim verisi bulunamadi.", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val maxMs = topApps.first().second.coerceAtLeast(1)
            topApps.forEachIndexed { index, (app, ms) ->
                TopAppRow(rank = index + 1, app = app, ms = ms, maxMs = maxMs)
                if (index < topApps.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopAppRow(rank: Int, app: AppInfo, ms: Long, maxMs: Long) {
    val minutes = ms / 60_000
    val fraction = (ms.toFloat() / maxMs).coerceIn(0f, 1f)
    val animFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(600))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$rank",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(app.appName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF26C6DA)
                                )
                            )
                        )
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (minutes >= 60) "${minutes / 60}s ${minutes % 60}d" else "${minutes}d",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(52.dp)
        )
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: List<Pair<String, Int>>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (breakdown.isEmpty()) {
                Text("Kategori verisi yok.", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            val maxCount = breakdown.first().second.coerceAtLeast(1)
            breakdown.forEach { (catName, count) ->
                CategoryBar(name = catName, count = count, max = maxCount)
            }
        }
    }
}

@Composable
private fun CategoryBar(name: String, count: Int, max: Int) {
    val fraction = (count.toFloat() / max).coerceIn(0f, 1f)
    val animFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(700))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 12.sp, modifier = Modifier.width(90.dp),
            color = MaterialTheme.colorScheme.onSurface)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF00897B))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun UnusedAppsCard(unusedCount: Int, neverUsedCount: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UnusedChip(
                modifier = Modifier.weight(1f),
                value = "$unusedCount",
                label = "30 gundur\nacilmadi",
                color = MaterialTheme.colorScheme.error
            )
            UnusedChip(
                modifier = Modifier.weight(1f),
                value = "$neverUsedCount",
                label = "Hic\nacilamadi",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnusedChip(modifier: Modifier, value: String, label: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp)
    }
}

@Composable
private fun EfficiencyCard(stats: DashboardStats) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EfficiencyRow(
                icon = Icons.Default.Category,
                label = "Kategori Organizasyonu",
                value = "%${stats.organizedPercent}",
                color = Color(0xFF00897B)
            )
            EfficiencyRow(
                icon = Icons.Default.TrendingUp,
                label = "Bu Hafta Toplam Kullanim",
                value = if (stats.totalUsageMinutesThisWeek >= 60)
                    "${stats.totalUsageMinutesThisWeek / 60}s ${stats.totalUsageMinutesThisWeek % 60}d"
                else
                    "${stats.totalUsageMinutesThisWeek}d",
                color = Color(0xFF26C6DA)
            )
            EfficiencyRow(
                icon = Icons.Default.AutoAwesome,
                label = "Aktif Klasor Basina Uygulama",
                value = if (stats.totalCategories > 0)
                    "%.1f".format(stats.totalApps.toFloat() / stats.totalCategories)
                else "-",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EfficiencyRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
    }
}
