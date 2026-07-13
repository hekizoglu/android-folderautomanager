package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.viewmodel.PulseClockViewModel
import com.armutlu.apporganizer.utils.AppPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Pulse Clock — ana ekranın imza saat bileşeni (D244). Üç stil (Minimal/Pulse/Glass) destekler,
 * ayarlar AppPrefs.KEY_CLOCK_STYLE / KEY_HOME_SCORE_VISIBLE / KEY_HOME_INSIGHT_VISIBLE'dan okunur.
 *
 * Performans: saat metni dakika sınırında güncellenir (her saniye DEĞİL); skor/içgörü
 * PulseClockViewModel'den TEK motor (DigitalPulseEngine) üzerinden gelir ve 15dk cache'lidir —
 * saat tik'i asla skor hesabı tetiklemez.
 *
 * @param compact Dar ekran / kalabalık grid'de küçülür — klasörler kaybolmasın diye.
 * @param onLongPress Uzun basma → mevcut launcher yönetim ekranı davranışı (BOZULMAZ) —
 *                     tek gesture detector (combinedClickable) ile iç içe çakışma önlenir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PulseClockWidget(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onOpenWeeklyReport: () -> Unit = {},
    onOpenScoreDetails: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: PulseClockViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshIfStale() }

    var clockStyle by remember { mutableStateOf(AppPrefs.getClockStyle(context)) }
    var scoreVisible by remember { mutableStateOf(AppPrefs.isHomeScoreVisible(context)) }
    var insightVisible by remember { mutableStateOf(AppPrefs.isHomeInsightVisible(context)) }
    var usageChartVisible by remember { mutableStateOf(AppPrefs.isHomeUsageChartVisible(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_CLOCK_STYLE -> clockStyle = AppPrefs.getClockStyle(context)
                AppPrefs.KEY_HOME_SCORE_VISIBLE -> scoreVisible = AppPrefs.isHomeScoreVisible(context)
                AppPrefs.KEY_HOME_INSIGHT_VISIBLE -> insightVisible = AppPrefs.isHomeInsightVisible(context)
                AppPrefs.KEY_HOME_USAGE_CHART_VISIBLE -> usageChartVisible = AppPrefs.isHomeUsageChartVisible(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Saat/tarih — dakika sınırında güncellenir (skor hesabını TETİKLEMEZ)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale("tr")) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val secondsToNextMinute = 60 - Calendar.getInstance().get(Calendar.SECOND)
            delay(secondsToNextMinute * 1000L)
        }
    }

    when (clockStyle) {
        AppPrefs.CLOCK_STYLE_MINIMAL -> MinimalClockCard(now, timeFormat, dateFormat, compact, onLongPress, modifier)
        else -> PulseCard(
            now = now,
            timeFormat = timeFormat,
            dateFormat = dateFormat,
            compact = compact,
            glass = clockStyle == AppPrefs.CLOCK_STYLE_GLASS,
            scoreVisible = scoreVisible,
            insightVisible = insightVisible,
            usageChartVisible = usageChartVisible,
            uiState = uiState,
            onOpenWeeklyReport = onOpenWeeklyReport,
            onOpenScoreDetails = onOpenScoreDetails,
            onLongPress = onLongPress,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalClockCard(
    now: Long,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    compact: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onLongPress),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = if (compact) 52.sp else 76.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp,
            textAlign = TextAlign.Center,
        )
        if (!compact) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulseCard(
    now: Long,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    compact: Boolean,
    glass: Boolean,
    scoreVisible: Boolean,
    insightVisible: Boolean,
    usageChartVisible: Boolean,
    uiState: PulseClockViewModel.PulseClockUiState,
    onOpenWeeklyReport: () -> Unit,
    onOpenScoreDetails: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val cardHeight = if (compact) 124.dp else 168.dp
    val bgAlpha = if (glass) 0.16f else 0.10f
    val borderAlpha = if (glass) 0.28f else 0.16f

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(cardHeight)
            .animateContentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = if (glass) {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f),
                            Color(0xFF80DEEA).copy(alpha = 0.10f),
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = bgAlpha),
                            Color.White.copy(alpha = bgAlpha),
                        )
                    )
                },
                shape = RoundedCornerShape(24.dp),
            )
            .border(0.5.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onOpenWeeklyReport, onLongClick = onLongPress)
            .semantics { contentDescription = context.getString(R.string.pulse_clock_open_weekly_report) }
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = if (compact) 12.sp else 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timeFormat.format(now),
                    color = Color.White,
                    fontSize = if (compact) 54.sp else 76.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-2).sp,
                )
                if (insightVisible) {
                    Spacer(Modifier.height(if (compact) 2.dp else 6.dp))
                    AnimatedContent(
                        targetState = uiState.insightText,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
                        label = "pulse_insight_crossfade",
                    ) { text ->
                        if (!text.isNullOrBlank()) {
                            // D245: sabit 220dp genislik metni yariladan kesiyordu (kisa
                            // cumlelerde bile). Kolonun gercek genisligini kullan, 2 satira
                            // kadar izin ver — ellipsis artik sadece gercekten sigmayinca devreye girer.
                            Text(
                                text = text,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = if (compact) 11.sp else 13.sp,
                                lineHeight = if (compact) 13.sp else 16.sp,
                                maxLines = if (compact) 1 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (!compact) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setClassName(
                                        "com.google.android.googlequicksearchbox",
                                        "com.google.android.googlequicksearchbox.SearchActivity",
                                    )
                                    putExtra("query", "hava durumu")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }.onFailure {
                                    val fallback = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/search?q=hava+durumu"),
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    runCatching { context.startActivity(fallback) }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("🌤 ", fontSize = 12.sp)
                        Text(
                            text = "Hava durumunu aç",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            if (scoreVisible && uiState.score != null) {
                Spacer(Modifier.width(12.dp))
                PulseScoreRing(
                    score = uiState.score,
                    delta = uiState.scoreDelta,
                    weeklyScreenTimeMinutes = uiState.weeklyScreenTimeMinutes,
                    hourlyUsageMinutes = if (usageChartVisible) uiState.hourlyUsageMinutes else null,
                    compact = compact,
                    onClick = onOpenScoreDetails,
                )
            }
        }
    }
}

@Composable
private fun PulseScoreRing(
    score: Int,
    delta: Int?,
    weeklyScreenTimeMinutes: Int?,
    hourlyUsageMinutes: List<Int>?,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val ringSize = if (compact) 42.dp else 52.dp
    val stroke = 5.dp
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "pulse_score_ring_progress",
    )
    val ringColor = pulseScoreColor(score)
    val deltaText = when {
        delta == null -> null
        delta > 0 -> "+$delta"
        delta < 0 -> "$delta"
        else -> null
    }
    val contentDesc = when {
        delta == null -> context.getString(R.string.pulse_score_content_desc_no_baseline, score)
        delta == 0 -> context.getString(R.string.pulse_score_content_desc_flat, score)
        else -> context.getString(
            R.string.pulse_score_content_desc,
            score,
            kotlin.math.abs(delta),
            if (delta > 0) context.getString(R.string.pulse_score_delta_up) else context.getString(R.string.pulse_score_delta_down),
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp) // >= 48dp dokunma hedefi (erişilebilirlik)
                .clickable(onClick = onClick)
                .semantics { contentDescription = contentDesc },
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val strokeWidth = stroke.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "$score",
                color = Color.White,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (deltaText != null) {
            Spacer(Modifier.height(2.dp))
            val arrowColor = if (delta!! > 0) Color(0xFF69F0AE) else Color(0xFFFF8A80)
            Text(
                text = "$deltaText ${if (delta > 0) "↗" else "↘"}",
                color = arrowColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        // Mini açıklama — skorun ne anlama geldiği ilk bakışta belli olsun (D245)
        if (!compact) {
            Spacer(Modifier.height(1.dp))
            val screenTimeText = weeklyScreenTimeMinutes
                ?.takeIf { it > 0 }
                ?.let { minutes ->
                    if (minutes >= 60) {
                        stringResource(R.string.pulse_score_screen_time_hours, minutes / 60, minutes % 60)
                    } else {
                        stringResource(R.string.pulse_score_screen_time_minutes, minutes)
                    }
                }
            Text(
                text = listOfNotNull(stringResource(R.string.pulse_score_ring_caption), screenTimeText).joinToString(" · "),
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 9.sp,
            )
            // 24 saatlik mini kullanım grafiği — Ayarlar'dan kapatılabilir (KEY_HOME_USAGE_CHART_VISIBLE)
            if (hourlyUsageMinutes != null && hourlyUsageMinutes.any { it > 0 }) {
                Spacer(Modifier.height(3.dp))
                HourlyUsageSparkline(
                    minutes = hourlyUsageMinutes,
                    barColor = pulseScoreColor(score),
                )
            }
        }
    }
}

/**
 * 24 kovalı mini çubuk grafik — son 24 saatin ekran süresi, sağdaki çubuk şu anki saat.
 * Skor halkasının genişliğine sığacak kadar küçük; dokunma hedefi yok, salt görsel.
 */
@Composable
private fun HourlyUsageSparkline(
    minutes: List<Int>,
    barColor: Color,
    width: androidx.compose.ui.unit.Dp = 52.dp,
    height: androidx.compose.ui.unit.Dp = 12.dp,
) {
    val context = LocalContext.current
    val maxMinutes = (minutes.maxOrNull() ?: 0).coerceAtLeast(1)
    val chartDesc = context.getString(R.string.pulse_usage_chart_content_desc)
    Canvas(
        modifier = Modifier
            .width(width)
            .height(height)
            .semantics { contentDescription = chartDesc },
    ) {
        val gap = 1.dp.toPx()
        val barWidth = (size.width - gap * (minutes.size - 1)) / minutes.size
        minutes.forEachIndexed { index, value ->
            val barHeight = (size.height * value / maxMinutes).coerceAtLeast(1.5.dp.toPx())
            drawRoundRect(
                color = if (value > 0) barColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.20f),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = index * (barWidth + gap),
                    y = size.height - barHeight,
                ),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
    }
}

/** Skor renkleri sadece bilgilendirici — anlam yalnızca renkle taşınmaz (sayı + ok/metin de var). */
private fun pulseScoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF26C6DA) // primary/teal ailesi
    score >= 60 -> Color(0xFF4DD0E1)
    score >= 40 -> Color(0xFFFFB74D) // amber
    else -> Color(0xFFFF8A80) // coral
}
