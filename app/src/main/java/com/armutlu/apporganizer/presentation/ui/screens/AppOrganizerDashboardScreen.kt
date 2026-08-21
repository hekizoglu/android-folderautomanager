package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.armutlu.apporganizer.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.models.WeeklyGoalMode
import com.armutlu.apporganizer.domain.models.WeeklyGoalStatus
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel
import com.armutlu.apporganizer.presentation.viewmodel.DashboardViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.UsageStatsHelper
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOrganizerDashboardScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToUsageReport: () -> Unit = {},
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsState()
    val allApps = screenState.apps
    val categories = screenState.categories
    val goalsUiState by dashboardViewModel.uiState.collectAsState()
    var goalsEnabled by remember { mutableStateOf(AppPrefs.isGoalsEnabled(context)) }

    var hasUsagePermission by remember { mutableStateOf(UsageStatsHelper.hasPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = UsageStatsHelper.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val usageTimes by produceState<Map<String, Long>>(initialValue = emptyMap(), hasUsagePermission, allApps) {
        value = if (hasUsagePermission) {
            withContext(Dispatchers.IO) {
                UsageStatsHelper.getUsageCounts(context, days = 7)
            }
        } else {
            emptyMap()
        }
    }

    val now = System.currentTimeMillis()
    val sevenDaysMs = TimeUnit.DAYS.toMillis(7)
    val thirtyDaysMs = TimeUnit.DAYS.toMillis(30)

    val stats = remember(allApps, usageTimes) {
        DashboardStats.compute(allApps, categories, usageTimes, now, sevenDaysMs, thirtyDaysMs)
    }

    // Metrik toggle: Süre (ön plan ms) / Adet (kaç kez açıldı) — madde 1
    var usageMetric by remember { mutableStateOf(UsageMetric.DURATION) }

    val topAppsForMetric = remember(allApps, usageMetric) {
        allApps.filter { !it.isHidden && !it.isSystemApp }
            .map { app ->
                val value = when (usageMetric) {
                    UsageMetric.DURATION -> usageTimes[app.packageName] ?: app.usageCount
                    UsageMetric.COUNT -> app.launchCount     // kaç kez açıldı
                }
                app to value
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AppOrganizer Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
            item { UsageMetricToggleRow(usageMetric) { usageMetric = it } }
            item { TopAppsCard(topAppsForMetric, hasUsagePermission, usageMetric) }
            if (goalsEnabled) {
                item { DashSectionHeader(stringResource(R.string.dashboard_smart_category_goals_title)) }
                item {
                    SmartCategoryGoalsCard(
                        uiState = goalsUiState,
                        categories = categories,
                        onSwitchToManual = dashboardViewModel::switchToManual,
                        onSwitchToAuto = dashboardViewModel::switchToAuto,
                        onDeleteGoal = dashboardViewModel::deleteWeeklyGoal,
                    )
                }
            }
            item { DashSectionHeader("Kategori Yogunlugu") }
            item { CategoryBreakdownCard(stats.categoryBreakdown, onClick = onNavigateToUsageReport) }
            item { DashSectionHeader("Kullanilmayan Uygulamalar") }
            item { UnusedAppsCard(stats.unusedCount, stats.neverUsedCount, onClick = onNavigateToUsageReport) }
            item { DashSectionHeader("Verimlilik Ozeti") }
            item { EfficiencyCard(stats, onClick = onNavigateToUsageReport) }
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
    val organizedPercent: Int,
    // P0.2: ClassificationAttentionPolicy ile tek kaynaktan hesaplanan dikkat
    // gerektiren uygulama sayisi — ClassificationReviewScreen ve SettingsAppsSection
    // ile ayni deger (sayac/liste tutarliligi).
    val attentionCount: Int
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

            val organizedPercent = if (visible.isEmpty()) 0
            else (
                visible.count { it.categoryId.isNotBlank() && it.categoryId != Category.CAT_UNCATEGORIZED } *
                    100 / visible.size
                )

            return DashboardStats(
                totalApps = visible.size,
                totalCategories = categories.size,
                hiddenApps = apps.count { it.isHidden },
                topApps = topApps,
                categoryBreakdown = catBreakdown,
                unusedCount = unusedCount,
                neverUsedCount = neverUsedCount,
                totalUsageMinutesThisWeek = totalMinutes,
                organizedPercent = organizedPercent,
                attentionCount = com.armutlu.apporganizer.domain.usecase.classify.ClassificationAttentionPolicy
                    .attentionCount(apps, now)
            )
        }
    }
}

// ── Bilesenler ───────────────────────────────────────────────────────────────

/**
 * P5 — roadmap §9. Eski "kategori seç + dakika yaz + Kaydet" akışı KALDIRILDI (S2 fix).
 * Her satır: geçen hafta / bu haftaki hedef / şu ana kadar / kalan / yüzde / trend / durum /
 * mod gösterir. Hedef aşıldığında ilerleme %100'de KESİLMEZ — gerçek aşım metinle gösterilir
 * (roadmap §9 "Hedef 35 dakika aşıldı" örneği). Renk tek başına anlam taşımaz, metin+ikon da var.
 */
@Composable
private fun SmartCategoryGoalsCard(
    uiState: DashboardViewModel.DashboardGoalsUiState,
    categories: List<Category>,
    onSwitchToManual: (String) -> Unit,
    onSwitchToAuto: (String) -> Unit,
    onDeleteGoal: (String) -> Unit,
) {
    val categoryNames = remember(categories) { categories.associate { it.categoryId to it.categoryName } }
    val categoryEmojis = remember(categories) { categories.associate { it.categoryId to it.iconEmoji } }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.dashboard_smart_category_goals_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                uiState.isLoading -> Text(
                    stringResource(R.string.dashboard_smart_category_goals_loading),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.isLearningMode -> Text(
                    stringResource(R.string.dashboard_smart_category_goals_learning_mode),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.goals.isEmpty() -> Text(
                    stringResource(R.string.dashboard_smart_category_goals_empty),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> uiState.goals.forEach { goalUi ->
                    CategoryGoalRow(
                        goalUi = goalUi,
                        categoryName = categoryNames[goalUi.categoryId] ?: goalUi.categoryId,
                        categoryEmoji = categoryEmojis[goalUi.categoryId] ?: "📱",
                        onSwitchToManual = onSwitchToManual,
                        onSwitchToAuto = onSwitchToAuto,
                        onDeleteGoal = onDeleteGoal,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGoalRow(
    goalUi: com.armutlu.apporganizer.domain.advice.CategoryGoalForAdvice,
    categoryName: String,
    categoryEmoji: String,
    onSwitchToManual: (String) -> Unit,
    onSwitchToAuto: (String) -> Unit,
    onDeleteGoal: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val goal = goalUi.goal
    val usedSoFar = goalUi.currentWeekMinutesSoFar
    val target = goal.targetMinutes
    val progress = usedSoFar?.let { (it.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 0f
    val isExceeded = usedSoFar != null && usedSoFar > target
    val exceededBy = if (isExceeded) usedSoFar!! - target else 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(categoryEmoji, fontSize = 14.sp)
                Text(categoryName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                ModeBadge(mode = goal.mode)
            }
            StatusBadge(status = goal.status, isExceeded = isExceeded)
        }

        if (usedSoFar == null) {
            Text(
                stringResource(R.string.dashboard_category_goal_data_unavailable),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            val usedText = formatMinutes(usedSoFar)
            val targetText = formatMinutes(target.toLong())
            if (isExceeded) {
                Text(
                    "$usedText / $targetText — " +
                        stringResource(R.string.dashboard_category_goal_exceeded_by, formatMinutes(exceededBy)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                val remaining = (target - usedSoFar).coerceAtLeast(0L)
                Text(
                    "$usedText / $targetText — " +
                        stringResource(R.string.dashboard_category_goal_remaining, formatMinutes(remaining)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        goalUi.previousWeekMinutes?.let { previous ->
            Text(
                stringResource(R.string.dashboard_category_goal_previous_week, formatMinutes(previous)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goal.mode == WeeklyGoalMode.AUTO) {
                    TextButton(onClick = { onSwitchToManual(goalUi.categoryId) }) {
                        Text(stringResource(R.string.dashboard_category_goal_switch_to_manual))
                    }
                } else {
                    TextButton(onClick = { onSwitchToAuto(goalUi.categoryId) }) {
                        Text(stringResource(R.string.dashboard_category_goal_switch_to_auto))
                    }
                }
                TextButton(onClick = { onDeleteGoal(goalUi.categoryId) }) {
                    Text(stringResource(R.string.dashboard_category_goal_delete))
                }
            }
        }
    }
}

@Composable
private fun ModeBadge(mode: WeeklyGoalMode) {
    val label = if (mode == WeeklyGoalMode.AUTO) {
        stringResource(R.string.dashboard_category_goal_mode_auto)
    } else {
        stringResource(R.string.dashboard_category_goal_mode_manual)
    }
    Text(
        label,
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun StatusBadge(status: WeeklyGoalStatus, isExceeded: Boolean) {
    val (labelRes, color) = when {
        isExceeded -> R.string.dashboard_category_goal_status_at_risk to MaterialTheme.colorScheme.error
        status == WeeklyGoalStatus.COMPLETED ->
            R.string.dashboard_category_goal_status_completed to MaterialTheme.colorScheme.primary
        status == WeeklyGoalStatus.EXCEEDED ->
            R.string.dashboard_category_goal_status_exceeded to MaterialTheme.colorScheme.error
        status == WeeklyGoalStatus.DATA_UNAVAILABLE ->
            R.string.dashboard_category_goal_status_data_unavailable to MaterialTheme.colorScheme.onSurfaceVariant
        else -> R.string.dashboard_category_goal_status_on_track to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(stringResource(labelRes), fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
}

private fun formatMinutes(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours} sa. ${mins} dk." else "${mins} dk."
}

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
private fun UsageMetricToggleRow(selected: UsageMetric, onSelect: (UsageMetric) -> Unit) {
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

@Composable
private fun TopAppsCard(topApps: List<Pair<AppInfo, Long>>, hasPermission: Boolean, metric: UsageMetric) {
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
            val maxValue = topApps.first().second.coerceAtLeast(1)
            topApps.forEachIndexed { index, (app, value) ->
                TopAppRow(rank = index + 1, app = app, value = value, maxValue = maxValue, metric = metric)
                if (index < topApps.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TopAppRow(rank: Int, app: AppInfo, value: Long, maxValue: Long, metric: UsageMetric) {
    val context = LocalContext.current
    val fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f)
    val animFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(600))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openAppInfoSettings(context, app.packageName) },
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            formatUsageMetric(value, metric),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: List<Pair<String, Int>>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
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
private fun UnusedAppsCard(unusedCount: Int, neverUsedCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
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
private fun EfficiencyCard(stats: DashboardStats, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EfficiencyRow(
                icon = Icons.Default.Category,
                label = "Kategori Organizasyonu",
                value = "%${stats.organizedPercent}",
                color = Color(0xFF00897B)
            )
            EfficiencyRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
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
            EfficiencyRow(
                icon = Icons.Default.PhoneAndroid,
                label = "Kontrol Bekleyenler",
                value = stats.attentionCount.toString(),
                color = if (stats.attentionCount > 0) Color(0xFFEF6C00) else MaterialTheme.colorScheme.primary
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
