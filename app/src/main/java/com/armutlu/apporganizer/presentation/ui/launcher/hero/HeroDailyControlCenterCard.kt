package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryManager

private val DigitalBlue = Color(0xFF29B6F6)
private val MissionGreen = Color(0xFF54E67C)
private val NotificationOrange = Color(0xFFFF8A3D)

@Composable
internal fun HeroDailyControlCenterCard(
    pulse: HomePulseSummary?,
    missionSummary: HomeMissionSummary?,
    notificationCount24h: Int,
    notificationAccessGranted: Boolean,
    spec: HomeHeroLayoutSpec,
    onOpenPulse: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenNotificationHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val missionStatus = missionSummary.toWireStatus()
    val missionProgress = missionSummary?.primaryProgressFraction.toProgressBucket()
    LaunchedEffect(missionStatus, missionProgress) {
        TelemetryManager.log(TelemetryEvent.HomeMissionCardViewed(TelemetryEvent.HomeMissionType.NONE, missionStatus))
        TelemetryManager.log(TelemetryEvent.MissionProgressViewed(TelemetryEvent.HomeMissionType.NONE, missionProgress))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.hero_daily_control_center_title),
            color = Color.White.copy(alpha = .76f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        PremiumGlassSurface(
            modifier = Modifier
                .testTag("hero_daily_control_center")
                .fillMaxWidth()
                .height(spec.digitalLifeHeightDp.dp),
        ) {
            Row(Modifier.fillMaxSize()) {
                DailyControlSegment(
                    modifier = Modifier.testTag("hero_daily_control_digital_life"),
                    title = stringResource(R.string.hero_daily_control_digital_life),
                    value = pulsePrimaryValue(pulse),
                    detail = heroPulseDetail(pulse),
                    icon = { Icon(Icons.Default.PhoneAndroid, null, tint = DigitalBlue, modifier = Modifier.size(20.dp)) },
                    accent = DigitalBlue,
                    progress = pulse?.score?.takeUnless { pulse.shouldHideScore }?.div(100f),
                    onClick = onOpenPulse,
                    enabled = pulse?.isActionable == true,
                    contentDescription = stringResource(R.string.hero_daily_control_digital_description, pulseAccessibleValue(pulse)),
                )
                SegmentDivider()
                DailyControlSegment(
                    modifier = Modifier.testTag("hero_daily_control_missions"),
                    title = stringResource(R.string.hero_daily_control_missions),
                    value = missionPrimaryValue(missionSummary),
                    detail = missionDetail(missionSummary),
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = MissionGreen, modifier = Modifier.size(20.dp)) },
                    accent = MissionGreen,
                    progress = missionSummary?.takeIf { it.totalCount > 0 }?.let {
                        (it.completedCount.toFloat() / it.totalCount).coerceIn(0f, 1f)
                    },
                    onClick = {
                        TelemetryManager.log(TelemetryEvent.HomeMissionCardOpened(TelemetryEvent.HomeMissionType.NONE, missionStatus))
                        onOpenMissions()
                    },
                    contentDescription = stringResource(
                        R.string.hero_daily_control_missions_description,
                        missionAccessibleValue(missionSummary),
                    ),
                )
                SegmentDivider()
                DailyControlSegment(
                    modifier = Modifier.testTag("hero_daily_control_notifications"),
                    title = stringResource(R.string.hero_daily_control_notifications),
                    value = notificationPrimaryValue(notificationCount24h, notificationAccessGranted),
                    detail = if (notificationAccessGranted) {
                        stringResource(R.string.hero_daily_control_last_24_hours_with_history_limit)
                    } else {
                        stringResource(R.string.hero_daily_control_access_required)
                    },
                    icon = { Icon(Icons.Default.Notifications, null, tint = NotificationOrange, modifier = Modifier.size(20.dp)) },
                    accent = NotificationOrange,
                    progress = null,
                    decorativeAccent = true,
                    onClick = onOpenNotificationHistory,
                    contentDescription = if (notificationAccessGranted) {
                        stringResource(
                            R.string.hero_daily_control_notifications_description,
                            notificationCount24h.coerceAtLeast(0),
                        )
                    } else {
                        stringResource(R.string.hero_daily_control_notifications_access_description)
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.DailyControlSegment(
    title: String,
    value: String,
    detail: String,
    icon: @Composable () -> Unit,
    accent: Color,
    progress: Float?,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    decorativeAccent: Boolean = false,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(accent.copy(alpha = .13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = detail,
            color = Color.White.copy(alpha = .58f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 7.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = .14f), RoundedCornerShape(4.dp)),
        ) {
            if (progress != null || decorativeAccent) {
                Box(
                    Modifier
                        .fillMaxWidth(if (decorativeAccent) .62f else progress!!.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(accent, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun SegmentDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(Color.White.copy(alpha = .12f)),
    )
}

@Composable
private fun pulsePrimaryValue(pulse: HomePulseSummary?): String = when {
    pulse == null -> stringResource(R.string.hero_daily_control_unavailable)
    pulse.shouldHideScore -> stringResource(R.string.digital_life_card_low_confidence)
    pulse.score != null -> stringResource(R.string.hero_daily_control_score, pulse.score)
    else -> stringResource(R.string.hero_daily_control_unavailable)
}

@Composable
private fun heroPulseDetail(pulse: HomePulseSummary?): String = when {
    pulse == null -> stringResource(R.string.hero_digital_life_permission_detail)
    pulse.freshness == com.armutlu.apporganizer.domain.common.DataFreshness.STALE && pulse.staleMinutes != null ->
        stringResource(R.string.digital_life_card_stale_minutes, pulse.staleMinutes.toInt())
    pulse.delta == null -> stringResource(R.string.digital_life_card_delta_first_week)
    pulse.delta > 0 -> stringResource(R.string.digital_life_card_delta_up, pulse.delta)
    pulse.delta < 0 -> stringResource(R.string.digital_life_card_delta_down, pulse.delta)
    else -> stringResource(R.string.digital_life_card_delta_flat)
}

@Composable
private fun missionPrimaryValue(summary: HomeMissionSummary?): String = when {
    summary == null || summary.totalCount <= 0 -> stringResource(R.string.hero_daily_control_unavailable)
    summary.completedCount == summary.totalCount -> stringResource(R.string.hero_daily_control_missions_complete, summary.totalCount)
    else -> stringResource(R.string.hero_daily_control_missions_progress, summary.completedCount, summary.totalCount)
}

@Composable
private fun missionDetail(summary: HomeMissionSummary?): String = when {
    summary == null || summary.primaryStatus == MissionStatus.DATA_UNAVAILABLE -> stringResource(
        R.string.hero_daily_control_access_required,
    )
    summary.totalCount > 0 && summary.completedCount == summary.totalCount -> stringResource(R.string.missions_home_card_all_completed)
    else -> summary?.primaryCurrentText ?: summary?.primaryTitle ?: stringResource(R.string.missions_home_chip_subtitle)
}

@Composable
private fun notificationPrimaryValue(count: Int, accessGranted: Boolean): String = if (!accessGranted) {
    stringResource(R.string.hero_daily_control_access_required)
} else if (count > 0) {
    stringResource(R.string.hero_daily_control_notifications_count, count)
} else {
    stringResource(R.string.hero_daily_control_notifications_empty)
}

@Composable
private fun pulseAccessibleValue(pulse: HomePulseSummary?): String = pulsePrimaryValue(pulse)

@Composable
private fun missionAccessibleValue(summary: HomeMissionSummary?): String = missionPrimaryValue(summary)

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

private fun Float?.toProgressBucket(): TelemetryEvent.ProgressBucket = when {
    this == null -> TelemetryEvent.ProgressBucket.UNKNOWN
    this <= 0f -> TelemetryEvent.ProgressBucket.ZERO
    this < .34f -> TelemetryEvent.ProgressBucket.LOW
    this < .67f -> TelemetryEvent.ProgressBucket.MEDIUM
    this < 1f -> TelemetryEvent.ProgressBucket.HIGH
    else -> TelemetryEvent.ProgressBucket.COMPLETE
}
