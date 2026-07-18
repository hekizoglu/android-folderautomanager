package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryManager

/**
 * Dongu M07 — Ana ekran "Görevler" kartinin canli hali. Eskiden statik "Bugünün görevleri"
 * chip'inin yerini alir (bkz. HomeScreen.kt eski satir 591-642, missions_home_chip_title/
 * missions_home_chip_subtitle). Ayni GlassCard gorsel deseni korunur.
 *
 * [summary] null ise (kaynak Missing/Failed veya henuz hic refresh olmadi) kart "Kullanım
 * erişimi gerekli" gosterir — DATA_UNAVAILABLE'i primaryStatus alaninda da tasiyabildigimiz
 * icin iki sinyal de ayni sekilde ele alinir (asagida [showsUsageAccessRequired]).
 */
@Composable
internal fun HomeMissionCard(
    summary: HomeMissionSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showsUsageAccessRequired = summary == null || summary.primaryStatus == MissionStatus.DATA_UNAVAILABLE
    val allCompleted = summary != null && summary.totalCount > 0 && summary.completedCount == summary.totalCount

    // Döngü U02 — kart her göründüğünde bir kez "viewed" (isim/içerik taşımaz, sadece
    // durum+ilerleme bucket'ı). recomposition'da tekrar tetiklenmemesi için summary'nin
    // durum+ilerleme çiftine anahtarlanır (aynı özet için yeniden loglanmaz).
    val wireStatus = summary.toWireStatus()
    val wireProgress = progressBucketOf(summary?.primaryProgressFraction)
    LaunchedEffect(wireStatus, wireProgress) {
        TelemetryManager.log(TelemetryEvent.HomeMissionCardViewed(TelemetryEvent.HomeMissionType.NONE, wireStatus))
        TelemetryManager.log(TelemetryEvent.MissionProgressViewed(TelemetryEvent.HomeMissionType.NONE, wireProgress))
    }

    GlassCard(
        modifier = modifier.clickable(onClick = {
            TelemetryManager.log(TelemetryEvent.HomeMissionCardOpened(TelemetryEvent.HomeMissionType.NONE, wireStatus))
            onClick()
        }),
        cornerRadius = 18.dp,
        backgroundAlpha = 0.10f,
        borderAlpha = 0.18f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.missions_home_chip_title),
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (summary != null && summary.totalCount > 0) {
                    Text(
                        text = "${summary.completedCount}/${summary.totalCount}",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            val subtitle: String
            val subtitleSecondary: String?
            when {
                showsUsageAccessRequired -> {
                    subtitle = stringResource(R.string.missions_home_card_usage_access_required)
                    subtitleSecondary = null
                }
                allCompleted -> {
                    subtitle = stringResource(R.string.missions_home_card_all_completed)
                    subtitleSecondary = null
                }
                else -> {
                    subtitle = summary?.primaryCurrentText
                        ?: summary?.primaryTitle
                        ?: stringResource(R.string.missions_home_chip_subtitle)
                    subtitleSecondary = summary?.primaryRemainingText
                }
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 11.sp,
                maxLines = 1,
            )
            if (subtitleSecondary != null) {
                Text(
                    text = subtitleSecondary,
                    color = Color.White.copy(alpha = 0.52f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** [HomeMissionSummary.primaryStatus] -> kapalı telemetri enum'u (Döngü U02). */
private fun HomeMissionSummary?.toWireStatus(): TelemetryEvent.HomeMissionStatus = when (this?.primaryStatus) {
    MissionStatus.DATA_UNAVAILABLE, null -> TelemetryEvent.HomeMissionStatus.DATA_UNAVAILABLE
    MissionStatus.NOT_STARTED -> TelemetryEvent.HomeMissionStatus.NOT_STARTED
    MissionStatus.IN_PROGRESS -> TelemetryEvent.HomeMissionStatus.IN_PROGRESS
    MissionStatus.SAFE -> TelemetryEvent.HomeMissionStatus.SAFE
    MissionStatus.AT_RISK -> TelemetryEvent.HomeMissionStatus.AT_RISK
    MissionStatus.AWAITING_SETTLEMENT -> TelemetryEvent.HomeMissionStatus.AWAITING_SETTLEMENT
    MissionStatus.COMPLETED -> TelemetryEvent.HomeMissionStatus.COMPLETED
    MissionStatus.FAILED -> TelemetryEvent.HomeMissionStatus.FAILED
}

/** Ham ilerleme oranı (0f-1f) -> kapalı bucket (Döngü U02) — sayı değil bant gönderilir. */
private fun progressBucketOf(fraction: Float?): TelemetryEvent.ProgressBucket = when {
    fraction == null -> TelemetryEvent.ProgressBucket.UNKNOWN
    fraction <= 0f -> TelemetryEvent.ProgressBucket.ZERO
    fraction < 0.34f -> TelemetryEvent.ProgressBucket.LOW
    fraction < 0.67f -> TelemetryEvent.ProgressBucket.MEDIUM
    fraction < 1f -> TelemetryEvent.ProgressBucket.HIGH
    else -> TelemetryEvent.ProgressBucket.COMPLETE
}
