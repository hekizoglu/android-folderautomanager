package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseStatusBand

@Composable
internal fun HeroDigitalLifeCard(
    summary: HomePulseSummary?,
    spec: HomeHeroLayoutSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(
        modifier = modifier
            .testTag("hero_digital_life_card")
            .fillMaxWidth()
            .height(spec.digitalLifeHeightDp.dp)
            .clickable(enabled = summary?.isActionable == true, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(8.dp)) { drawCircle(Color(0xFF54E67C)) }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.digital_life_card_title),
                        color = Color.White.copy(alpha = .92f),
                        fontSize = HomeHeroTokens.CardTitleTextSize,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = heroPulseMessage(summary),
                    color = Color.White,
                    fontSize = HomeHeroTokens.DigitalLifeMessageTextSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = heroPulseDetail(summary),
                    color = Color.White.copy(alpha = .62f),
                    fontSize = HomeHeroTokens.BodyTextSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HeroScoreRing(summary)
        }
    }
}

@Composable
private fun HeroScoreRing(summary: HomePulseSummary?) {
    val visibleScore = summary?.score?.takeUnless { summary.shouldHideScore }
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = .12f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (visibleScore != null) {
                drawArc(
                    color = heroScoreColor(visibleScore),
                    startAngle = -90f,
                    sweepAngle = 360f * (visibleScore.coerceIn(0, 100) / 100f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = visibleScore?.toString() ?: "—",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun heroPulseMessage(summary: HomePulseSummary?): String = when {
    summary == null || summary.freshness == DataFreshness.UNAVAILABLE ->
        stringResource(R.string.digital_life_card_unavailable_cta)
    summary.shouldHideScore -> stringResource(R.string.digital_life_card_low_confidence)
    else -> stringResource(summary.statusBand.heroLabelRes())
}

@Composable
private fun heroPulseDetail(summary: HomePulseSummary?): String = when {
    summary == null || summary.freshness == DataFreshness.UNAVAILABLE ->
        stringResource(R.string.hero_digital_life_permission_detail)
    summary.freshness == DataFreshness.STALE && summary.staleMinutes != null ->
        stringResource(R.string.digital_life_card_stale_minutes, summary.staleMinutes.toInt())
    summary.delta == null -> stringResource(R.string.digital_life_card_delta_first_week)
    summary.delta > 0 -> stringResource(R.string.digital_life_card_delta_up, summary.delta)
    summary.delta < 0 -> stringResource(R.string.digital_life_card_delta_down, summary.delta)
    else -> stringResource(R.string.digital_life_card_delta_flat)
}

private fun PulseStatusBand?.heroLabelRes(): Int = when (this) {
    PulseStatusBand.EXCELLENT -> R.string.digital_life_card_status_excellent
    PulseStatusBand.GOOD -> R.string.digital_life_card_status_good
    PulseStatusBand.BALANCED -> R.string.digital_life_card_status_balanced
    PulseStatusBand.NEEDS_FOCUS -> R.string.digital_life_card_status_needs_focus
    PulseStatusBand.IMPROVING, null -> R.string.digital_life_card_status_improving
}

private fun heroScoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF54E67C)
    score >= 60 -> Color(0xFF9CEA64)
    score >= 40 -> Color(0xFFFFD166)
    else -> Color(0xFFFF6B6B)
}
