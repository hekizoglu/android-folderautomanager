package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine
import com.armutlu.apporganizer.presentation.viewmodel.CategoryShare
import com.armutlu.apporganizer.presentation.viewmodel.WrappedChartData
import com.armutlu.apporganizer.presentation.viewmodel.WrappedViewModel
import kotlin.math.max
import kotlin.math.roundToInt

private val defaultCategoriesById: Map<String, Category> by lazy {
    Category.getDefaultCategories().associateBy { it.categoryId }
}

private fun categoryLabel(categoryId: String): String =
    defaultCategoriesById[categoryId]?.categoryName ?: categoryId

private fun categoryEmoji(categoryId: String): String =
    defaultCategoriesById[categoryId]?.iconEmoji ?: "📁"

/**
 * Haftalık Rapor ("Wrapped") — Spotify Wrapped tarzı dikey kaydırmalı özet.
 * Tüm veri cihazda hesaplanır (WrappedEngine); sunucuya hiçbir şey gönderilmez,
 * uydurma metrik gösterilmez — veri yoksa ilgili bölüm nazikçe atlanır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedReportScreen(
    viewModel: WrappedViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToNotificationReport: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wrapped_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.wrapped_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.report == null -> EmptyWrappedState(padding)

            else -> WrappedContent(
                padding = padding,
                report = state.report!!,
                charts = state.charts,
                hasUsagePermission = state.hasUsagePermission,
                previousScore = state.previousScore,
                aiCoachLoading = state.aiCoachLoading,
                aiCoachComment = state.aiCoachComment,
                unlockCount = state.unlockCount,
                previousUnlockCount = state.previousUnlockCount,
                onRequestPermission = { viewModel.enableUsagePermission() },
                onNavigateToNotificationReport = onNavigateToNotificationReport,
            )
        }
    }
}

@Composable
private fun EmptyWrappedState(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📊", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.wrapped_empty_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.wrapped_empty_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WrappedContent(
    padding: PaddingValues,
    report: WrappedEngine.WrappedReport,
    charts: WrappedChartData,
    hasUsagePermission: Boolean,
    previousScore: Int?,
    aiCoachLoading: Boolean,
    aiCoachComment: String?,
    unlockCount: Int?,
    previousUnlockCount: Int?,
    onRequestPermission: () -> Unit,
    onNavigateToNotificationReport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!hasUsagePermission) {
            item { UsagePermissionCard(onRequestPermission) }
        }

        // D244 bug fix: previousScore artık WrappedSnapshotPrefs → ViewModel.previousScore →
        // buradaki state parametresinden akar (engine'in hep null dönen weeklyComparison
        // alanı yerine). "Geçen haftaya göre" rozeti artık gerçekten görünür.
        item { ScoreCard(report.score, previousScore) }

        item { PulseSubScoresCard(report.pulse) }

        if (aiCoachLoading || !aiCoachComment.isNullOrBlank()) {
            item { AiCoachCard(aiCoachLoading, aiCoachComment) }
        }

        item { PersonalityCard(report.personality) }

        item { InterestingStatsGrid(report.stats) }

        item { NotificationLinkCard(onNavigateToNotificationReport) }

        item { BadgesGrid(report.badges) }

        report.weeklyComparison?.let { comparison ->
            if (comparison.topGrowingCategories.isNotEmpty()) {
                item { CategoryGrowthCard(comparison.topGrowingCategories) }
            }
        } ?: item { ComparisonPendingCard() }

        if (charts.hasAnyData()) {
            item { WeeklyChartsCard(charts) }
        }

        unlockCount?.let { count ->
            item { UnlockCountCard(count, previousUnlockCount) }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── İzin kartı ────────────────────────────────────────────────────────────

@Composable
private fun UsagePermissionCard(onRequestPermission: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.wrapped_perm_card_title), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.wrapped_perm_card_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestPermission) { Text(stringResource(R.string.wrapped_perm_card_btn)) }
        }
    }
}

// ── a) Skor dairesi ──────────────────────────────────────────────────────

@Composable
private fun AiCoachCard(loading: Boolean, comment: String?) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("AI Koçu", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    comment ?: "Haftalık yorum hazırlanıyor...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UnlockCountCard(unlockCount: Int, previousUnlockCount: Int?) {
    val comparisonText = when {
        previousUnlockCount == null -> "Karsilastirma icin bir hafta daha veri birikiyor."
        previousUnlockCount == 0 && unlockCount == 0 -> "Gecen haftayla ayni: hic kilit acma kaydi yok."
        previousUnlockCount == 0 -> "Gecen hafta kayit yoktu; bu hafta ilk net veri geldi."
        else -> {
            val delta = unlockCount - previousUnlockCount
            val percent = (delta * 100f / previousUnlockCount).roundToInt()
            val direction = when {
                delta > 0 -> "artti"
                delta < 0 -> "azaldi"
                else -> "degismedi"
            }
            "Gecen haftaya gore ${kotlin.math.abs(percent)}% $direction (${if (delta >= 0) "+" else ""}$delta)."
        }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Telefon Kilidi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bu hafta telefonunu $unlockCount kez actin.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    comparisonText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(score: WrappedEngine.DigitalLifeScore, previousScore: Int?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.wrapped_score_title), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            ScoreCircle(score.score)
            if (previousScore != null) {
                Spacer(Modifier.height(12.dp))
                val delta = score.score - previousScore
                val deltaText = if (delta >= 0) "+$delta" else "$delta"
                val deltaColor = when {
                    delta > 0 -> Color(0xFF2E7D32)
                    delta < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = deltaColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        stringResource(R.string.wrapped_score_delta_vs_last_week, deltaText),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = deltaColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (score.reasons.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    score.reasons.forEach { reason -> ScoreReasonRow(reason) }
                }
            }
        }
    }
}

@Composable
private fun ScoreCircle(score: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 900),
        label = "wrapped_score_progress",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val progressColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(animatedProgress * 100).roundToInt()}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.wrapped_score_max), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScoreReasonRow(reason: WrappedEngine.ScoreReason) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            reason.label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        if (reason.delta != 0) {
            val color = if (reason.delta > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            Text(
                if (reason.delta > 0) "+${reason.delta}" else "${reason.delta}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

// ── Dijital Nabız alt skorları (V2, D244) — tek motor DigitalPulseEngine ────

@Composable
private fun PulseSubScoresCard(pulse: com.armutlu.apporganizer.domain.usecase.pulse.DigitalPulseScore) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Alt Skorlar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PersonalityBar("Düzen", pulse.organization)
                PersonalityBar("Dikkat Yönetimi", pulse.attention)
                PersonalityBar("Kullanım Dengesi", pulse.balance)
                PersonalityBar("Dijital Temizlik", pulse.cleanup)
                PersonalityBar("İstikrar", pulse.consistency)
            }
        }
    }
}

// ── b) Kişilik kartı ──────────────────────────────────────────────────────

private fun WrappedChartData.hasAnyData(): Boolean =
    dailyUsageMinutes.any { it > 0 } ||
        dailyNotificationCounts.any { it > 0 } ||
        categoryShares.isNotEmpty()

@Composable
private fun WeeklyChartsCard(charts: WrappedChartData) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Haftalik Grafikler", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Kullanim, bildirim ve kategori dagilimi cihazda cizilir.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            ChartSectionTitle("Kullanim trendi", formatUsageTotal(charts.dailyUsageMinutes))
            WeeklyUsageBars(charts.dailyUsageMinutes)

            Spacer(Modifier.height(16.dp))
            ChartSectionTitle("Bildirim trendi", "${charts.dailyNotificationCounts.sum()} bildirim")
            NotificationSparkline(
                counts = charts.dailyNotificationCounts,
                nightCounts = charts.dailyNightNotificationCounts,
            )

            if (charts.categoryShares.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ChartSectionTitle("Kategori dagilimi", "ilk ${charts.categoryShares.size}")
                CategoryDistributionChart(charts.categoryShares)
            }
        }
    }
}

@Composable
private fun ChartSectionTitle(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        Text(value, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun WeeklyUsageBars(values: List<Int>) {
    val normalized = values.takeLast(7).let { data -> List(7 - data.size) { 0 } + data }
    val maxValue = max(1, normalized.maxOrNull() ?: 0)
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(86.dp)) {
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * 6) / 7f
            val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())
            normalized.forEachIndexed { index, value ->
                val left = index * (barWidth + gap)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = corner,
                )
                val height = (size.height * (value.toFloat() / maxValue)).coerceAtLeast(if (value > 0) 6.dp.toPx() else 0f)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = corner,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Pzt", "Sal", "Car", "Per", "Cum", "Cmt", "Paz").forEach {
                Text(it, fontSize = 9.sp, color = labelColor)
            }
        }
    }
}

@Composable
private fun NotificationSparkline(counts: List<Int>, nightCounts: List<Int>) {
    val values = counts.takeLast(7).let { data -> List(7 - data.size) { 0 } + data }
    val nights = nightCounts.takeLast(7).let { data -> List(7 - data.size) { 0 } + data }
    val maxValue = max(1, values.maxOrNull() ?: 0)
    val lineColor = MaterialTheme.colorScheme.secondary
    val pointColor = MaterialTheme.colorScheme.primary
    val nightColor = Color(0xFFFFB74D)
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(74.dp)) {
            val step = size.width / 6f
            val topPadding = 8.dp.toPx()
            val bottomPadding = 12.dp.toPx()
            val chartHeight = size.height - topPadding - bottomPadding
            drawLine(
                color = trackColor,
                start = Offset(0f, size.height - bottomPadding),
                end = Offset(size.width, size.height - bottomPadding),
                strokeWidth = 1.dp.toPx(),
            )
            val points = values.mapIndexed { index, value ->
                Offset(
                    x = index * step,
                    y = topPadding + chartHeight * (1f - value.toFloat() / maxValue),
                )
            }
            points.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = lineColor,
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            points.forEachIndexed { index, point ->
                drawCircle(color = pointColor, radius = 3.5.dp.toPx(), center = point)
                if (nights.getOrElse(index) { 0 } > 0) {
                    drawCircle(
                        color = nightColor,
                        radius = 3.dp.toPx(),
                        center = Offset(point.x, size.height - 4.dp.toPx()),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(nightColor),
            )
            Spacer(Modifier.width(6.dp))
            Text("Gece bildirimi olan gunler isaretli", fontSize = 10.sp, color = labelColor)
        }
    }
}

@Composable
private fun CategoryDistributionChart(shares: List<CategoryShare>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shares.take(5).forEachIndexed { index, share ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${categoryEmoji(share.categoryId)} ${categoryLabel(share.categoryId)}",
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(0.42f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CategoryShareBar(
                    percent = share.percent,
                    index = index,
                    modifier = Modifier.weight(0.58f),
                )
            }
        }
    }
}

@Composable
private fun CategoryShareBar(percent: Int, index: Int, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF26C6DA),
        Color(0xFFFFB74D),
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Canvas(modifier = Modifier.weight(1f).height(12.dp)) {
            val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            drawRoundRect(
                color = trackColor,
                size = Size(size.width, size.height),
                cornerRadius = radius,
            )
            drawRoundRect(
                color = colors[index % colors.size],
                size = Size(size.width * (percent / 100f).coerceIn(0f, 1f), size.height),
                cornerRadius = radius,
            )
        }
        Text("%$percent", fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatUsageTotal(values: List<Int>): String {
    val total = values.sum()
    val hours = total / 60
    val minutes = total % 60
    return if (hours > 0) "${hours}sa ${minutes}dk" else "${minutes}dk"
}

@Composable
private fun PersonalityCard(personality: WrappedEngine.PersonalityResult) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.wrapped_personality_title), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(personality.type.emoji, fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(personality.type.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (personality.dominantPercentage > 0) {
                        Text(
                            stringResource(R.string.wrapped_personality_dominant_pct, personality.dominantPercentage),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val nonZero = personality.categoryPercentages.filterValues { it > 0 }
            if (nonZero.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    nonZero.entries.sortedByDescending { it.value }.forEach { (catId, pct) ->
                        PersonalityBar(categoryLabel(catId), pct)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalityBar(label: String, percent: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%$percent", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

// ── c) İlginç istatistikler ──────────────────────────────────────────────

@Composable
private fun InterestingStatsGrid(stats: WrappedEngine.InterestingStats) {
    val mostOpenedTitle = stringResource(R.string.wrapped_stat_most_opened)
    val leastOpenedTitle = stringResource(R.string.wrapped_stat_least_opened)
    val largestAppTitle = stringResource(R.string.wrapped_stat_largest_app)
    val oldestInstalledTitle = stringResource(R.string.wrapped_stat_oldest_installed)
    val newestInstalledTitle = stringResource(R.string.wrapped_stat_newest_installed)
    val longestUnusedTitle = stringResource(R.string.wrapped_stat_longest_unused)
    val entries = buildList {
        stats.mostOpenedApp?.let { add(Triple(mostOpenedTitle, it.appName, stringResource(R.string.wrapped_stat_usage_count, it.usageCount.toInt()))) }
        stats.leastOpenedApp?.let { add(Triple(leastOpenedTitle, it.appName, stringResource(R.string.wrapped_stat_usage_count, it.usageCount.toInt()))) }
        stats.largestApp?.let { add(Triple(largestAppTitle, it.appName, formatSizeMb(it.appSizeBytes))) }
        stats.oldestInstalledApp?.let { add(Triple(oldestInstalledTitle, it.appName, null)) }
        stats.newestInstalledApp?.let { add(Triple(newestInstalledTitle, it.appName, null)) }
        stats.longestUnusedApp?.let { add(Triple(longestUnusedTitle, it.appName, null)) }
    }
    if (entries.isEmpty()) return

    Column {
        Text(stringResource(R.string.wrapped_stats_title), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            entries.chunked(2).forEach { rowEntries ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowEntries.forEach { (title, appName, extra) ->
                        StatTile(title, appName, extra, modifier = Modifier.weight(1f))
                    }
                    if (rowEntries.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, appName: String, extra: String?, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(appName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (extra != null) {
                Text(extra, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatSizeMb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}

// ── d) Rozetler ────────────────────────────────────────────────────────────

@Composable
private fun BadgesGrid(badges: List<WrappedEngine.Badge>) {
    if (badges.isEmpty()) return
    Column {
        Text(stringResource(R.string.wrapped_badges_title), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            badges.chunked(2).forEach { rowBadges ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowBadges.forEach { badge -> BadgeTile(badge, modifier = Modifier.weight(1f)) }
                    if (rowBadges.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: WrappedEngine.Badge, modifier: Modifier = Modifier) {
    val alphaValue = if (badge.earned) 1f else 0.45f
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (badge.earned) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier.alpha(alphaValue),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (badge.earned) badge.emoji else "🔒", fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                badge.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                badge.criteriaDescription,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

// ── e) Kategori büyümesi / haftalık karşılaştırma ───────────────────────

@Composable
private fun CategoryGrowthCard(topGrowing: List<WrappedEngine.CategoryGrowth>) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.wrapped_growth_title), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topGrowing.forEach { growth ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${categoryEmoji(growth.categoryId)} ${categoryLabel(growth.categoryId)}",
                            fontSize = 13.sp,
                        )
                        val color = if (growth.deltaPercent >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        val sign = if (growth.deltaPercent >= 0) "+" else ""
                        Text(
                            "$sign${growth.deltaPercent}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonPendingCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏳", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.wrapped_comparison_pending),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Bildirim özeti linki ─────────────────────────────────────────────────

@Composable
private fun NotificationLinkCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wrapped_notif_link_title), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.wrapped_notif_link_desc),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClick) { Text(stringResource(R.string.wrapped_notif_link_open)) }
        }
    }
}
