package com.armutlu.apporganizer.domain.home

import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.usecase.pulse.PulseReasonId
import com.armutlu.apporganizer.domain.usecase.pulse.PulseScoreReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PulseReasonPresenter] — her [PulseReasonId] doğru etiket/eylem/pozitiflik üretir
 * (Döngü D04 — ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1500-1549).
 */
class PulseReasonPresenterTest {

    @Test
    fun organizationHigh_isPositive_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.ORGANIZATION_HIGH, delta = 10))
        assertEquals(R.string.digital_life_card_reason_organization_high, presented.label.resId)
        assertEquals(PulseAction.None, presented.action)
        assertEquals(true, presented.positive)
    }

    @Test
    fun organizationUncategorized_carriesCountArg_routesToClassificationReview() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.ORGANIZATION_UNCATEGORIZED, value = 8, delta = -10),
        )
        assertEquals(R.string.pulse_reason_organization_uncategorized_count, presented.label.resId)
        assertEquals(listOf(8), presented.label.args)
        assertEquals(PulseAction.OpenClassificationReview, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun attentionCalm_isPositive_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.ATTENTION_CALM, delta = 10))
        assertEquals(PulseAction.None, presented.action)
        assertEquals(true, presented.positive)
    }

    @Test
    fun attentionNoisy_routesToNotificationReport() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.ATTENTION_NOISY, value = 5, delta = -18),
        )
        assertEquals(R.string.pulse_reason_attention_noisy_label, presented.label.resId)
        assertEquals(PulseAction.OpenNotificationReport, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun attentionNight_routesToNotificationReport() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.ATTENTION_NIGHT, value = 40, delta = -10),
        )
        assertEquals(PulseAction.OpenNotificationReport, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun attentionNoPermission_isNeutral_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.ATTENTION_NO_PERMISSION))
        assertEquals(PulseAction.None, presented.action)
        assertNull(presented.positive)
    }

    @Test
    fun balanceSteady_isPositive_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.BALANCE_STEADY, delta = 0))
        assertEquals(PulseAction.None, presented.action)
        assertEquals(true, presented.positive)
    }

    @Test
    fun balanceShift_routesToWeeklyReport() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.BALANCE_SHIFT, value = 45, delta = -20),
        )
        assertEquals(R.string.pulse_reason_balance_shift_label, presented.label.resId)
        assertEquals(PulseAction.OpenWeeklyReport, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun balanceNoBaseline_isNeutral_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.BALANCE_NO_BASELINE))
        assertEquals(PulseAction.None, presented.action)
        assertNull(presented.positive)
    }

    @Test
    fun cleanupTidy_isPositive_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.CLEANUP_TIDY, delta = 5))
        assertEquals(PulseAction.None, presented.action)
        assertEquals(true, presented.positive)
    }

    @Test
    fun cleanupUnused_carriesCountArg_routesToAppList() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.CLEANUP_UNUSED, value = 6, delta = -25),
        )
        assertEquals(R.string.pulse_reason_cleanup_unused_count, presented.label.resId)
        assertEquals(listOf(6), presented.label.args)
        assertEquals(PulseAction.OpenAppList, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun consistencySteady_isPositive_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.CONSISTENCY_STEADY, delta = 0))
        assertEquals(PulseAction.None, presented.action)
        assertEquals(true, presented.positive)
    }

    @Test
    fun consistencyVolatile_routesToWeeklyReport() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.CONSISTENCY_VOLATILE, value = 80, delta = -30),
        )
        assertEquals(PulseAction.OpenWeeklyReport, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun consistencyNoData_isNeutral_noAction() {
        val presented = PulseReasonPresenter.present(PulseScoreReason(PulseReasonId.CONSISTENCY_NO_DATA))
        assertEquals(PulseAction.None, presented.action)
        assertNull(presented.positive)
    }

    @Test
    fun taskMissions_positiveDelta_routesToMissions_positiveLabel() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.TASK_MISSIONS, delta = 4),
        )
        assertEquals(R.string.pulse_reason_task_missions_positive, presented.label.resId)
        assertEquals(listOf(4), presented.label.args)
        assertEquals(PulseAction.OpenMissions, presented.action)
        assertEquals(true, presented.positive)
    }

    @Test
    fun taskMissions_negativeDelta_routesToMissions_negativeLabel() {
        val presented = PulseReasonPresenter.present(
            PulseScoreReason(PulseReasonId.TASK_MISSIONS, delta = -3),
        )
        assertEquals(R.string.pulse_reason_task_missions_negative, presented.label.resId)
        assertEquals(listOf(-3), presented.label.args)
        assertEquals(PulseAction.OpenMissions, presented.action)
        assertEquals(false, presented.positive)
    }

    @Test
    fun allReasonIds_produceNonNullPresentation() {
        PulseReasonId.entries.forEach { id ->
            val presented = PulseReasonPresenter.present(PulseScoreReason(id))
            assertTrue("label resId must be a valid resource for $id", presented.label.resId != 0)
        }
    }
}
