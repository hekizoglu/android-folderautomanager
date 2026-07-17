package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.usecase.missions.MissionTextSpec
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason

/**
 * [PulseScoreReason] -> [PresentedPulseReason] eşlemesi (Döngü D04 —
 * ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1500-1549). Eskiden
 * `DigitalLifeCard.kt` içinde inline `PulseReasonId.labelRes()` when bloğu vardı (Döngü D02);
 * artık etiket + eylem + pozitiflik TEK bu sınıfta üretilir, UI yalnızca tüketir.
 *
 * Saf Kotlin, Android bağımlılığı yok — [MissionTextSpec] resource id taşır, gerçek string
 * çözümü UI katmanında `stringResource`/`getString` ile yapılır.
 */
object PulseReasonPresenter {

    fun present(reason: PulseScoreReason): PresentedPulseReason = when (reason.id) {
        PulseReasonId.ORGANIZATION_HIGH -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_organization_high),
            action = PulseAction.None,
            positive = true,
        )
        PulseReasonId.ORGANIZATION_UNCATEGORIZED -> PresentedPulseReason(
            label = MissionTextSpec(
                R.string.pulse_reason_organization_uncategorized_count,
                listOf(reason.value),
            ),
            action = PulseAction.OpenClassificationReview,
            positive = false,
        )
        PulseReasonId.ATTENTION_CALM -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_attention_calm),
            action = PulseAction.None,
            positive = true,
        )
        PulseReasonId.ATTENTION_NOISY -> PresentedPulseReason(
            label = MissionTextSpec(R.string.pulse_reason_attention_noisy_label),
            action = PulseAction.OpenNotificationReport,
            positive = false,
        )
        PulseReasonId.ATTENTION_NIGHT -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_attention_night),
            action = PulseAction.OpenNotificationReport,
            positive = false,
        )
        PulseReasonId.ATTENTION_NO_PERMISSION -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_attention_no_permission),
            action = PulseAction.None,
            positive = null,
        )
        PulseReasonId.BALANCE_STEADY -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_balance_steady),
            action = PulseAction.None,
            positive = true,
        )
        PulseReasonId.BALANCE_SHIFT -> PresentedPulseReason(
            label = MissionTextSpec(R.string.pulse_reason_balance_shift_label),
            action = PulseAction.OpenWeeklyReport,
            positive = false,
        )
        PulseReasonId.BALANCE_NO_BASELINE -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_balance_no_baseline),
            action = PulseAction.None,
            positive = null,
        )
        PulseReasonId.CLEANUP_TIDY -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_cleanup_tidy),
            action = PulseAction.None,
            positive = true,
        )
        PulseReasonId.CLEANUP_UNUSED -> PresentedPulseReason(
            label = MissionTextSpec(
                R.string.pulse_reason_cleanup_unused_count,
                listOf(reason.value),
            ),
            action = PulseAction.OpenAppList,
            positive = false,
        )
        PulseReasonId.CONSISTENCY_STEADY -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_consistency_steady),
            action = PulseAction.None,
            positive = true,
        )
        PulseReasonId.CONSISTENCY_VOLATILE -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_consistency_volatile),
            action = PulseAction.OpenWeeklyReport,
            positive = false,
        )
        PulseReasonId.CONSISTENCY_NO_DATA -> PresentedPulseReason(
            label = MissionTextSpec(R.string.digital_life_card_reason_consistency_no_data),
            action = PulseAction.None,
            positive = null,
        )
        PulseReasonId.TASK_MISSIONS -> PresentedPulseReason(
            label = if (reason.delta >= 0) {
                MissionTextSpec(R.string.pulse_reason_task_missions_positive, listOf(reason.delta))
            } else {
                MissionTextSpec(R.string.pulse_reason_task_missions_negative, listOf(reason.delta))
            },
            action = PulseAction.OpenMissions,
            positive = reason.delta > 0,
        )
    }
}
