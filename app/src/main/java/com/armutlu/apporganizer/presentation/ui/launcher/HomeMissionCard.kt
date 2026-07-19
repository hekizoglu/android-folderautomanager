package com.armutlu.apporganizer.presentation.ui.launcher

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryManager
import com.armutlu.apporganizer.utils.AppPrefs
import java.time.LocalDate

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

    // Döngü G5 — "3/3 günü" parıltısı: gün başına BİR KEZ (AppPrefs epochDay bayrağı) hafif bir
    // arka plan parıltısı + "Bugünü topladın ⭐⭐⭐" alt yazısı gösterilir. Kutlama ayarı kapalıysa
    // (KEY_MISSION_CELEBRATIONS) hem parıltı hem alt yazı DEVREDIŞI — normal "tamamlandı" metni
    // kalır (missions_home_card_all_completed, aşağıda subtitle bloğunda).
    val context = LocalContext.current
    val celebrationsEnabled = remember { AppPrefs.isMissionCelebrationsEnabled(context) }
    val glow = remember { Animatable(0f) }
    var showAllCompletedMessage by remember { mutableStateOf(false) }
    LaunchedEffect(allCompleted) {
        if (!allCompleted || !celebrationsEnabled) return@LaunchedEffect
        val todayEpochDay = LocalDate.now().toEpochDay()
        if (AppPrefs.wasAllCompletedCelebrated(context, todayEpochDay)) {
            showAllCompletedMessage = true
            return@LaunchedEffect
        }
        AppPrefs.markAllCompletedCelebrated(context, todayEpochDay)
        showAllCompletedMessage = true
        if (ValueAnimator.areAnimatorsEnabled()) {
            glow.animateTo(0.22f, tween(260))
            glow.animateTo(0f, tween(900))
        } else {
            // Reduced-motion: animasyon yok, kisa sabit vurgu (kart pasif görüntülenir, haptic
            // burada gereksiz — dokunma sonucu tetiklenmiyor).
            glow.snapTo(0.18f)
        }
    }

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
                .background(
                    color = Color.White.copy(alpha = glow.value),
                    shape = RoundedCornerShape(18.dp),
                )
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
                if (summary != null && summary.currentStreak >= 2) {
                    val streakDescription = stringResource(
                        R.string.home_mission_streak_content_description,
                        summary.currentStreak,
                    )
                    Text(
                        text = stringResource(R.string.home_mission_streak_badge, summary.currentStreak),
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .semantics { contentDescription = streakDescription },
                    )
                }
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
                allCompleted && showAllCompletedMessage -> {
                    // Dongu G5 — gunun ILK "3/3 tamamlandi" goruntulenmesinde ozel kutlama metni.
                    subtitle = stringResource(R.string.missions_all_completed_today)
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
