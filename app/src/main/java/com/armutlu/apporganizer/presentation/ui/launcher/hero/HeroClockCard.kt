package com.armutlu.apporganizer.presentation.ui.launcher.hero

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HeroClockCard(
    spec: HomeHeroLayoutSpec,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val timeFormat = remember(locale, DateFormat.is24HourFormat(context)) {
        DateFormat.getTimeFormat(context)
    }
    val dateFormat = remember(locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEE d MMMM"), locale)
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val millisToNextMinute = 60_000L - (now % 60_000L)
            delay(millisToNextMinute.coerceAtLeast(1_000L))
        }
    }

    PremiumGlassSurface(
        modifier = modifier
            .size(spec.clockWidthDp.dp, spec.clockHeightDp.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = context.getString(R.string.pulse_clock_open_weekly_report) },
        cornerRadius = HomeHeroTokens.ClockCorner,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = timeFormat.format(now),
                color = accentColor,
                fontSize = spec.clockTextSizeSp.sp,
                fontWeight = FontWeight.Thin,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
            )
            Text(
                text = dateFormat.format(now).replaceFirstChar { it.titlecase(locale) },
                color = Color.White.copy(alpha = .72f),
                fontSize = HomeHeroTokens.DateTextSize,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}
