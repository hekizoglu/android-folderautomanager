package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.common.DataFreshness
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseAction
import com.armutlu.apporganizer.domain.home.PulseReasonPresenter
import com.armutlu.apporganizer.domain.home.PulseStatusBand
import com.armutlu.apporganizer.domain.usecase.pulse.DataConfidence

/**
 * Döngü D02 — "Dijital Yaşam" bilgi kartı, eski [DigitalScoreCard]'ın yerini alır
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1360-1450).
 *
 * Eskiden yalnızca "Skor NN / Dijital yaşam" gösteriyordu; artık durum + trend + en büyük
 * etki nedenini tek bakışta anlatır:
 * ```
 * Dijital Yaşam                                 72
 * İyi · Geçen haftaya göre +4
 * En büyük etki: Bildirim yoğunluğu
 * ```
 * [summary] null ise kart hiç gösterilmez (eski davranışla aynı — yeterli veri yok).
 */
@Composable
internal fun DigitalLifeCard(
    summary: HomePulseSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReasonAction: (PulseAction) -> Unit = {},
) {
    if (summary == null) return
    GlassCard(
        modifier = modifier.clickable(enabled = summary.isActionable, onClick = onClick),
        cornerRadius = 18.dp,
        backgroundAlpha = 0.10f,
        borderAlpha = 0.18f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Satır 1: başlık solda, skor sağda (freshness=UNAVAILABLE'da skor yerine CTA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.digital_life_card_title),
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                DigitalLifeScoreTrailing(summary)
            }

            when (summary.freshness) {
                DataFreshness.UNAVAILABLE -> {
                    Text(
                        text = stringResource(R.string.digital_life_card_unavailable_cta),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
                else -> {
                    // Satır 2: durum · trend (veya "Veri birikiyor" LOW confidence'ta)
                    Text(
                        text = statusLine(summary),
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Satır 3: en büyük etki (varsa) — Döngü D04: PulseReasonPresenter'dan
                    // gelen action varsa satır ayrıca tıklanabilir, doğru çözüm ekranına gider.
                    summary.topReason?.let { reason ->
                        val presented = PulseReasonPresenter.present(reason)
                        val hasAction = presented.action != PulseAction.None
                        Text(
                            text = stringResource(
                                R.string.digital_life_card_top_reason_prefix,
                                stringResource(presented.label.resId, *presented.label.args.toTypedArray()),
                            ),
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (hasAction) {
                                Modifier.clickable { onReasonAction(presented.action) }
                            } else {
                                Modifier
                            },
                        )
                    }
                    // Satır 4: bayat veri uyarısı (STALE)
                    if (summary.freshness == DataFreshness.STALE && summary.staleMinutes != null) {
                        Text(
                            text = stringResource(
                                R.string.digital_life_card_stale_minutes,
                                summary.staleMinutes.toInt(),
                            ),
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 9.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalLifeScoreTrailing(summary: HomePulseSummary) {
    when {
        summary.freshness == DataFreshness.UNAVAILABLE -> {
            Text("›", color = Color.White.copy(alpha = 0.45f), fontSize = 18.sp)
        }
        summary.shouldHideScore -> {
            Text(
                text = stringResource(R.string.digital_life_card_low_confidence),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        else -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (summary.confidence == DataConfidence.MEDIUM) {
                    Text(
                        text = stringResource(R.string.digital_life_card_medium_confidence_badge),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "${summary.score}",
                    color = digitalLifeScoreColor(summary.score ?: 0),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun statusLine(summary: HomePulseSummary): String {
    if (summary.shouldHideScore) {
        return stringResource(R.string.digital_life_card_low_confidence)
    }
    val statusText = summary.statusBand?.let { stringResource(it.labelRes()) }.orEmpty()
    val deltaText = deltaLabel(summary.delta)
    return if (statusText.isEmpty()) deltaText else "$statusText · $deltaText"
}

@Composable
private fun deltaLabel(delta: Int?): String = when {
    delta == null -> stringResource(R.string.digital_life_card_delta_first_week)
    delta > 0 -> stringResource(R.string.digital_life_card_delta_up, delta)
    delta < 0 -> stringResource(R.string.digital_life_card_delta_down, delta)
    else -> stringResource(R.string.digital_life_card_delta_flat)
}

private fun PulseStatusBand.labelRes(): Int = when (this) {
    PulseStatusBand.EXCELLENT -> R.string.digital_life_card_status_excellent
    PulseStatusBand.GOOD -> R.string.digital_life_card_status_good
    PulseStatusBand.BALANCED -> R.string.digital_life_card_status_balanced
    PulseStatusBand.NEEDS_FOCUS -> R.string.digital_life_card_status_needs_focus
    PulseStatusBand.IMPROVING -> R.string.digital_life_card_status_improving
}

