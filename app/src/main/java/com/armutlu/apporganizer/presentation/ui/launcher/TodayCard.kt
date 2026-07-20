package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.TodayCardKind
import com.armutlu.apporganizer.domain.home.TodayCardSpec

/**
 * Görev S1 — Dashboard'daki tek bağlamsal "BUGÜN" kartı. [TodayCardSelector] tarafından seçilen
 * [TodayCardSpec]'i, mevcut zeka kartlarıyla (HomeMissionCard/DigitalLifeCard) aynı GlassCard
 * görsel dilinde çizer. Tıklanınca [TodayCardSpec.kind]'a göre ilgili DashboardActions callback'i
 * çağrılır (görev → onMissionClick, dijital yaşam/denge/izin → onPulseClick, klasör incelemesi →
 * onOpenFolderStats, rapor hazır → onOpenWeeklyReport).
 *
 * [spec] null ise kart hiç çizilmez (SmartDashboardPage çağıran taraf zaten bunu kontrol eder).
 */
@Composable
internal fun TodayCard(
    spec: TodayCardSpec,
    onMissionClick: () -> Unit,
    onPulseClick: () -> Unit,
    onFolderReviewClick: () -> Unit,
    onReportReadyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick = when (spec.kind) {
        TodayCardKind.CRITICAL_PERMISSION -> onPulseClick
        TodayCardKind.RISKY_MISSION -> onMissionClick
        TodayCardKind.FOLDER_REVIEW -> onFolderReviewClick
        TodayCardKind.REPORT_READY -> onReportReadyClick
        TodayCardKind.DAILY_MISSIONS -> onMissionClick
        TodayCardKind.BALANCE_SUMMARY -> onPulseClick
    }

    val title = stringResource(spec.titleRes)
    val subtitle = if (spec.kind == TodayCardKind.DAILY_MISSIONS) {
        stringResource(
            spec.subtitleRes,
            spec.missionCompletedCount ?: 0,
            spec.missionTotalCount ?: 0,
            spec.missionTotalStars ?: 0,
        )
    } else {
        stringResource(spec.subtitleRes)
    }

    GlassCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $subtitle"
                role = Role.Button
            },
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
                    text = title,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (spec.kind == TodayCardKind.BALANCE_SUMMARY && spec.pulseScore != null) {
                    Text(
                        text = "${spec.pulseScore}",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.52f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (spec.kind == TodayCardKind.RISKY_MISSION) {
                val missionLine = spec.missionCurrentText ?: spec.missionTitle
                if (missionLine != null) {
                    Text(
                        text = missionLine,
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (spec.missionRemainingText != null) {
                    Text(
                        text = spec.missionRemainingText,
                        color = Color.White.copy(alpha = 0.52f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
