package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.layout.heightIn
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
import com.armutlu.apporganizer.utils.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Pulse Clock — ana ekranın imza saat bileşeni (D244). Üç stil (Minimal/Pulse/Glass) destekler,
 * ayarlar AppPrefs.KEY_CLOCK_STYLE / KEY_HOME_INSIGHT_VISIBLE'dan okunur.
 *
 * D03: Skor artık burada gösterilmez — birincil ve tek gösterim DigitalLifeCard'dır
 * (KEY_DIGITAL_LIFE_CARD_VISIBLE, HomeScreen). PulseClockWidget saat/tarih/hava ve isteğe
 * bağlı kısa içgörü (KEY_HOME_INSIGHT_VISIBLE) gösterir.
 *
 * Performans: saat metni dakika sınırında güncellenir (her saniye DEĞİL); içgörü
 * PulseClockViewModel'den TEK motor (DigitalPulseEngine) üzerinden gelir ve 15dk cache'lidir —
 * saat tik'i asla skor hesabı tetiklemez.
 *
 * @param compact Dar ekran / kalabalık grid'de küçülür — klasörler kaybolmasın diye.
 * @param onLongPress Uzun basma → mevcut launcher yönetim ekranı davranışı (BOZULMAZ) —
 *                     tek gesture detector (combinedClickable) ile iç içe çakışma önlenir.
 * @param masterGoldAccent Görev S2 — Usta (100⭐) ödülü: açıkken saat metni altın tonlu renge
 *                          döner (abartısız). Kilit kontrolü çağıran tarafta (SmartDashboardPage,
 *                          [com.armutlu.apporganizer.domain.usecase.missions.MasterRewardPolicy])
 *                          yapılır — bu composable sadece boolean'ı görsele çevirir.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PulseClockWidget(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onOpenWeeklyReport: () -> Unit = {},
    onOpenScoreDetails: () -> Unit = {},
    onLongPress: () -> Unit = {},
    masterGoldAccent: Boolean = false,
) {
    val context = LocalContext.current
    val viewModel: PulseClockViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshIfStale() }

    var clockStyle by remember { mutableStateOf(AppPrefs.getClockStyle(context)) }
    var insightVisible by remember { mutableStateOf(AppPrefs.isHomeInsightVisible(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPrefs.KEY_CLOCK_STYLE -> clockStyle = AppPrefs.getClockStyle(context)
                AppPrefs.KEY_HOME_INSIGHT_VISIBLE -> insightVisible = AppPrefs.isHomeInsightVisible(context)
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
        AppPrefs.CLOCK_STYLE_MINIMAL -> MinimalClockCard(now, timeFormat, dateFormat, compact, onLongPress, modifier, masterGoldAccent)
        else -> PulseCard(
            now = now,
            timeFormat = timeFormat,
            dateFormat = dateFormat,
            compact = compact,
            glass = clockStyle == AppPrefs.CLOCK_STYLE_GLASS,
            insightVisible = insightVisible,
            uiState = uiState,
            onOpenWeeklyReport = onOpenWeeklyReport,
            onOpenScoreDetails = onOpenScoreDetails,
            onLongPress = onLongPress,
            modifier = modifier,
            masterGoldAccent = masterGoldAccent,
        )
    }
}

// Görev S2 — Usta ödülü altın rengi: abartısız, sıcak altın ton. Tek yerde tanımlı, hem
// Minimal hem Pulse/Glass stilde aynı renk kullanılır.
private val MASTER_GOLD_ACCENT = Color(0xFFFFD54F)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalClockCard(
    now: Long,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    compact: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier,
    masterGoldAccent: Boolean = false,
) {
    Column(
        modifier = modifier.combinedClickable(onClick = {}, onLongClick = onLongPress),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = timeFormat.format(now),
            color = if (masterGoldAccent) MASTER_GOLD_ACCENT else Color.White,
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
    insightVisible: Boolean,
    uiState: PulseClockViewModel.PulseClockUiState,
    onOpenWeeklyReport: () -> Unit,
    onOpenScoreDetails: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier,
    masterGoldAccent: Boolean = false,
) {
    val context = LocalContext.current
    // D03: PulseScoreRing kaldırıldı (skor artık tek yerde — DigitalLifeCard). Ring alanı
    // boşaldığı için kart yüksekliği sıkılaştırıldı, klasör gridine alan kazandırıldı.
    val cardHeight = if (compact) 96.dp else 128.dp
    val bgAlpha = if (glass) 0.16f else 0.10f
    val borderAlpha = if (glass) 0.28f else 0.16f

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            // İçgörü ve hava durumu dar ekranlarda iki satıra çıktığında saat kartı
            // sabit yüksekliğe sığmaya çalışıp alt içeriği kırpmamalı.
            .heightIn(min = cardHeight)
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
                    color = if (masterGoldAccent) MASTER_GOLD_ACCENT else Color.White,
                    fontSize = if (compact) 46.sp else 64.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    softWrap = false,
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
                    WeatherSummary(
                        weather = uiState.weather,
                        weatherError = uiState.weatherError,
                    )
                }
            }

            // D03/U01: Skor halkası (PulseScoreRing) ana ekran düzeninden kaldırıldı — skorun
            // birincil ve tek gösterimi artık DigitalLifeCard'dır (HomeScreen). PulseClockWidget
            // artık yalnız saat/tarih/hava/kısa içgörü gösterir; ham skor burada tekrarlanmaz.
            // scoreVisible/usageChartVisible/missionsEnabled parametreleri Döngü U01'de silindi
            // (kullanılmayan parametre temizliği — PulseScoreRing zaten kaldırılmıştı).
        }
    }
}

@Composable
private fun WeatherSummary(
    weather: WeatherRepository.Snapshot?,
    weatherError: String?,
) {
    val context = LocalContext.current
    if (weather == null && weatherError.isNullOrBlank()) {
        val manualCity = AppPrefs.getHomeWeatherManualCity(context)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("🌤", fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (manualCity.isBlank()) stringResource(R.string.weather_setup_needed)
                else stringResource(R.string.weather_loading),
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (weather != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${weather.conditionEmoji} ${weather.locationLabel}",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            R.string.weather_current_line,
                            weather.currentTempC,
                            weather.minTempC,
                            weather.maxTempC,
                        ),
                        color = Color.White.copy(alpha = 0.76f),
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.weather_updated_at, weather.fetchedAtLabel),
                    color = if (weather.isStale) Color(0xFFFFE082) else Color.White.copy(alpha = 0.56f),
                    fontSize = 10.sp,
                )
            }
            if (weather.hourly.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    weather.hourly.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.hourLabel, color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp)
                            Text("${item.tempC}°", color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp)
                        }
                    }
                }
            }
        } else if (!weatherError.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.weather_fetch_failed),
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
            )
        }
    }
}

// D03: PulseScoreRing/HourlyUsageSparkline/pulseScoreColor kaldırıldı (Döngü U01) — skorun
// birincil gösterimi DigitalLifeCard'a taşındığından beri (bkz. ANA_EKRAN_AKILLI_NABIZ_GOREVLER_
// DIJITAL_YASAM_ROADMAP.md Döngü D03) hiçbir çağrı kalmamıştı; grep ile doğrulandı.
