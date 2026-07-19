package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.usecase.missions.MissionStatus
import com.armutlu.apporganizer.presentation.viewmodel.MissionsViewModel

/**
 * Gorev satiri (Dongu M06 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md
 * satir 1051-1114). MissionsScreen.kt'nin 300 satir kuralini asmamasi icin ayri dosyaya
 * cikarilmistir.
 *
 * Duzen: [durum ikonu] baslik + yildiz / "Su an" / "Kalan" / ilerleme cubugu / deadline+eylem.
 * AVOID_AFTER_TIME gibi sayisal metin uretmeyen gorevlerde currentText/remainingText/
 * progressFraction null gelir — bu satirlar gosterilmez, sadece durum rozeti kalir (M03 notu).
 *
 * Erisilebilirlik: durum renkle degil ikon+metin ile anlatilir; LinearProgressIndicator
 * progressBarRangeInfo semantics tasir; hicbir Text maxLines ile kirpilmaz (dogal wrap).
 *
 * Dongu G2: satirin TAMAMI tiklanabilir (Card degil, Row.clickable) — eylem gerektiren
 * gorevlerde (actionLabel != null, tamamlanmamis) tum satir en anlamli ekrana goturur.
 * Eski TextButton yerini kucuk bir chevron ikonu alir (M06 gorsel dilini sadelestirir,
 * cift dokunma alani olusturmaz). Satir role=Button + P19 pattern'i ile
 * ".. Acmak icin dokun." son eki tasir.
 */
@Composable
internal fun MissionRow(
    mission: MissionsViewModel.MissionUi,
    onActionClick: () -> Unit,
) {
    val statusMeta = mission.status.toStatusMeta()
    val isActionable = !mission.completed && mission.actionLabel != null
    val haptic = LocalHapticFeedback.current
    val rowModifier = if (isActionable) {
        val tapHint = stringResource(R.string.mission_row_tap_hint, mission.title)
        Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onActionClick()
            }
            .semantics {
                role = Role.Button
                contentDescription = tapHint
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = statusMeta.icon,
            contentDescription = stringResource(statusMeta.labelRes),
            tint = statusMeta.tint(),
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (mission.completed) FontWeight.Normal else FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "⭐ ${mission.starReward}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Durum yalniz ikonla degil, her zaman kisa metinle de anlatilir (erisilebilirlik).
            Text(
                text = stringResource(statusMeta.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = statusMeta.tint(),
                modifier = Modifier.padding(top = 2.dp),
            )

            // Dongu M03: AVOID_AFTER_TIME gibi gorevlerde bu alanlar null gelir — sayisal
            // metin YOK, sadece yukaridaki durum rozeti gosterilir.
            mission.currentText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            mission.remainingText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            mission.progressFraction?.let { fraction ->
                val clamped = fraction.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { clamped },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(6.dp)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
                            mission.progressText?.let { contentDescription = it }
                        },
                    color = statusMeta.tint(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            if (mission.deadlineText != null || (!mission.completed && mission.actionLabel != null)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = mission.deadlineText.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // Dongu G2: satirin tamami zaten tiklanabilir (onActionClick) — buton
                    // yerine sade bir yon oku ipucu birakilir, cift dokunma alani olusmaz.
                    // Ikon kendi basina odaklanabilir/tiklanabilir DEGIL (clearAndSetSemantics
                    // yerine sadece gorsel ipucu; ekran okuyucu ust satirin role=Button + tap
                    // hint semantics'ini kullanir).
                    if (isActionable) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .sizeIn(minWidth = 24.dp, minHeight = 24.dp)
                                .padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class MissionStatusMeta(
    val icon: ImageVector,
    val labelRes: Int,
    val tint: @Composable () -> Color,
)

private fun MissionStatus.toStatusMeta(): MissionStatusMeta = when (this) {
    MissionStatus.SAFE -> MissionStatusMeta(
        icon = Icons.Default.CheckCircle,
        labelRes = R.string.mission_status_safe,
        tint = { MaterialTheme.colorScheme.primary },
    )
    MissionStatus.AT_RISK -> MissionStatusMeta(
        icon = Icons.Default.Warning,
        labelRes = R.string.mission_status_at_risk,
        tint = { MaterialTheme.colorScheme.error },
    )
    MissionStatus.AWAITING_SETTLEMENT -> MissionStatusMeta(
        icon = Icons.Default.HourglassEmpty,
        labelRes = R.string.mission_status_awaiting_settlement,
        tint = { MaterialTheme.colorScheme.tertiary },
    )
    MissionStatus.FAILED -> MissionStatusMeta(
        icon = Icons.Default.ErrorOutline,
        labelRes = R.string.mission_status_failed,
        tint = { MaterialTheme.colorScheme.error },
    )
    MissionStatus.DATA_UNAVAILABLE -> MissionStatusMeta(
        icon = Icons.Default.WifiOff,
        labelRes = R.string.mission_status_data_unavailable,
        tint = { MaterialTheme.colorScheme.onSurfaceVariant },
    )
    MissionStatus.IN_PROGRESS -> MissionStatusMeta(
        icon = Icons.Default.PlayCircleOutline,
        labelRes = R.string.mission_status_in_progress,
        tint = { MaterialTheme.colorScheme.onSurfaceVariant },
    )
    MissionStatus.NOT_STARTED -> MissionStatusMeta(
        icon = Icons.Default.NightsStay,
        labelRes = R.string.mission_status_not_started,
        tint = { MaterialTheme.colorScheme.onSurfaceVariant },
    )
    MissionStatus.COMPLETED -> MissionStatusMeta(
        icon = Icons.Default.CheckCircle,
        labelRes = R.string.mission_status_completed,
        tint = { MaterialTheme.colorScheme.primary },
    )
}
